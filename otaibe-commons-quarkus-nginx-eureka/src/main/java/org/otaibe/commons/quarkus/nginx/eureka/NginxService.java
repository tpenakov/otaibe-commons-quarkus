package org.otaibe.commons.quarkus.nginx.eureka;

import io.vertx.mutiny.core.Vertx;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.otaibe.commons.quarkus.eureka.client.service.EurekaClient;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter(AccessLevel.PROTECTED)
@Setter(AccessLevel.PROTECTED)
@Slf4j
public abstract class NginxService {

    public static final String SERVER_PORT_SSL_PLACEHOLDER = "__SERVER_PORT__";
    public static final String SERVER_PORT_PLACEHOLDER = "__SERVER_HTTP_PORT__";
    public static final String SERVER_BAD_GATEWAY_PORT_PLACEHOLDER = "__SERVER_BAD_GATEWAY_PORT__";
    public static final String SSL_ON_PLACEHOLDER = "__SSL_ON__";
    public static final String SSL_CONFIGURATION_PLACEHOLDER = "__SSL_CONFIGURATION__";
    public static final String SSL_CERTIFICATE_NAME_PLACEHOLDER = "__SSL_CERTIFICATE_NAME__";

    private String nginxConfigTemplate;
    private String badGatewayServer;

    @ConfigProperty(name = "cloud-gateway.http.port")
    Integer cloudGatewayPort;
    @ConfigProperty(name = "cloud-gateway.ssl.port")
    Integer cloudGatewaySslPort;
    @ConfigProperty(name = "cloud-gateway.bad-gateway.port")
    Integer cloudGatewayBadGatewayPort;
    @ConfigProperty(name = "nginx.ssl")
    Boolean isSsl;
    @ConfigProperty(name = "nginx.ssl.cert-name")
    Optional<String> sslCertificateName;
    @ConfigProperty(name = "nginx.config.file")
    String nginxConfigTemplatePath;
    @ConfigProperty(name = "nginx.ssl.file")
    Optional<String> nginxSslTemplatePath;

    @Inject
    Vertx vertx;
    @Inject
    EurekaClient eurekaClient;

    @PostConstruct
    public void init() {
        log.info("init start");
        badGatewayServer = MessageFormat.format("server localhost:{0,number,#};\n", getCloudGatewayBadGatewayPort());
        initNginxConfigTemplate();
        log.info("init end");
    }

    protected String initNginxConfigTemplate() {
        nginxConfigTemplate = getVertx().fileSystem().readFileBlocking(getNginxConfigTemplatePath()).toString();
        nginxConfigTemplate = StringUtils.replace(nginxConfigTemplate,
                SSL_ON_PLACEHOLDER,
                getIsSsl() ? "ssl" : StringUtils.EMPTY);
        String sslConfigTemplate = StringUtils.EMPTY;
        if (getIsSsl()) {
            sslConfigTemplate = initSslTemplate();
        }
        nginxConfigTemplate = StringUtils.replace(nginxConfigTemplate, SSL_CONFIGURATION_PLACEHOLDER, sslConfigTemplate);

        nginxConfigTemplate = StringUtils.replace(nginxConfigTemplate, SERVER_PORT_SSL_PLACEHOLDER, getCloudGatewaySslPort().toString());
        nginxConfigTemplate = StringUtils.replace(nginxConfigTemplate, SERVER_PORT_PLACEHOLDER, getCloudGatewayPort().toString());
        nginxConfigTemplate = StringUtils.replace(nginxConfigTemplate, SERVER_BAD_GATEWAY_PORT_PLACEHOLDER, getCloudGatewayBadGatewayPort().toString());

        return nginxConfigTemplate;
    }

    protected String initSslTemplate() {
        return getNginxSslTemplatePath()
                .map(s ->
                        getVertx().fileSystem()
                                .readFileBlocking(s)
                                .toString()
                )
                .flatMap(s -> getSslCertificateName()
                        .map(s1 -> StringUtils.replace(s, SSL_CERTIFICATE_NAME_PLACEHOLDER, s1)))
                .orElse(StringUtils.EMPTY);
    }

    public abstract String getNginxConfig();

    protected Optional<String> buildUpstreamServers(List<String> servers) {
        if (CollectionUtils.isEmpty(servers)) {
            return Optional.of(badGatewayServer);
        }
        return servers.stream()
                .map(s -> MessageFormat.format("server {0};", s))
                .reduce((s, s1) -> StringUtils.join(s, s1, "\n"));
    }

    protected List<String> getServers(String name) {
        Collection<String> servers = new ConcurrentLinkedQueue<>();
        AtomicBoolean found = new AtomicBoolean(false);

        return Mono.defer(() -> getEurekaClient().getNextServer(name))
                .map(s -> {
                    found.set(found.get() || servers.contains(s));
                    boolean isFound = found.get();
                    if (!isFound) {
                        servers.add(s);
                        return s;
                    }
                    return StringUtils.EMPTY;
                })
                .onErrorResume(throwable -> {
                    found.set(true);
                    return Mono.just(StringUtils.EMPTY);
                })
                .repeat(() -> !found.get())
                .filter(s -> StringUtils.isNotBlank(s))
                .map(s -> {
                    String s1 = StringUtils.replace(s, "http://", StringUtils.EMPTY);
                    String s2 = StringUtils.replace(s1, "https://", StringUtils.EMPTY);
                    while (StringUtils.isNotBlank(s2) && StringUtils.endsWith(s2, "/")) {
                        s2 = StringUtils.substring(s2, 0, s2.length() - 1);
                    }
                    log.debug("service={}, server={}", name, s2);
                    return s2;
                })
                .collectList()
                .block();
    }
}

package org.otaibe.commons.quarkus.eureka.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.impl.VertxImpl;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.WebClient;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.otaibe.commons.quarkus.core.utils.JsonUtils;
import org.otaibe.commons.quarkus.core.utils.MapWrapper;
import org.otaibe.commons.quarkus.eureka.client.domain.InstanceInfo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.Inet4Address;
import java.net.URI;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * based on:
 * https://github.com/Netflix/eureka/wiki/Eureka-REST-operations
 */

@ApplicationScoped
@Getter
@Setter
@Slf4j
public class EurekaClient {

    @ConfigProperty(name = "quarkus.servlet.context-path")
    Optional<String> contextPath;
    @ConfigProperty(name = "quarkus.application.name")
    String appName;
    @ConfigProperty(name = "eureka.instance.hostname")
    String hostNameForEureka;
    @ConfigProperty(name = "eureka.client.serviceUrl.defaultZone")
    String eurekaDefaultZone;
    @ConfigProperty(name = "eureka.server.path")
    String eurekaServerPath;
    @ConfigProperty(name = "quarkus.http.port")
    Integer port;

    @Inject
    Vertx vertx;
    @Inject
    JsonUtils jsonUtils;
    @Inject
    ObjectMapper objectMapper;
    @Inject
    MapWrapper mapWrapper;

    WebClient client;
    UriBuilder apiPath;
    VertxImpl vertxDelegate;

    InstanceInfo instanceInfo;

    Map<String, Tuple2<LocalDateTime, List<String>>> serversMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("init started");
        UriBuilder path = UriBuilder.fromUri(getEurekaDefaultZone()).path(getEurekaServerPath());
        URI build = path.build();
        this.client = WebClient.create(getVertx(),
                new WebClientOptions()
                        .setDefaultHost(build.getHost())
                        .setDefaultPort(build.getPort())
                        .setSsl(StringUtils.startsWithIgnoreCase(build.getScheme(), "https"))
        );

        this.apiPath = path;

        vertxDelegate = (VertxImpl) getVertx().getDelegate();

        initInstanceInfo();
        Duration period = Duration.ofSeconds(30);

        registerApp()
                .subscribeOn(Schedulers.fromExecutorService(getVertxDelegate().getWorkerPool()))
                .flatMapMany(aBoolean -> Flux.interval(period))
                .flatMap(aLong -> {
                    LocalDateTime threshold = LocalDateTime.now().minus(period);
                    getServersMap().entrySet()
                            .stream()
                            .filter(entry -> threshold.isAfter(entry.getValue().getT1()))
                            .map(Map.Entry::getKey)
                            .collect(Collectors.toList())
                            .forEach(s -> getServersMap().remove(s))
                    ;
                    return registerApp();
                })
                .subscribe()
        ;
    }

    public Mono<Map<String, Object>> getAllApps() {
        /**
         * curl -v -H 'Accept: application/json' http://eureka-at-staging.otaibe.org:9333/eureka/apps
         */
        return getApps(getApiPath().build().getPath());
    }

    public Mono<String> getNextServer(String serviceName) {
        /**
         * curl -v -H 'Accept: application/json' http://eureka-at-staging.otaibe.org:9333/eureka/apps/{APP_ID}
         */
        String key = serviceName.toLowerCase();
        if (getServersMap().containsKey(key)) {
            Tuple2<LocalDateTime, List<String>> objects = getServersMap().get(key);
            List<String> t2 = objects.getT2();
            if (!t2.isEmpty()) {

                if (t2.size() == 1) {
                    return Mono.just(t2.get(0));
                }

                List<String> urls = new ArrayList<>(t2);
                String result = urls.remove(0);
                urls.add(result);
                getServersMap().put(key, Tuples.of(objects.getT1(), urls));
                return Mono.just(result);
            }
        }

        return getApps(getPath(serviceName))
                .map(map ->
                        Optional.ofNullable(
                                getMapWrapper().getValue(map, List.class, "application", "instance")))
                .map(list -> list.orElseThrow(() -> new RuntimeException("unable to find instane of type " + key)))
                .map(list -> list
                        .stream()
                        .filter(o -> StringUtils.equals(InstanceInfo.UP, getMapWrapper().getStringValue((Map) o, "status")))
                        .map(o -> getMapWrapper().getStringValue((Map) o, "homePageUrl"))
                        .collect(Collectors.toList())
                )
                .map(o -> (List) o)
                .filter(list -> !list.isEmpty())
                .flatMap(list -> {
                    getServersMap().put(key, Tuples.of(LocalDateTime.now(), list));
                    return getNextServer(serviceName);
                })
                .switchIfEmpty(Mono.error(new RuntimeException("unable to find instane of type " + key)))
                ;

    }

    public Mono<Boolean> registerApp() {


        String rqString = instanceInfo.toXmlString();

        /**
         curl -v -H 'Content-Type: application/json' http://eureka-at-staging.otaibe.org:9333/eureka/apps/{appName} \
         -X POST -d @/home/triphon/tmp.json
         */
        return Mono.from(getClient()
                        .post(getPath(appName))
//                .putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                        .putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                        .sendBuffer(Buffer.buffer(rqString))
                        .convert()
                        .toPublisher()
        )
                .doOnSubscribe(subscription -> log.trace("instance info: {}", rqString))
                .doOnNext(response -> log.debug("status code: {}, body: {}", response.statusCode(), response.bodyAsString()))
                .map(bufferHttpResponse -> bufferHttpResponse.statusCode())
                .map(integer -> Response.Status.Family.SUCCESSFUL.equals(Response.Status.fromStatusCode(integer).getFamily()))
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new RuntimeException("not registered")))
                //.doOnError(throwable -> log.error("error", throwable))
                .retryBackoff(Long.MAX_VALUE, Duration.ofMillis(100), Duration.ofSeconds(15), .5)
                .doOnError(throwable -> log.error("unable to registerApp", throwable))
                ;
    }

    private Mono<Map<String, Object>> getApps(String path) {
        return Mono.from(getClient()
                .get(path)
                .putHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                .send()
                .convert()
                .toPublisher()
        )
                .map(bufferHttpResponse -> bufferHttpResponse.bodyAsString())
                .doOnNext(s -> log.debug("all apps: {}", s))
                .map(s -> (Map<String, Object>) getJsonUtils().readValue(s, Map.class, getObjectMapper()).get())
                //.doOnError(throwable -> log.error("error", throwable))
                .retryBackoff(10, Duration.ofMillis(100), Duration.ofSeconds(1), .5)
                .doOnError(throwable -> log.error(MessageFormat.format("unable to get apps for {0}", path) , throwable))
                ;
    }

    private void initInstanceInfo() {
        String hostName = StringUtils.EMPTY;
        String hostAddress = StringUtils.EMPTY;
        try {
            hostName = Inet4Address.getLocalHost().getHostName();
            hostAddress = Inet4Address.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.error("unknown host", e);
        }

        instanceInfo = InstanceInfo.builder()
                .localHostName(hostName)
                .app(getAppName())
                .port(getPort())
                .eurekaHostName(getHostNameForEureka())
                .contextPath(getContextPath().orElse(StringUtils.EMPTY))
                .ipAddress(hostAddress)
                .build();
    }

    private String getPath(String subPath) {
        return getApiPath().clone().path(subPath).build().getPath();
    }

}

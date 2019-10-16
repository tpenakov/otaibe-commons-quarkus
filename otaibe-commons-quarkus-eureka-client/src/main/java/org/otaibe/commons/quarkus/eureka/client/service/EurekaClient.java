package org.otaibe.commons.quarkus.eureka.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.impl.VertxImpl;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.WebClient;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.otaibe.commons.quarkus.core.utils.JsonUtils;
import org.otaibe.commons.quarkus.eureka.client.domain.InstanceInfo;
import reactor.adapter.rxjava.RxJava2Adapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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
import java.time.Duration;
import java.util.Map;

/**
 * based on:
 * https://github.com/Netflix/eureka/wiki/Eureka-REST-operations
 */

@ApplicationScoped
@Getter
@Setter
@Slf4j
public class EurekaClient {

    @ConfigProperty(name = "quarkus.servlet.context-path", defaultValue = StringUtils.EMPTY)
    String contextPath;
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

    WebClient client;
    UriBuilder apiPath;
    VertxImpl vertxDelegate;

    InstanceInfo instanceInfo;

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

        registerApp()
                .subscribeOn(Schedulers.fromExecutorService(getVertxDelegate().getWorkerPool()))
                .flatMapMany(aBoolean -> Flux.interval(Duration.ofSeconds(30)))
                .flatMap(aLong -> registerApp())
                .subscribe()
        ;
    }

    public Mono<Map<String, Object>> getAllApps() {
        /**
         * curl -v -H 'Accept: application/json' http://eureka-at-staging.otaibe.org:9333/eureka/apps
         */
        return RxJava2Adapter.singleToMono(getClient()
                .get(getApiPath().build().getPath())
                .putHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                .rxSend()
        )
                .map(bufferHttpResponse -> bufferHttpResponse.bodyAsString())
                .doOnNext(s -> log.debug("all apps: {}", s))
                .map(s -> (Map<String, Object>) getJsonUtils().readValue(s, Map.class, getObjectMapper()).get())
                //.doOnError(throwable -> log.error("error", throwable))
                .retryBackoff(10, Duration.ofMillis(100), Duration.ofSeconds(1), .5)
                .doOnError(throwable -> log.error("unable to get all apps", throwable))
                ;
    }

    public Mono<Boolean> registerApp() {


        String rqString = instanceInfo.toXmlString();

        /**
          curl -v -H 'Content-Type: application/json' http://eureka-at-staging.otaibe.org:9333/eureka/apps/{appName} \
           -X POST -d @/home/triphon/tmp.json
         */
        return RxJava2Adapter.singleToMono(getClient()
                .post(getPath(appName))
//                .putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                .rxSendBuffer(Buffer.buffer(rqString))
        )
                .doOnSubscribe(subscription -> log.trace("instance info: {}", rqString))
                .doOnNext(response -> log.debug("status code: {}, body: {}", response.statusCode(), response.bodyAsString()))
                .map(bufferHttpResponse -> bufferHttpResponse.statusCode())
                .map(integer -> Response.Status.Family.SUCCESSFUL.equals(Response.Status.fromStatusCode(integer).getFamily()))
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new RuntimeException("not registered")))
                //.doOnError(throwable -> log.error("error", throwable))
                .retryBackoff(Long.MAX_VALUE, Duration.ofMillis(100), Duration.ofSeconds(15), .5)
                .doOnError(throwable -> log.error("unable to get all apps", throwable))
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
                .contextPath(getContextPath())
                .ipAddress(hostAddress)
                .build();
    }

    private String getPath(String subPath) {
        return getApiPath().clone().path(subPath).build().getPath();
    }

}

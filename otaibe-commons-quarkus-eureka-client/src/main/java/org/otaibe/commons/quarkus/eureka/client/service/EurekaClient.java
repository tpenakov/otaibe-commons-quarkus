package org.otaibe.commons.quarkus.eureka.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.impl.VertxImpl;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
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
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.otaibe.commons.quarkus.core.utils.JsonUtils;
import org.otaibe.commons.quarkus.core.utils.MapWrapper;
import org.otaibe.commons.quarkus.eureka.client.domain.EurekaSettings;
import org.otaibe.commons.quarkus.eureka.client.domain.InstanceInfo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import reactor.util.retry.Retry;

/**
 * based on:
 * https://github.com/Netflix/eureka/wiki/Eureka-REST-operations
 */

@Getter
@Setter
@Slf4j
public class EurekaClient {

    @ConfigProperty(name = "quarkus.servlet.context-path")
    Optional<String> contextPath;
    @ConfigProperty(name = "quarkus.application.name")
    String appName;
    @ConfigProperty(name = "quarkus.http.port")
    Integer port;

    @Inject
    EurekaSettings eurekaSettings;

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

    Duration period = Duration.ofSeconds(30);

    @PostConstruct
    public void init() {
        log.info("init started");
    final UriBuilder path =
        UriBuilder.fromUri(getEurekaSettings().getEurekaDefaultZone())
            .path(getEurekaSettings().getEurekaServerPath());
    final URI build = path.build();
    client =
        WebClient.create(
            getVertx(),
            new WebClientOptions()
                .setDefaultHost(build.getHost())
                .setDefaultPort(build.getPort())
                .setSsl(StringUtils.startsWithIgnoreCase(build.getScheme(), "https")));

    apiPath = path;

        vertxDelegate = (VertxImpl) getVertx().getDelegate();

        initInstanceInfo();

        if (getEurekaSettings().getRegisterEurekaClient()) {
      registerApp()
          .subscribeOn(
              Schedulers.fromExecutorService(getVertxDelegate().getWorkerPool().executor()))
          .flatMapMany(aBoolean -> Flux.interval(period))
          .flatMap(
              aLong -> {
                fixServersMap();
                return registerApp();
              })
          .subscribe();
        }
    }

    protected void fixServersMap() {
    final LocalDateTime threshold = LocalDateTime.now().minus(period);
        getServersMap().entrySet()
                .stream()
                .filter(entry -> threshold.isAfter(entry.getValue().getT1()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList())
                .forEach(s -> getServersMap().remove(s))
        ;
    }

    public Mono<Map<String, Object>> getAllApps() {
        /**
         * curl -v -H 'Accept: application/json' http://eureka-at-staging.otaibe.org:9333/eureka/apps
         */
        return getApps(getApiPath().build().getPath());
    }

  public Mono<String> getNextServer(final String serviceName) {
    /**
     * curl -v -H 'Accept: application/json'
     * http://eureka-at-staging.otaibe.org:9333/eureka/apps/{APP_ID}
     */
    final String key = serviceName.toLowerCase();

        boolean isRegistered = getEurekaSettings().getRegisterEurekaClient();

        if (!isRegistered) {
      final String serviceShouldBeRegisteredKey =
          MessageFormat.format("eureka.app.{0}.register", key);
            isRegistered = ConfigProvider.getConfig()
                    .getOptionalValue(serviceShouldBeRegisteredKey, Boolean.class)
                    .orElse(true);
        }

        if (!getEurekaSettings().getRegisterEurekaClient()) {
            fixServersMap();
        }

        if (!isRegistered) {
      final String serviceUrlKey = MessageFormat.format("eureka.app.{0}.url", key);
            return ConfigProvider.getConfig()
                    .getOptionalValue(serviceUrlKey, String.class)
                    .map(Mono::just)
                    .orElseGet (() -> Mono.error(
                            new RuntimeException("Missing configuration property " + serviceUrlKey)
                    ));
        }

        if (getServersMap().containsKey(key)) {
      final Tuple2<LocalDateTime, List<String>> objects = getServersMap().get(key);
      final List<String> t2 = objects.getT2();
            if (!t2.isEmpty()) {

                if (t2.size() == 1) {
                    return Mono.just(t2.get(0));
                }

        final List<String> urls = new ArrayList<>(t2);
        final String result = urls.remove(0);
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

    final String rqString = instanceInfo.toXmlString();

    /**
     * curl -v -H 'Content-Type: application/json'
     * http://eureka-at-staging.otaibe.org:9333/eureka/apps/{appName} \ -X POST
     * -d @/home/triphon/tmp.json
     */
    final String path = getPath(appName);
    return Mono.defer(
            () ->
                Mono.fromCompletionStage(
                    getClient()
                        .post(path)
                        .putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                        .sendBuffer(Buffer.buffer(rqString))
                        .ifNoItem()
                        .after(Duration.ofSeconds(15))
                        .fail()
                        .convert()
                        .toCompletionStage()))
        .doOnSubscribe(subscription -> log.trace("url: {}, instance info: \n{}", path, rqString))
        .doOnNext(
            response ->
                log.debug(
                    "status code: {}, body: {}", response.statusCode(), response.bodyAsString()))
        .map(bufferHttpResponse -> bufferHttpResponse.statusCode())
        .map(
            integer ->
                Response.Status.Family.SUCCESSFUL.equals(
                    Response.Status.fromStatusCode(integer).getFamily()))
        .filter(Boolean::booleanValue)
        .switchIfEmpty(Mono.error(new RuntimeException("not registered")))
        // .doOnError(throwable -> log.error("error", throwable))
        .doOnError(throwable -> log.trace("unable to registerApp", throwable))
        .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(15)))
        .doOnError(throwable -> log.error("unable to registerApp", throwable));
    }

  private Mono<Map<String, Object>> getApps(final String path) {
    return Mono.fromCompletionStage(
            getClient()
                .get(path)
                .putHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                .send()
                .convert()
                .toCompletionStage())
        .map(bufferHttpResponse -> bufferHttpResponse.bodyAsString())
        .doOnNext(s -> log.debug("all apps: {}", s))
        .map(
            s ->
                (Map<String, Object>)
                    getJsonUtils().readValue(s, Map.class, getObjectMapper()).get())
        // .doOnError(throwable -> log.error("error", throwable))
        .retryWhen(Retry.backoff(10, Duration.ofMillis(100)))
        .doOnError(
            throwable ->
                log.error(MessageFormat.format("unable to get apps for {0}", path), throwable));
    }

    private void initInstanceInfo() {
        String hostName = StringUtils.EMPTY;
        String hostAddress = StringUtils.EMPTY;
        try {
            hostName = Inet4Address.getLocalHost().getHostName();
            hostAddress = Inet4Address.getLocalHost().getHostAddress();
    } catch (final UnknownHostException e) {
            log.error("unknown host", e);
        }

        instanceInfo = InstanceInfo.builder()
                .localHostName(hostName)
                .app(getAppName())
                .port(getPort())
                .eurekaHostName(getEurekaSettings().getHostNameForEureka())
                .contextPath(getContextPath().orElse(StringUtils.EMPTY))
                .ipAddress(hostAddress)
                .build();
    }

  private String getPath(final String subPath) {
        return getApiPath().clone().path(subPath).build().getPath();
    }

}

package org.otaibe.commons.quarkus.yml.config.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.otaibe.commons.quarkus.core.utils.JsonUtils;
import org.otaibe.commons.quarkus.core.utils.MapWrapper;
import reactor.adapter.rxjava.RxJava2Adapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;


@Getter
@Setter
@Slf4j
public abstract class ConfigService {
    public static final String SABRE_SERVICE = "sabre-service";
    public static final String OTAIBE_APPLICATIONS_SETTINGS = "otaibe-applications-settings";
    public static final String UAPI_SERVICE = "uapi-service";
    public static final String MAIL_SETTINGS[] = {"spring", "mail"};
    public static final String MARKUPS_FLAT[] = {"markups", "flat"};
    public static final String MARKUPS_PERCENT[] = {"markups", "percent"};
    public static final String SERVICE_FEE = "service-fee";
    public static final String EXCHANGE_RATE = "exchange-rate";
    @Inject
    Vertx vertx;
    @Inject
    MapWrapper mapWrapper;
    @Inject
    JsonUtils jsonUtils;
    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "service.config.files")
    List<String> configFiles;

    private ObjectMapper yamlMapper;
    private Map<String, Object> allSettings = new HashMap<>();
    private AtomicBoolean isInitialized = new AtomicBoolean(false);

    @PostConstruct
    public void init() {
        yamlMapper = new ObjectMapper(new YAMLFactory())
                .configure(SerializationFeature.INDENT_OUTPUT, true)
        ;

        Flux.fromIterable(getConfigFiles())
                .flatMap(s -> readYmlMap(s)
                        .doOnNext(map -> log.info("file {} was loaded", s))
                        .map(map -> Tuples.of(s, map))
                )
                .collectMap(objects -> objects.getT1(), objects -> objects.getT2())
                .map(map -> getConfigFiles().stream()
                        .map(s -> map.get(s))
                        .collect(Collectors.toList())
                )
                //.doOnNext(maps -> log.info("yml config list: {}", getJsonUtils().toStringLazy(maps, getObjectMapper())))
                .map(maps -> maps
                        .stream()
                        .reduce(allSettings, (map, map2) -> {
                            try {
                                return getMapWrapper().mergeStringObjectMap(map, map2);
                            } catch (Exception e) {
                                log.error("unable to merge maps", e);
                                log.error("map: {}", getJsonUtils().toStringLazy(map, getObjectMapper()));
                                log.error("map2: {}", getJsonUtils().toStringLazy(map2, getObjectMapper()));
                                throw new RuntimeException(e);
                            }
                        })
                )
                .map(map -> {
                    //log.info("yml config: {}", getJsonUtils().toStringLazy(map, getObjectMapper()));
                    readAllSettings(map);
                    setAllSettings(map);
                    return true;
                })
                .retry(10)
                .map(aBoolean -> {
                    getIsInitialized().set(true);
                    return true;
                })
                .subscribe();

    }

    protected abstract void readAllSettings(Map<String, Object> allSettings1);

    protected  <T> T getSettings(Map<String, Object> allSettings1, Class<T> clazz, String... path) {
        return Optional.ofNullable(getMapWrapper().getObjectValue(allSettings1, path))
                .map(o -> getJsonUtils().toStringLazy(o, getObjectMapper()).toString())
                .flatMap(s -> getJsonUtils().readValue(s, clazz, getObjectMapper()))
                .orElseThrow(() -> new RuntimeException(
                        MessageFormat.format("Unable find default config! Path: {0}, Class: {1}",
                                path, clazz.getName())));
    }

    public Mono<Map<String, Object>> readYmlMap(String path) {
        return RxJava2Adapter.singleToMono(
                getVertx().fileSystem().rxReadFile(path)
        )
                .map(Buffer::getBytes)
                //.doOnNext(bytes -> log.info("fileName: {}", path))
                //.doOnNext(bytes -> log.info("file: {}", new String(bytes)))
                .map(bytes -> {
                    try {
                        return getYamlMapper().readValue(bytes, Map.class);
                    } catch (Exception e) {
                        log.error("unable to read from file:" + path, e);
                        throw new RuntimeException(e);
                    }
                });
    }

}

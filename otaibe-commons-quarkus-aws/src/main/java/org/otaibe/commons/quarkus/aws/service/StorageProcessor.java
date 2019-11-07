package org.otaibe.commons.quarkus.aws.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.otaibe.commons.quarkus.core.utils.JsonUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * https://docs.aws.amazon.com/sdk-for-java/v2/developer-guide/welcome.html
 */
@ApplicationScoped
@Getter
@Setter
@Slf4j
public class StorageProcessor {

    public static final int NUM_RETRIES = 40;
    public static final int FIRST_DELAY_MILLIS = 10;
    public static final int MAX_RETRY_DELAY_SECONDS = 1;

    @ConfigProperty(name = "cloud.aws.region.static")
    Optional<String> awsRegion;
    @ConfigProperty(name = "cloud.aws.s3OtaIbeBucketRoot")
    String awsBucket1;

    @Inject
    JsonUtils jsonUtils;
    @Inject
    ObjectMapper objectMapper;

    String awsBucket;
    S3AsyncClient s3AsyncClient;

    @PostConstruct
    public void init() {

        S3AsyncClientBuilder s3AsyncClientBuilder = S3AsyncClient.builder()
                .httpClientBuilder(NettyNioAsyncHttpClient.builder()
                        .maxConcurrency(100)
                        .maxPendingConnectionAcquires(10_000))
                .credentialsProvider(DefaultCredentialsProvider.create());
        awsRegion.ifPresent(s -> s3AsyncClientBuilder.region(Region.of(s)));
        s3AsyncClient = s3AsyncClientBuilder.build();
        String strip = StringUtils.replace(getAwsBucket1(), "s3://", StringUtils.EMPTY);
        awsBucket = StringUtils.substring(strip, 0, StringUtils.length(strip) - 1);

    }

    public <T> Mono<T> readObject(String key, Class<T> tClass) {
        return read(key)
                .doOnSubscribe(subscription -> log.trace("readObject {} {}", key, tClass.getSimpleName()))
                .map(s -> getJsonUtils().readValue(s, tClass, getObjectMapper()))
                .doOnNext(t -> {
                    if (t.isPresent()) {
                        log.trace("readObject - not exists: class={} key={}", tClass.getSimpleName(), key);
                    }
                })
                .filter(t -> t.isPresent())
                .map(t -> t.get());
    }

    public <T> Mono<Object> writeObject(String key, T object) {
        return write(key, getJsonUtils().toStringLazy(object, getObjectMapper()).toString());
    }

    public Mono<Object> write(String key, String text) {
        AtomicBoolean isWritten = new AtomicBoolean(false);
        return Flux.<PutObjectResponse>create(fluxSink ->
                getS3AsyncClient().putObject(
                        builder -> builder.key(key).bucket(getAwsBucket()),
                        AsyncRequestBody.fromString(text)
                )
                        .whenComplete((putObjectResponse, throwable) -> {
                            if (putObjectResponse != null) {
                                fluxSink.next(putObjectResponse);
                                fluxSink.complete();
                                return;
                            }
                            fluxSink.error(throwable);
                        })
        )
                .next()
                .flatMap(putObjectResponse -> ensureWriteMono(key, isWritten, putObjectResponse.eTag())
                        .then(Mono.just((Object) putObjectResponse))
                )
                .retryBackoff(NUM_RETRIES, Duration.ofMillis(FIRST_DELAY_MILLIS), Duration.ofSeconds(MAX_RETRY_DELAY_SECONDS))
                .doOnError(throwable -> log.error("unable to write text with key: " + key, throwable));
    }

    public Mono<Object> write(String key, byte[] data) {
        AtomicBoolean isWritten = new AtomicBoolean(false);
        return Flux.<PutObjectResponse>create(fluxSink ->
                getS3AsyncClient().putObject(
                        builder -> builder.key(key).bucket(getAwsBucket()),
                        AsyncRequestBody.fromBytes(data)
                )
                        .whenComplete((putObjectResponse, throwable) -> {
                            if (putObjectResponse != null) {
                                fluxSink.next(putObjectResponse);
                                fluxSink.complete();
                                return;
                            }
                            fluxSink.error(throwable);
                        })
        )
                .next()
                .flatMap(putObjectResponse -> ensureWriteMono(key, isWritten, putObjectResponse.eTag())
                        .then(Mono.just((Object) putObjectResponse))
                )
                .retryBackoff(NUM_RETRIES, Duration.ofMillis(FIRST_DELAY_MILLIS), Duration.ofSeconds(MAX_RETRY_DELAY_SECONDS))
                .doOnError(throwable -> log.error("unable to write object with key: " + key, throwable));
    }

    public Mono<String> read(String key) {
        return readBytes(key)
                .map(bytes ->
                        software.amazon.awssdk.utils.StringUtils.fromBytes(
                                bytes, StandardCharsets.UTF_8))
                .filter(s -> StringUtils.isNotBlank(s));
    }

    public Mono<byte[]> readBytes(String key) {
        return Flux.<byte[]>create(fluxSink ->
                getS3AsyncClient().getObject(
                        builder -> builder.key(key).bucket(getAwsBucket()),
                        AsyncResponseTransformer.toBytes()
                )
                        .whenComplete((getObjectResponseResponseBytes, throwable) -> {
                            if (getObjectResponseResponseBytes != null) {
                                if (getObjectResponseResponseBytes.response().contentLength() == 0) {
                                    fluxSink.complete();
                                    return;
                                }
                                /*
                                Instant lastModified = getObjectResponseResponseBytes.response().lastModified();
                                log.info("lastModified: {}", lastModified);
                                Map<String, String> metadata = getObjectResponseResponseBytes.response().metadata();
                                log.info("metadata: {}", metadata);
                                S3ResponseMetadata s3ResponseMetadata = getObjectResponseResponseBytes
                                        .response()
                                        .responseMetadata();
                                log.info("s3ResponseMetadata: {}", s3ResponseMetadata);
                                */
                                fluxSink.next(getObjectResponseResponseBytes.asByteArray());
                                fluxSink.complete();
                                return;
                            }
                            if (CompletionException.class.isAssignableFrom(throwable.getClass())) {
                                Class<? extends Throwable> causeException = ((CompletionException) throwable).getCause().getClass();
                                if (NoSuchKeyException.class.isAssignableFrom(causeException)) {
                                    fluxSink.complete();
                                    return;
                                }
                            }
                            fluxSink.error(throwable);
                        })
        )
                .next()
                .retryBackoff(NUM_RETRIES, Duration.ofMillis(FIRST_DELAY_MILLIS), Duration.ofSeconds(MAX_RETRY_DELAY_SECONDS))
                .doOnError(throwable -> log.error("unable to read object with key: " + key, throwable))
                ;
    }

    public Mono<String> readETag(String key) {
        return Flux.<String>create(fluxSink ->
                getS3AsyncClient().headObject(
                        builder -> builder.key(key).bucket(getAwsBucket())
                )
                        .whenComplete((headObjectResponse, throwable) -> {
                            if (headObjectResponse != null) {
                                String etag = headObjectResponse.eTag();
                                if (StringUtils.isNotBlank(etag)) {
                                    fluxSink.next(etag);
                                }
                                fluxSink.complete();
                                return;
                            }
                            if (CompletionException.class.isAssignableFrom(throwable.getClass())) {
                                Class<? extends Throwable> causeException = ((CompletionException) throwable).getCause().getClass();
                                if (NoSuchKeyException.class.isAssignableFrom(causeException)) {
                                    fluxSink.complete();
                                    return;
                                }
                            }
                            fluxSink.error(throwable);
                        })
        )
                .next()
                .retryBackoff(NUM_RETRIES, Duration.ofMillis(FIRST_DELAY_MILLIS), Duration.ofSeconds(MAX_RETRY_DELAY_SECONDS))
                .doOnError(throwable -> log.error("unable to read object with key: " + key, throwable))
                ;

    }

    Mono<Boolean> ensureWriteMono(String key, AtomicBoolean isWritten, String etag) {
        return readETag(key)
                .defaultIfEmpty(StringUtils.EMPTY)
                .flatMap(s -> {
                    boolean result = StringUtils.equals(etag, s);
                    isWritten.set(result);
                    return result ?
                            Mono.just(result) : Flux.interval(Duration.ofMillis(50)).next().map(aLong -> result);
                })
                .repeat()
                .filter(aBoolean -> aBoolean)
                .next();
    }


}

package org.otaibe.commons.quarkus.aws.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.otaibe.commons.quarkus.core.utils.JsonUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.SdkEventLoopGroup;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

/**
 * https://docs.aws.amazon.com/sdk-for-java/v2/developer-guide/welcome.html
 */
@Getter
@Setter
@Slf4j
public class StorageProcessor {

    public static final int NUM_RETRIES = 40;
    public static final int FIRST_DELAY_MILLIS = 10;
    public static final int MAX_RETRY_DELAY_SECONDS = 1;

    public static final String AWS_ACCESS_KEY_ID = "aws.accessKeyId";
    public static final String AWS_SECRET_ACCESS_KEY = "aws.secretAccessKey";

    @ConfigProperty(name = AWS_ACCESS_KEY_ID)
    Optional<String> awsAccessKeyId;
    @ConfigProperty(name = AWS_SECRET_ACCESS_KEY)
    Optional<String> awsSecretAccessKey;
    @ConfigProperty(name = "cloud.aws.region.static")
    Optional<String> awsRegion;
    @ConfigProperty(name = "cloud.aws.num.threads")
    Optional<Integer> numThreads;
    @ConfigProperty(name = "cloud.aws.s3OtaIbeBucketRoot")
    String awsBucket1;
    @ConfigProperty(name = "cloud.aws.storage.processor.ensure.write", defaultValue = "false")
    Boolean ensureWrite;
    @ConfigProperty(name = "cloud.aws.endpoint")
    Optional<String> s3AwsEndpoint;

    @Inject
    JsonUtils jsonUtils;
    @Inject
    ObjectMapper objectMapper;

    String awsBucket;
    S3AsyncClient s3AsyncClient;

    @PostConstruct
    public void init() throws Exception {
        getAwsAccessKeyId().ifPresent(s -> {
            System.setProperty(AWS_ACCESS_KEY_ID, s);
            System.setProperty(AWS_SECRET_ACCESS_KEY, getAwsSecretAccessKey().get());
        });

        s3AsyncClient = createS3AsyncClientBuilder().build();

        final String strip = StringUtils.replace(getAwsBucket1(), "s3://", StringUtils.EMPTY);
        awsBucket = StringUtils.substring(strip, 0, StringUtils.length(strip) - 1);
        log.info("initialized ensureWrite={}", getEnsureWrite());
    }

    protected S3AsyncClientBuilder createS3AsyncClientBuilder() throws Exception {
        final S3AsyncClientBuilder s3AsyncClientBuilder = S3AsyncClient.builder()
                .httpClientBuilder(NettyNioAsyncHttpClient.builder()
                        .maxConcurrency(100)
                        .maxPendingConnectionAcquires(10_000)
                        .eventLoopGroupBuilder(SdkEventLoopGroup.builder().numberOfThreads(getNumThreads().orElse(10)))
                )
                .credentialsProvider(DefaultCredentialsProvider.create());
        awsRegion.ifPresent(s -> s3AsyncClientBuilder.region(Region.of(s)));
        if (getS3AwsEndpoint().isPresent()) {
            s3AsyncClientBuilder.endpointOverride(new URI(getS3AwsEndpoint().get()));
        }
        return s3AsyncClientBuilder;
    }

    public <T> Mono<T> readObject(final String key, final Class<T> tClass) {
        return readBytes(key)
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

    public <T> Mono<Object> writeObject(final String key, final T object) {
        return write(key, getJsonUtils().toStringLazy(object, getObjectMapper()).toString());
    }

    public Mono<Object> write(final String key, final String text) {
        final AtomicBoolean isWritten = new AtomicBoolean(false);
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
                .flatMap(putObjectResponse -> (getEnsureWrite() ?
                        ensureWriteMono(key, isWritten, putObjectResponse.eTag()) : Mono.just(true))
                        .then(Mono.just((Object) putObjectResponse))
                )
                .doOnError(throwable -> log.trace("unable to write text (before retry) with key: " + key, throwable))
                .retryWhen(Retry.backoff(NUM_RETRIES, Duration.ofMillis(FIRST_DELAY_MILLIS)))
                .doOnError(throwable -> log.error("unable to write text with key: " + key, throwable));
    }

    public Mono<Object> write(final String key, final byte[] data) {
        final AtomicBoolean isWritten = new AtomicBoolean(false);
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
                .flatMap(putObjectResponse -> (getEnsureWrite() ?
                        ensureWriteMono(key, isWritten, putObjectResponse.eTag()) : Mono.just(true))
                        .then(Mono.just((Object) putObjectResponse))
                )
                .doOnError(throwable -> log.trace("unable to write bytes (before retry) with key: " + key, throwable))
                .retryWhen(Retry.backoff(NUM_RETRIES, Duration.ofMillis(FIRST_DELAY_MILLIS)))
                .doOnError(throwable -> log.error("unable to write bytes with key: " + key, throwable));
    }

    public Mono<String> read(final String key) {
        return readBytes(key)
                .map(bytes ->
                        software.amazon.awssdk.utils.StringUtils.fromBytes(
                                bytes, StandardCharsets.UTF_8))
                .filter(s -> StringUtils.isNotBlank(s));
    }

    public Mono<byte[]> readBytes(final String key) {
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
                                final Class<? extends Throwable> causeException = ((CompletionException) throwable).getCause().getClass();
                                if (NoSuchKeyException.class.isAssignableFrom(causeException)) {
                                    fluxSink.complete();
                                    return;
                                }
                            }
                            log.trace("unable to readBytes (before retry)", throwable);
                            fluxSink.error(throwable);
                        })
        )
                .next()
                .retryWhen(Retry.backoff(NUM_RETRIES, Duration.ofMillis(FIRST_DELAY_MILLIS)))
                .doOnError(throwable -> log.error("unable to read object with key: " + key, throwable))
                ;
    }

    public Mono<String> readETag(final String key) {
        return Flux.<String>create(fluxSink ->
                getS3AsyncClient().headObject(
                        builder -> builder.key(key).bucket(getAwsBucket())
                )
                        .whenComplete((headObjectResponse, throwable) -> {
                            if (headObjectResponse != null) {
                                final String etag = headObjectResponse.eTag();
                                if (StringUtils.isNotBlank(etag)) {
                                    fluxSink.next(etag);
                                }
                                fluxSink.complete();
                                return;
                            }
                            if (CompletionException.class.isAssignableFrom(throwable.getClass())) {
                                final Class<? extends Throwable> causeException = ((CompletionException) throwable).getCause().getClass();
                                if (NoSuchKeyException.class.isAssignableFrom(causeException)) {
                                    fluxSink.complete();
                                    return;
                                }
                            }
                            log.trace("unable to readETag (before retry)", throwable);
                            fluxSink.error(throwable);
                        })
        )
                .next()
                .retryWhen(Retry.backoff(NUM_RETRIES, Duration.ofMillis(FIRST_DELAY_MILLIS)))
                .doOnError(throwable -> log.error("unable to read object with key: " + key, throwable))
                ;

    }

    Mono<Boolean> ensureWriteMono(final String key, final AtomicBoolean isWritten, final String etag) {
        return readETag(key)
                .defaultIfEmpty(StringUtils.EMPTY)
                .flatMap(s -> {
                    final boolean result = StringUtils.equals(etag, s);
                    isWritten.set(result);
                    return result ?
                            Mono.just(result) : Flux.interval(Duration.ofMillis(50)).next().map(aLong -> result);
                })
                .repeat()
                .filter(aBoolean -> aBoolean)
                .next();
    }


}

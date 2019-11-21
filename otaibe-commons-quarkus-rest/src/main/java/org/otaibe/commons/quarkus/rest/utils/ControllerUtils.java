package org.otaibe.commons.quarkus.rest.utils;

import io.vertx.ext.web.RoutingContext;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.otaibe.commons.quarkus.core.utils.JsonUtils;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@ApplicationScoped
@Getter
@Setter
@Slf4j
public class ControllerUtils {

    public static final String CLIENT_ERROR_KEY = "client-error-key";

    @Inject
    JsonUtils jsonUtils;

    public <T> Mono<T> processResult(RoutingContext rc,
                                     Mono<T> result,
                                     T defaultIfEmpty,
                                     Function<T, Boolean> isValid) {
        try {
            return result
                    .defaultIfEmpty(defaultIfEmpty)
                    .flatMap(t -> Mono.subscriberContext()
                            .map(context -> context.getOrEmpty(CLIENT_ERROR_KEY))
                            .map(o -> Tuples.of(t, o))
                    )
                    .doOnNext(objects -> Optional.ofNullable(objects.getT1())
                            .filter(t -> isValid.apply(t))
                            .map(t -> {
                                String body = String.class.isAssignableFrom(t.getClass()) ?
                                        (String) t : getJsonUtils().toStringLazy(t).toString();
                                rc.response().end(body);
                                return true;
                            })
                            .orElseGet(() -> {
                                Response.Status status = objects.getT2().map(o -> (Response.Status) o).orElse(Response.Status.BAD_REQUEST);
                                setError(rc, status, status.getReasonPhrase());
                                return false;
                            }))
                    .map(objects -> objects.getT1())
                    .doOnError(throwable -> setError(rc, Response.Status.INTERNAL_SERVER_ERROR, throwable.getMessage()))
                    .doOnTerminate(() -> log.debug("processResult end."));
        } catch (Exception e) {
            log.error("unhandled exception", e);
            setError(rc, Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
            return Mono.just(defaultIfEmpty);
        }
    }

    public <T> Mono<T> processResult(CompletableFuture<Response> webResult,
                                     Mono<T> result,
                                     T defaultIfEmpty,
                                     Function<T, Boolean> isValid) {
        try {
            return result
                    .defaultIfEmpty(defaultIfEmpty)
                    .flatMap(t -> Mono.subscriberContext()
                            .map(context -> context.getOrEmpty(CLIENT_ERROR_KEY))
                            .map(o -> Tuples.of(t, o))
                    )
                    .doOnNext(objects -> Optional.ofNullable(objects.getT1())
                            .filter(t -> isValid.apply(t))
                            .map(t -> webResult.complete(Response.ok(t).build()))
                            .orElseGet(() -> {
                                Response.Status status = objects.getT2().map(o -> (Response.Status) o).orElse(Response.Status.BAD_REQUEST);
                                return webResult.complete(
                                        buildErrorResponse(status,
                                                status.getReasonPhrase()));
                                    }
                            ))
                    .map(objects -> objects.getT1())
                    .doOnError(throwable -> webResult.complete(
                            buildErrorResponse(
                                    Response.Status.INTERNAL_SERVER_ERROR,
                                    throwable.getMessage())))
                    .doOnTerminate(() -> log.debug("processResult end."));
        } catch (Exception e) {
            log.error("unhandled exception", e);
            webResult.complete(
                    buildErrorResponse(
                            Response.Status.INTERNAL_SERVER_ERROR,
                            e.getMessage()));
            return Mono.just(defaultIfEmpty);
        }
    }

    public void setError(RoutingContext rc, Response.Status status, String message) {
        rc.response()
                .setStatusCode(status.getStatusCode())
                .end(status.getReasonPhrase());
    }

    public Response buildErrorResponse(Response.Status status, String message) {
        return Response.serverError()
                .status(status)
                .entity(message)
                .build();
    }

}

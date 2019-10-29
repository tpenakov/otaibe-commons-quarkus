package org.otaibe.commons.quarkus.rest.utils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@ApplicationScoped
@Getter
@Setter
@Slf4j
public class ControllerUtils {
    public <T> Mono<T> processResult(CompletableFuture<Response> webResult,
                                     Mono<T> result,
                                     T defaultIfEmpty,
                                     Function<T, Boolean> isValid) {
        return result
                .defaultIfEmpty(defaultIfEmpty)
                .doOnNext(t1 -> Optional.ofNullable(t1)
                        .filter(t -> isValid.apply(t))
                        .map(t -> webResult.complete(Response.ok(t).build()))
                        .orElseGet(() -> webResult.complete(
                                buildErrorResponse(Response.Status.BAD_REQUEST,
                                        Response.Status.BAD_REQUEST.getReasonPhrase()))
                        ))
                .doOnError(throwable -> webResult.complete(
                        buildErrorResponse(
                                Response.Status.INTERNAL_SERVER_ERROR,
                                throwable.getMessage())))
                .doOnTerminate(() -> log.debug("processResult end."));
    }

    private Response buildErrorResponse(Response.Status status, String message) {
        return Response.serverError()
                .status(status)
                .entity(message)
                .build();
    }

}

package org.otaibe.commons.quarkus.rest.service;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.otaibe.commons.quarkus.core.utils.JsonUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Getter
@Slf4j
public abstract class RestService<RQ, RS> {

    List<RestProcessor> processors;
    JsonUtils jsonUtils;

    protected abstract RS createRs();

    protected RestService(final List<RestProcessor> processors, final JsonUtils jsonUtils) {
        this.processors = processors;
        this.jsonUtils = jsonUtils;
    }

    public Mono<RS> process(@NotNull final RQ rq) {
        return Mono.just(createRs())
                .flatMap(rs -> Flux.fromIterable(getProcessors())
                        .flatMap(restProcessor -> restProcessor.process(rq, rs))
                        .then(Mono.just(rs))
                )
                .doOnSubscribe(subscription -> {
                    //log levels are different because the output in IDEA is not ok if started from run/debug menu
                    //if you want to see the message there just change the lint to log.isInfoEnabled()
                    if (log.isDebugEnabled()) {
                        log.info("entry request: {}", getJsonUtils().toStringLazy(rq));
                    }
                })
                .doOnNext(o -> {
                    //log levels are different because the output in IDEA is not ok if started from run/debug menu
                    //if you want to see the message there just change the lint to log.isInfoEnabled()
                    if (log.isDebugEnabled()) {
                        log.info("entry response: {}", getJsonUtils().toStringLazy(o));
                    }
                })
                ;
    }
}

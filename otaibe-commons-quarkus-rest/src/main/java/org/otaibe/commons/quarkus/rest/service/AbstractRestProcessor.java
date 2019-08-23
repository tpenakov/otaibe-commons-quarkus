package org.otaibe.commons.quarkus.rest.service;

import reactor.core.publisher.Mono;

import java.util.Optional;

public abstract class AbstractRestProcessor<ROOT_RQ, ROOT_RS, RQ, RS> implements RestProcessor<ROOT_RQ, ROOT_RS, RQ, RS> {

    @Override
    public Mono<Void> process(ROOT_RQ rq, ROOT_RS rs) {
        return Optional.ofNullable(getRq(rq))
                .map(rq1 -> process(rq1, rq, rs))
                .orElse(Mono.empty());
    }

    protected abstract Mono<Void> process(RQ rq1, ROOT_RQ rq, ROOT_RS rs);

}

package org.otaibe.commons.quarkus.rest.service;

import reactor.core.publisher.Mono;

public interface RestProcessor<ROOT_RQ, ROOT_RS, RQ, RS> {
    RQ getRq(ROOT_RQ rq);

    RS getRs(ROOT_RS rs);

    Mono<Void> process(ROOT_RQ rq, ROOT_RS rs);
}

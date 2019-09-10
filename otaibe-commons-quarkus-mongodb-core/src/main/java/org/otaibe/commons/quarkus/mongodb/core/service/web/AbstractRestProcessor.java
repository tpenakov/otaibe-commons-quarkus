package org.otaibe.commons.quarkus.mongodb.core.service.web;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.otaibe.commons.quarkus.mongodb.core.domain.IdEntity;
import org.otaibe.commons.quarkus.mongodb.core.domain.web.AbstractMicroRq;
import org.otaibe.commons.quarkus.mongodb.core.domain.web.AbstractMicroRs;
import org.otaibe.commons.quarkus.mongodb.core.service.MongoDbCollectionService;
import org.otaibe.commons.quarkus.web.domain.Page;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

@Getter
@Slf4j
public abstract class AbstractRestProcessor<
        ROOT_RQ, ROOT_RS,
        T extends IdEntity,
        RQ extends AbstractMicroRq<T>, RS extends AbstractMicroRs<T>>
        extends org.otaibe.commons.quarkus.rest.service.AbstractRestProcessor<ROOT_RQ, ROOT_RS, RQ, RS> {

    MongoDbCollectionService<T> service;

    public AbstractRestProcessor() {
    }

    public AbstractRestProcessor(MongoDbCollectionService<T> service) {
        this.service = service;
    }

    protected abstract RS createAndStoreMicroRS(ROOT_RS rs);

    protected abstract T createEmptyEntity();

    @Override
    protected Mono<Void> process(RQ rq1, ROOT_RQ rq, ROOT_RS rs) {

        RS rs2 = createAndStoreMicroRS(rs);

        Page page = Optional.ofNullable(rq1.getPage())
                .orElse(new Page());
        Optional<T> firstEntityOptional = Optional
                .ofNullable(rq1.getData())
                .orElse(new ArrayList<>())
                .stream()
                .findFirst();

        switch (rq1.getAction()) {
            case DELETE:
                return delete(rs2, firstEntityOptional);
            case FIND:
                return find(firstEntityOptional, rs, page);
            case SAVE:
                return save(firstEntityOptional, rs2);
            case BULK_SAVE:
                return bulkSave(rq1, rs2);
            default:
                return Mono.error(new RuntimeException("unknown action"));
        }

    }

    protected Mono<Void> bulkSave(RQ rq1, RS rs2) {
        return getService().bulkSave(rq1.getData())
                .collectList()
                .map(ts -> {
                    rs2.setResult(ts);
                    return ts;
                })
                .then();
    }

    protected Mono<Void> save(Optional<T> firstEntityOptional, RS rs2) {
        if (firstEntityOptional.isPresent()) {
            return getService().save(firstEntityOptional.get())
                    .map(entity1 -> {
                        rs2.setResult(Arrays.asList(entity1));
                        return entity1;
                    })
                    .then();
        }
        rs2.setResult(new ArrayList<>());
        return Mono.just(true).then();
    }

    protected Mono<Void> find(Optional<T> firstEntityOptional, ROOT_RS rs, Page page) {
        T entity = firstEntityOptional
                .orElse(createEmptyEntity());
        return getService().findByAllNotNullFields(entity, page.getNumber() * page.getSize(), page.getSize())
                .collectList()
                .map(entities -> {
                    RS rs1 = createAndStoreMicroRS(rs);
                    rs1.setResult(entities);
                    rs1.setPage(page);
                    return true;
                })
                .then();
    }

    protected Mono<Void> delete(RS rs2, Optional<T> firstEntityOptional) {
        if (firstEntityOptional.isPresent()) {
            return getService().deleteByIdPretty(firstEntityOptional.get().getIdPretty())
                    .filter(aBoolean -> aBoolean)
                    .switchIfEmpty(Mono.error(new RuntimeException("unable to delete entity")))
                    .then();
        }
        rs2.setResult(new ArrayList<>());
        return Mono.just(true).then();
    }
}

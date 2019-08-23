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
        extends org.otaibe.commons.quarkus.rest.service.AbstractRestProcessor <ROOT_RQ, ROOT_RS, RQ, RS> {

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

        return Optional.ofNullable(rq1.getDeleteById())
                .filter(s -> s.length() > 0)
                .map(s -> getService().deleteByIdPretty(s)
                        .map(aBoolean -> {
                            rs2.setDeleteByIdResult(aBoolean);
                            return aBoolean;
                        })
                        .then()
                )
                .orElseGet(() -> {
                    Page page = Optional.ofNullable(rq1.getPage())
                            .orElse(new Page());
                    Optional<T> firstEntityOptional = Optional
                            .ofNullable(rq1.getData())
                            .orElse(new ArrayList<>())
                            .stream()
                            .findFirst()
                            ;

                    switch (rq1.getAction()) {
                        case FIND:
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
                        case SAVE:
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
                        case BULK_SAVE:
                            return getService().bulkSave(rq1.getData())
                                    .collectList()
                                    .map(ts -> {
                                        rs2.setResult(ts);
                                        return ts;
                                    })
                                    .then();
                        default:
                            return Mono.error(new RuntimeException("unknown action"));
                    }
                })

                ;
    }
}

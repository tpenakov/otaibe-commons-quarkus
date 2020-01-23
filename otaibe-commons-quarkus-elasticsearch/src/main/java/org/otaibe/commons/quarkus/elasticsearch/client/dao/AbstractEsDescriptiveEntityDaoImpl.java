package org.otaibe.commons.quarkus.elasticsearch.client.dao;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.otaibe.commons.quarkus.elasticsearch.client.domain.DescriptiveEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Getter
@Setter
@Slf4j
public abstract class AbstractEsDescriptiveEntityDaoImpl<T extends DescriptiveEntity>
        extends AbstractEsBusinessEntityDaoImpl<T> {

    public static final String STANDARD_ANALIZER = "standard";

    public Flux<T> findByDescription(String value) {
        return findByMatch(DescriptiveEntity.DESCRIPTION, value);
    }

    @Override
    protected Mono<Boolean> createIndex(Map<String, Object> propsMapping) {

        if (!propsMapping.containsKey(DescriptiveEntity.DESCRIPTION)) {
            propsMapping.put(DescriptiveEntity.DESCRIPTION, getTextAnalizer(STANDARD_ANALIZER));
        }

        return super.createIndex(propsMapping);
    }
}

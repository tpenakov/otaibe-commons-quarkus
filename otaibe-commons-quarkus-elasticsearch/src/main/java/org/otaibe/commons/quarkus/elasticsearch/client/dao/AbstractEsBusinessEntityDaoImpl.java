package org.otaibe.commons.quarkus.elasticsearch.client.dao;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.joda.time.DateTime;
import org.otaibe.commons.quarkus.elasticsearch.client.domain.BusinessEntity;
import reactor.core.publisher.Mono;

import java.util.Map;

@Getter
@Setter
@Slf4j
public abstract class AbstractEsBusinessEntityDaoImpl<T extends BusinessEntity>
        extends AbstractEsIdEntityDaoImpl<T> {

    @Override
    public Mono<T> save(T data) {
        if (data == null) {
            return Mono.empty();
        }
        DateTime now = DateTime.now();
        if (data.getCreated() == null) {
            data.setCreated(now);
        }
        data.setUpdated(now);

        if (data.getIsDeleted() == null) {
            data.setIsDeleted(false);
        }

        return super.save(data);
    }

    @Override
    public Mono<T> update(T data) {
        data.setCreated(null);
        data.setUpdated(DateTime.now());
        data.setDbVersion(null);
        return super.update(data);
    }

    protected SearchSourceBuilder addNotDeleted(SearchSourceBuilder result) {
        QueryBuilder query = result.query();
        result.query(QueryBuilders.boolQuery()
                .must(query)
                .must(QueryBuilders.termQuery(BusinessEntity.IS_DELETED, false))
        );
        return result;
    }

    @Override
    protected Mono<Boolean> createIndex(Map<String, Object> propsMapping) {

        propsMapping.put(BusinessEntity.CREATED, getDateFieldType());
        propsMapping.put(BusinessEntity.UPDATED, getDateFieldType());
        propsMapping.put(BusinessEntity.IS_DELETED, getBooleanFieldType());

        return super.createIndex(propsMapping);
    }
}

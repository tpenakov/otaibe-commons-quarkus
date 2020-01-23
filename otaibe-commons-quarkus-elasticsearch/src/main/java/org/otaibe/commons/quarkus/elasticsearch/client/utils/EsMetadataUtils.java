package org.otaibe.commons.quarkus.elasticsearch.client.utils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.search.sort.SortOrder;
import org.otaibe.commons.quarkus.elasticsearch.client.domain.EsMetadata;
import reactor.util.context.Context;

import javax.enterprise.context.ApplicationScoped;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
@Getter
@Setter
@Slf4j
public class EsMetadataUtils {

    public static final String METADATA = "metadata";

    public Optional<EsMetadata> extract(Context context) {
        return context.getOrEmpty(METADATA);
    }

    public Context put(Context context, EsMetadata entity) {
        return context.put(METADATA, entity);
    }

    public void setQueryMetadataFrom(EsMetadata entity, Integer value) {
        ensureQueryMetadata(entity).setFrom(value);
    }

    public void setQueryMetadataSize(EsMetadata entity, Integer value) {
        ensureQueryMetadata(entity).setSize(value);
    }

    public void setQueryMetadataAskForScrollId(EsMetadata entity, Boolean value) {
        ensureQueryMetadata(entity).setAskForScrollId(value);
    }

    public void setQueryMetadataIsOpTypeCreate(EsMetadata entity, Boolean value) {
        ensureQueryMetadata(entity).setIsOpTypeCreate(value);
    }

    public void addQueryMetadataSort(EsMetadata entity, String key, SortOrder value) {
        ensureQueryMetadataSort(entity).put(key, value);
        return;
    }

    public Map<String, SortOrder> ensureQueryMetadataSort(EsMetadata entity) {
        EsMetadata.EsQueryMetadata esQueryMetadata = ensureQueryMetadata(entity);
        return Optional.ofNullable(esQueryMetadata.getSort())
                .orElseGet(() -> {
                    esQueryMetadata.setSort(new LinkedHashMap<>());
                    return esQueryMetadata.getSort();
                })
                ;
    }

    public EsMetadata.EsQueryMetadata ensureQueryMetadata(EsMetadata entity) {
        return Optional.ofNullable(entity.getQuery())
                .orElseGet(() -> {
                    entity.setQuery(new EsMetadata.EsQueryMetadata());
                    return entity.getQuery();
                });
    }

    public Map<String, EsMetadata.EsDaoMetadata> ensureDaoMap(EsMetadata entity) {
        return Optional.ofNullable(entity.getDaoMap())
                .orElseGet(() -> {
                    entity.setDaoMap(new ConcurrentHashMap<>());
                    return entity.getDaoMap();
                });
    }

}

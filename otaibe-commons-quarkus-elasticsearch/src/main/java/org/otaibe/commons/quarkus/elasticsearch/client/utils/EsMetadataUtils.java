package org.otaibe.commons.quarkus.elasticsearch.client.utils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.search.sort.SortOrder;
import org.otaibe.commons.quarkus.elasticsearch.client.domain.EsMetadata;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

@Getter
@Setter
@Slf4j
public class EsMetadataUtils {

    public static final String METADATA = "metadata";

    public Optional<EsMetadata> extract(final ContextView context) {
        return context.getOrEmpty(METADATA);
    }

    public Context put(final Context context, final EsMetadata entity) {
        return context.put(METADATA, entity);
    }

    public void setQueryMetadataFrom(final EsMetadata entity, final Integer value) {
        ensureQueryMetadata(entity).setFrom(value);
    }

    public void setQueryMetadataSize(final EsMetadata entity, final Integer value) {
        ensureQueryMetadata(entity).setSize(value);
    }

    public void setQueryMetadataAskForScrollId(final EsMetadata entity, final Boolean value) {
        ensureQueryMetadata(entity).setAskForScrollId(value);
    }

    public void setQueryMetadataIsOpTypeCreate(final EsMetadata entity, final Boolean value) {
        ensureQueryMetadata(entity).setIsOpTypeCreate(value);
    }

    public void addQueryMetadataSort(final EsMetadata entity, final String key, final SortOrder value) {
        ensureQueryMetadataSort(entity).put(key, value);
        return;
    }

    public Map<String, SortOrder> ensureQueryMetadataSort(final EsMetadata entity) {
        final EsMetadata.EsQueryMetadata esQueryMetadata = ensureQueryMetadata(entity);
        return Optional.ofNullable(esQueryMetadata.getSort())
                .orElseGet(() -> {
                    esQueryMetadata.setSort(new LinkedHashMap<>());
                    return esQueryMetadata.getSort();
                })
                ;
    }

    public EsMetadata.EsQueryMetadata ensureQueryMetadata(final EsMetadata entity) {
        return Optional.ofNullable(entity.getQuery())
                .orElseGet(() -> {
                    entity.setQuery(new EsMetadata.EsQueryMetadata());
                    return entity.getQuery();
                });
    }

    public Map<String, EsMetadata.EsDaoMetadata> ensureDaoMap(final EsMetadata entity) {
        return Optional.ofNullable(entity.getDaoMap())
                .orElseGet(() -> {
                    entity.setDaoMap(new ConcurrentHashMap<>());
                    return entity.getDaoMap();
                });
    }

}

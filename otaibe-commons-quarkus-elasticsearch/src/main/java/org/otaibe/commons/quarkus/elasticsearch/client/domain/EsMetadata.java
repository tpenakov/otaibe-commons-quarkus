package org.otaibe.commons.quarkus.elasticsearch.client.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.elasticsearch.search.sort.SortOrder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@NoArgsConstructor
public class EsMetadata {

    private EsQueryMetadata query = new EsQueryMetadata();
    private Map<String, EsDaoMetadata> daoMap = new ConcurrentHashMap<>();

    @Data
    @NoArgsConstructor
    public static class EsQueryMetadata {
        private Integer from;
        private Integer size;
        private Map<String, SortOrder> sort;
        private Boolean askForScrollId;
    }
    @Data
    @NoArgsConstructor
    public static class EsDaoMetadata {
        private String scrollId;
        private Long totalResults;
    }
}

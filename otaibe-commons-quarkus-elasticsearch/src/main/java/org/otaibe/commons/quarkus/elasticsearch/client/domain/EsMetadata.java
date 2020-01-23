package org.otaibe.commons.quarkus.elasticsearch.client.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.elasticsearch.search.sort.SortOrder;

import java.util.Map;

@Data
@NoArgsConstructor
public class EsMetadata {

    private EsQueryMetadata query;
    private Map<String, EsDaoMetadata> daoMap;

    @Data
    @NoArgsConstructor
    public static class EsQueryMetadata {
        private Integer from;
        private Integer size;
        private Map<String, SortOrder> sort;
        private Boolean askForScrollId;
        private Boolean isOpTypeCreate;
    }
    @Data
    @NoArgsConstructor
    public static class EsDaoMetadata {
        private String scrollId;
        private Long totalResults;
    }
}

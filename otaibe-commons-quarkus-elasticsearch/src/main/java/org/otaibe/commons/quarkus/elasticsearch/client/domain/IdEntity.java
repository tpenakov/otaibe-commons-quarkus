package org.otaibe.commons.quarkus.elasticsearch.client.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class IdEntity {
    public static final char DOT = '.';
    public static final String ID = "id";
    public static final String DB_VERSION = "db_version";

    @JsonProperty(ID)
    private String id;

    @JsonProperty(DB_VERSION)
    private Long dbVersion;
}

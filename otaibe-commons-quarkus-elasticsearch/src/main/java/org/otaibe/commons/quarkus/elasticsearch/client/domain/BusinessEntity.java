package org.otaibe.commons.quarkus.elasticsearch.client.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.joda.time.DateTime;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class BusinessEntity extends IdEntity {
    public static final String IS_DELETED = "is_deleted";
    public static final String CREATED = "created";
    public static final String UPDATED = "updated";
    public static final String API_VERSION = "api_version";

    @JsonProperty(CREATED)
    private DateTime created;
    @JsonProperty(UPDATED)
    private DateTime updated;
    @JsonProperty(IS_DELETED)
    private Boolean isDeleted;

    @JsonProperty(API_VERSION)
    private Long apiVersion;

}

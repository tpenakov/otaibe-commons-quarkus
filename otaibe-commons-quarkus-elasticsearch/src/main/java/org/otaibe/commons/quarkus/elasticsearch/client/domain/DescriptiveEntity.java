package org.otaibe.commons.quarkus.elasticsearch.client.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DescriptiveEntity extends BusinessEntity {
    public static final String DESCRIPTION = "description";
    public static final String NAME = "name";
    public static final String KEY = "key";

    @JsonProperty(DESCRIPTION)
    private String description;
}

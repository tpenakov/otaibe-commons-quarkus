package org.otaibe.commons.quarkus.mongodb.core.domain.web;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.otaibe.commons.quarkus.mongodb.core.domain.IdEntity;

@Data
@NoArgsConstructor
public class AbstractMicroRq<T extends IdEntity>
        extends org.otaibe.commons.quarkus.web.domain.AbstractMicroRq<T> {

}

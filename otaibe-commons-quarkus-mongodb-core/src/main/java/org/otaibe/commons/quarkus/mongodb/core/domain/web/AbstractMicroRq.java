package org.otaibe.commons.quarkus.mongodb.core.domain.web;

import lombok.*;
import org.otaibe.commons.quarkus.mongodb.core.domain.IdEntity;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class AbstractMicroRq<T extends IdEntity>
        extends org.otaibe.commons.quarkus.web.domain.AbstractMicroRq<T> {

}

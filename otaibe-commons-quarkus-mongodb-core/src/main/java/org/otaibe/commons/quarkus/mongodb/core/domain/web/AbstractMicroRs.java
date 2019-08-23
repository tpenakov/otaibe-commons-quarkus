package org.otaibe.commons.quarkus.mongodb.core.domain.web;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.otaibe.commons.quarkus.mongodb.core.domain.IdEntity;
import org.otaibe.commons.quarkus.web.domain.ErrorRs;
import org.otaibe.commons.quarkus.web.domain.Page;

import java.util.List;

@Data
@NoArgsConstructor
public class AbstractMicroRs<T extends IdEntity> {
    List<T> result;
    Boolean deleteByIdResult;
    Page page;
    ErrorRs error;
}

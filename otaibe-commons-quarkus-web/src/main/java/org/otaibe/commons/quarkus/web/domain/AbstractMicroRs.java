package org.otaibe.commons.quarkus.web.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class AbstractMicroRs<T> {
    List<T> result;
    Boolean deleteByIdResult;
    Page page;
    ErrorRs error;
}

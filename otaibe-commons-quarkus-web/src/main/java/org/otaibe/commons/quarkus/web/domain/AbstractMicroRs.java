package org.otaibe.commons.quarkus.web.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class AbstractMicroRs<T> {
    List<T> result;
    Page page;
    ErrorRs error;

    public void add(T entity) {
        if (getResult() == null) {
            setResult(new ArrayList<>());
        }

        getResult().add(entity);
    }
}

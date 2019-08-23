package org.otaibe.commons.quarkus.web.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class AbstractMicroRq<T> {

    public enum ACTION {
        FIND,
        SAVE,
        BULK_SAVE,
        DELETE,
        LOGIN,
    }

    List<T> data;
    ACTION action;
    Page page;

    public void add(T entity) {
        if (getData() == null) {
            setData(new ArrayList<>());
        }

        getData().add(entity);
    }
}

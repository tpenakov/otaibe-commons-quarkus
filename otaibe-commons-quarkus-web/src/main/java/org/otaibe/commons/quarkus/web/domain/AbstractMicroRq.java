package org.otaibe.commons.quarkus.web.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class AbstractMicroRq<T> {

    public enum ACTION {
        FIND,
        SAVE,
        BULK_SAVE,
        CREATE,
        DELETE,
        LOGIN,
    }

    public enum SORT {
        ASC,
        DESC,
    }

    List<T> data;
    ACTION action;
    Map<String, SORT> sort;
    Page page;

    public void add(T entity) {
        if (getData() == null) {
            setData(new ArrayList<>());
        }

        getData().add(entity);
    }
}

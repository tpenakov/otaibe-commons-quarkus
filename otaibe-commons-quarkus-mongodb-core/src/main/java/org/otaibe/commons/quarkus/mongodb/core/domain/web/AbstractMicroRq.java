package org.otaibe.commons.quarkus.mongodb.core.domain.web;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.otaibe.commons.quarkus.mongodb.core.domain.IdEntity;
import org.otaibe.commons.quarkus.web.domain.Page;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class AbstractMicroRq<T extends IdEntity> {

    public enum ACTION {FIND, SAVE, BULK_SAVE}

    List<T> data;
    ACTION action;
    String deleteById;
    Page page;

    public void add(T entity) {
        if (getData() == null) {
            setData(new ArrayList<>());
        }

        getData().add(entity);
    }
}

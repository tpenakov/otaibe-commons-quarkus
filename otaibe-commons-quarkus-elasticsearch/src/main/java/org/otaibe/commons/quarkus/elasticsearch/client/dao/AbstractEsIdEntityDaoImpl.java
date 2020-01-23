package org.otaibe.commons.quarkus.elasticsearch.client.dao;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.otaibe.commons.quarkus.elasticsearch.client.domain.IdEntity;

@Getter
@Setter
@Slf4j
public abstract class AbstractEsIdEntityDaoImpl<T extends IdEntity>
        extends AbstractElasticsearchReactiveDaoImplementation<T> {
    @Override
    protected String getId(T entity) {
        if (entity == null) {
            return null;
        }
        return entity.getId();
    }

    @Override
    protected void setId(T entity, String id) {
        if (entity == null) {
            return;
        }
        entity.setId(id);
    }
}

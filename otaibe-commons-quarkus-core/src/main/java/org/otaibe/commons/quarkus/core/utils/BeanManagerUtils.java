package org.otaibe.commons.quarkus.core.utils;

import lombok.Getter;
import lombok.Setter;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
@Getter
@Setter
public class BeanManagerUtils {

    public <T> List<T> getReferences(BeanManager beanManager, Class<T> clazz) {
        Set<Bean<?>> beans = beanManager.getBeans(clazz);
        return beans.stream()
                .map(bean1 -> bean1.getTypes()
                        .stream()
                        .filter(type -> !type.equals(clazz) && clazz.isAssignableFrom((Class<?>) type))
                        .findFirst()
                        .map(type -> (Class<?>) type)
                        .orElse(null)
                )
                .filter(aClass -> aClass != null)
                .map(aClass -> createBean(beanManager, aClass))
                .map(o -> (T) o)
                .collect(Collectors.toList());
    }

    public <T> T createBean(BeanManager beanManager, Class<T> clazz) {
        Set<Bean<?>> beans = beanManager.getBeans(clazz);
        return createBean(beanManager, clazz, beans);
    }

    public <T> T createBean(BeanManager beanManager, String name, Class<T> clazz) {
        Set<Bean<?>> beans = beanManager.getBeans(name);
        return createBean(beanManager, clazz, beans);
    }

    public <T> T createBean(BeanManager beanManager, Class<T> clazz, Set<Bean<?>> beans) {
        Bean<?> bean = beans.stream()
                .filter(bean1 -> bean1.getTypes()
                        .stream()
                        .filter(type -> type.equals(clazz))
                        .findFirst()
                        .isPresent()
                )
                .findFirst()
                .get();
        CreationalContext<?> creationalContext = beanManager.createCreationalContext(bean);

        return (T) beanManager.getReference(
                bean,
                clazz,
                creationalContext);
    }
}

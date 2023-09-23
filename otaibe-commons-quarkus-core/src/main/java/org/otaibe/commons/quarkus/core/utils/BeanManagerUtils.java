package org.otaibe.commons.quarkus.core.utils;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BeanManagerUtils {

    public <T> List<T> getReferences(final BeanManager beanManager, final Class<T> clazz) {
        final Set<Bean<?>> beans = beanManager.getBeans(clazz);
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

    public <T> T createBean(final BeanManager beanManager, final Class<T> clazz) {
        final Set<Bean<?>> beans = beanManager.getBeans(clazz);
        return createBean(beanManager, clazz, beans);
    }

    public <T> T createBean(final BeanManager beanManager, final String name, final Class<T> clazz) {
        final Set<Bean<?>> beans = beanManager.getBeans(name);
        return createBean(beanManager, clazz, beans);
    }

    public <T> T createBean(final BeanManager beanManager, final Class<T> clazz, final Set<Bean<?>> beans) {
        final Bean<?> bean = beans.stream()
                .filter(bean1 -> bean1.getTypes()
                        .stream()
                        .filter(type -> type.equals(clazz))
                        .findFirst()
                        .isPresent()
                )
                .findFirst()
                .get();
        final CreationalContext<?> creationalContext = beanManager.createCreationalContext(bean);

        return (T) beanManager.getReference(
                bean,
                clazz,
                creationalContext);
    }
}

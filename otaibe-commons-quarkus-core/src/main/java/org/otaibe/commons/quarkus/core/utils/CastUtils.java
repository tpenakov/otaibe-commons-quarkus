package org.otaibe.commons.quarkus.core.utils;

import javax.enterprise.context.ApplicationScoped;
import java.util.Optional;

/**
 * Created by triphon on 07.07.16.
 */
@ApplicationScoped
public class CastUtils {

    public <T> T as(Class<T> t, Object o) {
        return t.isInstance(o) ? t.cast(o) : null;
    }

    public <T> T as(Optional<T> t) {
        return t.isPresent() ? t.get() : null;
    }

    public <T> Optional<T> asOptional(T o) {
        return o == null ? Optional.empty() : Optional.of(o);
    }

}
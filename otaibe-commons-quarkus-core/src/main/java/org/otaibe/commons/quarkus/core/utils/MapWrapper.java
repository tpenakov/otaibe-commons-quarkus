package org.otaibe.commons.quarkus.core.utils;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * Created by triphon on 14-10-11.
 */
@Getter
@Setter
@Slf4j
public class MapWrapper {

    @Inject
    CastUtils castUtils;

    @PostConstruct
    public void init() {
    }

    public String getStringValue(final Map node, final String... path) {
        return getValue(node, String.class, path);
    }

    public Integer getIntegerValue(final Map node, final String... path) {
        final Integer result = getValue(node, Integer.class, path);
        if (result != null) {
            return result;
        }
        return NumberUtils.createInteger(getStringValue(node, path));
    }

    public BigDecimal getBigDecimalValue(final Map node, final String... path) {
        final BigDecimal result = getValue(node, BigDecimal.class, path);
        if (result != null) {
            return result;
        }
        return NumberUtils.createBigDecimal(getStringValue(node, path));
    }

    public Boolean getBooleanValue(final Map node, final String... path) {
        final Boolean result = getValue(node, Boolean.class, path);
        if (result != null) {
            return result;
        }
        final String stringValue = getStringValue(node, path);
        return Boolean.valueOf(stringValue);
    }

    public Object getObjectValue(final Map node, final String... path) {
        return getValue(node, Object.class, path);
    }

    public <T> T getValue(final Map node, final Class<T> clazz, final String... path) {
        Optional nodeOptional = Optional.ofNullable(node);
        if (ArrayUtils.isNotEmpty(path)) {
            for (final String key : path) {
                nodeOptional = nodeOptional.map(o -> ((Map) o).get(key));
            }
        }
        return (T) nodeOptional
                .map(o -> getCastUtils().as(clazz, o))
                .orElse(null);
    }

    /**
     * @param map1
     * @param map2
     * @return a new merged map where if duplicate key exists the map2 will override the value of map1
     */
    public Map<String, Object> mergeStringObjectMap(final Map<String, Object> map1, final Map<String, Object> map2) {
        return Stream.of(map1, map2)
                .flatMap(map -> map.entrySet().stream())
                .filter(entry -> null != entry.getKey() && null != entry.getValue())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (v1, v2) -> {
                            if (Map.class.isAssignableFrom(v2.getClass())) {
                                //log.info("{} {}", v1, v2);
                                return mergeStringObjectMap((Map) v1, (Map) v2);
                            }
                            return v2;
                        })
                );
    }

}

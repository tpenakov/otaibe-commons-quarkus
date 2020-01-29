package org.otaibe.commons.quarkus.core.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.otaibe.commons.quarkus.core.beans.CustomObjectMapperConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by triphon on 13.08.19.
 */
@ApplicationScoped
@Getter
@Slf4j
public class JsonUtils {
    private final static Logger logger = LoggerFactory.getLogger(JsonUtils.class);

    @Inject
    ObjectMapper objectMapper;
    @Inject
    CustomObjectMapperConfig customObjectMapperConfig;

    @PostConstruct
    void init() {
        // perform configuration
        //getCustomObjectMapperConfig().fillObjectMapper(getObjectMapper());
    }

    public <T> T fromMap(Map input, Class<T> outputClass) {
        return fromMap(input, getObjectMapper(), outputClass);
    }

    public <T> T fromMap(Map input, ObjectMapper objectMapper, Class<T> outputClass) {
        try {
            T value = objectMapper.readValue(objectMapper.writeValueAsBytes(input), outputClass);
            return value;
        } catch (Exception e) {
            logger.error("unable to transform to outputClass", e);
            throw new RuntimeException(e);
        }

    }

    public Map toMap(Object input) {
        return toMap(input, getObjectMapper());
    }

    public Map toMap(Object input, ObjectMapper objectMapper) {
        return toMap(input, objectMapper, new HashMap<>());
    }

    public Map<String, Object> toMap(Object input, Map<String, Function<Object, Object>> valueChangeMap) {
        return toMap(input, getObjectMapper(), valueChangeMap);
    }

    public Map<String, Object> toMap(Object input, ObjectMapper objectMapper, Map<String, Function<Object, Object>> valueChangeMap) {
        try {
            Map<String, Object> map = objectMapper.readValue(objectMapper.writeValueAsBytes(input), Map.class);
            return map.keySet().stream()
                    .filter(s -> s != null)
                    .filter(s -> null != map.get(s))
                    //fix for unicode null char - it is shown as "\u0000" string
                    .filter(s -> false == map.get(s).toString().isEmpty() && 0x0 != map.get(s).toString().charAt(0))
                    .collect(Collectors.toMap(
                            o -> o,
                            o -> {
                                Object val = map.get(o);
                                if (valueChangeMap.containsKey(o)) {
                                    return valueChangeMap.get(o).apply(val);
                                }
                                return val;
                            }
                    ));
        } catch (Exception e) {
            logger.error("unable to transform to Map", e);
            throw new RuntimeException(e);
        }
    }

    public <T> T deepClone(final Object input, Class<T> resultType) {
        return deepClone(input, getObjectMapper(), resultType);
    }

    public <T> T deepClone(final Object input, ObjectMapper objectMapper, Class<T> resultType) {
        if (input == null) {
            return null;
        }
        try {
            return objectMapper.readValue(objectMapper.writeValueAsBytes(input), resultType);
        } catch (Exception e) {
            logger.error("unable to serialize", e);
            throw new RuntimeException(e);
        }
    }

    public Object toStringLazy(Object input) {
        return toStringLazy(input, getObjectMapper());
    }

    public Object toStringLazy(Object input, ObjectMapper objectMapper) {
        return new ToStringLazy(input, objectMapper);
    }

    public <T> Optional<T> readValue(String value, Class<T> clazz) {
        return readValue(value, clazz, getObjectMapper());
    }

    public <T> Optional<T> readValue(byte[] value, Class<T> clazz) {
        return readValue(value, clazz, getObjectMapper());
    }

    public <T> Optional<T> readValue(InputStream value, Class<T> clazz) {
        return readValue(value, clazz, getObjectMapper());
    }

    public <T> Optional<T> readValue(String value, Class<T> clazz, ObjectMapper objectMapper1) {
        return Optional.ofNullable(objectMapper1)
                .map(objectMapper -> {
                    try {
                        return objectMapper.readValue(value, clazz);
                    } catch (Exception e) {
                        logger.error("unable to deserialize", e);
                    }
                    return null;
                });
    }

    public <T> Optional<T> readValue(byte[] value, Class<T> clazz, ObjectMapper objectMapper1) {
        return Optional.ofNullable(objectMapper1)
                .map(objectMapper -> {
                    try {
                        return objectMapper.readValue(value, clazz);
                    } catch (Exception e) {
                        logger.error("unable to deserialize", e);
                    }
                    return null;
                });
    }

    public <T> Optional<T> readValue(InputStream value, Class<T> clazz, ObjectMapper objectMapper1) {
        return Optional.ofNullable(objectMapper1)
                .map(objectMapper -> {
                    try {
                        return objectMapper.readValue(value, clazz);
                    } catch (Exception e) {
                        logger.error("unable to deserialize", e);
                    }
                    return null;
                });
    }

    @AllArgsConstructor
    private static class ToStringLazy {
        private Object input;
        private ObjectMapper objectMapper;

        @Override
        public String toString() {
            if (objectMapper == null || input == null) {
                return StringUtils.EMPTY;
            }
            try {
                String value = objectMapper.writeValueAsString(input);
                return value;
            } catch (Exception e) {
                logger.error("unable to serialize to json", e);
                return StringUtils.EMPTY;
            }
        }
    }

}

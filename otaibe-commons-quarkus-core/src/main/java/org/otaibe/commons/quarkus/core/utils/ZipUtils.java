package org.otaibe.commons.quarkus.core.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.apache.commons.io.IOUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

/**
 * Created by triphon on 15-8-16.
 */
@ApplicationScoped
@Getter
public class ZipUtils {

    @Inject
    JsonUtils jsonUtils;

    public String gunZip(byte[] data) throws Exception {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data)) {
            try (InputStream inputStream = new GZIPInputStream(byteArrayInputStream)){
                return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            }
        }
    }

    public <T> Optional<T> gunZip(byte[] data, Class<T> clazz) throws Exception {
        return gunZip(data, clazz, getJsonUtils().getObjectMapper());
    }

    public <T> Optional<T> gunZip(byte[] data, Class<T> clazz, ObjectMapper objectMapper) throws Exception {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data)) {
            try (InputStream inputStream = new GZIPInputStream(byteArrayInputStream)){
                if (String.class.equals(clazz)) {
                    return Optional.ofNullable(IOUtils.toString(inputStream, StandardCharsets.UTF_8))
                            .map(s -> (T) s);
                }
                return getJsonUtils().readValue(inputStream, clazz, objectMapper);
            }
        }
    }

}

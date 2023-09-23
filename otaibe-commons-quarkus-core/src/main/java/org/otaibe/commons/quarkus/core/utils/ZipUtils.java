package org.otaibe.commons.quarkus.core.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import lombok.Getter;
import org.apache.commons.io.IOUtils;

/**
 * Created by triphon on 15-8-16.
 */
@Getter
public class ZipUtils {

    @Inject
    JsonUtils jsonUtils;

    public String gunZip(final byte[] data) throws Exception {
        try (final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data)) {
            try (final InputStream inputStream = new GZIPInputStream(byteArrayInputStream)){
                return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            }
        }
    }

    public <T> Optional<T> gunZip(final byte[] data, final Class<T> clazz) throws Exception {
        return gunZip(data, clazz, getJsonUtils().getObjectMapper());
    }

    public <T> Optional<T> gunZip(final byte[] data, final Class<T> clazz, final ObjectMapper objectMapper) throws Exception {
        try (final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data)) {
            try (final InputStream inputStream = new GZIPInputStream(byteArrayInputStream)){
                if (String.class.equals(clazz)) {
                    return Optional.ofNullable(IOUtils.toString(inputStream, StandardCharsets.UTF_8))
                            .map(s -> (T) s);
                }
                return getJsonUtils().readValue(inputStream, clazz, objectMapper);
            }
        }
    }

}

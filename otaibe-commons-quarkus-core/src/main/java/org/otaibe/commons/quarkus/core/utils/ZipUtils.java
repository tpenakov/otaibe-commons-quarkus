package org.otaibe.commons.quarkus.core.utils;

import org.apache.commons.io.IOUtils;

import javax.enterprise.context.ApplicationScoped;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

/**
 * Created by triphon on 15-8-16.
 */
@ApplicationScoped
public class ZipUtils {
    public String gunZip(byte[] data) throws Exception {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data)) {
            try (InputStream inputStream = new GZIPInputStream(byteArrayInputStream)){
                return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            }
        }
    }
}

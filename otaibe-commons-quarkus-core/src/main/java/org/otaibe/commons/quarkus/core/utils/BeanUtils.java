package org.otaibe.commons.quarkus.core.utils;

import java.io.*;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by triphon on 11.11.16.
 */
@Slf4j
public class BeanUtils {

    public <T extends Serializable> T deepClone(final T input) {
        if (input == null) {
            return null;
        }

        try {
            try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                try (final ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)) {
                    objectOutputStream.writeObject(input);
                }

                try (final ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray())) {
                    try (final ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
                        final T res = (T) objectInputStream.readObject();
                        return res;
                    }
                }
            }
        } catch (final Exception e) {
            log.error("unable to deep clone object", e);
            throw new RuntimeException(e);
        }
    }

    public <T extends Serializable> byte[] toBytes(final T input) {
        try {
            try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                try (final ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)) {
                    objectOutputStream.writeObject(input);
                    return outputStream.toByteArray();
                }
            }
        } catch (final Exception e) {
            log.error("unable to serialize object", e);
            throw new RuntimeException(e);
        }
    }

    public <T extends Serializable> T fromBytes(final byte[] input) {
        try {
            try (final ByteArrayInputStream inputStream = new ByteArrayInputStream(input)) {
                try (final ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
                    final T res = (T) objectInputStream.readObject();
                    return res;
                }
            }
        } catch (final Exception e) {
            log.error("unable to deserialize object", e);
            throw new RuntimeException(e);
        }
    }
}

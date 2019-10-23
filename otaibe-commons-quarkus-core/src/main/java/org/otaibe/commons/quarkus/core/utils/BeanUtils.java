package org.otaibe.commons.quarkus.core.utils;

import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import java.io.*;

/**
 * Created by triphon on 11.11.16.
 */
@ApplicationScoped
@Slf4j
public class BeanUtils {

    public <T extends Serializable> T deepClone(T input) {
        if (input == null) {
            return null;
        }

        try {
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)) {
                    objectOutputStream.writeObject(input);
                }

                try (ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray())) {
                    try (ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
                        T res = (T) objectInputStream.readObject();
                        return res;
                    }
                }
            }
        } catch (Exception e) {
            log.error("unable to deep clone object", e);
            throw new RuntimeException(e);
        }
    }

    public <T extends Serializable> byte[] toBytes(T input) {
        try {
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)) {
                    objectOutputStream.writeObject(input);
                    return outputStream.toByteArray();
                }
            }
        } catch (Exception e) {
            log.error("unable to serialize object", e);
            throw new RuntimeException(e);
        }
    }

    public <T extends Serializable> T fromBytes(byte[] input) {
        try {
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(input)) {
                try (ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
                    T res = (T) objectInputStream.readObject();
                    return res;
                }
            }
        } catch (Exception e) {
            log.error("unable to deserialize object", e);
            throw new RuntimeException(e);
        }
    }
}

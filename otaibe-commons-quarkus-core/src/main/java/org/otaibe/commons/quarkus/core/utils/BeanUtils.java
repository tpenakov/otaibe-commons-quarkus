package org.otaibe.commons.quarkus.core.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.*;

/**
 * Does not work very well on quarkus - instantinate by yourself in the desured project
 * Created by triphon on 11.11.16.
 */
@Slf4j
public class BeanUtils {

    public <T extends Serializable> T deepClone(T input) {
        if (input == null) {
            return null;
        }

        try {
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()){
                try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)){
                    objectOutputStream.writeObject(input);
                }

                try (ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray())){
                    try (ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)){
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
}

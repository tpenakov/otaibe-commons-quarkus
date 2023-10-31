package org.otaibe.commons.quarkus.mongodb.core.utils;

import static org.bson.codecs.configuration.CodecRegistries.*;

import com.mongodb.reactivestreams.client.MongoClients;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.BsonDocumentWriter;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.otaibe.commons.quarkus.mongodb.core.codec.DateTimeCodec;

@Getter
@Slf4j
public class BsonUtils {
    public static final EncoderContext ENCODER_CONTEXT = EncoderContext.builder().build();
    public static final DecoderContext DECODER_CONTEXT = DecoderContext.builder().build();

    public static CodecRegistry createPojoCodecRegistry(final Class mainClass, final Class... clasesArray) {
        final PojoCodecProvider.Builder builder = PojoCodecProvider.builder();
        final List<Class> classesList = new ArrayList<>();
        classesList.add(mainClass);
        if (ArrayUtils.isNotEmpty(clasesArray)) {
            classesList.addAll(Arrays.asList(clasesArray));
        }
        classesList.stream()
                .map(clazz -> clazz.getPackage().getName())
                .distinct()
                .forEach(s -> builder.register(s));

        final CodecProvider pojoCodecProvider = builder.build();
        return fromRegistries(
                MongoClients.getDefaultCodecRegistry(),
                fromProviders(pojoCodecProvider),
                fromCodecs(new DateTimeCodec())
        );
    }

    public <T> BsonDocument toBsonDocument(final T template, final Class<T> clazz, final CodecRegistry pojoCodecRegistry) {
        final BsonDocument bsonDocument = new BsonDocument();

        pojoCodecRegistry
                .get(clazz)
                .encode(new BsonDocumentWriter(bsonDocument), template, ENCODER_CONTEXT);
        return bsonDocument;
    }

    public <T> T fromMap(final Map<String, Object> map, final Class<T> clazz, final CodecRegistry pojoCodecRegistry) {
        final BsonDocument bsonDocument = new BsonDocument();

        pojoCodecRegistry
                .get(Map.class)
                .encode(new BsonDocumentWriter(bsonDocument), map, ENCODER_CONTEXT);

        final T result = pojoCodecRegistry
                .get(clazz)
                .decode(new BsonDocumentReader(bsonDocument), DECODER_CONTEXT);
        return result;
    }


}

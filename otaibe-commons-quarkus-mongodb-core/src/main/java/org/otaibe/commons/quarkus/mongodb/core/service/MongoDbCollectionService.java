package org.otaibe.commons.quarkus.mongodb.core.service;

import com.mongodb.client.model.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.reactivestreams.client.FindPublisher;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoCollection;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.bson.BsonDocument;
import org.bson.BsonDocumentWriter;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.otaibe.commons.quarkus.mongodb.core.codec.DateTimeCodec;
import org.otaibe.commons.quarkus.mongodb.core.domain.IdEntity;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.bson.codecs.configuration.CodecRegistries.*;

@Getter
@Slf4j
public abstract class MongoDbCollectionService<T extends IdEntity> {

    public static final EncoderContext ENCODER_CONTEXT = EncoderContext.builder().build();

    Class<T> mainClass;
    MongoCollection<T> collection;
    CodecRegistry pojoCodecRegistry;

    public MongoDbCollectionService() {

    }

    public MongoDbCollectionService(MongoDbDatabaseService service,
                                    String collectionName,
                                    Class<T> mainClass,
                                    Class... clasesArray) {
        this.mainClass = mainClass;
        PojoCodecProvider.Builder builder = PojoCodecProvider.builder();
        List<Class> classesList = new ArrayList<>();
        classesList.add(mainClass);
        if (ArrayUtils.isNotEmpty(clasesArray)) {
            classesList.addAll(Arrays.asList(clasesArray));
        }
        classesList.stream()
                .map(clazz -> clazz.getPackage().getName())
                .distinct()
                .forEach(s -> builder.register(s));

        CodecProvider pojoCodecProvider = builder.build();
        pojoCodecRegistry = fromRegistries(
                MongoClients.getDefaultCodecRegistry(),
                fromProviders(pojoCodecProvider),
                fromCodecs(new DateTimeCodec())
        );
        collection = service.getDatabase()
                .getCollection(collectionName, mainClass)
                .withCodecRegistry(pojoCodecRegistry)
        ;
    }

    public BsonDocument toBsonDocument(T template, Class<T> clazz) {
        BsonDocument bsonDocument = new BsonDocument();

        getPojoCodecRegistry()
                .get(clazz)
                .encode(new BsonDocumentWriter(bsonDocument), template, ENCODER_CONTEXT);
        return bsonDocument;
    }

    /**
     * find all Objects that contains the not null fields from the template param
     *
     * @param template - will search for formOptInBRs containing all not null fields
     * @return
     */
    public Flux<T> findByAllNotNullFields(T template, Integer skip, Integer size) {
        return findByAllNotNullFields(template, skip, size, null);
    }

    /**
     * find all Objects that contains the not null fields from the template param
     *
     * @param template - will search for formOptInBRs containing all not null fields
     * @return
     */
    public Flux<T> findByAllNotNullFields(T template, Integer skip, Integer size, Bson sort) {

        Class<T> clazz = (Class<T>) template.getClass();
        BsonDocument bsonDocument = toBsonDocument(template, clazz);

        FindPublisher<T> find = getCollection()
                .find(bsonDocument, clazz)
                .skip(skip)
                .limit(size);
        if (sort != null) {
            find.sort(sort);
        }
        return Mono.defer(() -> Mono.just(true))
                .flatMapMany(processor -> Flux.from(find))
                .retryBackoff(5, Duration.ofMillis(200), Duration.ofSeconds(2))
                .doOnNext(formOptInBR -> log.debug("found: {}", formOptInBR))
                .doOnError(throwable -> log.error("byAllNotNullFields error", throwable));
    }

    public Mono<T> save(@Valid T entity) {
        return Optional.ofNullable(entity.getId())
                .map(objectId -> Mono.from(
                        getCollection().findOneAndReplace(
                                Filters.eq(IdEntity.ID, entity.getId()),
                                entity,
                                (new FindOneAndReplaceOptions())
                                        .returnDocument(ReturnDocument.AFTER)
                                        .upsert(true)
                        )
                ))
                .orElseGet(() -> {
                            entity.setId(ObjectId.get());
                            return Mono.from(getCollection().insertOne(entity))
                                    .map(success -> entity);
                        }
                )
                .doOnNext(entity1 -> log.debug("saved entity: {}", entity1))
                ;
    }

    public Flux<T> bulkSave(List<T> entities) {

        List<WriteModel<T>> writes = entities.stream()
                .map(t -> {
                    if (null == t.getId()) {
                        t.setId(ObjectId.get());
                        return new InsertOneModel<>(t);
                    }
                    return new ReplaceOneModel<>(
                            Filters.eq(IdEntity.ID, t.getId()),
                            t,
                            new ReplaceOptions()
                                    .upsert(true)
                    );
                })
                .collect(Collectors.toList());

        return Mono.from(getCollection().bulkWrite(writes))
                .doOnNext(bulkWriteResult -> log.debug("bulkWriteResult Inserted={} Modified={}",
                        bulkWriteResult.getInsertedCount(),
                        bulkWriteResult.getModifiedCount()))
                .flatMapMany(bulkWriteUpserts -> Flux.fromIterable(entities))
                ;
    }

    public Mono<Long> bulkDelete(List<T> entities) {
        List<ObjectId> idList = entities.stream().map(t -> t.getId()).collect(Collectors.toList());
        Publisher<DeleteResult> deleteMany = getCollection().deleteMany(
                Filters.in(IdEntity.ID, idList)
        );
        return Mono.from(deleteMany)
                .map(deleteResult -> deleteResult.getDeletedCount())
                .doOnNext(aLong -> log.debug("num deleted: {}", aLong))
                ;
    }

    public Mono<Boolean> deleteByIdPretty(String id) {
        return Mono.from(getCollection().deleteOne(Filters.eq(IdEntity.ID, new ObjectId(id))))
                .doOnNext(deleteResult -> log.debug("for id={} deleted {} entities", id, deleteResult.getDeletedCount()))
                .map(deleteResult -> true);
    }

}

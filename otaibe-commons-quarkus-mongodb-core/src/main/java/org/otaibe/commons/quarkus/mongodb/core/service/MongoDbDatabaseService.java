package org.otaibe.commons.quarkus.mongodb.core.service;

import com.mongodb.reactivestreams.client.MongoDatabase;
import lombok.Getter;

@Getter
public abstract class MongoDbDatabaseService {

    MongoDatabase database;

    public MongoDbDatabaseService() {

    }

    public MongoDbDatabaseService(MongoDbClientService mongoDbClientService, String databaseName) {
        database = mongoDbClientService.getClient().getDatabase(databaseName);
    }
}

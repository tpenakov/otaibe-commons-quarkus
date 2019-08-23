package org.otaibe.commons.quarkus.mongodb.core.service;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import lombok.Getter;
import org.otaibe.commons.quarkus.mongodb.core.domain.MongoDbSettings;

@Getter
public abstract class MongoDbClientService {

    MongoClient client;

    public MongoDbClientService() {

    }

    public MongoDbClientService(MongoDbSettings mongoDbSettings) {
        client = MongoClients.create(mongoDbSettings.getConnectionString());
    }

}

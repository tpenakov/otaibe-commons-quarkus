package org.otaibe.commons.quarkus.mongodb.core.domain;

public interface MongoDbSettings {
    String getConnectionString();

    String getDatabaseName();
}

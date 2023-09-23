package org.otaibe.commons.quarkus.keycloak.extension.config;


import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

public class OtaibeKeycloakQuarkusProducer {

    private volatile OtaibeKeycloakConfig otaibeKeycloakConfig;

    void initialize(final OtaibeKeycloakConfig config) {
        otaibeKeycloakConfig = config;
    }

    @Singleton
    @Produces
    public OtaibeKeycloakConfig otaibeKeycloakConfig() {
        return otaibeKeycloakConfig;
    }

}

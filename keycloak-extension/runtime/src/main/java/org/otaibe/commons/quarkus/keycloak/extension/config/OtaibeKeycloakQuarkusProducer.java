package org.otaibe.commons.quarkus.keycloak.extension.config;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

@ApplicationScoped
public class OtaibeKeycloakQuarkusProducer {

    private volatile OtaibeKeycloakConfig otaibeKeycloakConfig;

    void initialize(OtaibeKeycloakConfig config) {
        this.otaibeKeycloakConfig = config;
    }

    @Singleton
    @Produces
    public OtaibeKeycloakConfig otaibeKeycloakConfig() {
        return otaibeKeycloakConfig;
    }

}

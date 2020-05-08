package org.otaibe.commons.quarkus.keycloak.extension.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import org.otaibe.commons.quarkus.keycloak.extension.config.OtaibeKeycloakConfig;

public final class OtaibeKeycloackConfigBuildItem extends SimpleBuildItem {
    private OtaibeKeycloakConfig otaibeKeycloakConfig;

    public OtaibeKeycloackConfigBuildItem(OtaibeKeycloakConfig otaibeKeycloakConfig) {
        this.otaibeKeycloakConfig = otaibeKeycloakConfig;
    }

    public OtaibeKeycloakConfig getOtaibeKeycloakConfig() {
        return otaibeKeycloakConfig;
    }
}

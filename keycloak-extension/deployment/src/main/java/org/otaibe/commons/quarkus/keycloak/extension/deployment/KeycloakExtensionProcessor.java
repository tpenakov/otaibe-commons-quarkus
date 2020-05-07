package org.otaibe.commons.quarkus.keycloak.extension.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.otaibe.commons.quarkus.keycloak.extension.config.OtaibeKeycloakConfig;

class KeycloakExtensionProcessor {

    private static final String FEATURE = "keycloak-extension";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem beans() {
        return AdditionalBeanBuildItem.builder().addBeanClasses(OtaibeKeycloakConfig.class).build();
    }
}

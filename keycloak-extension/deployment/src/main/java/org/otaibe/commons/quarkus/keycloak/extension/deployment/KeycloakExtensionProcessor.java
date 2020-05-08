package org.otaibe.commons.quarkus.keycloak.extension.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.otaibe.commons.quarkus.keycloak.extension.config.OtaibeKeycloakConfig;
import org.otaibe.commons.quarkus.keycloak.extension.config.OtaibeKeycloakQuarkusProducer;
import org.otaibe.commons.quarkus.keycloak.extension.config.OtaibeKeycloakRecorder;

import java.util.Optional;

class KeycloakExtensionProcessor {

    private static final String FEATURE = "keycloak-extension";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem beans() {
        return AdditionalBeanBuildItem.builder().addBeanClasses(OtaibeKeycloakQuarkusProducer.class).build();
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void otaibeKeycloackConfigBuildItem(BeanContainerBuildItem beanContainer,
                                        OtaibeKeycloakConfig otaibeKeycloakConfig,
                                        OtaibeKeycloakRecorder recorder) {
        Optional.ofNullable(otaibeKeycloakConfig)
                .filter(config -> null != config.realm)
                .ifPresentOrElse(
                        config -> recorder.initOtaQuarkusProducer(beanContainer.getValue(), config),
                        () -> {
                            throw new RuntimeException("otaibeKeycloakConfig.realm is null");
                        }
                );
    }

}

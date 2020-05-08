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
    OtaibeKeycloackConfigBuildItem otaibeKeycloackConfigBuildItem(OtaibeKeycloakConfig otaibeKeycloakConfig) throws Exception {
        if (null == otaibeKeycloakConfig.realm) {
            throw new RuntimeException("null == otaibeKeycloakConfig.realm");
        }
        return new OtaibeKeycloackConfigBuildItem(otaibeKeycloakConfig);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void otaibeKeycloackConfigBuildItem(BeanContainerBuildItem beanContainer,
                                        OtaibeKeycloackConfigBuildItem buildItem,
                                        OtaibeKeycloakRecorder recorder) throws Exception {
        OtaibeKeycloakConfig otaibeKeycloakConfig1 = buildItem.getOtaibeKeycloakConfig();
        if (null == otaibeKeycloakConfig1.realm) {
            throw new RuntimeException("null == otaibeKeycloakConfig1.realm");
        }
        recorder.initOtaQuarkusProducer(beanContainer.getValue(), otaibeKeycloakConfig1);
    }

}

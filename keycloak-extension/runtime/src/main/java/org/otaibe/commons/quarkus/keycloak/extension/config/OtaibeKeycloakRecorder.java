package org.otaibe.commons.quarkus.keycloak.extension.config;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class OtaibeKeycloakRecorder {

    public void initOtaQuarkusProducer(BeanContainer container, OtaibeKeycloakConfig configuration) throws Exception {
        OtaibeKeycloakQuarkusProducer producer = container.instance(OtaibeKeycloakQuarkusProducer.class);
        producer.initialize(configuration);
    }

}

package org.otaibe.commons.quarkus.re.read.http.request.body.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.vertx.http.deployment.RequireBodyHandlerBuildItem;

class ReReadHttpRequestBodyProcessor {

    private static final String FEATURE = "re-read-http-request-body";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    RequireBodyHandlerBuildItem requireBodyHandlerBuildItem() {
        return new RequireBodyHandlerBuildItem();
    }

}

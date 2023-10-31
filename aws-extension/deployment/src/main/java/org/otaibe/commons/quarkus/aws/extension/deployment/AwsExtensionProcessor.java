package org.otaibe.commons.quarkus.aws.extension.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.otaibe.commons.quarkus.reflection.utils.ReflectUtils;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;

class AwsExtensionProcessor {

    private static final String FEATURE = "aws-extension";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

  @BuildStep
  public void registerReflection(final BuildProducer<ReflectiveClassBuildItem> resource) {
    resource.produce(
        ReflectiveClassBuildItem.builder(ExecutionInterceptor.class)
            .constructors(true)
            .methods(true)
            .fields(true)
            .build());

    ReflectUtils.getAllClassesFromPackage(
            software.amazon.awssdk.services.s3.internal.handlers.AsyncChecksumValidationInterceptor
                .class
                .getPackage()
                .getName(),
            Object.class)
        .forEach(
            aClass ->
                resource.produce(
                    ReflectiveClassBuildItem.builder(ExecutionInterceptor.class)
                        .constructors(true)
                        .methods(true)
                        .fields(true)
                        .build()));
    }

}

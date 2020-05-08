package org.otaibe.commons.quarkus.keycloak.extension.config;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

import java.util.Optional;

@ConfigGroup
public class OtaibeKeycloakRoleConfig {
    /**
     * id of the role
     */
    @ConfigItem
    public Optional<String> id;
    /**
     * name of the role
     */
    @ConfigItem
    public String name;
}

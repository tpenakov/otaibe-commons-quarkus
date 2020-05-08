package org.otaibe.commons.quarkus.keycloak.extension.config;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

import java.util.Map;
import java.util.Optional;

@ConfigRoot(name = "otaibe.keycloak", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class OtaibeKeycloakConfig {

    /**
     * Map of realms used and available for configuration
     */
    @ConfigItem
    public Map<String, KeycloakRealmConfig> realm;

    @ConfigGroup
    public static class KeycloakRealmConfig {

        /**
         * id of the realm
         */
        @ConfigItem
        public Optional<String> id;
        /**
         * name of the realm
         */
        @ConfigItem
        public String name;
        /**
         * map of clients used for the realm
         */
        @ConfigItem
        public Map<String, KeycloakClientConfig> client;
        /**
         * map of roles used for the realm
         */
        @ConfigItem
        public Map<String, OtaibeKeycloakRoleConfig> role;
        /**
         * default roles for the realm
         */
        @ConfigItem
        public String defaultRoles[];

        @ConfigGroup
        public static class KeycloakClientConfig {
            /**
             * id of the client
             */
            @ConfigItem
            public Optional<String> id;
            /**
             * name of the client
             */
            @ConfigItem
            public String name;
            /**
             * secret of the client
             */
            @ConfigItem
            public String secret;
            /**
             * map of roles used for the client
             */
            @ConfigItem
            public Map<String, OtaibeKeycloakRoleConfig> role;
            /**
             * default roles for the client
             */
            @ConfigItem
            public String defaultRoles[];
        }
    }
}

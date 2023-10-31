package org.otaibe.commons.quarkus.keycloack.users.settings;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Getter
@Setter
public class UserServiceSettings {

    public static final String SECURITY_CONTEXT = "securityContext";
    public static final String TOKEN = "token";

    @ConfigProperty(name = "service.user.host")
    String host;

    @ConfigProperty(name = "service.user.port")
    Integer port;

    @ConfigProperty(name = "service.user.ssl")
    Boolean ssl;

    @ConfigProperty(name = "service.user.context-path")
    String contextPath;

}

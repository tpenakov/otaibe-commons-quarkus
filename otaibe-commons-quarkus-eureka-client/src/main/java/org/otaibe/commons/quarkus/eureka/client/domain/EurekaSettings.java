package org.otaibe.commons.quarkus.eureka.client.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Getter
@Setter
@Slf4j
public class EurekaSettings {

    @ConfigProperty(name = "eureka.instance.hostname")
    String hostNameForEureka;
    @ConfigProperty(name = "eureka.client.serviceUrl.defaultZone")
    String eurekaDefaultZone;
    @ConfigProperty(name = "eureka.server.path")
    String eurekaServerPath;
    @ConfigProperty(name = "eureka.client.register", defaultValue = "true")
    Boolean registerEurekaClient;
}

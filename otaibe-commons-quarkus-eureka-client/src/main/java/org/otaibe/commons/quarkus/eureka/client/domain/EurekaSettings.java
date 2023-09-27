package org.otaibe.commons.quarkus.eureka.client.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Getter
@Setter
@Slf4j
public class EurekaSettings {

  private final String hostNameForEureka;

  private final String eurekaDefaultZone;

  private final String eurekaServerPath;

  private final Boolean registerEurekaClient;

  public EurekaSettings(
      @ConfigProperty(name = "eureka.instance.hostname") final String hostNameForEureka,
      @ConfigProperty(name = "eureka.client.serviceUrl.defaultZone") final String eurekaDefaultZone,
      @ConfigProperty(name = "eureka.server.path") final String eurekaServerPath,
      @ConfigProperty(name = "eureka.client.register", defaultValue = "true")
          final Boolean registerEurekaClient) {
    this.hostNameForEureka = hostNameForEureka;
    this.eurekaDefaultZone = eurekaDefaultZone;
    this.eurekaServerPath = eurekaServerPath;
    this.registerEurekaClient = registerEurekaClient;
  }
}

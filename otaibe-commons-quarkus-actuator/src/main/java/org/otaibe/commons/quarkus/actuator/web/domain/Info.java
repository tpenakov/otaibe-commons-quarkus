package org.otaibe.commons.quarkus.actuator.web.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@RegisterForReflection
public class Info {
    GitInfo git;
}

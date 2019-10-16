package org.otaibe.commons.quarkus.actuator.web.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@RegisterForReflection
public class Metrics {
    @JsonProperty("mem")
    Long memory;
    @JsonProperty("mem.free")
    Long memoryFree;
    @JsonProperty("systemload.average")
    Long systemLoadAverage = 0L;
    @JsonProperty("httpsessions.active")
    Long httpSessionsActive = 0L;
}

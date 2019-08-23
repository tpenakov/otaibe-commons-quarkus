package org.otaibe.commons.quarkus.web.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class Page {
    Integer number = 0;
    Integer size = 5;
}

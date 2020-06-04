package org.otaibe.commons.quarkus.rest.utils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import reactor.core.publisher.Mono;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@Getter
@Setter
@Slf4j
public class RestProcessorUtils {

    public static final String ERRORS_KEY = "rest-errors";

    public Mono<List<org.otaibe.commons.quarkus.web.domain.Error>> getErrorsIfPresent() {
        return getErrors()
                .filter(errors -> CollectionUtils.isNotEmpty(errors));
    }

    public Mono<List<org.otaibe.commons.quarkus.web.domain.Error>> getErrors() {
        return Mono.subscriberContext()
                .map(context -> context.<List<org.otaibe.commons.quarkus.web.domain.Error>>getOrEmpty(ERRORS_KEY))
                .filter(Optional::isPresent)
                .map(Optional::get)
                ;
    }

}

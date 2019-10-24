package org.otaibe.commons.quarkus.pg.reactive.client.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import java.util.concurrent.atomic.AtomicBoolean;

@ApplicationScoped
@Getter
@Setter
@Slf4j
public class JsonConfig {

    ObjectMapper dbPropsNamesMapper;

    AtomicBoolean isInitialized = new AtomicBoolean(false);

    @PostConstruct
    public void init() {
        dbPropsNamesMapper = new ObjectMapper();
        dbPropsNamesMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

        getIsInitialized().set(true);
    }

}

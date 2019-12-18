package org.otaibe.commons.quarkus.pg.reactive.client.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.otaibe.commons.quarkus.core.converter.json.LocalDateTimeSerializer;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
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

        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer());
        dbPropsNamesMapper.registerModule(module);

        getIsInitialized().set(true);
    }

}

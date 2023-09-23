package org.otaibe.commons.quarkus.web.client;

import io.vertx.mutiny.core.Vertx;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.otaibe.commons.quarkus.core.utils.ZipUtils;

@Getter
@Setter
@Slf4j
public class WebClient {

    public static io.vertx.mutiny.ext.web.client.WebClient INSTANCE;
    public static ZipUtils ZIP_UTILS;

    @Inject
    Vertx vertx;

    @Inject
    ZipUtils zipUtils;

    io.vertx.mutiny.ext.web.client.WebClient client;

    @PostConstruct
    public void init() {
        log.info("init started");
        client = io.vertx.mutiny.ext.web.client.WebClient.create(getVertx());
        INSTANCE = client;
        ZIP_UTILS = zipUtils;
    }

}

package org.otaibe.commons.quarkus.keycloack.users.service;

import io.smallrye.mutiny.converters.uni.UniReactorConverters;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.otaibe.commons.quarkus.core.utils.JsonUtils;
import org.otaibe.commons.quarkus.keycloack.users.settings.UserServiceSettings;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.text.MessageFormat;
import java.util.function.Predicate;

@ApplicationScoped
@Getter
@Setter
@Slf4j
public class UserServiceClient {
    public static final String ROOT_PATH = "/api";
    public static final String CREATE_USER = "/create-user";
    public static final String UPDATE_USER = "/update-user";
    public static final String DELETE_USER = "/delete-user";
    public static final String USER_LOGIN = "/user-login";
    public static final String CHECK_IS_USER_EXISTS = "/check-is-user-exists";
    public static final String GET_USER = "/get-user";
    public static final String GET_COMPLEX_SEQUENCE1 = "/get-complex-sequence";

    @Inject
    Vertx vertx;

    @Inject
    JsonUtils jsonUtils;

    @Inject
    UserServiceSettings settings;

    WebClient client;
    UriBuilder apiPath;

    @PostConstruct
    void initialize() {
        log.info("init user service client: ssl={} {}:{}",
                getSettings().getSsl(),
                getSettings().getHost(),
                getSettings().getPort());
        this.client = WebClient.create(getVertx(),
                new WebClientOptions()
                        .setDefaultHost(getSettings().getHost())
                        .setDefaultPort(getSettings().getPort())
                        .setSsl(getSettings().getSsl()));

        this.apiPath = UriBuilder.fromPath(getSettings().getContextPath())
                .path(ROOT_PATH)
        ;
    }


    public Mono<UserRepresentation> createUser(UserRepresentation user) {
        return upsertUser(user, getPath(CREATE_USER))
                .doOnError(throwable -> log.error("error create user", throwable))
                .doOnNext(userRepresentation -> log.debug("user is created: {}", getJsonUtils().toStringLazy(userRepresentation)))
        ;
    }

    public Mono<UserRepresentation> updateUser(UserRepresentation user) {
        return upsertUser(user, getPath(UPDATE_USER))
                .doOnError(throwable -> log.error("error update user", throwable))
                .doOnNext(userRepresentation -> log.debug("user is updated: {}", getJsonUtils().toStringLazy(userRepresentation)))
        ;
    }

    public Mono<UserRepresentation> checkIsUserExists(UserRepresentation user) {
        String path = getPath(CHECK_IS_USER_EXISTS);

        return getClient()
                .post(path)
                .sendJson(user)
                .convert()
                .with(UniReactorConverters.toMono())
                .filter(isOkResponse())
                .doOnError(throwable -> log.error("checkIsUserExists create user", throwable))
                .map(bufferHttpResponse -> bufferHttpResponse.bodyAsJson(UserRepresentation.class))
                .doOnNext(userRepresentation -> log.debug("checkIsUserExists result: {}", getJsonUtils().toStringLazy(userRepresentation)));
    }

    public Mono<UserRepresentation> getUserById(String id) {
        String path = getPath(MessageFormat.format("{0}/{1}", GET_USER, id));

        return getClient()
                .get(path)
                .send()
                .convert()
                .with(UniReactorConverters.toMono())
                .filter(isOkResponse())
                .doOnError(throwable -> log.error("getUserById", throwable))
                .map(bufferHttpResponse -> bufferHttpResponse.bodyAsJson(UserRepresentation.class))
                .doOnNext(userRepresentation -> log.debug("getUserById result: {}", getJsonUtils().toStringLazy(userRepresentation)));
    }

    public Mono<Boolean> deleteUser(UserRepresentation user) {
        String path = getPath(DELETE_USER);

        return getClient()
                .post(path)
                .sendJson(user)
                .convert()
                .with(UniReactorConverters.toMono())
                .filter(isOkResponse())
                .doOnError(throwable -> log.error("error delete user", throwable))
                .map(bufferHttpResponse -> bufferHttpResponse.bodyAsString())
                .doOnNext(s -> log.info("user is deleted: {}", s))
                .map(s -> Boolean.valueOf(s))
                ;
    }

    public Mono<String> userLogin(UserRepresentation user) {
        String path = getPath(USER_LOGIN);

        return getClient()
                .post(path)
                .sendJson(user)
                .convert()
                .with(UniReactorConverters.toMono())
                .filter(isOkResponse())
                .doOnError(throwable -> log.error("error user login", throwable))
                .map(bufferHttpResponse -> bufferHttpResponse.bodyAsString())
                .doOnNext(s -> log.debug("user is logged in: {}", s))
                ;
    }


    Mono<UserRepresentation> upsertUser(UserRepresentation user, String path) {
        return getClient()
                .post(path)
                .sendJson(user)
                .convert()
                .with(UniReactorConverters.toMono())
                .doOnError(throwable -> log.error("error upsert user", throwable))
                .filter(isOkResponse())
                .map(bufferHttpResponse -> bufferHttpResponse.bodyAsJson(UserRepresentation.class));
    }

    Predicate<HttpResponse<Buffer>> isOkResponse() {
        return response -> {
            boolean isOk = response.statusCode() == Response.Status.OK.getStatusCode();
            if (!isOk) {
                log.error("unexpected status: {} body: {}", response.statusCode(), response.bodyAsString());
            }
            return isOk;
        };
    }

    String getPath(String subPath) {
        return getApiPath().clone().path(subPath).build().getPath();
    }}

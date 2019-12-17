package org.otaibe.commons.quarkus.elasticsearch.client.dao;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.client.*;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.otaibe.commons.quarkus.core.utils.JsonUtils;
import org.otaibe.commons.quarkus.elasticsearch.client.service.AbstractElasticsearchService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.inject.Inject;
import javax.ws.rs.HttpMethod;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Slf4j
public abstract class AbstractElasticsearchReactiveDaoImplementation<T> {
    public static final char COMMA = ',';
    public static final String SINGLE_QUOTE = "'";
    public static final String DELETED = "DELETED";

    @Inject
    AbstractElasticsearchService abstractElasticsearchService;

    @Inject
    JsonUtils jsonUtils;

    private RestHighLevelClient restClient;

    protected abstract String getId(T entity);

    protected abstract void setId(T entity, String id);

    protected abstract String getTableName();

    protected abstract Class<T> getEntityClass();

    public void init() {
        log.info("init started");
        restClient = getAbstractElasticsearchService().getRestClient();
        ensureIndex()
                .doOnNext(aBoolean -> log.info("index {} exists={}", getTableName(), aBoolean))
                .filter(aBoolean -> !aBoolean)
                .flatMap(aBoolean -> createIndex())
                .doOnNext(aBoolean -> log.info("index {} created={}", getTableName(), aBoolean))
                .doOnTerminate(() -> log.info("init completed"))
                .subscribe()
        ;
    }

    public Mono<Boolean> deleteById(T data) {
        return Mono.just(getId(data))
                .filter(s -> StringUtils.isNotBlank(s))
                .flatMap(s -> Flux
                        .<Boolean>create(fluxSink -> {
                            DeleteRequest request = new DeleteRequest(getTableName(), s);
                            getRestClient().deleteAsync(request, RequestOptions.DEFAULT, new ActionListener<DeleteResponse>() {
                                @Override
                                public void onResponse(DeleteResponse deleteResponse) {
                                    log.debug("delete result: {}", deleteResponse);
                                    fluxSink.next(StringUtils.equalsAnyIgnoreCase(DELETED, deleteResponse.getResult().getLowercase()));
                                    fluxSink.complete();
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    log.error("unable to delete", e);
                                    fluxSink.error(new RuntimeException(e));
                                }
                            });
                        })
                        .next()
                )
                .defaultIfEmpty(false)
                ;
    }

    public Mono<T> findById(T pkData) {
        return Mono.just(getId(pkData))
                .filter(s -> StringUtils.isNotBlank(s))
                .flatMap(s -> Flux
                        .<T>create(fluxSink -> {
                            GetRequest request = new GetRequest(getTableName(), s);
                            getRestClient().getAsync(request, RequestOptions.DEFAULT, new ActionListener<GetResponse>() {
                                @Override
                                public void onResponse(GetResponse response) {
                                    log.debug("get result: {}", response);
                                    if (response.isSourceEmpty()) {
                                        fluxSink.complete();
                                        return;
                                    }
                                    Map<String, Object> map = response.getSourceAsMap();
                                    T result = getJsonUtils().fromMap(map, getEntityClass());
                                    fluxSink.next(result);
                                    fluxSink.complete();
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    log.error("unable to get", e);
                                    fluxSink.error(new RuntimeException(e));
                                }
                            });
                        })
                        .next()
                )
                ;
    }

    public Mono<T> save(T data) {

        return Mono.just(data)
                .flatMap(t -> {
                    if (StringUtils.isBlank(getId(t))) {
                        setId(t, UUID.randomUUID().toString());
                    }

                    IndexRequest indexRequest = new IndexRequest(getTableName());
                    indexRequest.source(getJsonUtils().toStringLazy(t).toString(), XContentType.JSON);
                    indexRequest.id(getId(t));
                    indexRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);

                    return Flux
                            .<T>create(fluxSink -> {
                                getRestClient().indexAsync(indexRequest, RequestOptions.DEFAULT, new ActionListener<IndexResponse>() {
                                    @Override
                                    public void onResponse(IndexResponse indexResponse) {
                                        log.info("save result: {}", indexResponse);
                                        fluxSink.next(t);
                                        fluxSink.complete();
                                    }

                                    @Override
                                    public void onFailure(Exception e) {
                                        log.error("unable to save", e);
                                        fluxSink.error(new RuntimeException(e));
                                    }
                                });
                            })
                            .next();
                })
                ;
    }

    protected Mono<Boolean> createIndex() {
        CreateIndexRequest request = new CreateIndexRequest(getTableName());

        return createIndex(request);
    }

    protected Mono<Boolean> createIndex(CreateIndexRequest request) {
        return Flux.<Boolean>create(fluxSink -> getRestClient().indices().createAsync(request, RequestOptions.DEFAULT, new ActionListener<CreateIndexResponse>() {
            @Override
            public void onResponse(CreateIndexResponse createIndexResponse) {
                log.info("CreateIndexResponse: {}", createIndexResponse);
                fluxSink.next(createIndexResponse.isAcknowledged());
                fluxSink.complete();
            }

            @Override
            public void onFailure(Exception e) {
                log.error("unable to create index", e);
                fluxSink.error(new RuntimeException(e));
            }
        }))
                .next();
    }

    protected Mono<Boolean> ensureIndex() {
        return Mono.just(false)
                .flatMap(atomicBoolean -> Flux
                        .<Boolean>create(fluxSink -> getRestClient().getLowLevelClient()
                                .performRequestAsync(
                                        new Request(HttpMethod.HEAD, getTableName()), new ResponseListener() {
                                            @Override
                                            public void onSuccess(Response response) {
                                                logResponse(response);
                                                boolean result =
                                                        response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
                                                fluxSink.next(result);
                                                fluxSink.complete();
                                            }

                                            @Override
                                            public void onFailure(Exception exception) {
                                                log.error("unable to check for index", exception);
                                                fluxSink.error(new RuntimeException(exception));
                                            }
                                        }))
                        .next()
                );
    }

    private void logResponse(Response response) {
        HttpEntity entity = response.getEntity();
        if (entity == null) {
            log.debug("entity is null");
            return;
        }
        try {
            InputStream content = entity.getContent();
            log.debug("response result: {}", IOUtils.toString(content, StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("unable to log response", e);
        }
    }


}

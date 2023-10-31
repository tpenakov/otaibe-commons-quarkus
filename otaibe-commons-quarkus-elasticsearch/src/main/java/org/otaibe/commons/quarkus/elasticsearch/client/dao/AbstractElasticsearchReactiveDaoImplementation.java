package org.otaibe.commons.quarkus.elasticsearch.client.dao;

import jakarta.inject.Inject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.ws.rs.HttpMethod;
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
import org.elasticsearch.action.search.*;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.*;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.index.get.GetResult;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.otaibe.commons.quarkus.core.utils.JsonUtils;
import org.otaibe.commons.quarkus.elasticsearch.client.domain.EsMetadata;
import org.otaibe.commons.quarkus.elasticsearch.client.service.AbstractElasticsearchService;
import org.otaibe.commons.quarkus.elasticsearch.client.utils.EsMetadataUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

@Getter
@Setter
@Slf4j
public abstract class AbstractElasticsearchReactiveDaoImplementation<T> {
    public static final char COMMA = ',';
    public static final char DOT = '.';
    public static final String SINGLE_QUOTE = "'";
    public static final String DELETED = "DELETED";
    public static final String TYPE = "type";
    public static final String TEXT = "text";
    public static final String ANALYZER = "analyzer";
    public static final String KEYWORD = "keyword";
    public static final String PROPERTIES = "properties";
    public static final String DATE = "date";
    public static final String FORMAT = "format";
    public static final String LONG = "long";
    public static final String BOOLEAN = "boolean";
    public static final TimeValue SCROLL_KEEP_ALIVE = TimeValue.timeValueMinutes(1);

    @Inject
    AbstractElasticsearchService abstractElasticsearchService;

    @Inject
    JsonUtils jsonUtils;

    @Inject
    EsMetadataUtils esMetadataUtils;

    private RestHighLevelClient restClient;

    protected abstract String getId(T entity);

    protected abstract void setId(T entity, String id);

    protected abstract String getTableName();

    protected abstract Class<T> getEntityClass();

    protected abstract Long getVersion(T entity);

    protected abstract void setVersion(T entity, Long version);

    public void init() {
        log.info("init started");
        restClient = getAbstractElasticsearchService().getRestClient();
        ensureIndex()
                .doOnNext(aBoolean -> log.info("index {} exists={}", getTableName(), aBoolean))
                .filter(aBoolean -> !aBoolean)
                .flatMap(aBoolean -> createIndex())
                .doOnNext(aBoolean -> log.info("index {} created={}", getTableName(), aBoolean))
                .doOnTerminate(() -> log.info("init completed"))
                .block()
        ;
    }

  public Mono<Boolean> deleteById(final T data) {
    return Mono.just(getId(data))
        .filter(s -> StringUtils.isNotBlank(s))
        .flatMap(
            s ->
                Flux.<Boolean>create(
                        fluxSink -> {
                          final DeleteRequest request = new DeleteRequest(getTableName(), s);
                          request.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
                          getRestClient()
                              .deleteAsync(
                                  request,
                                  RequestOptions.DEFAULT,
                                  new ActionListener<DeleteResponse>() {
                                    @Override
                                    public void onResponse(final DeleteResponse deleteResponse) {
                                      log.debug("delete result: {}", deleteResponse);
                                      fluxSink.next(
                                          StringUtils.equalsAnyIgnoreCase(
                                              DELETED, deleteResponse.getResult().getLowercase()));
                                      fluxSink.complete();
                                    }

                                    @Override
                                    public void onFailure(final Exception e) {
                                      log.error("unable to delete", e);
                                      fluxSink.error(new RuntimeException(e));
                                    }
                                  });
                        })
                    .next())
        .defaultIfEmpty(false);
    }

  public Mono<T> findById(final T pkData) {
    return Mono.just(getId(pkData))
        .filter(s -> StringUtils.isNotBlank(s))
        .flatMap(
            s ->
                Flux.<T>create(
                        fluxSink -> {
                          final GetRequest request = new GetRequest(getTableName(), s);
                          getRestClient()
                              .getAsync(
                                  request,
                                  RequestOptions.DEFAULT,
                                  new ActionListener<GetResponse>() {
                                    @Override
                                    public void onResponse(final GetResponse response) {
                                      log.debug("get result: {}", response);
                                      if (response.isSourceEmpty()) {
                                        fluxSink.complete();
                                        return;
                                      }
                                      final Map<String, Object> map = response.getSourceAsMap();
                                      final T result =
                                          getJsonUtils().fromMap(map, getEntityClass());
                                      fluxSink.next(result);
                                      fluxSink.complete();
                                    }

                                    @Override
                                    public void onFailure(final Exception e) {
                                      log.error("unable to get", e);
                                      fluxSink.error(new RuntimeException(e));
                                    }
                                  });
                        })
                    .next());
    }

  protected Flux<T> findByMatch(final Map<String, Object> map) {
    final BoolQueryBuilder query = QueryBuilders.boolQuery();
        return findBy(map, query,
                queryBuilder -> query.should(queryBuilder),
                (s, o) -> getSearchSourceBuilderByMatch(s, o));
    }

  protected Flux<T> findByMatch(final String fieldName, final Object value) {
    final SearchRequest searchRequest = getSearchRequestByMatch(fieldName, value);
        return search(searchRequest);
    }

  protected SearchRequest getSearchRequestByMatch(final String fieldName, final Object value) {
    final SearchRequest searchRequest = new SearchRequest(getTableName());
    final SearchSourceBuilder searchSourceBuilder = getSearchSourceBuilderByMatch(fieldName, value);
        searchRequest.source(searchSourceBuilder);
        return searchRequest;
    }

  protected SearchSourceBuilder getSearchSourceBuilderByMatch(
      final String fieldName, final Object value) {
    final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchQuery(fieldName, value));
        return searchSourceBuilder;
    }

  protected Flux<T> findByExactMatch(final Map<String, Object> map) {
    final BoolQueryBuilder query = QueryBuilders.boolQuery();
        return findBy(map, query,
                queryBuilder -> query.must(queryBuilder),
                (s, o) -> getSearchSourceBuilderByExactMatch(s, o));
    }

  protected Flux<T> findBy(
      final Map<String, Object> map,
      final BoolQueryBuilder query,
      final Function<QueryBuilder, BoolQueryBuilder> fn,
      final BiFunction<String, Object, SearchSourceBuilder> fn1) {
    final SearchRequest searchRequest = new SearchRequest(getTableName());
    final SearchSourceBuilder searchSourceBuilder1 = new SearchSourceBuilder();
        searchSourceBuilder1.query(query);
        searchRequest.source(searchSourceBuilder1);
        map.entrySet().stream()
                .filter(entry -> StringUtils.isNotBlank(entry.getKey()) && entry.getValue() != null)
                .map(entry -> fn1.apply(entry.getKey(), entry.getValue()))
                .map(searchSourceBuilder -> searchSourceBuilder.query())
                .forEach(queryBuilder -> fn.apply(queryBuilder));

        return search(searchRequest);
    }

  protected Flux<T> findByExactMatch(final String fieldName, final Object value) {
    final SearchRequest searchRequest = getSearchRequestByExactMatch(fieldName, value);
        return search(searchRequest);
    }

  protected SearchRequest getSearchRequestByExactMatch(final String fieldName, final Object value) {
    final SearchRequest searchRequest = new SearchRequest(getTableName());
    final SearchSourceBuilder searchSourceBuilder =
        getSearchSourceBuilderByExactMatch(fieldName, value);
        searchRequest.source(searchSourceBuilder);
        return searchRequest;
    }

  protected SearchSourceBuilder getSearchSourceBuilderByExactMatch(
      final String fieldName, final Object value) {
    final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.termQuery(fieldName, value));
        return searchSourceBuilder;
    }

  protected Flux<T> search(final SearchRequest searchRequest) {
    return Mono.deferContextual(
            context -> {
              final SearchSourceBuilder builder = searchRequest.source();
              getEsMetadataUtils()
                  .extract(context)
                  .ifPresent(
                      esMetadata -> {
                        Optional.ofNullable(esMetadata.getQuery())
                            .ifPresent(
                                query -> {
                                  Optional.ofNullable(query.getFrom())
                                      .ifPresent(integer -> builder.from(integer));
                                  Optional.ofNullable(query.getSize())
                                      .ifPresent(integer -> builder.size(integer));
                                  Optional.ofNullable(query.getSort())
                                      .ifPresent(
                                          map ->
                                              map.entrySet()
                                                  .forEach(
                                                      entry ->
                                                          builder.sort(
                                                              entry.getKey(), entry.getValue())));
                                  Optional.ofNullable(query.getAskForScrollId())
                                      .filter(Boolean::booleanValue)
                                      .ifPresent(
                                          aBoolean -> searchRequest.scroll(SCROLL_KEEP_ALIVE));
                                });
                      });
              return Mono.just(context);
            })
        .flatMapMany(
            context ->
                Flux.create(
                    fluxSink ->
                        getRestClient()
                            .searchAsync(
                                searchRequest,
                                RequestOptions.DEFAULT,
                                searchResponseAction(context, fluxSink))));
    }

  public Flux<T> search(final SearchScrollRequest searchRequest) {
        searchRequest.scroll(SCROLL_KEEP_ALIVE);
    return Flux.deferContextual(
            context ->
                Flux.<T>create(
                    fluxSink ->
                        getRestClient()
                            .scrollAsync(
                                searchRequest,
                                RequestOptions.DEFAULT,
                                searchResponseAction(context, (FluxSink<T>) fluxSink))))
        .doOnTerminate(() -> clearScroll(searchRequest.scrollId()));
    }

  protected ActionListener<SearchResponse> searchResponseAction(
      final ContextView context, final FluxSink<T> fluxSink) {
    return new ActionListener<SearchResponse>() {
      @Override
      public void onResponse(final SearchResponse searchResponse) {
        final SearchHits hits = searchResponse.getHits();
        getEsMetadataUtils()
            .extract(context)
            .ifPresent(
                metadata1 -> {
                  final EsMetadata.EsDaoMetadata metadata = new EsMetadata.EsDaoMetadata();
                  getEsMetadataUtils().ensureDaoMap(metadata1).put(getTableName(), metadata);

                  final String scrollId = searchResponse.getScrollId();
                  final boolean haveScrollId = StringUtils.isNotBlank(scrollId);
                  if (haveScrollId && hits.getHits().length == 0) {
                    clearScroll(scrollId);
                    metadata.setScrollId(null);
                  } else {
                    metadata.setScrollId(haveScrollId ? scrollId : null);
                  }

                  Optional.ofNullable(hits.getTotalHits())
                      .map(totalHits -> totalHits.value)
                      .ifPresent(aLong -> metadata.setTotalResults(aLong));
                });
        Arrays.stream(hits.getHits())
            .forEach(
                fields -> {
                  final Map<String, Object> map = fields.getSourceAsMap();
                  final T t = getJsonUtils().fromMap(map, getEntityClass());
                  fluxSink.next(t);
                });
        fluxSink.complete();
      }

      @Override
      public void onFailure(final Exception e) {
        log.error("search failed", e);
        fluxSink.error(new RuntimeException(e));
      }
    };
    }

  protected void clearScroll(final String scrollId) {
    final ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
        clearScrollRequest.addScrollId(scrollId);
    getRestClient()
        .clearScrollAsync(
            clearScrollRequest,
            RequestOptions.DEFAULT,
            new ActionListener<ClearScrollResponse>() {
              @Override
              public void onResponse(final ClearScrollResponse clearScrollResponse) {
                log.debug("clearScrollResponse.isSucceeded={}", clearScrollResponse.isSucceeded());
              }

              @Override
              public void onFailure(final Exception e) {
                log.error("unable to clear scroll response", e);
              }
            });
    }

  public Mono<T> save(final T t) {

    return Mono.deferContextual(
        context -> {
          if (StringUtils.isBlank(getId(t))) {
            setId(t, UUID.randomUUID().toString());
          }

          final IndexRequest request = new IndexRequest(getTableName());
          request.id(getId(t));
          request.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);

          final Optional<Boolean> isCreateOnly =
              getEsMetadataUtils()
                  .extract(context)
                  .map(EsMetadata::getQuery)
                  .map(EsMetadata.EsQueryMetadata::getIsOpTypeCreate);
          isCreateOnly.ifPresent(aBoolean -> request.create(aBoolean));
          if (!isCreateOnly.orElse(false)) {
            final Long versionNum =
                Optional.ofNullable(getVersion(t)).map(aLong -> aLong + 1).orElse(0l);
            request.version(versionNum);
            request.versionType(VersionType.EXTERNAL);
            setVersion(t, versionNum);
          }

          return Mono.just(getJsonUtils().toStringLazy(t).toString())
              .flatMapMany(
                  s ->
                      Flux.<T>create(
                          fluxSink -> {
                            request.source(s, XContentType.JSON);
                            getRestClient()
                                .indexAsync(
                                    request,
                                    RequestOptions.DEFAULT,
                                    new ActionListener<IndexResponse>() {
                                      @Override
                                      public void onResponse(final IndexResponse response) {
                                        log.debug("save result: {}", response);
                                        fluxSink.next(t);
                                        fluxSink.complete();
                                      }

                                      @Override
                                      public void onFailure(final Exception e) {
                                        log.error("unable to save", e);
                                        fluxSink.error(new RuntimeException(e));
                                      }
                                    });
                          }))
              .next();
        });
    }

  public Mono<T> update(final T data) {

        if (data == null) {
            return Mono.empty();
        }

    final String id = getId(data);

        if (StringUtils.isBlank(id)) {
            return Mono.error(new RuntimeException("id is blank"));
        }

    final UpdateRequest request = new UpdateRequest(getTableName(), id);
        request.doc(getJsonUtils().toStringLazy(data).toString(), XContentType.JSON);
        request.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
        request.retryOnConflict(5);
        request.fetchSource(true);

    return Mono.deferContextual(
        context ->
            Flux.<T>create(
                    fluxSink -> {
                      getRestClient()
                          .updateAsync(
                              request,
                              RequestOptions.DEFAULT,
                              new ActionListener<UpdateResponse>() {
                                @Override
                                public void onResponse(final UpdateResponse response) {
                                  final GetResult result = response.getGetResult();
                                  if (result.isExists() && !result.isSourceEmpty()) {
                                    final String sourceAsString = result.sourceAsString();
                                    getJsonUtils()
                                        .readValue(sourceAsString, getEntityClass())
                                        .ifPresent(
                                            t -> {
                                              setVersion(t, response.getVersion());
                                              fluxSink.next(t);
                                            });
                                  }
                                  fluxSink.complete();
                                }

                                @Override
                                public void onFailure(final Exception e) {
                                  log.error("unable to update", e);
                                  fluxSink.error(new RuntimeException(e));
                                }
                              });
                    })
                .next()
                .flatMap(
                    t -> {
                      final Optional<Boolean> isCreateOnly =
                          getEsMetadataUtils()
                              .extract(context)
                              .map(EsMetadata::getQuery)
                              .map(EsMetadata.EsQueryMetadata::getIsOpTypeCreate);
                      if (!isCreateOnly.orElse(false)) {
                        return save(t); // ugly hack in order to update version number in db
                      }
                      return Mono.just(t);
                    }));
    }

    protected Map<String, Object> getKeywordTextAnalizer() {
        return getTextAnalizer(KEYWORD);
    }

  protected Map<String, Object> getTextAnalizer(final String analyzer) {
    final Map<String, Object> result = new HashMap<>();
        result.put(TYPE, TEXT);
        result.put(ANALYZER, analyzer);
        return result;
    }

    protected Map<String, Object> getDateFieldType() {
    final Map<String, Object> result = new HashMap<>();
        result.put(TYPE, DATE);
        result.put(FORMAT, "strict_date_time||epoch_second||epoch_millis");
        return result;
    }

    protected Map<String, Object> getLongFieldType() {
    final Map<String, Object> result = new HashMap<>();
        result.put(TYPE, LONG);
        return result;
    }

    protected Map<String, Object> getBooleanFieldType() {
    final Map<String, Object> result = new HashMap<>();
        result.put(TYPE, BOOLEAN);
        return result;
    }

    protected Mono<Boolean> createIndex() {
    final CreateIndexRequest request = new CreateIndexRequest(getTableName());

        return createIndex(request);
    }

  protected Mono<Boolean> createIndex(final Map<String, Object> propsMapping) {
    final CreateIndexRequest request = new CreateIndexRequest(getTableName());
    final Map<String, Object> mapping = new HashMap();
        mapping.put(PROPERTIES, propsMapping);
        request.mapping(mapping);
        return createIndex(request);
    }

  protected Mono<Boolean> createIndex(final CreateIndexRequest request) {
    return Flux.<Boolean>create(
            fluxSink ->
                getRestClient()
                    .indices()
                    .createAsync(
                        request,
                        RequestOptions.DEFAULT,
                        new ActionListener<CreateIndexResponse>() {
                          @Override
                          public void onResponse(final CreateIndexResponse createIndexResponse) {
                            log.info("CreateIndexResponse: {}", createIndexResponse);
                            fluxSink.next(createIndexResponse.isAcknowledged());
                            fluxSink.complete();
                          }

                          @Override
                          public void onFailure(final Exception e) {
                            log.error("unable to create index", e);
                            fluxSink.error(new RuntimeException(e));
                          }
                        }))
        .next();
    }

    protected Mono<Boolean> ensureIndex() {
    return Mono.just(false)
        .flatMap(
            atomicBoolean ->
                Flux.<Boolean>create(
                        fluxSink ->
                            getRestClient()
                                .getLowLevelClient()
                                .performRequestAsync(
                                    new Request(HttpMethod.HEAD, getTableName()),
                                    new ResponseListener() {
                                      @Override
                                      public void onSuccess(final Response response) {
                                        logResponse(response);
                                        final boolean result =
                                            response.getStatusLine().getStatusCode()
                                                == HttpStatus.SC_OK;
                                        fluxSink.next(result);
                                        fluxSink.complete();
                                      }

                                      @Override
                                      public void onFailure(final Exception exception) {
                                        log.error("unable to check for index", exception);
                                        fluxSink.error(new RuntimeException(exception));
                                      }
                                    }))
                    .next());
    }

  private void logResponse(final Response response) {
    final HttpEntity entity = response.getEntity();
        if (entity == null) {
            log.debug("entity is null");
            return;
        }
        try {
      final InputStream content = entity.getContent();
            log.debug("response result: {}", IOUtils.toString(content, StandardCharsets.UTF_8));
    } catch (final Exception e) {
            log.error("unable to log response", e);
        }
    }


}

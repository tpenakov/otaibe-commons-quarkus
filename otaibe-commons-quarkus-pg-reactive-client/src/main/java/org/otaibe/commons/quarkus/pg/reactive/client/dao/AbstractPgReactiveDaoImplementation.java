package org.otaibe.commons.quarkus.pg.reactive.client.dao;

import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.SqlConnection;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.otaibe.commons.quarkus.core.utils.JsonUtils;
import org.otaibe.commons.quarkus.pg.reactive.client.config.JsonConfig;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Getter
@Setter
@Slf4j
public abstract class AbstractPgReactiveDaoImplementation<T, ID> {
  public static final char COMMA = ',';
  public static final String SINGLE_QUOTE = "'";
  public static final String SELECT_FROM = "SELECT {0} FROM {1}";
  public static final String INSERT_INTO = "INSERT INTO {0} ({1}) VALUES ({2})";
  public static final String DELETE_FROM_WHERE = "DELETE FROM {0} WHERE ";
  public static final String DELETE_FROM = DELETE_FROM_WHERE + "{1}=$1";
  public static final String FIND_BY_ID = "{0} WHERE {1}=$1";

  @Inject PgPool client;
  @Inject JsonConfig jsonConfig;
  @Inject JsonUtils jsonUtils;

  private String allColumnsHeader;
  private String selectFromSql;
  private String deleteByIdSql;
  private String findByIdSql;
  private AtomicBoolean isInited = new AtomicBoolean(false);

  protected abstract String getIdFieldName();

  protected abstract ID getId(T entity);

  protected abstract String getTableName();

  protected abstract T createDummyEntityWithAllFields();

  protected abstract T fromRow(Row row);

  protected abstract void fixDataMap(T data, Map<String, Object> entity);

  @PostConstruct
  public void init() {
    log.info("init started");
    final T dummyEntityWithAllFields = createDummyEntityWithAllFields();
    // log.info("init dummyEntityWithAllFields={}", dummyEntityWithAllFields);
    final Map<String, Object> entity =
        getJsonUtils().toMap(dummyEntityWithAllFields, getJsonConfig().getDbPropsNamesMapper());
    // log.info("init entity={}", entity);
    allColumnsHeader = StringUtils.join(entity.keySet(), COMMA);
    fillSelectFromSql();
    fillDeleteByIdTemplate();
    fillFindByIdTemplate();
    getIsInited().set(true);
    log.info("init completed");
  }

  public Mono<Boolean> deleteById(final T data) {
    // String sql = MessageFormat.format(DELETE_FROM, getTableName(), getIdFieldName());
    return Mono.fromCompletionStage(
            getClient()
                .preparedQuery(getDeleteByIdSql())
                .execute(getIdTuple(data))
                .convert()
                .toCompletionStage())
        .doOnNext(rows -> log.trace("delete data: {}", rows))
        .map(rows -> rows.rowCount() == 1);
  }

  public Mono<T> findById(final T pkData) {
    return Mono.fromCompletionStage(
            getClient()
                .preparedQuery(getFindByIdSql())
                .execute(getIdTuple(pkData))
                .convert()
                .toCompletionStage())
        .flatMapMany(rows -> Flux.fromIterable(rows))
        .next()
        .map(row -> fromRow(row));
  }

  public Mono<T> save(final T data) {

    final Tuple2<String, Tuple> objects1 = prepareForInsert(data);

    return Mono.fromCompletionStage(
        getClient()
            .withTransaction(
                sqlConnection ->
                    sqlConnection
                        .preparedQuery(getDeleteByIdSql())
                        .execute(getIdTuple(data))
                        .flatMap(
                            rows ->
                                sqlConnection
                                    .preparedQuery(objects1.getT1())
                                    .execute(objects1.getT2())
                                    .onItem()
                                    .transform(rows1 -> Tuples.of(rows, rows1)))
                        .onItem()
                        .invoke(rows -> log.trace("save data: {}", rows))
                        .replaceWith(data))
            .convert()
            .toCompletionStage());
  }

  protected Mono<T> save(final SqlConnection transaction, final T data) {
    final Tuple2<String, Tuple> objects = prepareForInsert(data);

    return Mono.fromCompletionStage(
            transaction
                .preparedQuery(objects.getT1())
                .execute(objects.getT2())
                .convert()
                .toCompletionStage())
        .map(rows -> data);
  }

  public Mono<Boolean> batchSave(final List<T> dataList) {
    if (CollectionUtils.isEmpty(dataList)) {
      return Mono.just(true);
    }

    final List<Tuple2<Tuple2<String, Tuple>, Tuple2<String, Tuple>>> batchData =
        dataList.stream()
            .map(
                containersStats ->
                    Tuples.of(
                        prepareForInsert(containersStats),
                        Tuples.of(getDeleteByIdSql(), getIdTuple(containersStats))))
            .collect(Collectors.toList());

    final Tuple2<String, List<Tuple>> deleteData =
        batchData.stream()
            .map(
                objects ->
                    Tuples.of(objects.getT2().getT1(), Arrays.asList(objects.getT2().getT2())))
            .reduce(
                (objects, objects2) -> {
                  final List<Tuple> list = new ArrayList(objects.getT2());
                  list.addAll(objects2.getT2());
                  return Tuples.of(objects.getT1(), list);
                })
            .orElseThrow(() -> new RuntimeException("unable to create delete data batch"));

    final Tuple2<String, List<Tuple>> insertData =
        batchData.stream()
            .map(
                objects ->
                    Tuples.of(objects.getT1().getT1(), Arrays.asList(objects.getT1().getT2())))
            .reduce(
                (objects, objects2) -> {
                  final List<Tuple> list = new ArrayList(objects.getT2());
                  list.addAll(objects2.getT2());
                  return Tuples.of(objects.getT1(), list);
                })
            .orElseThrow(() -> new RuntimeException("unable to create insert data batch"));

    return Mono.fromCompletionStage(
        getClient()
            .withTransaction(
                sqlConnection ->
                    sqlConnection
                        .preparedQuery(deleteData.getT1())
                        .executeBatch(deleteData.getT2())
                        .flatMap(
                            rows ->
                                sqlConnection
                                    .preparedQuery(insertData.getT1())
                                    .executeBatch(insertData.getT2())
                                    .onItem()
                                    .transform(rows1 -> Tuples.of(rows, rows1)))
                        .onItem()
                        .invoke(rows -> log.trace("save data: {}", rows))
                        .replaceWith(true))
            .convert()
            .toCompletionStage());
  }

  public Tuple2<String, Tuple> prepareForInsert(final T data) {
    final Map<String, Object> entity =
        getJsonUtils().toMap(data, getJsonConfig().getDbPropsNamesMapper());
    fixDataMap(data, entity);

    final List<Object> values = new ArrayList<>();
    final List<String> keys =
        entity.entrySet().stream()
            .filter(entry -> null != entry.getValue())
            .map(
                entry -> {
                  values.add(entry.getValue());
                  return entry.getKey();
                })
            .collect(Collectors.toList());

    final Object firstValue = values.get(0);

    Object nextValues[] = values.size() <= 1 ? null : (new Object[] {values.get(1)});

    for (int i = 2; i < values.size(); i++) {
      nextValues = ArrayUtils.addAll(nextValues, values.get(i));
    }

    final String headers = StringUtils.join(keys, COMMA);

    final AtomicInteger numParam = new AtomicInteger();
    final List<String> paramsList =
        values.stream()
            .map(o -> new StringBuilder().append('$').append(numParam.incrementAndGet()).toString())
            .collect(Collectors.toList());
    final String valuesString = StringUtils.join(paramsList, COMMA);

    final String sql = MessageFormat.format(INSERT_INTO, getTableName(), headers, valuesString);

    final Object[] nextValues1 = ArrayUtils.clone(nextValues);

    final Tuple tuple =
        Optional.ofNullable(nextValues)
            .map(objects -> new Tuple(io.vertx.sqlclient.Tuple.of(firstValue, nextValues1)))
            .orElseGet(() -> Tuple.of(firstValue));
    return Tuples.of(sql, tuple);
  }

  protected void fillSelectFromSql() {
    selectFromSql = MessageFormat.format(SELECT_FROM, allColumnsHeader, getTableName());
  }

  protected void fillFindByIdTemplate() {
    findByIdSql = MessageFormat.format(FIND_BY_ID, getSelectFromSql(), getIdFieldName());
  }

  protected void fillDeleteByIdTemplate() {
    deleteByIdSql = MessageFormat.format(DELETE_FROM, getTableName(), getIdFieldName());
  }

  protected Tuple getIdTuple(final T entity) {
    return Tuple.of(getId(entity));
  }
}

package org.otaibe.commons.quarkus.pg.reactive.client.dao;

import io.reactiverse.reactivex.pgclient.Row;
import io.reactiverse.reactivex.pgclient.Tuple;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.otaibe.commons.quarkus.core.utils.JsonUtils;
import org.otaibe.commons.quarkus.pg.reactive.client.config.JsonConfig;
import reactor.adapter.rxjava.RxJava2Adapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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

    @Inject
    io.reactiverse.reactivex.pgclient.PgPool client;
    @Inject
    JsonConfig jsonConfig;
    @Inject
    JsonUtils jsonUtils;

    private String allColumnsHeader;
    private String selectFromSql;
    private String deleteByIdSql;
    private String findByIdSql;

    protected abstract String getIdFieldName();

    protected abstract ID getId(T entity);

    protected abstract String getTableName();

    protected abstract T createDummyEntityWithAllFields();

    protected abstract T fromRow(Row row);

    protected abstract void fixDataMap(T data, Map<String, Object> entity);

    @PostConstruct
    public void init() {
        Map<String, Object> entity = getJsonUtils().toMap(
                createDummyEntityWithAllFields(),
                getJsonConfig().getDbPropsNamesMapper());
        allColumnsHeader = StringUtils.join(entity.keySet(), COMMA);
        fillSelectFromSql();
        fillDeleteByIdTemplate();
        fillFindByIdTemplate();
    }

    public Mono<Boolean> deleteById(T data) {
        //String sql = MessageFormat.format(DELETE_FROM, getTableName(), getIdFieldName());
        return RxJava2Adapter.singleToMono(getClient().rxPreparedQuery(getDeleteByIdSql(), getIdTuple(data)))
                .doOnNext(rows -> log.trace("delete data: {}", rows))
                .map(rows -> rows.rowCount() == 1);

    }

    public Mono<T> findById(T pkData) {
        return RxJava2Adapter.singleToMono(getClient().rxPreparedQuery(getFindByIdSql(), getIdTuple(pkData)))
                .flatMapMany(rows -> Flux.fromIterable(rows))
                .next()
                .map(row -> fromRow(row))
                ;
    }

    public Mono<T> save(T data) {

        Tuple2<String, Tuple> objects1 = prepareForInsert(data);

        return RxJava2Adapter
                .singleToMono(getClient().rxBegin())
                .flatMap(pgTransaction -> RxJava2Adapter
                        .singleToMono(pgTransaction.rxPreparedQuery(getDeleteByIdSql(), getIdTuple(data)))
                        .zipWhen(rows -> RxJava2Adapter
                                .singleToMono(pgTransaction.rxPreparedQuery(objects1.getT1(), objects1.getT2())))
                        .zipWhen(objects -> RxJava2Adapter
                                .completableToMono(pgTransaction.rxCommit())
                                .then(Mono.just(true))
                        )
                )
                .doOnNext(rows -> log.trace("save data: {}", rows))
                .map(rows -> data);
    }

    public Mono<Boolean> batchSave(List<T> dataList) {
        if (CollectionUtils.isEmpty(dataList)) {
            return Mono.just(true);
        }

        List<Tuple2<Tuple2<String, Tuple>, Tuple2<String, Tuple>>> batchData = dataList.stream()
                .map(containersStats ->
                        Tuples.of(
                                prepareForInsert(containersStats),
                                Tuples.of(getDeleteByIdSql(), getIdTuple(containersStats))
                        )
                )
                .collect(Collectors.toList());

        Tuple2<String, List<Tuple>> deleteData = batchData.stream()
                .map(objects -> Tuples.of(objects.getT2().getT1(), Arrays.asList(objects.getT2().getT2())))
                .reduce((objects, objects2) -> {
                    List<Tuple> list = new ArrayList(objects.getT2());
                    list.addAll(objects2.getT2());
                    return Tuples.of(objects.getT1(), list);
                })
                .orElseThrow(() -> new RuntimeException("unable to create delete data batch"))
                ;

        Tuple2<String, List<Tuple>> insertData = batchData.stream()
                .map(objects -> Tuples.of(objects.getT1().getT1(), Arrays.asList(objects.getT1().getT2())))
                .reduce((objects, objects2) -> {
                    List<Tuple> list = new ArrayList(objects.getT2());
                    list.addAll(objects2.getT2());
                    return Tuples.of(objects.getT1(), list);
                })
                .orElseThrow(() -> new RuntimeException("unable to create insert data batch"))
                ;

        return RxJava2Adapter
                .singleToMono(getClient().rxBegin())
                .flatMap(pgTransaction -> RxJava2Adapter
                        .singleToMono(pgTransaction.rxPreparedBatch(deleteData.getT1(), deleteData.getT2()))
                        .zipWhen(rows -> RxJava2Adapter
                                .singleToMono(pgTransaction.rxPreparedBatch(insertData.getT1(), insertData.getT2())))
                        .zipWhen(objects -> RxJava2Adapter
                                .completableToMono(pgTransaction.rxCommit())
                                .then(Mono.just(true))
                        )
                )
                .doOnNext(rows -> log.trace("save data: {}", rows))
                .map(rows -> rows.getT2());

    }

    public Tuple2<String, Tuple> prepareForInsert(T data) {
        Map<String, Object> entity = getJsonUtils().toMap(data, getJsonConfig().getDbPropsNamesMapper());
        fixDataMap(data, entity);

        List<Object> values = new ArrayList<>();
        List<String> keys = entity.entrySet().stream()
                .filter(entry -> null != entry.getValue())
                .map(entry -> {
                    values.add(entry.getValue());
                    return entry.getKey();
                })
                .collect(Collectors.toList());


        Object firstValue = values.get(0);

        Object nextValues[] = values.size() <= 1 ? null : (
                new Object[]{values.get(1)}
        );

        for (int i = 2; i < values.size(); i++) {
            nextValues = ArrayUtils.addAll(nextValues, values.get(i));
        }

        String headers = StringUtils.join(keys, COMMA);

        AtomicInteger numParam = new AtomicInteger();
        List<String> paramsList = values.stream()
                .map(o -> new StringBuilder().append('$').append(numParam.incrementAndGet()).toString())
                .collect(Collectors.toList());
        String valuesString = StringUtils.join(paramsList, COMMA);

        String sql = MessageFormat.format(INSERT_INTO, getTableName(), headers, valuesString);

        Object nextValues1[] = ArrayUtils.clone(nextValues);

        Tuple tuple = Optional.ofNullable(nextValues)
                .map(objects -> new Tuple(io.reactiverse.pgclient.Tuple.of(firstValue, nextValues1)))
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

    protected Tuple getIdTuple(T entity) {
        return Tuple.of(getId(entity));
    }

    protected String quoteString(String s) {
        return new StringBuilder()
                .append(SINGLE_QUOTE)
                .append(s)
                .append(SINGLE_QUOTE)
                .toString();
    }
}

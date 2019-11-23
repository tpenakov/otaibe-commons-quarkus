package org.otaibe.commons.quarkus.cache.service;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Builder
@Getter(AccessLevel.PACKAGE)
@Setter(AccessLevel.PACKAGE)
@Slf4j
public class CacheService<KEY, VALUE> {
    public static final String UNKNOWN = "UNKNOWN";
    private AtomicInteger numInCache;

    @Getter(AccessLevel.PRIVATE)
    private Map<KEY, CacheTimeEntry<VALUE>> cache;
    @Getter(AccessLevel.PRIVATE)
    private Queue<KEY> cacheKeyQueue;

    private Integer maxNumInCache;
    private Function<Long, Duration> durationFunction;
    private Long duration;


    public CacheService<KEY, VALUE> start() {
        log.info("CacheService start");
        numInCache = new AtomicInteger(0);
        cache = new ConcurrentHashMap<>();
        Optional.ofNullable(getMaxNumInCache()).ifPresent(integer -> {
            cacheKeyQueue = new ConcurrentLinkedQueue<>();
        });

        if (getDuration() != null || getDurationFunction() != null) {
            if (getDuration() == null || getDurationFunction() == null) {
                throw new RuntimeException("durationFunction and duration should be set together");
            }

            Flux.interval(getDurationFunction().apply(1l))
                    .retry()
                    .doOnNext(aLong -> {
                        LocalDateTime threshold = LocalDateTime.now().minus(getDurationFunction().apply(getDuration()));
                        log.debug("will clear cache by duration. numItems={}, maxInCache={}, firstDate={}, threshold={}",
                                getNumInCache().get(),
                                getMaxNumInCache(),
                                Optional.ofNullable(getMaxNumInCache())
                                .map(integer -> getCacheKeyQueue().peek())
                                .map(key -> getCache().get(key))
                                .map(valueCacheTimeEntry -> valueCacheTimeEntry.getDateTime())
                                .map(dateTime -> dateTime.toString())
                                .orElse(UNKNOWN),
                                threshold
                        );
                        clearByDateTime(threshold);
                        log.debug("cleared cache by duration. numItems={}, maxInCache={}, firstDate={}, threshold={}",
                                getNumInCache().get(),
                                getMaxNumInCache(),
                                Optional.ofNullable(getMaxNumInCache())
                                        .map(integer -> getCacheKeyQueue().peek())
                                        .map(key -> getCache().get(key))
                                        .map(valueCacheTimeEntry -> valueCacheTimeEntry.getDateTime())
                                        .map(dateTime -> dateTime.toString())
                                        .orElse(UNKNOWN),
                                threshold
                        );
                    })
                    .subscribe();
        }

        log.info("CacheService started");
        return this;
    }

    public VALUE get(KEY key) {
        return Optional.ofNullable(key)
                .map(key1 -> getCache().get(key1))
                .filter(entry -> entry.getDateTime() != null)
                .map(entry -> entry.getValue())
                .orElse(null);
    }

    public VALUE put(KEY key, VALUE value) {
        if (key == null) {
            throw new RuntimeException("key should not be null");
        }
        boolean isNotInCache = !getCache().containsKey(key);
        getCache().put(key, CacheTimeEntry.<VALUE>builder()
                .dateTime(LocalDateTime.now())
                .value(value)
                .build());
        if (isNotInCache) {
            int num = getNumInCache().incrementAndGet();
            if (getMaxNumInCache() != null) {
                if (num > getMaxNumInCache()) {
                    KEY key1 = getCacheKeyQueue().poll();
                    removeFromCacheOnly(key1);
                }
                getCacheKeyQueue().add(key);
            }
        }
        log.debug("put completed: wasNotInCache={} MaxNumInCache={} approxNumInCache={}, numInCache={}",
                isNotInCache,
                getMaxNumInCache(),
                getCache().keySet().size(),
                getNumInCache().get()
                );
        return value;
    }

    public void remove(KEY key) {
        if (getCacheKeyQueue() != null) {
            boolean result = getCacheKeyQueue().remove(key);
            log.debug("removed key={}, result={}", key, result);
        }
        removeFromCacheOnly(key);
    }

    void clearByDateTime(LocalDateTime threshold) {
        getCache().entrySet().stream()
                .filter(entry -> entry.getValue().getDateTime().isBefore(threshold))
                .map(entry -> entry.getKey())
                .collect(Collectors.toList())
                .forEach(key -> remove(key));
    }

    private void removeFromCacheOnly(KEY key) {
        CacheTimeEntry<VALUE> entry = getCache().remove(key);
        int numLeft = getNumInCache().decrementAndGet();
        log.debug("removed key={}, dateTime={}, numLeft={}",
                key,
                Optional.ofNullable(entry).map(CacheTimeEntry::getDateTime),
                numLeft
        );
    }

    @Builder
    @Getter
    public static final class CacheTimeEntry<V> {
        private LocalDateTime dateTime;
        private V value;
    }
}


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
                        clearByDateTime(threshold);
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
                getCacheKeyQueue().add(key);
                if (num > getMaxNumInCache()) {
                    KEY key1 = getCacheKeyQueue().poll();
                    removeFromCacheOnly(key1);
                }
            }
        }
        return value;
    }

    public void remove(KEY key) {
        if (getCacheKeyQueue() != null) {
            getCacheKeyQueue().remove(key);
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
        getCache().remove(key);
        getNumInCache().decrementAndGet();
    }

    @Builder
    @Getter
    public static final class CacheTimeEntry<V> {
        private LocalDateTime dateTime;
        private V value;
    }
}


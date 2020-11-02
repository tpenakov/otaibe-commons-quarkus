package org.otaibe.commons.quarkus.core.utils;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/*
 * Created by triphon on 2.11.20 г.
 */
@Slf4j
public class ReactorBackpressureHandler {
    public static ReactorBackpressureHandler of() {
        return new ReactorBackpressureHandler();
    }

    public <T, V> Flux<V> handle(final Integer rate,
                                 final Flux<T> upstream,
                                 final Function<T, Mono<V>> handleFn) {
        final AtomicInteger numSimultaneous = new AtomicInteger(0);
        final AtomicLong total = new AtomicLong(0);
        final AtomicBoolean isUpstreamCompleted = new AtomicBoolean(false);
        final AtomicLong numProcessed = new AtomicLong(0);
        final UnicastProcessor<T> processor = UnicastProcessor.create();
        final FluxSink<T> sink = processor.sink(FluxSink.OverflowStrategy.BUFFER);
        final Queue<T> buffer = new ConcurrentLinkedQueue<>();

        final Flux<T> upstream1 = upstream
                .doOnComplete(() -> {
                    isUpstreamCompleted.set(true);
                    if (total.get() == 0l) {
                        sink.complete();
                    }
                })
                .doOnError(throwable -> sink.error(throwable))
                .doOnNext(integer -> {
                    total.incrementAndGet();
                    sink.next(integer);
                });

        return processor
                .doOnSubscribe(subscription -> upstream1.subscribe())
                .flatMap(t -> {
                    if (numSimultaneous.get() >= rate) {
                        buffer.add(t);
                        return Mono.empty();
                    }
                    final int i = numSimultaneous.incrementAndGet();
                    log.trace("before numSimultaneous={} result={}", i, t);
                    return Mono.just(t);
                })
                .flatMap(t -> handleFn.apply(t))
                .doOnNext(v -> {
                    final int i = numSimultaneous.decrementAndGet();
                    numProcessed.incrementAndGet();
                    log.trace("after numSimultaneous={} result={}", i, v);

                    if (buffer.peek() != null) {
                        sink.next(buffer.poll());
                    }
                    if (numProcessed.get() == total.get() && isUpstreamCompleted.get()) {
                        log.trace("will complete numProcessed={} total={}", numProcessed.get(), total.get());
                        sink.complete();
                    }
                });
    }

}

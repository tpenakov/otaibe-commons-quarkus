package org.otaibe.commons.quarkus.core.utils;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

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
    public static final Sinks.EmitFailureHandler DEFAULT_EMIT_FAILURE_HANDLER =
            (signalType, emitResult) ->
                    emitResult.equals(Sinks.EmitResult.FAIL_NON_SERIALIZED) ? true : false;

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
        final Sinks.Many<T> sink =  Sinks.many().unicast().<T>onBackpressureBuffer();
        final Queue<T> buffer = new ConcurrentLinkedQueue<>();

        final Flux<T> upstream1 = upstream
                .doOnComplete(() -> {
                    isUpstreamCompleted.set(true);
                    if (total.get() == 0l) {
                        sink.emitComplete(DEFAULT_EMIT_FAILURE_HANDLER);
                    }
                })
                .doOnError(throwable -> sink.emitError(throwable, DEFAULT_EMIT_FAILURE_HANDLER))
                .doOnNext(integer -> {
                    total.incrementAndGet();
                    sink.emitNext(integer, DEFAULT_EMIT_FAILURE_HANDLER);
                });

        return sink.asFlux()
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
                        sink.emitNext(buffer.poll(), DEFAULT_EMIT_FAILURE_HANDLER);
                    }
                    if (numProcessed.get() == total.get() && isUpstreamCompleted.get()) {
                        log.trace("will complete numProcessed={} total={}", numProcessed.get(), total.get());
                        sink.emitComplete(DEFAULT_EMIT_FAILURE_HANDLER);
                    }
                });
    }

}

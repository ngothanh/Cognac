package com.tngo.cognac.pipe;

import com.github.benmanes.caffeine.cache.*;
import com.tngo.cognac.pipe.exceptions.CognacPipeException;
import io.micrometer.core.instrument.Clock;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import static io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics.monitor;

public class AutoPullingCognacPipe<KEY, VALUE> extends AbstractCognacPipe<KEY, VALUE> {
    private static final PrometheusMeterRegistry DEFAULT_STATISTICS_REGISTRY = new PrometheusMeterRegistry(
            PrometheusConfig.DEFAULT, CollectorRegistry.defaultRegistry, Clock.SYSTEM);

    private final AsyncLoadingCache<KEY, VALUE> caffeine;
    private final CognacSource<KEY, VALUE> dataProvider;

    @Builder
    private AutoPullingCognacPipe( //
                                   @NonNull CognacSource<KEY, VALUE> dataProvider, //
                                   Expiry<KEY, VALUE> expiry, //
                                   RemovalListener<KEY, VALUE> removalListener,
                                   @Singular List<CognacManufacturer<VALUE, KEY>> indexers, //
                                   Integer maximumSize, //
                                   Integer initialCapacity,
                                   boolean recordStats,
                                   String cacheName
    ) {
        this.dataProvider = dataProvider;
        this.caffeine = this.buildCache(
                maximumSize,
                initialCapacity,
                removalListener,
                indexers != null && !indexers.isEmpty(),
                expiry,
                recordStats
        );

        if (recordStats) {
            monitor(DEFAULT_STATISTICS_REGISTRY, this.caffeine, cacheName);
        }
        if (indexers != null)
            indexers.forEach(this::registerIndexer);
    }

    private AsyncLoadingCache<KEY, VALUE> buildCache(Integer maximumSize, Integer initialCapacity,
                                                     RemovalListener<KEY, VALUE> removalListener,
                                                     boolean hasIndexers,
                                                     Expiry<KEY, VALUE> expiry, boolean recordStats) {
        if (hasIndexers && removalListener != null) {
            throw new CognacPipeException("Cannot override removal listener while registering indexers");
        }

        var caffeineBuilder = Caffeine.newBuilder();
        if (removalListener != null) {
            caffeineBuilder.removalListener(removalListener);
        } else {
            caffeineBuilder.removalListener(this::onRemoval);
        }

        if (expiry != null)
            caffeineBuilder.expireAfter(expiry);

        if (maximumSize != null)
            caffeineBuilder.maximumSize(maximumSize);

        if (initialCapacity != null)
            caffeineBuilder.initialCapacity(initialCapacity);

        if (recordStats) {
            caffeineBuilder.recordStats();
        }

        var namePattern = "AutoPullingCognacPipe #%d";
        var idSeed = new AtomicInteger(0);
        var threadFactory = (ThreadFactory) r -> new Thread(r, String.format(namePattern, idSeed.incrementAndGet()));
        var executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), threadFactory);
        caffeineBuilder.executor(executorService);

        return caffeineBuilder.buildAsync(this::doLoad);
    }

    private void onRemoval(KEY key, VALUE value, RemovalCause cause) {
        if (cause != RemovalCause.REPLACED)
            dropIndex(key, value);
    }

    private CompletableFuture<VALUE> doLoad(KEY key, Executor executor) {
        return dataProvider.load(key, executor) //
                .thenApply(value -> {
                    if (value != null)
                        reindex(key, value);
                    return value;
                });

    }

    private CompletableFuture<Map<KEY, VALUE>> doLoadAll(Iterable<? extends KEY> keys, Executor executor) {
        return dataProvider.loadAll(keys, executor) //
                .thenApply(map -> {
                    if (map != null && !map.isEmpty())
                        map.forEach(this::reindex);
                    return map;
                });
    }

    @Override
    public CompletableFuture<VALUE> get(@NonNull KEY key) {
        return caffeine.get(key, this::doLoad);
    }

    @Override
    public CompletableFuture<Map<KEY, VALUE>> getAll(@NonNull Collection<KEY> keys) {
        return caffeine.getAll(keys, this::doLoadAll);
    }

    @Override
    public CompletableFuture<Map<KEY, VALUE>> bootstrap() {
        try {
            return dataProvider.loadAllKeys()
                    .thenCompose(keys -> caffeine.getAll(keys, dataProvider::loadAll));
        } catch (Exception e) {
            throw new CognacPipeException("Error bootstrapping CognacPipe", e);
        }
    }

    @Override
    public Set<KEY> keySet() {
        return caffeine.asMap().keySet();
    }

    @Override
    public void evict(KEY key) {
        caffeine.synchronous().invalidate(key);
    }

    @Override
    protected void onIndexerRegistered(@NonNull CognacManufacturer<VALUE, KEY> indexer) {
        var futures = caffeine.asMap().values();
        var joinedFutures = CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
        joinedFutures.thenAccept(res -> futures.stream().map(CompletableFuture::join).forEach(indexer::label));
    }

    @Override
    public void renew(@NonNull Collection<KEY> keys) {
        keys.forEach(caffeine.synchronous()::refresh);
    }

    @Override
    public void put(KEY key, VALUE value) {
        caffeine.synchronous().put(key, value);
    }

    @Override
    public void clear() {
        caffeine.synchronous().asMap().clear();
        indexers.forEach(CognacManufacturer::clear);
    }
}

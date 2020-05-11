package com.tngo.cognac.pipe;

import lombok.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import static java.util.concurrent.CompletableFuture.completedFuture;

public abstract class AbstractCognacPipe<KEY, VALUE> implements CognacPipe<KEY, VALUE> {

    private final CompletableFuture<Map<KEY, VALUE>> nullResponse = completedFuture(null);

    private final CompletableFuture<Map<KEY, VALUE>> emptyResponse = completedFuture(Collections.emptyMap());

    protected final List<CognacManufacturer<VALUE, KEY>> indexers = new CopyOnWriteArrayList<>();

    @Override
    public CompletableFuture<Map<KEY, VALUE>> query(@NonNull CognacLabel index) {
        for (CognacManufacturer<VALUE, KEY> indexer : indexers) {
            if (!indexer.acceptLabel(index))
                continue;
            return indexer.lookup(index)
                    .thenCompose(keys -> (keys == null || keys.isEmpty()) ? emptyResponse : this.getAll(keys));
        }
        return nullResponse;
    }

    protected void foreachIndexer(@NonNull Consumer<CognacManufacturer<VALUE, KEY>> consumer) {
        indexers.forEach(consumer);
    }

    protected void index(@NonNull VALUE value) {
        foreachIndexer(indexer -> indexer.label(value));
    }

    protected void reindex(@NonNull KEY key, @NonNull VALUE value) {
        foreachIndexer(indexer -> indexer.relabel(key, value));
    }

    protected void dropIndex(@NonNull KEY key, @NonNull VALUE value) {
        foreachIndexer(indexer -> indexer.drop(key));
    }

    @Override
    public final void registerIndexer(@NonNull CognacManufacturer<VALUE, KEY> indexer) {
        this.indexers.add(indexer);
        this.onIndexerRegistered(indexer);
    }

    protected void onIndexerRegistered(@NonNull CognacManufacturer<VALUE, KEY> indexer) {
        // do nothing
    }
}

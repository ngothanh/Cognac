package com.tngo.cognac.pipe;

import com.tngo.cognac.pipe.annotations.Nullable;
import com.tngo.cognac.pipe.exceptions.CognacPipeNonExistingKeyException;
import lombok.NonNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("squid:S00119")
public interface CognacPipe<KEY, VALUE> {

    CompletableFuture<VALUE> get(@NonNull KEY key) throws CognacPipeNonExistingKeyException;

    CompletableFuture<Map<KEY, VALUE>> getAll(@NonNull Collection<KEY> keys);

    CompletableFuture<Map<KEY, VALUE>> bootstrap();

    CompletableFuture<@Nullable Map<KEY, VALUE>> query(@NonNull CognacLabel index);

    void registerIndexer(@NonNull CognacManufacturer<VALUE, KEY> indexer);

    Set<KEY> keySet();

    void evict(KEY key);

    @SuppressWarnings("unchecked")
    default void renew(@NonNull KEY... keys) {
        renew(Arrays.asList(keys));
    }

    void renew(@NonNull Collection<KEY> keys);

    void put(KEY key, VALUE value);

    void clear();
}

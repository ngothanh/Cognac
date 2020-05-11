package com.tngo.cognac.pipe;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@SuppressWarnings("squid:S00119")
public interface CognacSource<KEY, VALUE> {

    CompletableFuture<Map<KEY, VALUE>> loadAll(Iterable<? extends KEY> keys, Executor executor);

    CompletableFuture<VALUE> load(KEY key, Executor executor);

    CompletableFuture<Set<KEY>> loadAllKeys();
}

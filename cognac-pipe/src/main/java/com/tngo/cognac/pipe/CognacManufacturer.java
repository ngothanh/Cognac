package com.tngo.cognac.pipe;

import lombok.NonNull;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public interface CognacManufacturer<VALUE, PrimaryKey> {

    void label(@NonNull VALUE value);

    void drop(@NonNull PrimaryKey key);

    default void relabel(@NonNull PrimaryKey key, @NonNull VALUE value) {
        drop(key);
        label(value);
    }

    default boolean acceptLabel(CognacLabel cognacLabel) {
        return true;
    }

    CompletableFuture<? extends Collection<PrimaryKey>> lookup(@NonNull CognacLabel cognacLabel);

    void clear();
}

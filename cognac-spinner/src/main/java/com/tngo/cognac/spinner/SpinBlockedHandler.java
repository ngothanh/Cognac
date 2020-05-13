package com.tngo.cognac.spinner;

import lombok.NonNull;

import java.util.Collection;
import java.util.stream.Collectors;

@FunctionalInterface
public interface SpinBlockedHandler<T> {
    void onSpinBlocked(Collection<T> records);

    static <K, V, O> SpinBlockedHandler<Record<K, V>> withTransformer(
            @NonNull Transformer<Record<K, V>, O> transformer,
            @NonNull SpinBlockedHandler<O> listener
    ) {
        return records -> {
            var transformedRecords = records.stream()
                    .map(transformer::transform)
                    .collect(Collectors.toList());

            listener.onSpinBlocked(transformedRecords);
        };
    }
}

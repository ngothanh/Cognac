package com.tngo.cognac.spinner;

public interface Spinner<K, V> {
    Disposable subscribe(K topic, SpinBlockedHandler<Record<K, V>> handler);

    default <O> Disposable subscribe(K topic, Transformer<Record<K, V>, O> transformer, SpinBlockedHandler<O> handler) {
        return subscribe(topic, SpinBlockedHandler.withTransformer(transformer, handler));
    }
}

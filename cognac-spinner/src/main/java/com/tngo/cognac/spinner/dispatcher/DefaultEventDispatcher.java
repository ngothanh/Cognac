package com.tngo.cognac.spinner.dispatcher;

import com.tngo.cognac.spinner.Disposable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

class DefaultEventDispatcher<EventType> implements EventDispatcher<EventType> {

    /**
     * Because listeners will only be registered on starting up, so we don't have to care about concurrently read-write here
     */
    private final List<Consumer<EventType>> listeners = new ArrayList<>();

    @Override
    public Disposable addListener(Consumer<EventType> listener) {
        synchronized (listeners) {
            if (listeners.add(listener)) {
                return createDisposer(listener);
            }
            return null;
        }
    }

    public Disposable createDisposer(Consumer<EventType> listener) {
        return () -> {
            synchronized (listeners) {
                return listeners.remove(listener);
            }
        };
    }

    @Override
    public void dispatch(EventType event) {
        listeners.forEach(listener -> listener.accept(event));
    }
}

package com.tngo.cognac.spinner.dispatcher;

import com.tngo.cognac.spinner.Disposable;

import java.util.function.Consumer;

public interface EventDispatcher<EventType> {
    Disposable addListener(Consumer<EventType> consumer);

    void dispatch(EventType event);

    static <T> EventDispatcher<T> newDefault() {
        return new DefaultEventDispatcher<>();
    }
}

package com.tngo.cognac.components;

public interface ComponentLifeCycle {

    default void start() {

    }

    default void stop() {

    }

    default boolean isStarted() {
        throw new UnsupportedOperationException();
    }
}

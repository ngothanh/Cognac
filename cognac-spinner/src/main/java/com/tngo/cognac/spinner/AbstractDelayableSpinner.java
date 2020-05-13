package com.tngo.cognac.spinner;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractDelayableSpinner<K, V> extends AbstractSpinner<K, V> implements DelayableSpinner {

    private final AtomicBoolean delaying = new AtomicBoolean(false);

    protected AbstractDelayableSpinner(boolean delayDispatch) {
        delaying.set(delayDispatch);
    }

    protected boolean isDelaying() {
        return delaying.get();
    }

    @Override
    public void startDispatch() {
        delaying.set(false);
    }
}

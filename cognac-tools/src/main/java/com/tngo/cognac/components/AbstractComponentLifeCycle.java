package com.tngo.cognac.components;

import com.tngo.cognac.tools.Loggable;
import io.gridgo.utils.ThreadUtils;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractComponentLifeCycle implements ComponentLifeCycle, Loggable {
    private final AtomicBoolean started = new AtomicBoolean(false);

    private volatile boolean running = false;

    private Logger logger;

    private String name;

    public AbstractComponentLifeCycle() {
        this.logger = getLogger();
    }

    @Override
    public final boolean isStarted() {
        ThreadUtils.busySpin(() -> started.get() ^ running);
        return this.running;
    }

    @Override
    public final void start() {
        if (!this.isStarted() && started.compareAndSet(false, true)) {
            if (logger.isTraceEnabled())
                logger.trace("Component starting {}", getName());
            try {
                this.onStart();
                this.running = true;
            } catch (Exception ex) {
                this.started.set(false);
                this.running = false;
                throw ex;
            }
            if (logger.isTraceEnabled())
                logger.trace("Component started {}", getName());
        }
    }

    @Override
    public final void stop() {
        if (this.isStarted() && started.compareAndSet(true, false)) {
            if (logger.isTraceEnabled())
                logger.trace("Component stopping {}", getName());
            try {
                this.onStop();
                this.running = false;
            } catch (Exception e) {
                this.running = false;
                throw e;
            }
            if (logger.isTraceEnabled())
                logger.trace("Component stopped {}", getName());
        }
    }

    @Override
    public String toString() {
        return getName();
    }

    public String getName() {
        if (name == null)
            name = generateName();
        return name;
    }

    protected abstract void onStart();

    protected abstract void onStop();

    protected abstract String generateName();
}

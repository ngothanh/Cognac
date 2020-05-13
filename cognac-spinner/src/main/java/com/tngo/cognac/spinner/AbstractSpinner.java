package com.tngo.cognac.spinner;

import com.lmax.disruptor.dsl.Disruptor;
import com.tngo.cognac.components.NoNameComponentLifeCycle;
import com.tngo.cognac.spinner.dispatcher.EventDispatcher;
import org.cliffc.high_scale_lib.NonBlockingHashMap;

import java.util.*;
import java.util.concurrent.ThreadFactory;


public abstract class AbstractSpinner<K, V> extends NoNameComponentLifeCycle implements Spinner<K, V> {

    private Map<K, EventDispatcher<Collection<Record<K, V>>>> dispatchers = new NonBlockingHashMap<>();

    private class EventHolder {
        EventDispatcher<Collection<Record<K, V>>> dispatcher;
        Collection<Record<K, V>> data;

        private void clear() {
            dispatcher = null;
            data = null;
        }
    }

    private final Disruptor<EventHolder> disruptor;

    protected AbstractSpinner() {
        var threadFactory = (ThreadFactory) r -> new Thread(r, "Spinner's dispatcher");
        disruptor = new Disruptor<>(EventHolder::new, 2048, threadFactory);
        disruptor.handleEventsWith(this::dispatch);
    }

    private void dispatch(EventHolder event, long sequence, boolean endOfBatch) {
        event.dispatcher.dispatch(event.data);
        event.clear();
    }

    @Override
    protected void onStart() {
        disruptor.start();
    }

    @Override
    protected void onStop() {
        disruptor.shutdown();
    }

    protected void publish(List<Record<K, V>> records) {
        var map = new HashMap<K, Collection<Record<K, V>>>();
        for (var record : records)
            map.computeIfAbsent(record.getKey(), k -> new LinkedList<>()).add(record);

        for (var entry : map.entrySet()) {
            var topic = entry.getKey();
            var topicDispatcher = dispatchers.get(topic);
            if (topicDispatcher == null)
                continue;

            disruptor.publishEvent((e, s) -> {
                e.dispatcher = topicDispatcher;
                e.data = entry.getValue();
            });
        }
    }

    @Override
    public Disposable subscribe(K topic, SpinBlockedHandler<Record<K, V>> listener) {
        if (isStarted() && !getTopics().contains(topic))
            throw new IllegalStateException("Cannot listen to non-existing topic while spinner already started");

        return dispatchers.computeIfAbsent(topic, k -> EventDispatcher.newDefault())
                .addListener(listener::onSpinBlocked);
    }

    public Set<K> getTopics() {
        return dispatchers.keySet();
    }
}

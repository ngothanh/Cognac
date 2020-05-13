package com.tngo.cognac.spinner.kafka;

import com.tngo.cognac.spinner.AbstractDelayableSpinner;
import com.tngo.cognac.spinner.Record;
import io.gridgo.utils.ThreadUtils;
import lombok.Builder;
import lombok.NonNull;
import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import static java.time.Duration.ofMillis;

public class Kafker extends AbstractDelayableSpinner<String, ConsumerRecord<String, String>> {
    private static final String THREAD_NAME = "kafka-spinner";
    private static final long DEFAULT_POLL_TIMEOUT_MILLIS = 100L;
    private static final Properties DEFAULT_PROPERTIES = new Properties();

    static {
        try (var input = Kafker.class.getClassLoader().getResourceAsStream("kafka/default-consumer.properties")) {
            DEFAULT_PROPERTIES.load(input);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Properties mergeWithDefault(Properties props) {
        props = props != null ? props : new Properties();
        for (var key : DEFAULT_PROPERTIES.keySet())
            if (!props.contains(key))
                props.put(key, DEFAULT_PROPERTIES.get(key));
        return props;
    }

    private final @NonNull Thread pollingThread;
    private final @NonNull Properties properties;
    private final Duration pollTimeout;

    @Builder
    private Kafker(Properties kafkaProperties, Integer pollTimeoutMillis, String pollingThreadName,
                   Boolean delayDispatch) {
        super(delayDispatch != null && delayDispatch.booleanValue());

        this.properties = mergeWithDefault(kafkaProperties);
        this.pollingThread = new Thread(this::poll, pollingThreadName == null ? THREAD_NAME : pollingThreadName);
        this.pollTimeout = ofMillis(pollTimeoutMillis != null ? pollTimeoutMillis : DEFAULT_POLL_TIMEOUT_MILLIS);
    }

    private void poll(KafkaConsumer<String, String> consumer) {
        consumer.subscribe(getTopics());

        List<ConsumerRecords<String, String>> buffer = null;

        while (!ThreadUtils.isShuttingDown() && !Thread.currentThread().isInterrupted()) {
            try {
                var records = consumer.poll(pollTimeout);
                if (records == null || records.isEmpty())
                    continue;

                if (buffer == null)
                    buffer = new LinkedList<>();
                buffer.add(records);

                if (!isDelaying()) {
                    try {
                        var list = new LinkedList<Record<String, ConsumerRecord<String, String>>>();
                        for (var batch : buffer) {
                            for (var _record : batch) {
                                list.add(Record.<String, ConsumerRecord<String, String>>builder() //
                                        .key(_record.topic()) //
                                        .value(_record) //
                                        .build());
                            }
                        }
                        publish(list);
                    } catch (Exception e) {
                        getLogger().error("Error when processing records", e);
                    } finally {
                        buffer = null;
                    }
                }

                consumer.commitSync();
            } catch (CommitFailedException e) {
                throw e;
            } catch (Exception e) {
                getLogger().error("Kafker error", e);
            }
        }
    }

    private void poll() {
        while (!ThreadUtils.isShuttingDown() && !Thread.currentThread().isInterrupted()) {
            try (var consumer = new KafkaConsumer<String, String>(properties)) {
                poll(consumer);
                return;
            } catch (CommitFailedException e) {
                getLogger().warn("Commit failed, try to restart kafka consumer", e);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        getLogger().debug("Starting kafker...");
        pollingThread.start();
        getLogger().debug("Kafker started");
    }

    @Override
    protected void onStop() {
        super.onStop();
        pollingThread.interrupt();
    }
}

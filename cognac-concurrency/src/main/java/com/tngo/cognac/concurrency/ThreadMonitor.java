package com.tngo.cognac.concurrency;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.cliffc.high_scale_lib.NonBlockingHashMap;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.onSpinWait;

@Slf4j
public class ThreadMonitor {

    @FunctionalInterface
    public interface ShutdownTaskDisposable {
        boolean dispose();
    }

    private static final AtomicBoolean SHUTTING_DOWN_FLAG = new AtomicBoolean(false);
    private static final AtomicInteger SHUTDOWN_TASK_COUNTER = new AtomicInteger(0);
    private static final Map<Integer, List<Runnable>> SHUTDOWN_TASKS = new NonBlockingHashMap<>();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(ThreadMonitor::doShutdown, "SHUTDOWN HOOK"));
    }

    protected static void doShutdown() {
        SHUTTING_DOWN_FLAG.set(true);

        if (SHUTDOWN_TASKS.size() == 0)
            return;

        var remaining = new LinkedList<Integer>();
        var list = new ArrayList<>(SHUTDOWN_TASKS.keySet());

        list.sort(Comparator.comparingInt(i -> i));

        for (Integer order : list) {
            if (order >= 0) {
                runShutdownTasks(order);
            } else {
                remaining.add(0, order);
            }
        }

        for (int order : remaining) {
            runShutdownTasks(order);
        }
    }

    private static void runShutdownTasks(int order) {
        var tasks = SHUTDOWN_TASKS.get(order);
        if (tasks == null) {
            return;
        }

        for (var task : tasks) {
            try {
                task.run();
            } catch (Exception e) {
                log.error("Error while trying to run shutdown task", e);
            }
        }
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ThreadException("Interrupted while sleeping", e);
        }
    }

    /**
     * Stop current thread using Thread.onBusySpin() calling inside a while loop
     * <br>
     * Break if process/currentThread shutdown or breakSignal return true
     *
     * @param continueUntilFalse continue spin when return true, break loop and
     *                           return when false
     */
    public static void busySpin(BooleanSupplier continueUntilFalse) {
        while (continueUntilFalse.getAsBoolean()) {
            if (isShuttingDown() || currentThread().isInterrupted())
                break;
            onSpinWait();
        }
    }

    /**
     * Return current process's state
     *
     * @return true if process is shutting down, false otherwise
     */
    public static boolean isShuttingDown() {
        return SHUTTING_DOWN_FLAG.get();
    }

    /**
     * register a task which can be processed when process shutdown
     *
     * @param task the task to be registered
     * @return disposable object
     */
    public static ShutdownTaskDisposable registerShutdownTask(@NonNull Runnable task) {
        return registerShutdownTask(task, SHUTDOWN_TASK_COUNTER.incrementAndGet());
    }

    public static ShutdownTaskDisposable registerShutdownTask(@NonNull Runnable task, int order) {
        if (isShuttingDown()) {
            return null;
        }

        SHUTDOWN_TASKS.computeIfAbsent(order, key -> new CopyOnWriteArrayList<>()).add(task);
        return () -> {
            if (isShuttingDown()) {
                return false;
            }

            var tasks = SHUTDOWN_TASKS.get(order);
            if (tasks == null) {
                return false;
            }

            return tasks.remove(task);
        };
    }
}

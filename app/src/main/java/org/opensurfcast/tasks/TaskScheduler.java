package org.opensurfcast.tasks;

import org.opensurfcast.log.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * Generic task scheduler for background task execution.
 * <p>
 * Manages task execution, ensures one-at-a-time execution per task key,
 * enforces cooldown periods between task runs, and provides callbacks
 * for task lifecycle events.
 * <p>
 * All public methods must be called on the main thread (or equivalent
 * synchronization context provided via {@code mainThreadExecutor}).
 */
public class TaskScheduler {
    private final ExecutorService executor;
    private final Consumer<Runnable> mainThreadExecutor;
    private final Map<String, Task> runningTasks;
    private final List<TaskListener> listeners;
    private final TaskCooldowns cooldowns;
    private final Logger logger;

    /**
     * Creates a new task scheduler.
     *
     * @param executor              executor for running tasks in the background
     * @param mainThreadExecutor    consumer that posts runnables to the main thread
     * @param cooldowns             task cooldown manager
     * @param logger                logger for lifecycle events
     */
    public TaskScheduler(
            ExecutorService executor,
            Consumer<Runnable> mainThreadExecutor,
            TaskCooldowns cooldowns,
            Logger logger) {
        this.executor = executor;
        this.mainThreadExecutor = mainThreadExecutor;
        this.runningTasks = new HashMap<>();
        this.listeners = new ArrayList<>();
        this.cooldowns = cooldowns;
        this.logger = logger;
    }

    /**
     * Submits a task for execution.
     * <p>
     * The task is ignored if:
     * - A task with the same key is already running
     * - The task is still within its cooldown period
     * <p>
     * The task is wrapped with completion handling logic before execution.
     * <p>
     * Must be called on the main thread.
     *
     * @param task the task to execute
     */
    public void submit(Task task) {
        String key = task.getKey();

        if (runningTasks.containsKey(key)) {
            logger.debug("[" + key + "] Task ignored (already running)");
            return;
        }

        if (cooldowns.isOnCooldown(task)) {
            logger.debug("[" + key + "] Task ignored (on cooldown)");
            return;
        }

        logger.debug("[" + key + "] Task submitted");
        runningTasks.put(key, task);
        notifyTaskStarted(task);

        // Wrap task with completion handling
        executor.submit(() -> {
            Exception error = null;
            Object result = null;
            try {
                result = task.call();
            } catch (Exception e) {
                logger.error("[" + key + "] Task failed", e);
                error = e;
            }

            // Post completion to main thread
            final Exception finalError = error;
            final Object finalResult = result;
            mainThreadExecutor.accept(() -> {
                runningTasks.remove(key);

                if (finalError != null) {
                    notifyTaskFailed(task, finalError);
                } else {
                    cooldowns.recordCompletion(task);
                    notifyTaskCompleted(task, finalResult);
                }
            });
        });
    }

    /**
     * Returns the set of currently running tasks.
     * <p>
     * Must be called on the main thread.
     * Task keys are opaque; use {@code instanceof} on returned tasks to identify types.
     *
     * @return immutable copy of running tasks
     */
    public Collection<Task> getRunningTasks() {
        return new ArrayList<>(runningTasks.values());
    }

    /**
     * Returns true if a task with the same key as the given task is currently running.
     * <p>
     * Must be called on the main thread.
     *
     * @param task the task to check (by key)
     * @return true if a task with that key is running
     */
    public boolean isRunning(Task task) {
        return runningTasks.containsKey(task.getKey());
    }

    /**
     * Returns the cooldown manager for this scheduler.
     * <p>
     * Use to check or modify cooldown periods.
     *
     * @return the task cooldowns manager
     */
    public TaskCooldowns getCooldowns() {
        return cooldowns;
    }

    /**
     * Registers a listener for task events.
     *
     * @param listener the listener to register
     */
    public void addListener(TaskListener listener) {
        listeners.add(listener);
    }

    /**
     * Unregisters a listener.
     *
     * @param listener the listener to unregister
     */
    public void removeListener(TaskListener listener) {
        listeners.remove(listener);
    }

    private void notifyTaskStarted(Task task) {
        logger.debug("[" + task.getKey() + "] Task started");
        for (TaskListener listener : listeners) {
            listener.onTaskStarted(task);
        }
    }

    private void notifyTaskCompleted(Task task, Object result) {
        logger.debug("[" + task.getKey() + "] Task completed");
        for (TaskListener listener : listeners) {
            listener.onTaskCompleted(task, result);
        }
    }

    private void notifyTaskFailed(Task task, Exception error) {
        logger.debug("[" + task.getKey() + "] Task failed (notifying listeners)");
        for (TaskListener listener : listeners) {
            listener.onTaskFailed(task, error);
        }
    }

    /**
     * Shuts down the scheduler.
     * <p>
     * Call this in Activity onDestroy to clean up resources.
     * After shutdown, no new tasks can be submitted.
     */
    public void shutdown() {
        logger.debug("TaskScheduler shutdown");
        executor.shutdown();
    }
}

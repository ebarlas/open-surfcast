package org.opensurfcast.tasks;

import java.time.Duration;

/**
 * Abstract base class for all background tasks.
 * <p>
 * Provides common implementation for task metadata.
 * Subclasses implement the {@link #execute()} method to perform the actual work.
 */
public abstract class BaseTask implements Task {
    private final String key;
    private final Duration cooldownPeriod;

    /**
     * Creates a new task.
     *
     * @param key            unique key for deduplication and cooldown tracking
     * @param cooldownPeriod minimum time before task can run again
     */
    protected BaseTask(String key, Duration cooldownPeriod) {
        this.key = key;
        this.cooldownPeriod = cooldownPeriod;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public Duration getCooldownPeriod() {
        return cooldownPeriod;
    }

    @Override
    public final void run() {
        try {
            execute();
        } catch (Exception e) {
            // Exception will be caught by TaskScheduler's wrapper
            throw new RuntimeException(e);
        }
    }

    /**
     * Subclasses implement the actual work here.
     * <p>
     * Called on a background thread.
     * Should fetch data from APIs and save to database.
     *
     * @throws Exception if an error occurs during execution
     */
    protected abstract void execute() throws Exception;
}

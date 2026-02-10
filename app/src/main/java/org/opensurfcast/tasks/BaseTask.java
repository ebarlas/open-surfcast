package org.opensurfcast.tasks;

import java.time.Duration;

/**
 * Abstract base class for all background tasks.
 * <p>
 * Provides common implementation for task metadata.
 * The task key defaults to the class name plus an optional suffix from
 * {@link #getKeySuffix()} (e.g. station ID). Subclasses implement
 * {@link #execute()} to perform the actual work.
 */
public abstract class BaseTask implements Task {
    private final Duration cooldownPeriod;

    /**
     * Creates a new task with a key derived from the class name and optional suffix.
     *
     * @param cooldownPeriod minimum time before task can run again
     */
    protected BaseTask(Duration cooldownPeriod) {
        this.cooldownPeriod = cooldownPeriod;
    }

    /**
     * Returns optional material to append to the class name in the key (e.g. station ID).
     * Override in subclasses that need per-instance keys.
     *
     * @return suffix to append after a colon, or null for no suffix
     */
    protected String getKeySuffix() {
        return null;
    }

    @Override
    public String getKey() {
        String suffix = getKeySuffix();
        return getClass().getSimpleName()
                + (suffix != null && !suffix.isEmpty() ? ":" + suffix : "");
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

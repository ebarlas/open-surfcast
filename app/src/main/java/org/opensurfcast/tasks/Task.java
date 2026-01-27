package org.opensurfcast.tasks;

import java.time.Duration;

/**
 * Interface for background tasks.
 * <p>
 * All tasks are Runnables that can be submitted to an executor.
 * Each task has a unique key for deduplication and a cooldown period.
 */
public interface Task extends Runnable {
    /**
     * Returns a unique key for this task instance.
     * <p>
     * Used to prevent duplicate concurrent execution and track cooldowns.
     *
     * @return unique key identifying this task instance
     */
    String getKey();

    /**
     * Returns the cooldown period for this task.
     * <p>
     * After successful completion, the task cannot run again until
     * the cooldown period has elapsed. Persists across app launches.
     *
     * @return cooldown period, or {@link Duration#ZERO} for no cooldown
     */
    Duration getCooldownPeriod();
}

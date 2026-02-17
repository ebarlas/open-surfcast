package org.opensurfcast.tasks;

import java.time.Duration;
import java.util.concurrent.Callable;

/**
 * Interface for background tasks.
 * <p>
 * Each task has a unique key for deduplication and a cooldown period.
 * Execution via {@link #call()} returns a result (or null) that the
 * scheduler passes to {@link TaskListener#onTaskCompleted(Task, Object)}.
 */
public interface Task extends Callable<Object> {

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

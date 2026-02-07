package org.opensurfcast.tasks;

/**
 * Callback interface for task lifecycle events.
 * <p>
 * All callbacks are invoked on the main thread, making them safe
 * for UI updates.
 */
public interface TaskListener {
    /**
     * Called when a task starts execution.
     * <p>
     * Invoked on the main thread.
     *
     * @param task the task that started
     */
    default void onTaskStarted(Task task) {
    }

    /**
     * Called when a task completes successfully.
     * <p>
     * Invoked on the main thread.
     *
     * @param task the task that completed
     */
    default void onTaskCompleted(Task task) {
    }

    /**
     * Called when a task fails with an error.
     * <p>
     * Invoked on the main thread.
     *
     * @param task  the task that failed
     * @param error the exception that caused the failure
     */
    default void onTaskFailed(Task task, Exception error) {
    }
}

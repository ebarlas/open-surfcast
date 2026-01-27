package org.opensurfcast.tasks;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.Duration;

/**
 * Tracks task completion times using SharedPreferences.
 * <p>
 * Used to enforce cooldown periods between task runs.
 * Each task defines its own cooldown period via {@link Task#getCooldownPeriod()}.
 * Completion timestamps persist across app launches.
 */
public class TaskCooldowns {
    private static final String PREFS_NAME = "task_cooldowns";
    private static final String KEY_PREFIX = "last_completed_";

    private final SharedPreferences prefs;

    /**
     * Creates a new TaskCooldowns instance.
     *
     * @param context application context
     */
    public TaskCooldowns(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Protected constructor for testing subclasses.
     */
    protected TaskCooldowns() {
        this.prefs = null;
    }

    /**
     * Checks if a task is currently in its cooldown period.
     *
     * @param task the task to check
     * @return true if the task cannot run yet due to cooldown
     */
    public boolean isOnCooldown(Task task) {
        Duration cooldownPeriod = task.getCooldownPeriod();
        if (cooldownPeriod.isZero()) {
            return false; // No cooldown configured
        }

        long lastCompleted = getLastCompletedTime(task.getKey());
        if (lastCompleted == 0) {
            return false; // Never completed
        }

        long elapsedMillis = System.currentTimeMillis() - lastCompleted;
        return elapsedMillis < cooldownPeriod.toMillis();
    }

    /**
     * Returns the remaining cooldown time for a task.
     *
     * @param task the task to check
     * @return remaining cooldown, or {@link Duration#ZERO} if not on cooldown
     */
    public Duration getRemainingCooldown(Task task) {
        Duration cooldownPeriod = task.getCooldownPeriod();
        if (cooldownPeriod.isZero()) {
            return Duration.ZERO;
        }

        long lastCompleted = getLastCompletedTime(task.getKey());
        if (lastCompleted == 0) {
            return Duration.ZERO;
        }

        long elapsedMillis = System.currentTimeMillis() - lastCompleted;
        long remainingMillis = cooldownPeriod.toMillis() - elapsedMillis;
        return remainingMillis > 0 ? Duration.ofMillis(remainingMillis) : Duration.ZERO;
    }

    /**
     * Records that a task has completed successfully.
     * <p>
     * Should only be called for successful completions, not failures.
     *
     * @param task the task that completed
     */
    public void recordCompletion(Task task) {
        prefs.edit()
                .putLong(KEY_PREFIX + task.getKey(), System.currentTimeMillis())
                .apply();
    }

    /**
     * Clears the completion record for a task, allowing it to run immediately.
     *
     * @param taskKey the task key to clear
     */
    public void clearCooldown(String taskKey) {
        prefs.edit()
                .remove(KEY_PREFIX + taskKey)
                .apply();
    }

    /**
     * Clears all completion records.
     */
    public void clearAllCooldowns() {
        prefs.edit().clear().apply();
    }

    private long getLastCompletedTime(String taskKey) {
        return prefs.getLong(KEY_PREFIX + taskKey, 0);
    }
}

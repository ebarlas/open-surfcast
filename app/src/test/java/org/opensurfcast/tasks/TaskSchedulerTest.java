package org.opensurfcast.tasks;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

public class TaskSchedulerTest {
    private ExecutorService executor;
    private TaskScheduler scheduler;
    private StubCooldowns cooldowns;
    private RecordingListener listener;

    @Before
    public void setUp() {
        executor = Executors.newSingleThreadExecutor();
        cooldowns = new StubCooldowns();
        scheduler = new TaskScheduler(executor, Runnable::run, cooldowns);
        listener = new RecordingListener();
        scheduler.addListener(listener);
    }

    @After
    public void tearDown() {
        scheduler.shutdown();
    }

    @Test
    public void submit_executesTask() throws InterruptedException {
        TestTask task = new TestTask();
        scheduler.submit(task);
        waitForCompletion();

        assertTrue(task.executed);
        assertTrue(listener.startedKeys.contains("TestTask"));
        assertTrue(listener.completedKeys.contains("TestTask"));
    }

    @Test
    public void submit_ignoresDuplicateKey() throws InterruptedException {
        TestTask task1 = new TestTask();
        TestTask task2 = new TestTask();

        scheduler.submit(task1);
        scheduler.submit(task2); // Same key, should be ignored

        waitForCompletion();

        assertTrue(task1.executed);
        assertFalse(task2.executed);
    }

    @Test
    public void submit_ignoresTaskOnCooldown() throws InterruptedException {
        cooldowns.onCooldownKeys.add("TestTask");
        TestTask task = new TestTask();

        scheduler.submit(task);
        waitForCompletion();

        assertFalse(task.executed);
        assertFalse(listener.startedKeys.contains("TestTask"));
    }

    @Test
    public void submit_recordsCompletionOnSuccess() throws InterruptedException {
        TestTask task = new TestTask();
        scheduler.submit(task);
        waitForCompletion();

        assertTrue(cooldowns.recordedKeys.contains("TestTask"));
    }

    @Test
    public void submit_notifiesFailureOnException() throws InterruptedException {
        FailingTask task = new FailingTask();
        scheduler.submit(task);
        waitForCompletion();

        assertTrue(listener.startedKeys.contains("FailingTask"));
        assertTrue(listener.failedKeys.contains("FailingTask"));
        assertFalse(listener.completedKeys.contains("FailingTask"));
        assertFalse(cooldowns.recordedKeys.contains("FailingTask"));
    }

    @Test
    public void isRunning_returnsTrueWhileRunning() throws InterruptedException {
        BlockingTask task = new BlockingTask();
        scheduler.submit(task);

        Thread.sleep(50); // Let task start
        assertTrue(scheduler.isRunning(task));

        task.unblock();
        waitForCompletion();
        assertFalse(scheduler.isRunning(task));
    }

    private void waitForCompletion() throws InterruptedException {
        Thread.sleep(100);
    }

    // Test doubles

    static class TestTask extends BaseTask {
        volatile boolean executed = false;

        TestTask() {
            super(Duration.ZERO);
        }

        @Override
        protected void execute() {
            executed = true;
        }
    }

    static class BlockingTask extends BaseTask {
        volatile boolean executed = false;
        private final Object lock = new Object();
        private volatile boolean blocked = true;

        BlockingTask() {
            super(Duration.ZERO);
        }

        @Override
        protected void execute() throws InterruptedException {
            synchronized (lock) {
                while (blocked) {
                    lock.wait();
                }
            }
            executed = true;
        }

        void unblock() {
            synchronized (lock) {
                blocked = false;
                lock.notifyAll();
            }
        }
    }

    static class FailingTask extends BaseTask {
        FailingTask() {
            super(Duration.ZERO);
        }

        @Override
        protected void execute() throws Exception {
            throw new RuntimeException("Test failure");
        }
    }

    static class StubCooldowns extends TaskCooldowns {
        Set<String> onCooldownKeys = new HashSet<>();
        Set<String> recordedKeys = new HashSet<>();

        StubCooldowns() {
            super();
        }

        @Override
        public boolean isOnCooldown(Task task) {
            return onCooldownKeys.contains(task.getKey());
        }

        @Override
        public void recordCompletion(Task task) {
            recordedKeys.add(task.getKey());
        }
    }

    static class RecordingListener implements TaskListener {
        List<String> startedKeys = new ArrayList<>();
        List<String> completedKeys = new ArrayList<>();
        List<String> failedKeys = new ArrayList<>();

        @Override
        public void onTaskStarted(Task task) {
            startedKeys.add(task.getKey());
        }

        @Override
        public void onTaskCompleted(Task task) {
            completedKeys.add(task.getKey());
        }

        @Override
        public void onTaskFailed(Task task, Exception error) {
            failedKeys.add(task.getKey());
        }
    }
}

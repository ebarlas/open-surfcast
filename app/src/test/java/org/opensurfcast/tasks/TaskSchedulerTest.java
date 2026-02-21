package org.opensurfcast.tasks;

import org.opensurfcast.log.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
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
        scheduler = new TaskScheduler(executor, Runnable::run, cooldowns, NoOpLogger.INSTANCE);
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
        BlockingTestTask task1 = new BlockingTestTask(); // blocks until we release, so it can't finish before submit(task2)
        TestTask task2 = new TestTask();

        scheduler.submit(task1);
        task1.awaitStarted();
        scheduler.submit(task2); // Same key, must be ignored while task1 is still running
        task1.release();
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

    static class NoOpLogger implements Logger {
        static final NoOpLogger INSTANCE = new NoOpLogger();

        @Override
        public void debug(String message) {}

        @Override
        public void info(String message) {}

        @Override
        public void warn(String message) {}

        @Override
        public void error(String message) {}

        @Override
        public void error(String message, Throwable throwable) {}
    }

    static class TestTask extends BaseTask {
        volatile boolean executed = false;

        TestTask() {
            super(Duration.ZERO);
        }

        @Override
        public Object call() {
            executed = true;
            return null;
        }
    }

    /** Blocks in call() until release(); key "TestTask". Cannot finish before test submits duplicate. */
    static class BlockingTestTask extends BaseTask {
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        volatile boolean executed = false;

        BlockingTestTask() {
            super(Duration.ZERO);
        }

        @Override
        public String getKey() {
            return "TestTask";
        }

        void awaitStarted() throws InterruptedException {
            started.await();
        }

        void release() {
            release.countDown();
        }

        @Override
        public Object call() throws InterruptedException {
            started.countDown();
            release.await();
            executed = true;
            return null;
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
        public Object call() throws InterruptedException {
            synchronized (lock) {
                while (blocked) {
                    lock.wait();
                }
            }
            executed = true;
            return null;
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
        public Object call() throws Exception {
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

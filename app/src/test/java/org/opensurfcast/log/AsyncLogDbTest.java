package org.opensurfcast.log;

import org.junit.Before;
import org.junit.Test;
import org.opensurfcast.db.LogDb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AsyncLogDbTest {

    private StubLogDb stubLogDb;
    private AsyncLogDb asyncLogDb;

    @Before
    public void setUp() {
        stubLogDb = new StubLogDb();
        // Use immediate executor for synchronous testing
        asyncLogDb = new AsyncLogDb(stubLogDb, new ImmediateExecutorService());
    }

    @Test
    public void insert_delegatesToLogDb() {
        LogEntry entry = new LogEntry(1000L, LogLevel.INFO, "Test", null);

        asyncLogDb.insert(entry);

        assertEquals(1, stubLogDb.insertedEntries.size());
        assertEquals(entry, stubLogDb.insertedEntries.get(0));
    }

    @Test
    public void getAll_returnsFromLogDb() throws ExecutionException, InterruptedException {
        LogEntry entry = new LogEntry(1, 1000L, LogLevel.INFO, "Test", null);
        stubLogDb.entries.add(entry);

        CompletableFuture<List<LogEntry>> future = asyncLogDb.getAll();

        assertEquals(1, future.get().size());
        assertEquals(entry, future.get().get(0));
    }

    @Test
    public void getRecent_passesLimitToLogDb() throws ExecutionException, InterruptedException {
        stubLogDb.entries.add(new LogEntry(1, 1000L, LogLevel.INFO, "One", null));
        stubLogDb.entries.add(new LogEntry(2, 2000L, LogLevel.INFO, "Two", null));

        asyncLogDb.getRecent(5);

        assertEquals(5, stubLogDb.lastRecentLimit);
    }

    @Test
    public void getByMinLevel_passesLevelToLogDb() throws ExecutionException, InterruptedException {
        asyncLogDb.getByMinLevel(LogLevel.WARN);

        assertEquals(LogLevel.WARN, stubLogDb.lastMinLevel);
    }

    @Test
    public void deleteOlderThan_delegatesToLogDb() {
        asyncLogDb.deleteOlderThan(5000L);

        assertEquals(5000L, stubLogDb.lastDeleteTimestamp);
    }

    @Test
    public void deleteAll_delegatesToLogDb() {
        asyncLogDb.deleteAll();

        assertTrue(stubLogDb.deleteAllCalled);
    }

    // Test double

    static class StubLogDb extends LogDb {
        List<LogEntry> insertedEntries = new ArrayList<>();
        List<LogEntry> entries = new ArrayList<>();
        int lastRecentLimit = -1;
        LogLevel lastMinLevel = null;
        long lastDeleteTimestamp = -1;
        boolean deleteAllCalled = false;

        StubLogDb() {
            super(null);
        }

        @Override
        public void insert(LogEntry entry) {
            insertedEntries.add(entry);
        }

        @Override
        public List<LogEntry> getAll() {
            return new ArrayList<>(entries);
        }

        @Override
        public List<LogEntry> getRecent(int limit) {
            lastRecentLimit = limit;
            return new ArrayList<>(entries);
        }

        @Override
        public List<LogEntry> getByMinLevel(LogLevel minLevel) {
            lastMinLevel = minLevel;
            return new ArrayList<>(entries);
        }

        @Override
        public void deleteOlderThan(long timestamp) {
            lastDeleteTimestamp = timestamp;
        }

        @Override
        public void deleteAll() {
            deleteAllCalled = true;
        }
    }

    static class ImmediateExecutorService extends AbstractExecutorService {
        @Override
        public void execute(Runnable command) {
            command.run();
        }

        @Override
        public void shutdown() {
        }

        @Override
        public List<Runnable> shutdownNow() {
            return Collections.emptyList();
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }
    }
}

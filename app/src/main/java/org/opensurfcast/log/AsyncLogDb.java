package org.opensurfcast.log;

import org.opensurfcast.db.LogDb;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Asynchronous wrapper around {@link LogDb} for use on the UI thread.
 * <p>
 * Inserts are fire-and-forget (non-blocking). Queries return {@link CompletableFuture}
 * for async consumption.
 */
public class AsyncLogDb {

    private final LogDb logDb;
    private final ExecutorService executor;

    public AsyncLogDb(LogDb logDb, ExecutorService executor) {
        this.logDb = logDb;
        this.executor = executor;
    }

    /**
     * Inserts a log entry asynchronously (fire-and-forget).
     */
    public void insert(LogEntry entry) {
        executor.execute(() -> logDb.insert(entry));
    }

    /**
     * Returns all log entries asynchronously.
     */
    public CompletableFuture<List<LogEntry>> getAll() {
        return CompletableFuture.supplyAsync(logDb::getAll, executor);
    }

    /**
     * Returns the most recent log entries asynchronously.
     *
     * @param limit maximum number of entries to return
     */
    public CompletableFuture<List<LogEntry>> getRecent(int limit) {
        return CompletableFuture.supplyAsync(() -> logDb.getRecent(limit), executor);
    }

    /**
     * Returns log entries at or above the specified level asynchronously.
     *
     * @param minLevel minimum log level to include
     */
    public CompletableFuture<List<LogEntry>> getByMinLevel(LogLevel minLevel) {
        return CompletableFuture.supplyAsync(() -> logDb.getByMinLevel(minLevel), executor);
    }

    /**
     * Returns log entries matching the search query asynchronously.
     *
     * @param query search text (matched in message and stack trace)
     * @param limit maximum number of entries to return
     */
    public CompletableFuture<List<LogEntry>> search(String query, int limit) {
        return CompletableFuture.supplyAsync(() -> logDb.search(query, limit), executor);
    }

    /**
     * Returns log entries matching the search query at or above the specified level asynchronously.
     *
     * @param query search text (matched in message and stack trace)
     * @param minLevel minimum log level to include, or null for all levels
     * @param limit maximum number of entries to return
     */
    public CompletableFuture<List<LogEntry>> search(String query, LogLevel minLevel, int limit) {
        return CompletableFuture.supplyAsync(() -> logDb.search(query, minLevel, limit), executor);
    }

    /**
     * Deletes log entries older than the specified timestamp asynchronously.
     */
    public void deleteOlderThan(long timestamp) {
        executor.execute(() -> logDb.deleteOlderThan(timestamp));
    }

    /**
     * Deletes all log entries asynchronously.
     */
    public void deleteAll() {
        executor.execute(logDb::deleteAll);
    }
}

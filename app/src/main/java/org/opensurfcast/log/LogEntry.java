package org.opensurfcast.log;

/**
 * Represents a single log entry.
 */
public class LogEntry {

    private final long id;
    private final long timestamp;
    private final LogLevel level;
    private final String message;
    private final String stackTrace;

    /**
     * Creates a new log entry without an ID (for inserts).
     */
    public LogEntry(long timestamp, LogLevel level, String message, String stackTrace) {
        this(0, timestamp, level, message, stackTrace);
    }

    /**
     * Creates a log entry with all fields (for reads from database).
     */
    public LogEntry(long id, long timestamp, LogLevel level, String message, String stackTrace) {
        this.id = id;
        this.timestamp = timestamp;
        this.level = level;
        this.message = message;
        this.stackTrace = stackTrace;
    }

    public long getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public LogLevel getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    /**
     * Returns the stack trace, or null if not present.
     */
    public String getStackTrace() {
        return stackTrace;
    }
}

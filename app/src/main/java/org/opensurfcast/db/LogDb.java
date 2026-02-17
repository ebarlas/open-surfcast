package org.opensurfcast.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.opensurfcast.log.LogEntry;
import org.opensurfcast.log.LogLevel;

import java.util.List;

/**
 * Database operations for log entries.
 */
public class LogDb {

    private final OpenSurfcastDbHelper dbHelper;

    public LogDb(OpenSurfcastDbHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * Inserts a log entry.
     */
    public void insert(LogEntry entry) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.insert("logs", null, toContentValues(entry));
    }

    /**
     * Returns all log entries, ordered by timestamp descending (newest first).
     */
    public List<LogEntry> getAll() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                "logs",
                null,
                null,
                null,
                null, null,
                "timestamp DESC")) {
            return SqlUtils.map(cursor, this::fromCursor);
        }
    }

    /**
     * Returns the most recent log entries.
     *
     * @param limit maximum number of entries to return
     */
    public List<LogEntry> getRecent(int limit) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                "logs",
                null,
                null,
                null,
                null, null,
                "timestamp DESC",
                String.valueOf(limit))) {
            return SqlUtils.map(cursor, this::fromCursor);
        }
    }

    /**
     * Returns log entries matching the search query in message or stack trace.
     *
     * @param query search text (matched via SQL LIKE, case-insensitive)
     * @param limit maximum number of entries to return
     */
    public List<LogEntry> search(String query, int limit) {
        return search(query, null, limit);
    }

    /**
     * Returns log entries matching the search query at or above the specified level.
     *
     * @param query search text (matched via SQL LIKE, case-insensitive)
     * @param minLevel minimum log level to include, or null for all levels
     * @param limit maximum number of entries to return
     */
    public List<LogEntry> search(String query, LogLevel minLevel, int limit) {
        String pattern = "%" + escapeLikeWildcards(query) + "%";
        String searchClause =
                "message LIKE ? ESCAPE '\\' OR (stack_trace IS NOT NULL AND stack_trace LIKE ? ESCAPE '\\')";
        String whereClause;
        String[] whereArgs;

        if (minLevel != null) {
            String[] levels = getLevelsAtOrAbove(minLevel);
            String placeholders = SqlUtils.buildPlaceholders(levels.length);
            whereClause = "(" + searchClause + ") AND level IN (" + placeholders + ")";
            whereArgs = new String[2 + levels.length];
            whereArgs[0] = pattern;
            whereArgs[1] = pattern;
            System.arraycopy(levels, 0, whereArgs, 2, levels.length);
        } else {
            whereClause = searchClause;
            whereArgs = new String[]{pattern, pattern};
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                "logs",
                null,
                whereClause,
                whereArgs,
                null, null,
                "timestamp DESC",
                String.valueOf(limit))) {
            return SqlUtils.map(cursor, this::fromCursor);
        }
    }

    /**
     * Returns log entries at or above the specified level.
     *
     * @param minLevel minimum log level to include
     */
    public List<LogEntry> getByMinLevel(LogLevel minLevel) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] levels = getLevelsAtOrAbove(minLevel);
        String placeholders = SqlUtils.buildPlaceholders(levels.length);
        try (Cursor cursor = db.query(
                "logs",
                null,
                "level IN (" + placeholders + ")",
                levels,
                null, null,
                "timestamp DESC")) {
            return SqlUtils.map(cursor, this::fromCursor);
        }
    }

    /**
     * Deletes log entries older than the specified timestamp.
     *
     * @param timestamp entries with timestamps before this value will be deleted
     */
    public void deleteOlderThan(long timestamp) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("logs", "timestamp < ?", new String[]{String.valueOf(timestamp)});
    }

    /**
     * Deletes all log entries.
     */
    public void deleteAll() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("logs", null, null);
    }

    private ContentValues toContentValues(LogEntry entry) {
        ContentValues values = new ContentValues();
        values.put("timestamp", entry.getTimestamp());
        values.put("level", entry.getLevel().name());
        values.put("message", entry.getMessage());
        if (entry.getStackTrace() != null) {
            values.put("stack_trace", entry.getStackTrace());
        }
        return values;
    }

    private LogEntry fromCursor(Cursor cursor) {
        return new LogEntry(
                cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
                LogLevel.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("level"))),
                cursor.getString(cursor.getColumnIndexOrThrow("message")),
                cursor.getString(cursor.getColumnIndexOrThrow("stack_trace"))
        );
    }

    private String[] getLevelsAtOrAbove(LogLevel minLevel) {
        LogLevel[] all = LogLevel.values();
        int startIndex = minLevel.ordinal();
        String[] result = new String[all.length - startIndex];
        for (int i = startIndex; i < all.length; i++) {
            result[i - startIndex] = all[i].name();
        }
        return result;
    }

    private static String escapeLikeWildcards(String input) {
        if (input == null) return "";
        return input
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}

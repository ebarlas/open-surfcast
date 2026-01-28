package org.opensurfcast.log;

import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Application logger that persists logs to the database and forwards to logcat.
 */
public class AppLogger implements Logger {

    private static final String TAG = "OpenSurfcast";

    private final AsyncLogDb asyncLogDb;

    public AppLogger(AsyncLogDb asyncLogDb) {
        this.asyncLogDb = asyncLogDb;
    }

    @Override
    public void debug(String message) {
        log(LogLevel.DEBUG, message, null);
    }

    @Override
    public void info(String message) {
        log(LogLevel.INFO, message, null);
    }

    @Override
    public void warn(String message) {
        log(LogLevel.WARN, message, null);
    }

    @Override
    public void error(String message) {
        log(LogLevel.ERROR, message, null);
    }

    @Override
    public void error(String message, Throwable throwable) {
        log(LogLevel.ERROR, message, throwable);
    }

    private void log(LogLevel level, String message, Throwable throwable) {
        String stackTrace = throwable != null ? getStackTrace(throwable) : null;
        LogEntry entry = new LogEntry(System.currentTimeMillis(), level, message, stackTrace);
        asyncLogDb.insert(entry);
        Log.println(toPriority(level), TAG, formatLogcatMessage(message, throwable));
    }

    private static String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private static String formatLogcatMessage(String message, Throwable throwable) {
        if (throwable == null) {
            return message;
        }
        return message + "\n" + getStackTrace(throwable);
    }

    private static int toPriority(LogLevel level) {
        return switch (level) {
            case DEBUG -> Log.DEBUG;
            case INFO -> Log.INFO;
            case WARN -> Log.WARN;
            case ERROR -> Log.ERROR;
        };
    }
}

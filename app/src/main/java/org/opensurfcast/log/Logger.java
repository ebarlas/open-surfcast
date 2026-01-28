package org.opensurfcast.log;

/**
 * Logging interface for application-wide logging.
 */
public interface Logger {

    void debug(String message);

    void info(String message);

    void warn(String message);

    void error(String message);

    void error(String message, Throwable throwable);
}

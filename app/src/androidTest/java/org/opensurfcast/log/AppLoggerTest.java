package org.opensurfcast.log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class AppLoggerTest {

    private RecordingAsyncLogDb recordingDb;
    private AppLogger logger;

    @Before
    public void setUp() {
        recordingDb = new RecordingAsyncLogDb();
        logger = new AppLogger(recordingDb);
    }

    @Test
    public void debug_logsWithDebugLevel() {
        logger.debug("Debug message");

        assertEquals(1, recordingDb.entries.size());
        assertEquals(LogLevel.DEBUG, recordingDb.entries.get(0).getLevel());
        assertEquals("Debug message", recordingDb.entries.get(0).getMessage());
    }

    @Test
    public void info_logsWithInfoLevel() {
        logger.info("Info message");

        assertEquals(1, recordingDb.entries.size());
        assertEquals(LogLevel.INFO, recordingDb.entries.get(0).getLevel());
        assertEquals("Info message", recordingDb.entries.get(0).getMessage());
    }

    @Test
    public void warn_logsWithWarnLevel() {
        logger.warn("Warn message");

        assertEquals(1, recordingDb.entries.size());
        assertEquals(LogLevel.WARN, recordingDb.entries.get(0).getLevel());
        assertEquals("Warn message", recordingDb.entries.get(0).getMessage());
    }

    @Test
    public void error_logsWithErrorLevel() {
        logger.error("Error message");

        assertEquals(1, recordingDb.entries.size());
        assertEquals(LogLevel.ERROR, recordingDb.entries.get(0).getLevel());
        assertEquals("Error message", recordingDb.entries.get(0).getMessage());
    }

    @Test
    public void error_withThrowable_includesStackTrace() {
        RuntimeException exception = new RuntimeException("Test exception");

        logger.error("Error occurred", exception);

        assertEquals(1, recordingDb.entries.size());
        assertEquals(LogLevel.ERROR, recordingDb.entries.get(0).getLevel());
        assertEquals("Error occurred", recordingDb.entries.get(0).getMessage());
        assertNotNull(recordingDb.entries.get(0).getStackTrace());
        assertTrue(recordingDb.entries.get(0).getStackTrace().contains("RuntimeException"));
        assertTrue(recordingDb.entries.get(0).getStackTrace().contains("Test exception"));
    }

    @Test
    public void log_setsTimestamp() {
        long before = System.currentTimeMillis();
        logger.info("Test");
        long after = System.currentTimeMillis();

        long timestamp = recordingDb.entries.get(0).getTimestamp();
        assertTrue(timestamp >= before && timestamp <= after);
    }

    @Test
    public void log_withoutThrowable_hasNullStackTrace() {
        logger.info("No exception");

        assertNull(recordingDb.entries.get(0).getStackTrace());
    }

    // Test double

    static class RecordingAsyncLogDb extends AsyncLogDb {
        List<LogEntry> entries = new ArrayList<>();

        RecordingAsyncLogDb() {
            super(null, null);
        }

        @Override
        public void insert(LogEntry entry) {
            entries.add(entry);
        }
    }
}

package org.opensurfcast.db;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensurfcast.log.LogEntry;
import org.opensurfcast.log.LogLevel;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class LogDbTest {

    private OpenSurfcastDbHelper dbHelper;
    private LogDb logDb;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase("opensurfcast.db");
        dbHelper = new OpenSurfcastDbHelper(context);
        logDb = new LogDb(dbHelper);
    }

    @After
    public void tearDown() {
        dbHelper.close();
    }

    @Test
    public void insert_andGetAll_returnsEntry() {
        LogEntry entry = new LogEntry(1000L, LogLevel.INFO, "Test message", null);
        logDb.insert(entry);

        List<LogEntry> results = logDb.getAll();

        assertEquals(1, results.size());
        assertEquals(1000L, results.get(0).getTimestamp());
        assertEquals(LogLevel.INFO, results.get(0).getLevel());
        assertEquals("Test message", results.get(0).getMessage());
        assertNull(results.get(0).getStackTrace());
    }

    @Test
    public void insert_withStackTrace_persistsStackTrace() {
        LogEntry entry = new LogEntry(1000L, LogLevel.ERROR, "Error occurred", "java.lang.Exception\n\tat Test.method");
        logDb.insert(entry);

        List<LogEntry> results = logDb.getAll();

        assertEquals(1, results.size());
        assertEquals("java.lang.Exception\n\tat Test.method", results.get(0).getStackTrace());
    }

    @Test
    public void getAll_returnsNewestFirst() {
        logDb.insert(new LogEntry(1000L, LogLevel.INFO, "First", null));
        logDb.insert(new LogEntry(2000L, LogLevel.INFO, "Second", null));
        logDb.insert(new LogEntry(3000L, LogLevel.INFO, "Third", null));

        List<LogEntry> results = logDb.getAll();

        assertEquals(3, results.size());
        assertEquals("Third", results.get(0).getMessage());
        assertEquals("Second", results.get(1).getMessage());
        assertEquals("First", results.get(2).getMessage());
    }

    @Test
    public void getRecent_limitsResults() {
        logDb.insert(new LogEntry(1000L, LogLevel.INFO, "First", null));
        logDb.insert(new LogEntry(2000L, LogLevel.INFO, "Second", null));
        logDb.insert(new LogEntry(3000L, LogLevel.INFO, "Third", null));

        List<LogEntry> results = logDb.getRecent(2);

        assertEquals(2, results.size());
        assertEquals("Third", results.get(0).getMessage());
        assertEquals("Second", results.get(1).getMessage());
    }

    @Test
    public void getByMinLevel_filtersLowerLevels() {
        logDb.insert(new LogEntry(1000L, LogLevel.DEBUG, "Debug", null));
        logDb.insert(new LogEntry(2000L, LogLevel.INFO, "Info", null));
        logDb.insert(new LogEntry(3000L, LogLevel.WARN, "Warn", null));
        logDb.insert(new LogEntry(4000L, LogLevel.ERROR, "Error", null));

        List<LogEntry> results = logDb.getByMinLevel(LogLevel.WARN);

        assertEquals(2, results.size());
        assertEquals("Error", results.get(0).getMessage());
        assertEquals("Warn", results.get(1).getMessage());
    }

    @Test
    public void getByMinLevel_includesAllAtDebug() {
        logDb.insert(new LogEntry(1000L, LogLevel.DEBUG, "Debug", null));
        logDb.insert(new LogEntry(2000L, LogLevel.INFO, "Info", null));
        logDb.insert(new LogEntry(3000L, LogLevel.WARN, "Warn", null));
        logDb.insert(new LogEntry(4000L, LogLevel.ERROR, "Error", null));

        List<LogEntry> results = logDb.getByMinLevel(LogLevel.DEBUG);

        assertEquals(4, results.size());
    }

    @Test
    public void deleteOlderThan_removesOldEntries() {
        logDb.insert(new LogEntry(1000L, LogLevel.INFO, "Old", null));
        logDb.insert(new LogEntry(2000L, LogLevel.INFO, "Keep", null));
        logDb.insert(new LogEntry(3000L, LogLevel.INFO, "Keep too", null));

        logDb.deleteOlderThan(2000L);

        List<LogEntry> results = logDb.getAll();
        assertEquals(2, results.size());
        assertEquals("Keep too", results.get(0).getMessage());
        assertEquals("Keep", results.get(1).getMessage());
    }

    @Test
    public void deleteAll_removesAllEntries() {
        logDb.insert(new LogEntry(1000L, LogLevel.INFO, "One", null));
        logDb.insert(new LogEntry(2000L, LogLevel.INFO, "Two", null));

        logDb.deleteAll();

        List<LogEntry> results = logDb.getAll();
        assertTrue(results.isEmpty());
    }

    @Test
    public void insert_assignsAutoIncrementId() {
        logDb.insert(new LogEntry(1000L, LogLevel.INFO, "First", null));
        logDb.insert(new LogEntry(2000L, LogLevel.INFO, "Second", null));

        List<LogEntry> results = logDb.getAll();

        assertTrue(results.get(0).getId() > 0);
        assertTrue(results.get(1).getId() > 0);
        assertTrue(results.get(0).getId() != results.get(1).getId());
    }
}

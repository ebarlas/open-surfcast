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

    @Test
    public void search_matchesMessage_returnsMatchingEntries() {
        logDb.insert(new LogEntry(1000L, LogLevel.INFO, "Connection timeout", null));
        logDb.insert(new LogEntry(2000L, LogLevel.INFO, "Sync completed", null));
        logDb.insert(new LogEntry(3000L, LogLevel.INFO, "Connection refused", null));

        List<LogEntry> results = logDb.search("Connection", 10);

        assertEquals(2, results.size());
        assertTrue(results.get(0).getMessage().contains("Connection"));
        assertTrue(results.get(1).getMessage().contains("Connection"));
    }

    @Test
    public void search_matchesStackTrace_returnsMatchingEntries() {
        logDb.insert(new LogEntry(1000L, LogLevel.INFO, "OK", null));
        logDb.insert(new LogEntry(2000L, LogLevel.ERROR, "Failed", "java.net.UnknownHostException\n\tat Foo.bar"));

        List<LogEntry> results = logDb.search("UnknownHostException", 10);

        assertEquals(1, results.size());
        assertEquals("Failed", results.get(0).getMessage());
        assertTrue(results.get(0).getStackTrace().contains("UnknownHostException"));
    }

    @Test
    public void search_withLevelFilter_combinesSearchAndLevel() {
        logDb.insert(new LogEntry(1000L, LogLevel.DEBUG, "Error in debug", null));
        logDb.insert(new LogEntry(2000L, LogLevel.INFO, "Error in info", null));
        logDb.insert(new LogEntry(3000L, LogLevel.WARN, "Error in warn", null));
        logDb.insert(new LogEntry(4000L, LogLevel.ERROR, "Error in error", null));

        List<LogEntry> results = logDb.search("Error", LogLevel.WARN, 10);

        assertEquals(2, results.size());
        assertEquals("Error in error", results.get(0).getMessage());
        assertEquals("Error in warn", results.get(1).getMessage());
    }

    @Test
    public void search_returnsNewestFirst() {
        logDb.insert(new LogEntry(1000L, LogLevel.INFO, "Match first", null));
        logDb.insert(new LogEntry(2000L, LogLevel.INFO, "Match second", null));
        logDb.insert(new LogEntry(3000L, LogLevel.INFO, "Match third", null));

        List<LogEntry> results = logDb.search("Match", 10);

        assertEquals(3, results.size());
        assertEquals("Match third", results.get(0).getMessage());
        assertEquals("Match second", results.get(1).getMessage());
        assertEquals("Match first", results.get(2).getMessage());
    }

    @Test
    public void search_limitsResults() {
        logDb.insert(new LogEntry(1000L, LogLevel.INFO, "Match A", null));
        logDb.insert(new LogEntry(2000L, LogLevel.INFO, "Match B", null));
        logDb.insert(new LogEntry(3000L, LogLevel.INFO, "Match C", null));

        List<LogEntry> results = logDb.search("Match", 2);

        assertEquals(2, results.size());
        assertEquals("Match C", results.get(0).getMessage());
        assertEquals("Match B", results.get(1).getMessage());
    }

    @Test
    public void search_escapesWildcards_doesNotTreatAsWildcard() {
        logDb.insert(new LogEntry(1000L, LogLevel.INFO, "50% complete", null));
        logDb.insert(new LogEntry(2000L, LogLevel.INFO, "All done", null));

        List<LogEntry> results = logDb.search("50%", 10);

        assertEquals(1, results.size());
        assertEquals("50% complete", results.get(0).getMessage());
    }

    @Test
    public void search_noMatch_returnsEmpty() {
        logDb.insert(new LogEntry(1000L, LogLevel.INFO, "No match here", null));

        List<LogEntry> results = logDb.search("xyzzy", 10);

        assertTrue(results.isEmpty());
    }
}

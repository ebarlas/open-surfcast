package org.opensurfcast.prefs;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class UserPreferencesTest {

    private Context context;
    private UserPreferences prefs;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        // Clear preferences before each test
        context.getSharedPreferences("opensurfcast_prefs", Context.MODE_PRIVATE)
                .edit().clear().commit();
        prefs = new UserPreferences(context);
    }

    @After
    public void tearDown() {
        // Clean up after each test
        prefs.clear();
    }

    // ========== Buoy Station Tests ==========

    @Test
    public void getPreferredBuoyStations_emptyByDefault() {
        Set<String> stations = prefs.getPreferredBuoyStations();
        assertTrue(stations.isEmpty());
    }

    @Test
    public void setAndGetBuoyStations_returnsCorrectSet() {
        Set<String> expected = new HashSet<>(Arrays.asList("44060", "44065", "44017"));
        prefs.setPreferredBuoyStations(expected);

        Set<String> actual = prefs.getPreferredBuoyStations();
        assertEquals(expected, actual);
    }

    @Test
    public void addBuoyStation_addsToSet() {
        prefs.addPreferredBuoyStation("44060");
        prefs.addPreferredBuoyStation("44065");

        Set<String> stations = prefs.getPreferredBuoyStations();
        assertEquals(2, stations.size());
        assertTrue(stations.contains("44060"));
        assertTrue(stations.contains("44065"));
    }

    @Test
    public void addBuoyStation_duplicateDoesNotCreateDuplicate() {
        prefs.addPreferredBuoyStation("44060");
        prefs.addPreferredBuoyStation("44060");

        Set<String> stations = prefs.getPreferredBuoyStations();
        assertEquals(1, stations.size());
    }

    @Test
    public void removeBuoyStation_removesFromSet() {
        prefs.setPreferredBuoyStations(new HashSet<>(Arrays.asList("44060", "44065")));
        prefs.removePreferredBuoyStation("44060");

        Set<String> stations = prefs.getPreferredBuoyStations();
        assertEquals(1, stations.size());
        assertFalse(stations.contains("44060"));
        assertTrue(stations.contains("44065"));
    }

    @Test
    public void removeBuoyStation_nonExistentIsNoOp() {
        prefs.setPreferredBuoyStations(new HashSet<>(Collections.singletonList("44060")));
        prefs.removePreferredBuoyStation("99999");

        Set<String> stations = prefs.getPreferredBuoyStations();
        assertEquals(1, stations.size());
        assertTrue(stations.contains("44060"));
    }

    @Test
    public void isPreferredBuoyStation_returnsTrueForPreferred() {
        prefs.addPreferredBuoyStation("44060");
        assertTrue(prefs.isPreferredBuoyStation("44060"));
    }

    @Test
    public void isPreferredBuoyStation_returnsFalseForNonPreferred() {
        prefs.addPreferredBuoyStation("44060");
        assertFalse(prefs.isPreferredBuoyStation("44065"));
    }

    // ========== Tide Station Tests ==========

    @Test
    public void getPreferredTideStations_emptyByDefault() {
        Set<String> stations = prefs.getPreferredTideStations();
        assertTrue(stations.isEmpty());
    }

    @Test
    public void setAndGetTideStations_returnsCorrectSet() {
        Set<String> expected = new HashSet<>(Arrays.asList("9414290", "9415020"));
        prefs.setPreferredTideStations(expected);

        Set<String> actual = prefs.getPreferredTideStations();
        assertEquals(expected, actual);
    }

    @Test
    public void addTideStation_addsToSet() {
        prefs.addPreferredTideStation("9414290");
        prefs.addPreferredTideStation("9415020");

        Set<String> stations = prefs.getPreferredTideStations();
        assertEquals(2, stations.size());
        assertTrue(stations.contains("9414290"));
        assertTrue(stations.contains("9415020"));
    }

    @Test
    public void removeTideStation_removesFromSet() {
        prefs.setPreferredTideStations(new HashSet<>(Arrays.asList("9414290", "9415020")));
        prefs.removePreferredTideStation("9414290");

        Set<String> stations = prefs.getPreferredTideStations();
        assertEquals(1, stations.size());
        assertTrue(stations.contains("9415020"));
    }

    @Test
    public void isPreferredTideStation_returnsTrueForPreferred() {
        prefs.addPreferredTideStation("9414290");
        assertTrue(prefs.isPreferredTideStation("9414290"));
    }

    @Test
    public void isPreferredTideStation_returnsFalseForNonPreferred() {
        prefs.addPreferredTideStation("9414290");
        assertFalse(prefs.isPreferredTideStation("9415020"));
    }

    // ========== Current Station Tests ==========

    @Test
    public void getPreferredCurrentStations_emptyByDefault() {
        Set<String> stations = prefs.getPreferredCurrentStations();
        assertTrue(stations.isEmpty());
    }

    @Test
    public void setAndGetCurrentStations_returnsCorrectSet() {
        Set<String> expected = new HashSet<>(Arrays.asList("ACT0091", "ACT0095"));
        prefs.setPreferredCurrentStations(expected);

        Set<String> actual = prefs.getPreferredCurrentStations();
        assertEquals(expected, actual);
    }

    @Test
    public void addCurrentStation_addsToSet() {
        prefs.addPreferredCurrentStation("ACT0091");
        prefs.addPreferredCurrentStation("ACT0095");

        Set<String> stations = prefs.getPreferredCurrentStations();
        assertEquals(2, stations.size());
        assertTrue(stations.contains("ACT0091"));
        assertTrue(stations.contains("ACT0095"));
    }

    @Test
    public void removeCurrentStation_removesFromSet() {
        prefs.setPreferredCurrentStations(new HashSet<>(Arrays.asList("ACT0091", "ACT0095")));
        prefs.removePreferredCurrentStation("ACT0091");

        Set<String> stations = prefs.getPreferredCurrentStations();
        assertEquals(1, stations.size());
        assertTrue(stations.contains("ACT0095"));
    }

    @Test
    public void isPreferredCurrentStation_returnsTrueForPreferred() {
        prefs.addPreferredCurrentStation("ACT0091");
        assertTrue(prefs.isPreferredCurrentStation("ACT0091"));
    }

    @Test
    public void isPreferredCurrentStation_returnsFalseForNonPreferred() {
        prefs.addPreferredCurrentStation("ACT0091");
        assertFalse(prefs.isPreferredCurrentStation("ACT0095"));
    }

    // ========== Home Location Tests ==========

    @Test
    public void getHomeLatitude_defaultsToPetaluma() {
        assertEquals(38.115307, prefs.getHomeLatitude(), 0.0001);
    }

    @Test
    public void getHomeLongitude_defaultsToPetaluma() {
        assertEquals(-122.50567, prefs.getHomeLongitude(), 0.0001);
    }

    @Test
    public void setAndGetHomeLatitude_returnsCorrectValue() {
        prefs.setHomeLatitude(37.7749);
        assertEquals(37.7749, prefs.getHomeLatitude(), 0.0001);
    }

    @Test
    public void setAndGetHomeLongitude_returnsCorrectValue() {
        prefs.setHomeLongitude(-122.4194);
        assertEquals(-122.4194, prefs.getHomeLongitude(), 0.0001);
    }

    @Test
    public void setHomeLatitude_nullResetsToDefault() {
        prefs.setHomeLatitude(37.7749);
        prefs.setHomeLatitude(null);
        assertEquals(38.115307, prefs.getHomeLatitude(), 0.0001);
    }

    @Test
    public void setHomeLongitude_nullResetsToDefault() {
        prefs.setHomeLongitude(-122.4194);
        prefs.setHomeLongitude(null);
        assertEquals(-122.50567, prefs.getHomeLongitude(), 0.0001);
    }

    // ========== Units Preferences Tests ==========

    @Test
    public void isMetric_falseByDefault() {
        assertFalse(prefs.isMetric());
    }

    @Test
    public void setMetric_storesCorrectValue() {
        prefs.setMetric(true);
        assertTrue(prefs.isMetric());

        prefs.setMetric(false);
        assertFalse(prefs.isMetric());
    }

    // ========== Utility Tests ==========

    @Test
    public void clear_removesAllPreferences() {
        // Set various preferences
        prefs.addPreferredBuoyStation("44060");
        prefs.addPreferredTideStation("9414290");
        prefs.addPreferredCurrentStation("ACT0091");
        prefs.setHomeLatitude(37.7749);
        prefs.setHomeLongitude(-122.4194);
        prefs.setMetric(true);

        // Clear all
        prefs.clear();

        // Verify all are cleared
        assertTrue(prefs.getPreferredBuoyStations().isEmpty());
        assertTrue(prefs.getPreferredTideStations().isEmpty());
        assertTrue(prefs.getPreferredCurrentStations().isEmpty());
        assertEquals(38.115307, prefs.getHomeLatitude(), 0.0001);
        assertEquals(-122.50567, prefs.getHomeLongitude(), 0.0001);
        assertFalse(prefs.isMetric());
    }

    @Test
    public void getPreferredBuoyStations_returnsImmutableSet() {
        prefs.addPreferredBuoyStation("44060");
        Set<String> stations = prefs.getPreferredBuoyStations();

        try {
            stations.add("44065");
            // If we get here, the set wasn't immutable
            assertTrue("Expected UnsupportedOperationException", false);
        } catch (UnsupportedOperationException e) {
            // Expected - the set is immutable
            assertTrue(true);
        }
    }

    @Test
    public void multipleStationTypes_remainIndependent() {
        prefs.addPreferredBuoyStation("44060");
        prefs.addPreferredTideStation("9414290");
        prefs.addPreferredCurrentStation("ACT0091");

        assertEquals(1, prefs.getPreferredBuoyStations().size());
        assertEquals(1, prefs.getPreferredTideStations().size());
        assertEquals(1, prefs.getPreferredCurrentStations().size());

        assertTrue(prefs.getPreferredBuoyStations().contains("44060"));
        assertTrue(prefs.getPreferredTideStations().contains("9414290"));
        assertTrue(prefs.getPreferredCurrentStations().contains("ACT0091"));
    }
}

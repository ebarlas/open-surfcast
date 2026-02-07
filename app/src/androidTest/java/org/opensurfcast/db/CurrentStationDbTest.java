package org.opensurfcast.db;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensurfcast.tide.CurrentStation;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class CurrentStationDbTest {

    private OpenSurfcastDbHelper dbHelper;
    private CurrentStationDb currentStationDb;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase("opensurfcast.db");
        dbHelper = new OpenSurfcastDbHelper(context);
        currentStationDb = new CurrentStationDb(dbHelper);
    }

    @After
    public void tearDown() {
        dbHelper.close();
    }

    @Test
    public void replaceAll_insertsStations() {
        CurrentStation station = createStation("ACT0091");
        currentStationDb.replaceAll(Collections.singletonList(station));

        List<CurrentStation> result = currentStationDb.queryAll();
        assertEquals(1, result.size());
        assertEquals("ACT0091", result.get(0).id);
        assertEquals("Eastport, Friar Roads", result.get(0).name);
    }

    @Test
    public void queryById_returnsStation() {
        CurrentStation station1 = createStation("ACT0091");
        CurrentStation station2 = createStation("ACT0096", "Passamaquoddy Bay");
        currentStationDb.replaceAll(Arrays.asList(station1, station2));

        CurrentStation result = currentStationDb.queryById("ACT0091");

        assertNotNull(result);
        assertEquals("ACT0091", result.id);
        assertEquals("Eastport, Friar Roads", result.name);
        assertEquals(44.9, result.latitude, 0.0001);
    }

    @Test
    public void queryById_returnsNull_whenNotFound() {
        CurrentStation station = createStation("ACT0091");
        currentStationDb.replaceAll(Collections.singletonList(station));

        CurrentStation result = currentStationDb.queryById("99999");

        assertNull(result);
    }

    @Test
    public void queryByIds_returnsMatchingStations() {
        CurrentStation station1 = createStation("ACT0091");
        CurrentStation station2 = createStation("ACT0096", "Passamaquoddy Bay");
        CurrentStation station3 = createStation("ACT0101", "Lubec Channel");
        currentStationDb.replaceAll(Arrays.asList(station1, station2, station3));

        Set<String> ids = new HashSet<>(Arrays.asList("ACT0091", "ACT0101"));
        List<CurrentStation> result = currentStationDb.queryByIds(ids);

        assertEquals(2, result.size());
        assertNotNull(findById(result, "ACT0091"));
        assertNotNull(findById(result, "ACT0101"));
        assertNull(findById(result, "ACT0096"));
    }

    @Test
    public void queryByIds_returnsEmpty_whenIdsEmpty() {
        CurrentStation station = createStation("ACT0091");
        currentStationDb.replaceAll(Collections.singletonList(station));

        List<CurrentStation> result = currentStationDb.queryByIds(new HashSet<>());

        assertTrue(result.isEmpty());
    }

    @Test
    public void queryByIds_returnsEmpty_whenIdsNull() {
        CurrentStation station = createStation("ACT0091");
        currentStationDb.replaceAll(Collections.singletonList(station));

        List<CurrentStation> result = currentStationDb.queryByIds(null);

        assertTrue(result.isEmpty());
    }

    private CurrentStation createStation(String id) {
        return createStation(id, "Eastport, Friar Roads");
    }

    private CurrentStation createStation(String id, String name) {
        CurrentStation station = new CurrentStation();
        station.id = id;
        station.name = name;
        station.latitude = 44.9;
        station.longitude = -66.98333;
        station.type = "S";
        return station;
    }

    private CurrentStation findById(List<CurrentStation> stations, String id) {
        for (CurrentStation s : stations) {
            if (id.equals(s.id)) return s;
        }
        return null;
    }
}

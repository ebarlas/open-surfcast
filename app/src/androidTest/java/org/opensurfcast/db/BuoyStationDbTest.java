package org.opensurfcast.db;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensurfcast.buoy.BuoyStation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class BuoyStationDbTest {

    private OpenSurfcastDbHelper dbHelper;
    private BuoyStationDb buoyStationDb;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase("opensurfcast.db");
        dbHelper = new OpenSurfcastDbHelper(context);
        buoyStationDb = new BuoyStationDb(dbHelper);
    }

    @After
    public void tearDown() {
        dbHelper.close();
    }

    @Test
    public void replaceAll_insertsStations() {
        BuoyStation station1 = createStation("44060", "Eastern Long Island Sound", 41.263, -72.067);
        BuoyStation station2 = createStation("44065", "New York Harbor Entrance", 40.369, -73.703);

        buoyStationDb.replaceAll(Arrays.asList(station1, station2));

        List<BuoyStation> result = buoyStationDb.queryAll();
        assertEquals(2, result.size());

        BuoyStation loaded = findById(result, "44060");
        assertEquals("Eastern Long Island Sound", loaded.getName());
        assertEquals(41.263, loaded.getLatitude(), 0.0001);
        assertEquals(-72.067, loaded.getLongitude(), 0.0001);
        assertTrue(loaded.hasMet());
    }

    @Test
    public void replaceAll_replacesExisting() {
        BuoyStation station1 = createStation("44060", "Original", 41.0, -72.0);
        buoyStationDb.replaceAll(Collections.singletonList(station1));

        BuoyStation station2 = createStation("44065", "New Station", 40.0, -73.0);
        buoyStationDb.replaceAll(Collections.singletonList(station2));

        List<BuoyStation> result = buoyStationDb.queryAll();
        assertEquals(1, result.size());
        assertEquals("44065", result.get(0).getId());
        assertNull(findById(result, "44060"));
    }

    private BuoyStation createStation(String id, String name, double lat, double lon) {
        BuoyStation station = new BuoyStation();
        station.setId(id);
        station.setName(name);
        station.setLatitude(lat);
        station.setLongitude(lon);
        station.setHasMet(true);
        station.setHasCurrents(false);
        station.setHasWaterQuality(false);
        station.setHasDart(false);
        return station;
    }

    private BuoyStation findById(List<BuoyStation> stations, String id) {
        for (BuoyStation s : stations) {
            if (id.equals(s.getId())) return s;
        }
        return null;
    }
}

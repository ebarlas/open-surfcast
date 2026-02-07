package org.opensurfcast.db;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensurfcast.tide.TideStation;

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
public class TideStationDbTest {

    private OpenSurfcastDbHelper dbHelper;
    private TideStationDb tideStationDb;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase("opensurfcast.db");
        dbHelper = new OpenSurfcastDbHelper(context);
        tideStationDb = new TideStationDb(dbHelper);
    }

    @After
    public void tearDown() {
        dbHelper.close();
    }

    @Test
    public void replaceAll_insertsStations() {
        TideStation station = createStation("9415252");
        tideStationDb.replaceAll(Collections.singletonList(station));

        List<TideStation> result = tideStationDb.queryAll();
        assertEquals(1, result.size());
        assertEquals("9415252", result.get(0).id);
        assertEquals("Petaluma River entrance", result.get(0).name);
    }

    @Test
    public void queryById_returnsStation() {
        TideStation station1 = createStation("9415252");
        TideStation station2 = createStation("9414290", "San Francisco", "CA");
        tideStationDb.replaceAll(Arrays.asList(station1, station2));

        TideStation result = tideStationDb.queryById("9415252");

        assertNotNull(result);
        assertEquals("9415252", result.id);
        assertEquals("Petaluma River entrance", result.name);
        assertEquals("CA", result.state);
    }

    @Test
    public void queryById_returnsNull_whenNotFound() {
        TideStation station = createStation("9415252");
        tideStationDb.replaceAll(Collections.singletonList(station));

        TideStation result = tideStationDb.queryById("99999");

        assertNull(result);
    }

    @Test
    public void queryByIds_returnsMatchingStations() {
        TideStation station1 = createStation("9415252");
        TideStation station2 = createStation("9414290", "San Francisco", "CA");
        TideStation station3 = createStation("9413450", "Monterey", "CA");
        tideStationDb.replaceAll(Arrays.asList(station1, station2, station3));

        Set<String> ids = new HashSet<>(Arrays.asList("9415252", "9413450"));
        List<TideStation> result = tideStationDb.queryByIds(ids);

        assertEquals(2, result.size());
        assertNotNull(findById(result, "9415252"));
        assertNotNull(findById(result, "9413450"));
        assertNull(findById(result, "9414290"));
    }

    @Test
    public void queryByIds_returnsEmpty_whenIdsEmpty() {
        TideStation station = createStation("9415252");
        tideStationDb.replaceAll(Collections.singletonList(station));

        List<TideStation> result = tideStationDb.queryByIds(new HashSet<>());

        assertTrue(result.isEmpty());
    }

    @Test
    public void queryByIds_returnsEmpty_whenIdsNull() {
        TideStation station = createStation("9415252");
        tideStationDb.replaceAll(Collections.singletonList(station));

        List<TideStation> result = tideStationDb.queryByIds(null);

        assertTrue(result.isEmpty());
    }

    private TideStation createStation(String id) {
        return createStation(id, "Petaluma River entrance", "CA");
    }

    private TideStation createStation(String id, String name, String state) {
        TideStation station = new TideStation();
        station.id = id;
        station.name = name;
        station.state = state;
        station.latitude = 38.115307;
        station.longitude = -122.50567;
        station.type = "S";
        station.referenceId = "9414290";
        station.timeZoneCorrection = -8;
        return station;
    }

    private TideStation findById(List<TideStation> stations, String id) {
        for (TideStation s : stations) {
            if (id.equals(s.id)) return s;
        }
        return null;
    }
}

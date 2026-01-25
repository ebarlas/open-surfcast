package org.opensurfcast.db;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensurfcast.tide.TideStation;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

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

    private TideStation createStation(String id) {
        TideStation station = new TideStation();
        station.id = id;
        station.name = "Petaluma River entrance";
        station.state = "CA";
        station.latitude = 38.115307;
        station.longitude = -122.50567;
        station.type = "S";
        station.referenceId = "9414290";
        station.timeZoneCorrection = -8;
        return station;
    }
}

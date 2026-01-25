package org.opensurfcast.db;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensurfcast.tide.CurrentStation;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

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

    private CurrentStation createStation(String id) {
        CurrentStation station = new CurrentStation();
        station.id = id;
        station.name = "Eastport, Friar Roads";
        station.latitude = 44.9;
        station.longitude = -66.98333;
        station.type = "S";
        return station;
    }
}

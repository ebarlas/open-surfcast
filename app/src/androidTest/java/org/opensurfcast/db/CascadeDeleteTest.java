package org.opensurfcast.db;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensurfcast.buoy.BuoyStation;
import org.opensurfcast.buoy.BuoyStdMetData;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class CascadeDeleteTest {

    private OpenSurfcastDbHelper dbHelper;
    private BuoyStationDb buoyStationDb;
    private BuoyStdMetDataDb buoyStdMetDataDb;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase("opensurfcast.db");
        dbHelper = new OpenSurfcastDbHelper(context);
        buoyStationDb = new BuoyStationDb(dbHelper);
        buoyStdMetDataDb = new BuoyStdMetDataDb(dbHelper);
    }

    @After
    public void tearDown() {
        dbHelper.close();
    }

    @Test
    public void deletingStations_cascadesToData() {
        // Insert stations with data
        BuoyStation station1 = createStation("44060");
        BuoyStation station2 = createStation("44065");
        buoyStationDb.replaceAll(Arrays.asList(station1, station2));

        buoyStdMetDataDb.replaceAllForStation("44060",
                Collections.singletonList(createData(1000L)));
        buoyStdMetDataDb.replaceAllForStation("44065",
                Collections.singletonList(createData(2000L)));

        // Verify data exists
        assertEquals(1, buoyStdMetDataDb.queryByStation("44060").size());
        assertEquals(1, buoyStdMetDataDb.queryByStation("44065").size());

        // Replace stations with only station2 (station1 gets deleted)
        buoyStationDb.replaceAll(Collections.singletonList(station2));

        // Station1 data should be cascade deleted, station2 should remain
        assertEquals(0, buoyStdMetDataDb.queryByStation("44060").size());
        assertEquals(1, buoyStdMetDataDb.queryByStation("44065").size());
    }

    private BuoyStation createStation(String id) {
        BuoyStation station = new BuoyStation();
        station.setId(id);
        station.setName("Test Station " + id);
        station.setLatitude(40.0);
        station.setLongitude(-70.0);
        station.setHasMet(true);
        station.setHasCurrents(false);
        station.setHasWaterQuality(false);
        station.setHasDart(false);
        return station;
    }

    private BuoyStdMetData createData(long epochSeconds) {
        BuoyStdMetData data = new BuoyStdMetData();
        data.setEpochSeconds(epochSeconds);
        data.setYear(2025);
        data.setMonth(1);
        data.setDay(15);
        data.setHour(12);
        data.setMinute(0);
        return data;
    }
}

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
public class BuoyStdMetDataDbTest {

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
    public void replaceAllForStation_insertsData() {
        buoyStationDb.replaceAll(Collections.singletonList(createStation("44060")));

        BuoyStdMetData data1 = createData(1000L, 15.5);
        BuoyStdMetData data2 = createData(2000L, 16.0);
        buoyStdMetDataDb.replaceAllForStation("44060", Arrays.asList(data1, data2));

        List<BuoyStdMetData> result = buoyStdMetDataDb.queryByStation("44060");
        assertEquals(2, result.size());
        assertEquals(1000L, result.get(0).getEpochSeconds());
        assertEquals(15.5, result.get(0).getWaterTemperature(), 0.01);
        assertEquals(2000L, result.get(1).getEpochSeconds());
    }

    @Test
    public void replaceAllForStation_replacesExisting() {
        buoyStationDb.replaceAll(Collections.singletonList(createStation("44060")));

        buoyStdMetDataDb.replaceAllForStation("44060",
                Collections.singletonList(createData(1000L, 15.0)));

        buoyStdMetDataDb.replaceAllForStation("44060",
                Collections.singletonList(createData(2000L, 20.0)));

        List<BuoyStdMetData> result = buoyStdMetDataDb.queryByStation("44060");
        assertEquals(1, result.size());
        assertEquals(2000L, result.get(0).getEpochSeconds());
        assertEquals(20.0, result.get(0).getWaterTemperature(), 0.01);
    }

    @Test
    public void queryByStations_returnsMultiple() {
        buoyStationDb.replaceAll(Arrays.asList(createStation("44060"), createStation("44065")));

        buoyStdMetDataDb.replaceAllForStation("44060",
                Collections.singletonList(createData(1000L, 15.0)));
        buoyStdMetDataDb.replaceAllForStation("44065",
                Collections.singletonList(createData(2000L, 18.0)));

        List<BuoyStdMetData> result = buoyStdMetDataDb.queryByStations(
                Arrays.asList("44060", "44065"));

        assertEquals(2, result.size());
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

    private BuoyStdMetData createData(long epochSeconds, double waterTemp) {
        BuoyStdMetData data = new BuoyStdMetData();
        data.setEpochSeconds(epochSeconds);
        data.setYear(2025);
        data.setMonth(1);
        data.setDay(15);
        data.setHour(12);
        data.setMinute(0);
        data.setWaterTemperature(waterTemp);
        return data;
    }
}

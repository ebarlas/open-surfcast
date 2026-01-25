package org.opensurfcast.db;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensurfcast.buoy.BuoySpecWaveData;
import org.opensurfcast.buoy.BuoyStation;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class BuoySpecWaveDataDbTest {

    private OpenSurfcastDbHelper dbHelper;
    private BuoyStationDb buoyStationDb;
    private BuoySpecWaveDataDb buoySpecWaveDataDb;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase("opensurfcast.db");
        dbHelper = new OpenSurfcastDbHelper(context);
        buoyStationDb = new BuoyStationDb(dbHelper);
        buoySpecWaveDataDb = new BuoySpecWaveDataDb(dbHelper);
    }

    @After
    public void tearDown() {
        dbHelper.close();
    }

    @Test
    public void replaceAllForStation_insertsData() {
        buoyStationDb.replaceAll(Collections.singletonList(createStation("44060")));

        BuoySpecWaveData data = createData(1000L, 1.2);
        buoySpecWaveDataDb.replaceAllForStation("44060", Collections.singletonList(data));

        List<BuoySpecWaveData> result = buoySpecWaveDataDb.queryByStation("44060");
        assertEquals(1, result.size());
        assertEquals(1000L, result.get(0).getEpochSeconds());
        assertEquals(1.2, result.get(0).getWaveHeight(), 0.01);
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

    private BuoySpecWaveData createData(long epochSeconds, double waveHeight) {
        BuoySpecWaveData data = new BuoySpecWaveData();
        data.setEpochSeconds(epochSeconds);
        data.setYear(2025);
        data.setMonth(1);
        data.setDay(15);
        data.setHour(12);
        data.setMinute(0);
        data.setWaveHeight(waveHeight);
        return data;
    }
}

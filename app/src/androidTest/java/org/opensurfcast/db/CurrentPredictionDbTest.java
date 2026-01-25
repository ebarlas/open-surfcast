package org.opensurfcast.db;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensurfcast.tide.CurrentPrediction;
import org.opensurfcast.tide.CurrentStation;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class CurrentPredictionDbTest {

    private OpenSurfcastDbHelper dbHelper;
    private CurrentStationDb currentStationDb;
    private CurrentPredictionDb currentPredictionDb;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase("opensurfcast.db");
        dbHelper = new OpenSurfcastDbHelper(context);
        currentStationDb = new CurrentStationDb(dbHelper);
        currentPredictionDb = new CurrentPredictionDb(dbHelper);
    }

    @After
    public void tearDown() {
        dbHelper.close();
    }

    @Test
    public void replaceAllForStation_insertsData() {
        currentStationDb.replaceAll(Collections.singletonList(createStation("ACT0091")));

        CurrentPrediction prediction = createPrediction(1000L, 160.7);
        currentPredictionDb.replaceAllForStation("ACT0091", Collections.singletonList(prediction));

        List<CurrentPrediction> result = currentPredictionDb.queryByStation("ACT0091");
        assertEquals(1, result.size());
        assertEquals(1000L, result.get(0).epochSeconds);
        assertEquals(160.7, result.get(0).velocityMajor, 0.01);
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

    private CurrentPrediction createPrediction(long epochSeconds, double velocityMajor) {
        CurrentPrediction prediction = new CurrentPrediction();
        prediction.epochSeconds = epochSeconds;
        prediction.timestamp = "2026-01-05 02:47";
        prediction.type = CurrentPrediction.TYPE_FLOOD;
        prediction.velocityMajor = velocityMajor;
        prediction.meanFloodDirection = 210.0;
        prediction.meanEbbDirection = 40.0;
        prediction.bin = "1";
        return prediction;
    }
}

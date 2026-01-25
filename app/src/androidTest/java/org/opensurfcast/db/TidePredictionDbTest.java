package org.opensurfcast.db;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensurfcast.tide.TidePrediction;
import org.opensurfcast.tide.TideStation;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class TidePredictionDbTest {

    private OpenSurfcastDbHelper dbHelper;
    private TideStationDb tideStationDb;
    private TidePredictionDb tidePredictionDb;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase("opensurfcast.db");
        dbHelper = new OpenSurfcastDbHelper(context);
        tideStationDb = new TideStationDb(dbHelper);
        tidePredictionDb = new TidePredictionDb(dbHelper);
    }

    @After
    public void tearDown() {
        dbHelper.close();
    }

    @Test
    public void replaceAllForStation_insertsData() {
        tideStationDb.replaceAll(Collections.singletonList(createStation("9415252")));

        TidePrediction prediction = createPrediction(1000L, 2.314);
        tidePredictionDb.replaceAllForStation("9415252", Collections.singletonList(prediction));

        List<TidePrediction> result = tidePredictionDb.queryByStation("9415252");
        assertEquals(1, result.size());
        assertEquals(1000L, result.get(0).epochSeconds);
        assertEquals(2.314, result.get(0).value, 0.0001);
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

    private TidePrediction createPrediction(long epochSeconds, double value) {
        TidePrediction prediction = new TidePrediction();
        prediction.epochSeconds = epochSeconds;
        prediction.timestamp = "2026-01-02 10:57";
        prediction.value = value;
        prediction.type = TidePrediction.TYPE_HIGH;
        return prediction;
    }
}

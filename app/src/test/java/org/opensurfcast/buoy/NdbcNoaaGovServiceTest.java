package org.opensurfcast.buoy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.opensurfcast.http.HttpResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@RunWith(Parameterized.class)
public class NdbcNoaaGovServiceTest {

    @Parameterized.Parameters(name = "station {0}")
    public static Collection<String> stationIds() {
        return Arrays.asList("46013", "46059", "46026");
    }

    private final String stationId;

    public NdbcNoaaGovServiceTest(String stationId) {
        this.stationId = stationId;
    }

    @Test
    public void testFetchBuoyStdMetData() throws IOException {
        HttpResponse<List<BuoyStdMetData>> response = NdbcNoaaGovService.fetchBuoyStdMetData(stationId, null, null);

        assertTrue(response.present());
        assertNotNull(response.body());
        assertFalse(response.body().isEmpty());
        assertValidStdMetObservation(response.body().get(0));

        fetchNotModified(response, (lastModified, eTag) ->
                NdbcNoaaGovService.fetchBuoyStdMetData(stationId, lastModified, eTag));
    }

    @Test
    public void testFetchBuoySpecWaveData() throws IOException {
        HttpResponse<List<BuoySpecWaveData>> response = NdbcNoaaGovService.fetchBuoySpecWaveData(stationId, null, null);

        assertTrue(response.present());
        assertNotNull(response.body());
        assertFalse(response.body().isEmpty());
        assertValidSpecWaveObservation(response.body().get(0));

        fetchNotModified(response, (lastModified, eTag) ->
                NdbcNoaaGovService.fetchBuoySpecWaveData(stationId, lastModified, eTag));
    }

    interface Fetcher<T> {
        HttpResponse<T> fetch(String lastModified, String eTag) throws IOException;
    }

    private static <T> void fetchNotModified(HttpResponse<T> response, Fetcher<T> fn) throws IOException {
        assertNotNull("Last-Modified should be returned", response.lastModified());
        assertNotNull("ETag should be returned", response.eTag());

        // Repeat with Last-Modified should return not modified
        HttpResponse<T> cachedByLastModified = fn.fetch(response.lastModified(), null);
        assertFalse("Should return 304 with Last-Modified", cachedByLastModified.present());

        // Repeat with ETag should return not modified
        HttpResponse<T> cachedByEtag = fn.fetch(null, response.eTag());
        assertFalse("Should return 304 with ETag", cachedByEtag.present());
    }

    private void assertValidStdMetObservation(BuoyStdMetData obs) {
        assertTrue("Year should be recent", obs.getYear() >= 2020);
        assertTrue("Month should be 1-12", obs.getMonth() >= 1 && obs.getMonth() <= 12);
        assertTrue("Day should be 1-31", obs.getDay() >= 1 && obs.getDay() <= 31);
        assertTrue("Hour should be 0-23", obs.getHour() >= 0 && obs.getHour() <= 23);
        assertTrue("Minute should be 0-59", obs.getMinute() >= 0 && obs.getMinute() <= 59);
        assertTrue("Epoch should be positive", obs.getEpochSeconds() > 0);
    }

    private void assertValidSpecWaveObservation(BuoySpecWaveData obs) {
        assertTrue("Year should be recent", obs.getYear() >= 2020);
        assertTrue("Month should be 1-12", obs.getMonth() >= 1 && obs.getMonth() <= 12);
        assertTrue("Day should be 1-31", obs.getDay() >= 1 && obs.getDay() <= 31);
        assertTrue("Hour should be 0-23", obs.getHour() >= 0 && obs.getHour() <= 23);
        assertTrue("Minute should be 0-59", obs.getMinute() >= 0 && obs.getMinute() <= 59);
        assertTrue("Epoch should be positive", obs.getEpochSeconds() > 0);
    }

    @Test
    public void testFetchBuoyStations() throws IOException {
        HttpResponse<List<BuoyStation>> response = NdbcNoaaGovService.fetchBuoyStations(null, null);

        assertTrue(response.present());
        assertNotNull(response.body());
        assertFalse(response.body().isEmpty());
        assertTrue("Should have many stations", response.body().size() > 100);

        BuoyStation station = response.body().get(0);
        assertNotNull("Station ID should not be null", station.getId());
        assertTrue("Latitude should be valid", station.getLatitude() >= -90 && station.getLatitude() <= 90);
        assertTrue("Longitude should be valid", station.getLongitude() >= -180 && station.getLongitude() <= 180);

        fetchNotModified(response, NdbcNoaaGovService::fetchBuoyStations);
    }

    @Test
    public void testFetchStationIdsWithStdMetAndSpecWave() throws IOException {
        Set<String> stationIds = NdbcNoaaGovService.fetchStationIdsWithStdMetAndSpecWave();

        assertTrue("Should have many stations with both data types", stationIds.size() > 50);

        // Verify parameterized station is in the set
        assertTrue("Test station " + stationId + " should have both data types",
                stationIds.contains(stationId));
    }
}

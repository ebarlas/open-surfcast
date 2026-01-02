package org.opensurfcast.buoy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.opensurfcast.http.Modified;

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
        Modified<List<BuoyStdMetData>> response = NdbcNoaaGovService.fetchBuoyStdMetData(stationId, null);

        assertNotNull(response);
        assertNotNull(response.value());
        assertFalse(response.value().isEmpty());
        assertValidStdMetObservation(response.value().get(0));

        assertNotModified(response, lastModified ->
                NdbcNoaaGovService.fetchBuoyStdMetData(stationId, lastModified));
    }

    @Test
    public void testFetchBuoySpecWaveData() throws IOException {
        Modified<List<BuoySpecWaveData>> response = NdbcNoaaGovService.fetchBuoySpecWaveData(stationId, null);

        assertNotNull(response);
        assertNotNull(response.value());
        assertFalse(response.value().isEmpty());
        assertValidSpecWaveObservation(response.value().get(0));

        assertNotModified(response, lastModified ->
                NdbcNoaaGovService.fetchBuoySpecWaveData(stationId, lastModified));
    }

    @Test
    public void testFetchBuoyStations() throws IOException {
        Modified<List<BuoyStation>> response = NdbcNoaaGovService.fetchBuoyStations(null);

        assertNotNull(response);
        assertNotNull(response.value());
        assertFalse(response.value().isEmpty());
        assertTrue("Should have many stations", response.value().size() > 100);

        BuoyStation station = response.value().get(0);
        assertNotNull("Station ID should not be null", station.getId());
        assertTrue("Latitude should be valid", station.getLatitude() >= -90 && station.getLatitude() <= 90);
        assertTrue("Longitude should be valid", station.getLongitude() >= -180 && station.getLongitude() <= 180);

        assertNotModified(response, NdbcNoaaGovService::fetchBuoyStations);
    }

    @Test
    public void testFetchStationIdsWithStdMetAndSpecWave() throws IOException {
        Set<String> stationIds = NdbcNoaaGovService.fetchStationIdsWithStdMetAndSpecWave();

        assertTrue("Should have many stations with both data types", stationIds.size() > 50);

        // Verify parameterized station is in the set
        assertTrue("Test station " + stationId + " should have both data types",
                stationIds.contains(stationId));
    }

    interface Fetcher<T> {
        Modified<T> fetch(String lastModified) throws IOException;
    }

    private static <T> void assertNotModified(Modified<T> response, Fetcher<T> fn) throws IOException {
        assertNotNull("Last-Modified should be returned", response.lastModified());

        Modified<T> cached = fn.fetch(response.lastModified());
        assertNull("Should return null for 304 Not Modified", cached);
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
}

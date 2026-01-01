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
    public void testFetchStation() throws IOException {
        HttpResponse<List<BuoyStdMetData>> response = NdbcNoaaGovService.fetchBuoyStdMetData(stationId, null, null);

        assertTrue(response.present());
        assertNotNull(response.body());
        assertFalse(response.body().isEmpty());
        assertValidObservation(response.body().get(0));

        assertNotNull("Last-Modified should be returned", response.lastModified());
        assertNotNull("ETag should be returned", response.eTag());

        // Repeat with Last-Modified should return not modified
        HttpResponse<List<BuoyStdMetData>> cachedByLastModified =
                NdbcNoaaGovService.fetchBuoyStdMetData(stationId, response.lastModified(), null);
        assertFalse("Should return 304 with Last-Modified", cachedByLastModified.present());

        // Repeat with ETag should return not modified
        HttpResponse<List<BuoyStdMetData>> cachedByEtag =
                NdbcNoaaGovService.fetchBuoyStdMetData(stationId, null, response.eTag());
        assertFalse("Should return 304 with ETag", cachedByEtag.present());
    }

    private void assertValidObservation(BuoyStdMetData obs) {
        assertTrue("Year should be recent", obs.getYear() >= 2020);
        assertTrue("Month should be 1-12", obs.getMonth() >= 1 && obs.getMonth() <= 12);
        assertTrue("Day should be 1-31", obs.getDay() >= 1 && obs.getDay() <= 31);
        assertTrue("Hour should be 0-23", obs.getHour() >= 0 && obs.getHour() <= 23);
        assertTrue("Minute should be 0-59", obs.getMinute() >= 0 && obs.getMinute() <= 59);
        assertTrue("Epoch should be positive", obs.getEpochSeconds() > 0);
    }
}

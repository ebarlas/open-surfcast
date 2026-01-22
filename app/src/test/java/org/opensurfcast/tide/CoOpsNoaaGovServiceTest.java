package org.opensurfcast.tide;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class CoOpsNoaaGovServiceTest {

    @Test
    public void testFetchTideStations() throws IOException {
        List<TideStation> stations = CoOpsNoaaGovService.fetchTideStations();

        assertNotNull(stations);
        assertFalse(stations.isEmpty());
        assertTrue("Should have many stations", stations.size() > 3000);

        TideStation station = stations.get(0);
        assertNotNull("Station ID should not be null", station.id);
        assertFalse("Station ID should not be empty", station.id.isEmpty());
        assertTrue("Latitude should be valid", station.latitude >= -90 && station.latitude <= 90);
        assertTrue("Longitude should be valid", station.longitude >= -180 && station.longitude <= 180);
    }

    @Test
    public void testFetchTideStationsContainsKnownStation() throws IOException {
        List<TideStation> stations = CoOpsNoaaGovService.fetchTideStations();

        // San Francisco is a well-known reference station
        TideStation sanFrancisco = stations.stream()
                .filter(s -> "9414290".equals(s.id))
                .findFirst()
                .orElse(null);

        assertNotNull("San Francisco station (9414290) should exist", sanFrancisco);
        assertTrue("San Francisco should be a reference station", sanFrancisco.isReferenceStation());
        assertTrue("Name should contain San Francisco or SF",
                sanFrancisco.name.toUpperCase().contains("SAN FRANCISCO") ||
                sanFrancisco.name.toUpperCase().contains("SF"));
        assertEquals("CA", sanFrancisco.state);
    }

    @Test
    public void testFetchTideStationsContainsSubordinateStation() throws IOException {
        List<TideStation> stations = CoOpsNoaaGovService.fetchTideStations();

        // Petaluma River entrance is a subordinate station referencing San Francisco
        TideStation petaluma = stations.stream()
                .filter(s -> "9415252".equals(s.id))
                .findFirst()
                .orElse(null);

        assertNotNull("Petaluma River entrance station (9415252) should exist", petaluma);
        assertTrue("Petaluma should be a subordinate station", petaluma.isSubordinateStation());
        assertEquals("Should reference San Francisco", "9414290", petaluma.referenceId);
        assertEquals("CA", petaluma.state);
    }

    @Test
    public void testFetchTideStationsHasMultipleStates() throws IOException {
        List<TideStation> stations = CoOpsNoaaGovService.fetchTideStations();

        long californiaCount = stations.stream()
                .filter(s -> "CA".equals(s.state))
                .count();
        long hawaiiCount = stations.stream()
                .filter(s -> "HI".equals(s.state))
                .count();
        long floridaCount = stations.stream()
                .filter(s -> "FL".equals(s.state))
                .count();

        assertTrue("Should have California stations", californiaCount > 50);
        assertTrue("Should have Hawaii stations", hawaiiCount > 20);
        assertTrue("Should have Florida stations", floridaCount > 50);
    }

    @Test
    public void testFetchTidePredictions() throws IOException {
        // Fetch one week of predictions for San Francisco
        List<TidePrediction> predictions = CoOpsNoaaGovService.fetchTidePredictions(
                "9414290", "20260101", "20260107");

        assertNotNull(predictions);
        assertFalse(predictions.isEmpty());
        // Expect roughly 4 tides per day * 7 days = 28 predictions
        assertTrue("Should have multiple predictions", predictions.size() >= 20);

        TidePrediction prediction = predictions.get(0);
        assertNotNull("Timestamp should not be null", prediction.timestamp);
        assertFalse("Timestamp should not be empty", prediction.timestamp.isEmpty());
        assertTrue("Epoch seconds should be positive", prediction.epochSeconds > 0);
        assertNotNull("Type should not be null", prediction.type);
    }

    @Test
    public void testFetchTidePredictionsHasHighAndLowTides() throws IOException {
        List<TidePrediction> predictions = CoOpsNoaaGovService.fetchTidePredictions(
                "9414290", "20260101", "20260107");

        long highCount = predictions.stream()
                .filter(TidePrediction::isHighTide)
                .count();
        long lowCount = predictions.stream()
                .filter(TidePrediction::isLowTide)
                .count();

        assertTrue("Should have high tides", highCount > 0);
        assertTrue("Should have low tides", lowCount > 0);
        assertEquals("All predictions should be high or low",
                predictions.size(), highCount + lowCount);
    }

    @Test
    public void testFetchTidePredictionsValuesAreReasonable() throws IOException {
        List<TidePrediction> predictions = CoOpsNoaaGovService.fetchTidePredictions(
                "9414290", "20260101", "20260107");

        for (TidePrediction prediction : predictions) {
            // San Francisco tides typically range from -0.5m to 2.5m relative to MLLW
            assertTrue("Value should be reasonable: " + prediction.value,
                    prediction.value >= -2.0 && prediction.value <= 4.0);
        }
    }

    @Test
    public void testFetchTidePredictionsTimestampsAreChronological() throws IOException {
        List<TidePrediction> predictions = CoOpsNoaaGovService.fetchTidePredictions(
                "9414290", "20260101", "20260107");

        for (int i = 1; i < predictions.size(); i++) {
            TidePrediction prev = predictions.get(i - 1);
            TidePrediction curr = predictions.get(i);
            assertTrue("Predictions should be chronological",
                    curr.epochSeconds > prev.epochSeconds);
        }
    }

    @Test
    public void testFetchTidePredictionsEpochSecondsMatchesTimestamp() throws IOException {
        List<TidePrediction> predictions = CoOpsNoaaGovService.fetchTidePredictions(
                "9414290", "20260101", "20260102");

        TidePrediction prediction = predictions.get(0);
        // Verify timestamp starts with expected date
        assertTrue("Timestamp should start with 2026-01",
                prediction.timestamp.startsWith("2026-01"));
        // Epoch for 2026-01-01 00:00:00 UTC is 1767225600
        assertTrue("Epoch should be in January 2026",
                prediction.epochSeconds >= 1767225600L && prediction.epochSeconds < 1769904000L);
    }

    /**
     * Tests current station fetching. Combined into a single test to avoid
     * multiple expensive API calls (~1MB download).
     */
    @Test
    public void testFetchCurrentStations() throws IOException {
        List<CurrentStation> stations = CoOpsNoaaGovService.fetchCurrentStations();

        // Basic validation
        assertNotNull(stations);
        assertFalse(stations.isEmpty());
        assertTrue("Should have many stations", stations.size() > 1000);

        CurrentStation station = stations.get(0);
        assertNotNull("Station ID should not be null", station.id);
        assertFalse("Station ID should not be empty", station.id.isEmpty());
        assertTrue("Latitude should be valid", station.latitude >= -90 && station.latitude <= 90);
        assertTrue("Longitude should be valid", station.longitude >= -180 && station.longitude <= 180);

        // Check for known station (ACT0091 - Eastport, Friar Roads)
        CurrentStation knownStation = stations.stream()
                .filter(s -> s.id != null && s.id.startsWith("ACT0091"))
                .findFirst()
                .orElse(null);
        assertNotNull("Known station (ACT0091) should exist", knownStation);
        assertNotNull("Name should not be null", knownStation.name);

        // All stations should have a type defined
        long withTypeCount = stations.stream()
                .filter(s -> s.type != null && !s.type.isEmpty())
                .count();
        assertEquals("All stations should have a type", withTypeCount, stations.size());

        // Check that at least some stations are reference or subordinate
        long referenceCount = stations.stream()
                .filter(CurrentStation::isReferenceStation)
                .count();
        long subordinateCount = stations.stream()
                .filter(CurrentStation::isSubordinateStation)
                .count();
        assertTrue("Should have reference or subordinate stations",
                referenceCount > 0 || subordinateCount > 0);

        // Some stations have multiple bins (depth levels)
        long multipleBinCount = stations.stream()
                .filter(s -> s.currentBin != null && s.currentBin > 1)
                .count();
        assertTrue("Should have stations with multiple bins", multipleBinCount > 0);
    }

    @Test
    public void testFetchCurrentPredictions() throws IOException {
        // Fetch one week of predictions for Admiralty Inlet
        List<CurrentPrediction> predictions = CoOpsNoaaGovService.fetchCurrentPredictions(
                "ACT0091", "20260101", "20260107");

        assertNotNull(predictions);
        assertFalse(predictions.isEmpty());
        // Expect roughly 4 events per day (2 floods, 2 ebbs or slack) * 7 days
        assertTrue("Should have multiple predictions", predictions.size() >= 20);

        CurrentPrediction prediction = predictions.get(0);
        assertNotNull("Timestamp should not be null", prediction.timestamp);
        assertFalse("Timestamp should not be empty", prediction.timestamp.isEmpty());
        assertTrue("Epoch seconds should be positive", prediction.epochSeconds > 0);
        assertNotNull("Type should not be null", prediction.type);
    }

    @Test
    public void testFetchCurrentPredictionsHasFloodAndEbbTypes() throws IOException {
        List<CurrentPrediction> predictions = CoOpsNoaaGovService.fetchCurrentPredictions(
                "ACT0091", "20260101", "20260107");

        long floodCount = predictions.stream()
                .filter(CurrentPrediction::isFlood)
                .count();
        long ebbCount = predictions.stream()
                .filter(CurrentPrediction::isEbb)
                .count();

        assertTrue("Should have flood predictions", floodCount > 0);
        assertTrue("Should have ebb predictions", ebbCount > 0);
    }

    @Test
    public void testFetchCurrentPredictionsVelocitiesAreReasonable() throws IOException {
        List<CurrentPrediction> predictions = CoOpsNoaaGovService.fetchCurrentPredictions(
                "ACT0091", "20260101", "20260107");

        for (CurrentPrediction prediction : predictions) {
            // Velocities in cm/s - typical range for tidal currents
            assertTrue("Velocity should be reasonable: " + prediction.velocityMajor,
                    prediction.velocityMajor >= -500 && prediction.velocityMajor <= 500);
        }
    }

    @Test
    public void testFetchCurrentPredictionsTimestampsAreChronological() throws IOException {
        List<CurrentPrediction> predictions = CoOpsNoaaGovService.fetchCurrentPredictions(
                "ACT0091", "20260101", "20260107");

        for (int i = 1; i < predictions.size(); i++) {
            CurrentPrediction prev = predictions.get(i - 1);
            CurrentPrediction curr = predictions.get(i);
            assertTrue("Predictions should be chronological",
                    curr.epochSeconds > prev.epochSeconds);
        }
    }

    @Test
    public void testFetchCurrentPredictionsHasDirections() throws IOException {
        List<CurrentPrediction> predictions = CoOpsNoaaGovService.fetchCurrentPredictions(
                "ACT0091", "20260101", "20260107");

        // All predictions should have valid direction data (0-360 degrees)
        for (CurrentPrediction prediction : predictions) {
            assertTrue("Flood direction should be valid: " + prediction.meanFloodDirection,
                    prediction.meanFloodDirection >= 0 && prediction.meanFloodDirection <= 360);
            assertTrue("Ebb direction should be valid: " + prediction.meanEbbDirection,
                    prediction.meanEbbDirection >= 0 && prediction.meanEbbDirection <= 360);
        }
    }

    @Test
    public void testFetchCurrentPredictionsEpochSecondsMatchesTimestamp() throws IOException {
        List<CurrentPrediction> predictions = CoOpsNoaaGovService.fetchCurrentPredictions(
                "ACT0091", "20260101", "20260102");

        CurrentPrediction prediction = predictions.get(0);
        // Verify timestamp starts with expected date
        assertTrue("Timestamp should start with 2026-01",
                prediction.timestamp.startsWith("2026-01"));
        // Epoch for 2026-01-01 00:00:00 UTC is 1767225600
        assertTrue("Epoch should be in January 2026",
                prediction.epochSeconds >= 1767225600L && prediction.epochSeconds < 1769904000L);
    }
}


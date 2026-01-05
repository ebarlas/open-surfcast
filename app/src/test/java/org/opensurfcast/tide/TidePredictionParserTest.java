package org.opensurfcast.tide;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class TidePredictionParserTest {

    private static final String SINGLE_PREDICTION = """
            {
              "predictions": [
                {"t": "2026-01-02 10:57", "v": "7.591", "type": "H"}
              ]
            }
            """;

    private static final String MULTIPLE_PREDICTIONS = """
            {
              "predictions": [
                {"t": "2026-01-02 00:41", "v": "5.161", "type": "H"},
                {"t": "2026-01-02 05:31", "v": "2.720", "type": "L"},
                {"t": "2026-01-02 10:57", "v": "7.591", "type": "H"},
                {"t": "2026-01-02 18:50", "v": "-1.427", "type": "L"}
              ]
            }
            """;

    private static InputStream toInputStream(String text) {
        return new ByteArrayInputStream(text.getBytes());
    }

    private static List<TidePrediction> parse(String text) throws IOException {
        return TidePredictionParser.parse(toInputStream(text));
    }

    @Test
    public void testParseSinglePrediction() throws IOException {
        List<TidePrediction> predictions = parse(SINGLE_PREDICTION);

        assertEquals(1, predictions.size());
        TidePrediction prediction = predictions.get(0);
        assertEquals("2026-01-02 10:57", prediction.timestamp);
        assertEquals(1767351420L, prediction.epochSeconds); // 2026-01-02 10:57:00 UTC
        assertEquals(7.591, prediction.value, 0.0001);
        assertEquals("H", prediction.type);
    }

    @Test
    public void testParseMultiplePredictions() throws IOException {
        List<TidePrediction> predictions = parse(MULTIPLE_PREDICTIONS);

        assertEquals(4, predictions.size());
        assertEquals("2026-01-02 00:41", predictions.get(0).timestamp);
        assertEquals("2026-01-02 05:31", predictions.get(1).timestamp);
        assertEquals("2026-01-02 10:57", predictions.get(2).timestamp);
        assertEquals("2026-01-02 18:50", predictions.get(3).timestamp);
    }

    @Test
    public void testParseHighTide() throws IOException {
        List<TidePrediction> predictions = parse(MULTIPLE_PREDICTIONS);

        TidePrediction highTide = predictions.get(0);
        assertEquals("H", highTide.type);
        assertTrue(highTide.isHighTide());
        assertFalse(highTide.isLowTide());
    }

    @Test
    public void testParseLowTide() throws IOException {
        List<TidePrediction> predictions = parse(MULTIPLE_PREDICTIONS);

        TidePrediction lowTide = predictions.get(1);
        assertEquals("L", lowTide.type);
        assertTrue(lowTide.isLowTide());
        assertFalse(lowTide.isHighTide());
    }

    @Test
    public void testParseNegativeValue() throws IOException {
        List<TidePrediction> predictions = parse(MULTIPLE_PREDICTIONS);

        TidePrediction negativeTide = predictions.get(3);
        assertEquals(-1.427, negativeTide.value, 0.0001);
        assertTrue(negativeTide.isLowTide());
    }

    @Test
    public void testParseValues() throws IOException {
        List<TidePrediction> predictions = parse(MULTIPLE_PREDICTIONS);

        assertEquals(5.161, predictions.get(0).value, 0.0001);
        assertEquals(2.720, predictions.get(1).value, 0.0001);
        assertEquals(7.591, predictions.get(2).value, 0.0001);
        assertEquals(-1.427, predictions.get(3).value, 0.0001);
    }

    @Test
    public void testParseEpochSeconds() throws IOException {
        List<TidePrediction> predictions = parse(MULTIPLE_PREDICTIONS);

        // 2026-01-02 00:41:00 UTC
        assertEquals(1767314460L, predictions.get(0).epochSeconds);
        // 2026-01-02 05:31:00 UTC
        assertEquals(1767331860L, predictions.get(1).epochSeconds);
        // 2026-01-02 10:57:00 UTC
        assertEquals(1767351420L, predictions.get(2).epochSeconds);
        // 2026-01-02 18:50:00 UTC
        assertEquals(1767379800L, predictions.get(3).epochSeconds);
    }

    @Test
    public void testParseEmptyPredictions() throws IOException {
        String json = """
                {
                  "predictions": []
                }
                """;
        List<TidePrediction> predictions = parse(json);

        assertEquals(0, predictions.size());
    }

    @Test
    public void testParseMalformedJsonThrows() {
        String malformed = "not valid json {{{";
        assertThrows(IOException.class, () -> parse(malformed));
    }

    @Test
    public void testParseMissingPredictionsArrayThrows() {
        String json = """
                {
                  "other": "data"
                }
                """;
        assertThrows(IOException.class, () -> parse(json));
    }

    @Test
    public void testParseMissingTimestampThrows() {
        String json = """
                {
                  "predictions": [
                    {"v": "5.0", "type": "H"}
                  ]
                }
                """;
        assertThrows(IOException.class, () -> parse(json));
    }

    @Test
    public void testParseMissingValueThrows() {
        String json = """
                {
                  "predictions": [
                    {"t": "2026-01-02 10:57", "type": "H"}
                  ]
                }
                """;
        assertThrows(IOException.class, () -> parse(json));
    }

    @Test
    public void testParseInvalidValueThrows() {
        String json = """
                {
                  "predictions": [
                    {"t": "2026-01-02 10:57", "v": "not-a-number", "type": "H"}
                  ]
                }
                """;
        assertThrows(IOException.class, () -> parse(json));
    }
}


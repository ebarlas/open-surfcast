package org.opensurfcast.tide;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class CurrentPredictionParserTest {

    private static final String SINGLE_PREDICTION = """
            {
              "current_predictions": {
                "units": "meters, cm/s",
                "cp": [
                  {
                    "Type": "flood",
                    "Time": "2026-01-05 02:47",
                    "Velocity_Major": 160.7,
                    "meanFloodDir": 210,
                    "meanEbbDir": 40,
                    "Bin": "1",
                    "Depth": null
                  }
                ]
              }
            }
            """;

    private static final String MULTIPLE_PREDICTIONS = """
            {
              "current_predictions": {
                "units": "meters, cm/s",
                "cp": [
                  {
                    "Type": "flood",
                    "Time": "2026-01-05 02:47",
                    "Velocity_Major": 160.7,
                    "meanFloodDir": 210,
                    "meanEbbDir": 40,
                    "Bin": "1",
                    "Depth": null
                  },
                  {
                    "Type": "slack",
                    "Time": "2026-01-05 05:23",
                    "Velocity_Major": 0,
                    "meanFloodDir": 210,
                    "meanEbbDir": 40,
                    "Bin": "1",
                    "Depth": null
                  },
                  {
                    "Type": "ebb",
                    "Time": "2026-01-05 08:55",
                    "Velocity_Major": -169.3,
                    "meanFloodDir": 210,
                    "meanEbbDir": 40,
                    "Bin": "1",
                    "Depth": null
                  },
                  {
                    "Type": "slack",
                    "Time": "2026-01-05 11:33",
                    "Velocity_Major": 0,
                    "meanFloodDir": 210,
                    "meanEbbDir": 40,
                    "Bin": "1",
                    "Depth": null
                  }
                ]
              }
            }
            """;

    private static final String PREDICTION_WITH_DEPTH = """
            {
              "current_predictions": {
                "units": "meters, cm/s",
                "cp": [
                  {
                    "Type": "flood",
                    "Time": "2026-01-05 15:07",
                    "Velocity_Major": 160.8,
                    "meanFloodDir": 45,
                    "meanEbbDir": 225,
                    "Bin": "14",
                    "Depth": 5.5
                  }
                ]
              }
            }
            """;

    private static InputStream toInputStream(String text) {
        return new ByteArrayInputStream(text.getBytes());
    }

    private static List<CurrentPrediction> parse(String text) throws IOException {
        return CurrentPredictionParser.parse(toInputStream(text));
    }

    @Test
    public void testParseSinglePrediction() throws IOException {
        List<CurrentPrediction> predictions = parse(SINGLE_PREDICTION);

        assertEquals(1, predictions.size());
        CurrentPrediction prediction = predictions.get(0);
        assertEquals("2026-01-05 02:47", prediction.timestamp);
        assertEquals(1767581220L, prediction.epochSeconds); // 2026-01-05 02:47:00 UTC
        assertEquals("flood", prediction.type);
        assertEquals(160.7, prediction.velocityMajor, 0.01);
        assertEquals(210.0, prediction.meanFloodDirection, 0.01);
        assertEquals(40.0, prediction.meanEbbDirection, 0.01);
        assertEquals("1", prediction.bin);
        assertNull(prediction.depth);
    }

    @Test
    public void testParseMultiplePredictions() throws IOException {
        List<CurrentPrediction> predictions = parse(MULTIPLE_PREDICTIONS);

        assertEquals(4, predictions.size());
        assertEquals("2026-01-05 02:47", predictions.get(0).timestamp);
        assertEquals("2026-01-05 05:23", predictions.get(1).timestamp);
        assertEquals("2026-01-05 08:55", predictions.get(2).timestamp);
        assertEquals("2026-01-05 11:33", predictions.get(3).timestamp);
    }

    @Test
    public void testParseFloodCurrent() throws IOException {
        List<CurrentPrediction> predictions = parse(MULTIPLE_PREDICTIONS);

        CurrentPrediction flood = predictions.get(0);
        assertEquals("flood", flood.type);
        assertTrue(flood.isFlood());
        assertFalse(flood.isEbb());
        assertFalse(flood.isSlack());
        assertTrue(flood.velocityMajor > 0);
    }

    @Test
    public void testParseEbbCurrent() throws IOException {
        List<CurrentPrediction> predictions = parse(MULTIPLE_PREDICTIONS);

        CurrentPrediction ebb = predictions.get(2);
        assertEquals("ebb", ebb.type);
        assertTrue(ebb.isEbb());
        assertFalse(ebb.isFlood());
        assertFalse(ebb.isSlack());
        assertTrue(ebb.velocityMajor < 0);
    }

    @Test
    public void testParseSlackWater() throws IOException {
        List<CurrentPrediction> predictions = parse(MULTIPLE_PREDICTIONS);

        CurrentPrediction slack = predictions.get(1);
        assertEquals("slack", slack.type);
        assertTrue(slack.isSlack());
        assertFalse(slack.isFlood());
        assertFalse(slack.isEbb());
        assertEquals(0.0, slack.velocityMajor, 0.001);
    }

    @Test
    public void testParseNegativeVelocity() throws IOException {
        List<CurrentPrediction> predictions = parse(MULTIPLE_PREDICTIONS);

        CurrentPrediction ebb = predictions.get(2);
        assertEquals(-169.3, ebb.velocityMajor, 0.01);
        assertTrue(ebb.isEbb());
    }

    @Test
    public void testParseVelocityMajor() throws IOException {
        List<CurrentPrediction> predictions = parse(MULTIPLE_PREDICTIONS);

        assertEquals(160.7, predictions.get(0).velocityMajor, 0.01);
        assertEquals(0.0, predictions.get(1).velocityMajor, 0.01);
        assertEquals(-169.3, predictions.get(2).velocityMajor, 0.01);
        assertEquals(0.0, predictions.get(3).velocityMajor, 0.01);
    }

    @Test
    public void testParseDirections() throws IOException {
        List<CurrentPrediction> predictions = parse(MULTIPLE_PREDICTIONS);

        CurrentPrediction prediction = predictions.get(0);
        assertEquals(210.0, prediction.meanFloodDirection, 0.01);
        assertEquals(40.0, prediction.meanEbbDirection, 0.01);
    }

    @Test
    public void testGetCurrentDirection() throws IOException {
        List<CurrentPrediction> predictions = parse(MULTIPLE_PREDICTIONS);

        // Flood should return flood direction
        CurrentPrediction flood = predictions.get(0);
        assertEquals(210.0, flood.getCurrentDirection(), 0.01);

        // Ebb should return ebb direction
        CurrentPrediction ebb = predictions.get(2);
        assertEquals(40.0, ebb.getCurrentDirection(), 0.01);

        // Slack should return flood direction (arbitrary but consistent)
        CurrentPrediction slack = predictions.get(1);
        assertEquals(210.0, slack.getCurrentDirection(), 0.01);
    }

    @Test
    public void testGetSpeed() throws IOException {
        List<CurrentPrediction> predictions = parse(MULTIPLE_PREDICTIONS);

        assertEquals(160.7, predictions.get(0).getSpeed(), 0.01);
        assertEquals(0.0, predictions.get(1).getSpeed(), 0.01);
        assertEquals(169.3, predictions.get(2).getSpeed(), 0.01); // Absolute value
        assertEquals(0.0, predictions.get(3).getSpeed(), 0.01);
    }

    @Test
    public void testParseEpochSeconds() throws IOException {
        List<CurrentPrediction> predictions = parse(MULTIPLE_PREDICTIONS);

        // 2026-01-05 02:47:00 UTC
        assertEquals(1767581220L, predictions.get(0).epochSeconds);
        // 2026-01-05 05:23:00 UTC
        assertEquals(1767590580L, predictions.get(1).epochSeconds);
        // 2026-01-05 08:55:00 UTC
        assertEquals(1767603300L, predictions.get(2).epochSeconds);
        // 2026-01-05 11:33:00 UTC
        assertEquals(1767612780L, predictions.get(3).epochSeconds);
    }

    @Test
    public void testParseBin() throws IOException {
        List<CurrentPrediction> predictions = parse(MULTIPLE_PREDICTIONS);

        assertEquals("1", predictions.get(0).bin);
    }

    @Test
    public void testParseBinDifferentValue() throws IOException {
        List<CurrentPrediction> predictions = parse(PREDICTION_WITH_DEPTH);

        assertEquals("14", predictions.get(0).bin);
    }

    @Test
    public void testParseDepth() throws IOException {
        List<CurrentPrediction> predictions = parse(PREDICTION_WITH_DEPTH);

        assertEquals(Double.valueOf(5.5), predictions.get(0).depth);
    }

    @Test
    public void testParseNullDepth() throws IOException {
        List<CurrentPrediction> predictions = parse(SINGLE_PREDICTION);

        assertNull(predictions.get(0).depth);
    }

    @Test
    public void testParseDifferentDirections() throws IOException {
        List<CurrentPrediction> predictions = parse(PREDICTION_WITH_DEPTH);

        CurrentPrediction prediction = predictions.get(0);
        assertEquals(45.0, prediction.meanFloodDirection, 0.01);
        assertEquals(225.0, prediction.meanEbbDirection, 0.01);
    }

    @Test
    public void testParseEmptyPredictions() throws IOException {
        String json = """
                {
                  "current_predictions": {
                    "units": "meters, cm/s",
                    "cp": []
                  }
                }
                """;
        List<CurrentPrediction> predictions = parse(json);

        assertEquals(0, predictions.size());
    }

    @Test
    public void testParseFromString() throws IOException {
        List<CurrentPrediction> predictions = CurrentPredictionParser.parse(SINGLE_PREDICTION);

        assertEquals(1, predictions.size());
        assertEquals("flood", predictions.get(0).type);
    }

    @Test
    public void testParseMalformedJsonThrows() {
        String malformed = "not valid json {{{";
        assertThrows(IOException.class, () -> parse(malformed));
    }

    @Test
    public void testParseMissingCurrentPredictionsThrows() {
        String json = """
                {
                  "other": "data"
                }
                """;
        assertThrows(IOException.class, () -> parse(json));
    }

    @Test
    public void testParseMissingCpArrayThrows() {
        String json = """
                {
                  "current_predictions": {
                    "units": "meters, cm/s"
                  }
                }
                """;
        assertThrows(IOException.class, () -> parse(json));
    }

    @Test
    public void testParseMissingTimeThrows() {
        String json = """
                {
                  "current_predictions": {
                    "cp": [
                      {
                        "Type": "flood",
                        "Velocity_Major": 160.7,
                        "meanFloodDir": 210,
                        "meanEbbDir": 40
                      }
                    ]
                  }
                }
                """;
        assertThrows(IOException.class, () -> parse(json));
    }

    @Test
    public void testParseMissingTypeThrows() {
        String json = """
                {
                  "current_predictions": {
                    "cp": [
                      {
                        "Time": "2026-01-05 02:47",
                        "Velocity_Major": 160.7,
                        "meanFloodDir": 210,
                        "meanEbbDir": 40
                      }
                    ]
                  }
                }
                """;
        assertThrows(IOException.class, () -> parse(json));
    }

    @Test
    public void testParseMissingVelocityThrows() {
        String json = """
                {
                  "current_predictions": {
                    "cp": [
                      {
                        "Type": "flood",
                        "Time": "2026-01-05 02:47",
                        "meanFloodDir": 210,
                        "meanEbbDir": 40
                      }
                    ]
                  }
                }
                """;
        assertThrows(IOException.class, () -> parse(json));
    }

    @Test
    public void testParseInvalidTimestampThrows() {
        String json = """
                {
                  "current_predictions": {
                    "cp": [
                      {
                        "Type": "flood",
                        "Time": "invalid-timestamp",
                        "Velocity_Major": 160.7,
                        "meanFloodDir": 210,
                        "meanEbbDir": 40
                      }
                    ]
                  }
                }
                """;
        assertThrows(IOException.class, () -> parse(json));
    }
}


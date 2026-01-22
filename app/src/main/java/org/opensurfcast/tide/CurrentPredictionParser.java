package org.opensurfcast.tide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser for NOAA CO-OPS current prediction JSON responses.
 * <p>
 * Expects data requested with {@code units=metric} and {@code time_zone=gmt}.
 * <p>
 * Data source: <a href="https://api.tidesandcurrents.noaa.gov/api/prod/">CO-OPS Data API</a>
 * <p>
 * Example JSON structure:
 * <pre>
 * {
 *   "current_predictions": {
 *     "units": "meters, cm/s",
 *     "cp": [
 *       {
 *         "Type": "flood",
 *         "Time": "2026-01-05 02:47",
 *         "Velocity_Major": 160.7,
 *         "meanFloodDir": 210,
 *         "meanEbbDir": 40,
 *         "Bin": "1",
 *         "Depth": null
 *       },
 *       {
 *         "Type": "slack",
 *         "Time": "2026-01-05 05:23",
 *         "Velocity_Major": 0,
 *         "meanFloodDir": 210,
 *         "meanEbbDir": 40,
 *         "Bin": "1",
 *         "Depth": null
 *       },
 *       {
 *         "Type": "ebb",
 *         "Time": "2026-01-05 08:55",
 *         "Velocity_Major": -169.3,
 *         "meanFloodDir": 210,
 *         "meanEbbDir": 40,
 *         "Bin": "1",
 *         "Depth": null
 *       }
 *     ]
 *   }
 * }
 * </pre>
 */
public class CurrentPredictionParser {

    private static final String FIELD_CURRENT_PREDICTIONS = "current_predictions";
    private static final String FIELD_CP = "cp";
    private static final String FIELD_TYPE = "Type";
    private static final String FIELD_TIME = "Time";
    private static final String FIELD_VELOCITY_MAJOR = "Velocity_Major";
    private static final String FIELD_MEAN_FLOOD_DIR = "meanFloodDir";
    private static final String FIELD_MEAN_EBB_DIR = "meanEbbDir";
    private static final String FIELD_BIN = "Bin";
    private static final String FIELD_DEPTH = "Depth";

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Parses a list of current predictions from the Data API JSON response.
     *
     * @param inputStream the input stream containing the JSON data
     * @return list of parsed CurrentPrediction objects
     * @throws IOException if an error occurs during parsing
     */
    public static List<CurrentPrediction> parse(InputStream inputStream) throws IOException {
        String json = readStream(inputStream);
        return parse(json);
    }

    /**
     * Parses a list of current predictions from the Data API JSON response.
     *
     * @param json the JSON string containing the prediction data
     * @return list of parsed CurrentPrediction objects
     * @throws IOException if an error occurs during parsing
     */
    public static List<CurrentPrediction> parse(String json) throws IOException {
        try {
            JSONObject root = new JSONObject(json);
            JSONObject currentPredictions = root.getJSONObject(FIELD_CURRENT_PREDICTIONS);
            JSONArray cpArray = currentPredictions.getJSONArray(FIELD_CP);

            List<CurrentPrediction> predictions = new ArrayList<>(cpArray.length());
            for (int i = 0; i < cpArray.length(); i++) {
                JSONObject predictionJson = cpArray.getJSONObject(i);
                predictions.add(parsePrediction(predictionJson));
            }

            return predictions;
        } catch (JSONException e) {
            throw new IOException("Failed to parse current predictions JSON", e);
        }
    }

    /**
     * Parses a single current prediction from a JSON object.
     *
     * @param json the JSON object containing prediction data
     * @return parsed CurrentPrediction object
     * @throws JSONException if required fields are missing or invalid
     */
    public static CurrentPrediction parsePrediction(JSONObject json) throws JSONException {
        CurrentPrediction prediction = new CurrentPrediction();

        prediction.timestamp = json.getString(FIELD_TIME);
        prediction.epochSeconds = parseEpochSeconds(prediction.timestamp);
        prediction.type = json.getString(FIELD_TYPE);
        prediction.velocityMajor = json.getDouble(FIELD_VELOCITY_MAJOR);
        prediction.meanFloodDirection = json.getDouble(FIELD_MEAN_FLOOD_DIR);
        prediction.meanEbbDirection = json.getDouble(FIELD_MEAN_EBB_DIR);
        prediction.bin = json.optString(FIELD_BIN, null);
        prediction.depth = optDouble(json, FIELD_DEPTH);

        return prediction;
    }

    /**
     * Parses a timestamp string to Unix epoch seconds.
     * Assumes the timestamp is in UTC (time_zone=gmt).
     *
     * @param timestamp timestamp in format "yyyy-MM-dd HH:mm"
     * @return Unix epoch seconds
     * @throws JSONException if the timestamp format is invalid
     */
    private static long parseEpochSeconds(String timestamp) throws JSONException {
        try {
            return LocalDateTime
                    .parse(timestamp, TIMESTAMP_FORMAT)
                    .toInstant(ZoneOffset.UTC)
                    .getEpochSecond();
        } catch (DateTimeParseException e) {
            throw new JSONException("Invalid timestamp format: " + timestamp);
        }
    }

    /**
     * Returns a Double value from the JSON object, or null if the field is missing or null.
     */
    private static Double optDouble(JSONObject json, String field) {
        if (json.isNull(field)) {
            return null;
        }
        double value = json.optDouble(field, Double.NaN);
        return Double.isNaN(value) ? null : value;
    }

    /**
     * Reads the entire input stream into a String.
     */
    private static String readStream(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
}


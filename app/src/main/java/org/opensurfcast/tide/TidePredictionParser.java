package org.opensurfcast.tide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

import org.opensurfcast.io.IoUtils;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser for NOAA CO-OPS tide prediction JSON responses.
 * <p>
 * Expects data requested with {@code units=metric} and {@code time_zone=gmt}.
 * <p>
 * Data source: <a href="https://api.tidesandcurrents.noaa.gov/api/prod/">CO-OPS Data API</a>
 * <p>
 * Example JSON structure:
 * <pre>
 * {
 *   "predictions": [
 *     {"t": "2026-01-02 00:41", "v": "2.314", "type": "H"},
 *     {"t": "2026-01-02 05:31", "v": "0.829", "type": "L"},
 *     {"t": "2026-01-02 10:57", "v": "2.314", "type": "H"},
 *     {"t": "2026-01-02 18:50", "v": "-0.435", "type": "L"}
 *   ]
 * }
 * </pre>
 */
public class TidePredictionParser {

    private static final String FIELD_PREDICTIONS = "predictions";
    private static final String FIELD_TIMESTAMP = "t";
    private static final String FIELD_VALUE = "v";
    private static final String FIELD_TYPE = "type";

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Parses a list of tide predictions from the Data API JSON response.
     *
     * @param inputStream the input stream containing the JSON data
     * @return list of parsed TidePrediction objects
     * @throws IOException if an error occurs during parsing
     */
    public static List<TidePrediction> parse(InputStream inputStream) throws IOException {
        String json = IoUtils.readStreamToString(inputStream);

        try {
            JSONObject root = new JSONObject(json);
            JSONArray predictionsArray = root.getJSONArray(FIELD_PREDICTIONS);

            List<TidePrediction> predictions = new ArrayList<>(predictionsArray.length());
            for (int i = 0; i < predictionsArray.length(); i++) {
                JSONObject predictionJson = predictionsArray.getJSONObject(i);
                predictions.add(parsePrediction(predictionJson));
            }

            return predictions;
        } catch (JSONException e) {
            throw new IOException("Failed to parse tide predictions JSON", e);
        }
    }

    /**
     * Parses a single tide prediction from a JSON object.
     *
     * @param json the JSON object containing prediction data
     * @return parsed TidePrediction object
     * @throws JSONException if required fields are missing or invalid
     */
    public static TidePrediction parsePrediction(JSONObject json) throws JSONException {
        TidePrediction prediction = new TidePrediction();

        prediction.timestamp = json.getString(FIELD_TIMESTAMP);
        prediction.epochSeconds = parseEpochSeconds(prediction.timestamp);
        prediction.value = parseDouble(json.getString(FIELD_VALUE));
        prediction.type = json.getString(FIELD_TYPE);

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
                    .toInstant(ZoneOffset.UTC).getEpochSecond();
        } catch (DateTimeParseException e) {
            throw new JSONException("Invalid timestamp format: " + timestamp);
        }
    }

    /**
     * Parses a double value from a string.
     * The API returns numeric values as strings.
     */
    private static double parseDouble(String value) throws JSONException {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new JSONException("Invalid numeric value: " + value);
        }
    }

}

package org.opensurfcast.tide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

import org.opensurfcast.io.IoUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser for NOAA CO-OPS current station JSON responses.
 * Data source: <a href="https://api.tidesandcurrents.noaa.gov/mdapi/prod/webapi/stations.json?type=currentpredictions&units=metric">Current Prediction Stations</a>
 * <p>
 * Note: This parser assumes metric units (meters for depth).
 * API Documentation: <a href="https://api.tidesandcurrents.noaa.gov/mdapi/prod/">CO-OPS Metadata API v1.0</a>
 * <p>
 * Example JSON structure (list response):
 * <pre>
 * {
 *   "count": 4432,
 *   "units": "metric",
 *   "stations": [
 *     {
 *       "currentpredictionoffsets": {
 *         "self": "https://api.tidesandcurrents.noaa.gov/mdapi/prod/webapi/stations/ACT0091_1/currentpredictionoffsets.json"
 *       },
 *       "currbin": 1,
 *       "type": "S",
 *       "depth": null,
 *       "depthType": "U",
 *       "timezone_offset": "",
 *       "harmonicConstituents": {
 *         "self": "https://api.tidesandcurrents.noaa.gov/mdapi/prod/webapi/stations/ACT0091/harcon.json"
 *       },
 *       "id": "ACT0091",
 *       "name": "Eastport, Friar Roads",
 *       "lat": 44.9,
 *       "lng": -66.98333,
 *       "affiliations": "",
 *       "portscode": "",
 *       "products": null,
 *       "disclaimers": null,
 *       "notices": null,
 *       "self": "https://api.tidesandcurrents.noaa.gov/mdapi/prod/webapi/stations/ACT0091.json",
 *       "expand": "currentpredictionoffsets,harcon",
 *       "tideType": ""
 *     },
 *     ...
 *   ]
 * }
 * </pre>
 */
public class CurrentStationParser {

    private static final String FIELD_STATIONS = "stations";
    private static final String FIELD_ID = "id";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_LAT = "lat";
    private static final String FIELD_LNG = "lng";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_CURR_BIN = "currbin";
    private static final String FIELD_DEPTH = "depth";
    private static final String FIELD_DEPTH_TYPE = "depthType";
    private static final String FIELD_TIMEZONE_OFFSET = "timezone_offset";
    private static final String FIELD_AFFILIATIONS = "affiliations";
    private static final String FIELD_PORTS_CODE = "portscode";
    private static final String FIELD_TIDE_TYPE = "tideType";
    private static final String FIELD_SELF = "self";
    private static final String FIELD_EXPAND = "expand";
    private static final String FIELD_CURRENT_PREDICTION_OFFSETS = "currentpredictionoffsets";
    private static final String FIELD_HARMONIC_CONSTITUENTS = "harmonicConstituents";

    /**
     * Parses a list of current stations from the MDAPI JSON response.
     *
     * @param inputStream the input stream containing the JSON data
     * @return list of parsed CurrentStation objects
     * @throws IOException if an error occurs during parsing
     */
    public static List<CurrentStation> parseStationList(InputStream inputStream) throws IOException {
        String json = IoUtils.readStreamToString(inputStream);
        return parseStationList(json);
    }

    /**
     * Parses a list of current stations from the MDAPI JSON response.
     *
     * @param json the JSON string containing the station data
     * @return list of parsed CurrentStation objects
     * @throws IOException if an error occurs during parsing
     */
    public static List<CurrentStation> parseStationList(String json) throws IOException {
        try {
            JSONObject root = new JSONObject(json);
            JSONArray stationsArray = root.getJSONArray(FIELD_STATIONS);

            List<CurrentStation> stations = new ArrayList<>(stationsArray.length());
            for (int i = 0; i < stationsArray.length(); i++) {
                JSONObject stationJson = stationsArray.getJSONObject(i);
                stations.add(parseStation(stationJson));
            }

            return stations;
        } catch (JSONException e) {
            throw new IOException("Failed to parse current stations JSON", e);
        }
    }

    /**
     * Parses a single current station from a JSON object.
     *
     * @param json the JSON object containing station data
     * @return parsed CurrentStation object
     * @throws JSONException if required fields are missing or invalid
     */
    public static CurrentStation parseStation(JSONObject json) throws JSONException {
        CurrentStation station = new CurrentStation();

        station.id = json.getString(FIELD_ID);
        station.name = json.optString(FIELD_NAME, "");
        station.latitude = json.getDouble(FIELD_LAT);
        station.longitude = json.getDouble(FIELD_LNG);
        station.type = json.optString(FIELD_TYPE, "");
        station.currentBin = optInteger(json, FIELD_CURR_BIN);
        station.depth = optDouble(json, FIELD_DEPTH);
        station.depthType = json.optString(FIELD_DEPTH_TYPE, "");
        station.timezoneOffset = json.optString(FIELD_TIMEZONE_OFFSET, "");
        station.affiliations = json.optString(FIELD_AFFILIATIONS, "");
        station.portsCode = json.optString(FIELD_PORTS_CODE, null);
        station.tideType = json.optString(FIELD_TIDE_TYPE, "");
        station.selfUrl = json.optString(FIELD_SELF, null);
        station.expand = json.optString(FIELD_EXPAND, null);

        // Parse nested objects for URLs
        station.currentPredictionOffsetsUrl = parseNestedSelfUrl(json, FIELD_CURRENT_PREDICTION_OFFSETS);
        station.harmonicConstituentsUrl = parseNestedSelfUrl(json, FIELD_HARMONIC_CONSTITUENTS);

        return station;
    }

    /**
     * Parses the "self" URL from a nested JSON object.
     *
     * @param json the parent JSON object
     * @param field the field name containing the nested object
     * @return the "self" URL, or null if not present
     */
    private static String parseNestedSelfUrl(JSONObject json, String field) {
        if (json.isNull(field) || !json.has(field)) {
            return null;
        }
        JSONObject nested = json.optJSONObject(field);
        if (nested == null) {
            return null;
        }
        return nested.optString(FIELD_SELF, null);
    }

    /**
     * Returns an Integer value from the JSON object, or null if the field is missing or null.
     */
    private static Integer optInteger(JSONObject json, String field) {
        if (json.isNull(field)) {
            return null;
        }
        int value = json.optInt(field, Integer.MIN_VALUE);
        return value == Integer.MIN_VALUE ? null : value;
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

}


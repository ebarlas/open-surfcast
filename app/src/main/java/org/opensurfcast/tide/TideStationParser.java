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
 * Parser for NOAA CO-OPS tide station JSON responses.
 * Data source: <a href="https://api.tidesandcurrents.noaa.gov/mdapi/prod/webapi/stations.json?type=tidepredictions">Tide Prediction Stations</a>
 * API Documentation: <a href="https://api.tidesandcurrents.noaa.gov/mdapi/prod/">CO-OPS Metadata API v1.0</a>
 * <p>
 * Example JSON structure (list response):
 * <pre>
 * {
 *   "count": 3379,
 *   "units": null,
 *   "stations": [
 *     {
 *       "id": "9415252",
 *       "name": "Petaluma River entrance",
 *       "state": "CA",
 *       "lat": 38.115307,
 *       "lng": -122.50567,
 *       "type": "S",
 *       "reference_id": "9414290",
 *       "timemeridian": -120,
 *       "timezonecorr": -8,
 *       "tideType": "Mixed",
 *       "affiliations": "",
 *       "portscode": ""
 *     },
 *     ...
 *   ]
 * }
 * </pre>
 */
public class TideStationParser {

    private static final String FIELD_STATIONS = "stations";
    private static final String FIELD_ID = "id";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_STATE = "state";
    private static final String FIELD_LAT = "lat";
    private static final String FIELD_LNG = "lng";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_REFERENCE_ID = "reference_id";
    private static final String FIELD_TIME_MERIDIAN = "timemeridian";
    private static final String FIELD_TIMEZONE_CORR = "timezonecorr";
    private static final String FIELD_TIDE_TYPE = "tideType";
    private static final String FIELD_AFFILIATIONS = "affiliations";
    private static final String FIELD_PORTS_CODE = "portscode";
    private static final String FIELD_TIMEZONE = "timezone";
    private static final String FIELD_OBSERVED_ST = "observedst";
    private static final String FIELD_TIDAL = "tidal";
    private static final String FIELD_GREAT_LAKES = "greatlakes";

    /**
     * Parses a list of tide stations from the MDAPI JSON response.
     *
     * @param inputStream the input stream containing the JSON data
     * @return list of parsed TideStation objects
     * @throws IOException if an error occurs during parsing
     */
    public static List<TideStation> parseStationList(InputStream inputStream) throws IOException {
        String json = IoUtils.readStreamToString(inputStream);

        try {
            JSONObject root = new JSONObject(json);
            JSONArray stationsArray = root.getJSONArray(FIELD_STATIONS);

            List<TideStation> stations = new ArrayList<>(stationsArray.length());
            for (int i = 0; i < stationsArray.length(); i++) {
                JSONObject stationJson = stationsArray.getJSONObject(i);
                stations.add(parseStation(stationJson));
            }

            return stations;
        } catch (JSONException e) {
            throw new IOException("Failed to parse tide stations JSON", e);
        }
    }

    /**
     * Parses a single tide station from a JSON object.
     *
     * @param json the JSON object containing station data
     * @return parsed TideStation object
     * @throws JSONException if required fields are missing or invalid
     */
    public static TideStation parseStation(JSONObject json) throws JSONException {
        TideStation station = new TideStation();

        station.id = json.getString(FIELD_ID);
        station.name = json.optString(FIELD_NAME, "");
        station.state = json.optString(FIELD_STATE, "");
        station.latitude = json.getDouble(FIELD_LAT);
        station.longitude = json.getDouble(FIELD_LNG);
        station.type = json.optString(FIELD_TYPE, "");
        station.referenceId = json.optString(FIELD_REFERENCE_ID, "");
        station.timeMeridian = optInteger(json, FIELD_TIME_MERIDIAN);
        station.timeZoneCorrection = json.optInt(FIELD_TIMEZONE_CORR, 0);
        station.tideType = json.optString(FIELD_TIDE_TYPE, "");
        station.affiliations = json.optString(FIELD_AFFILIATIONS, "");
        station.portsCode = json.optString(FIELD_PORTS_CODE, null);
        station.timezone = json.optString(FIELD_TIMEZONE, null);
        station.observesDst = optBoolean(json, FIELD_OBSERVED_ST);
        station.tidal = optBoolean(json, FIELD_TIDAL);
        station.greatLakes = optBoolean(json, FIELD_GREAT_LAKES);

        return station;
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
     * Returns a Boolean value from the JSON object, or null if the field is missing or null.
     */
    private static Boolean optBoolean(JSONObject json, String field) {
        if (json.isNull(field) || !json.has(field)) {
            return null;
        }
        return json.optBoolean(field);
    }

}


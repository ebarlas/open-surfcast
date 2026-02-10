package org.opensurfcast.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.opensurfcast.buoy.BuoyStdMetData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Database operations for buoy standard meteorological data.
 */
public class BuoyStdMetDataDb {

    private final OpenSurfcastDbHelper dbHelper;

    public BuoyStdMetDataDb(OpenSurfcastDbHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * Replaces all standard meteorological data for a station with the provided list.
     * This deletes all existing data for the station and inserts the new records.
     *
     * @param stationId the station ID
     * @param dataList  the new list of data records
     */
    public void replaceAllForStation(String stationId, List<BuoyStdMetData> dataList) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(OpenSurfcastDbHelper.TABLE_BUOY_STD_MET_DATA,
                    "id = ?", new String[]{stationId});
            for (BuoyStdMetData data : dataList) {
                db.insert(OpenSurfcastDbHelper.TABLE_BUOY_STD_MET_DATA, null,
                        toContentValues(stationId, data));
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Returns all standard meteorological data for a station.
     *
     * @param stationId the station ID
     * @return list of data records for the station
     */
    public List<BuoyStdMetData> queryByStation(String stationId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                OpenSurfcastDbHelper.TABLE_BUOY_STD_MET_DATA,
                null,
                "id = ?",
                new String[]{stationId},
                null, null,
                "epoch_seconds ASC")) {
            return SqlUtils.map(cursor, this::fromCursor);
        }
    }

    /**
     * Returns all standard meteorological data for a list of stations.
     *
     * @param stationIds the list of station IDs
     * @return list of data records for all specified stations
     */
    public List<BuoyStdMetData> queryByStations(List<String> stationIds) {
        if (stationIds.isEmpty()) {
            return Collections.emptyList();
        }
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String placeholders = SqlUtils.buildPlaceholders(stationIds.size());
        try (Cursor cursor = db.query(
                OpenSurfcastDbHelper.TABLE_BUOY_STD_MET_DATA,
                null,
                "id IN (" + placeholders + ")",
                stationIds.toArray(new String[0]),
                null, null,
                "id ASC, epoch_seconds ASC")) {
            return SqlUtils.map(cursor, this::fromCursor);
        }
    }

    /**
     * Returns the most recent observation for each of the given stations.
     *
     * @param stationIds the station IDs to query
     * @return map of station ID to its latest observation; stations with no data are absent
     */
    public Map<String, BuoyStdMetData> queryLatestByStations(Collection<String> stationIds) {
        if (stationIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> idList = new ArrayList<>(stationIds);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String placeholders = SqlUtils.buildPlaceholders(idList.size());
        try (Cursor cursor = db.query(
                OpenSurfcastDbHelper.TABLE_BUOY_STD_MET_DATA,
                null,
                "id IN (" + placeholders + ") AND wave_height IS NOT NULL AND dominant_wave_period IS NOT NULL",
                idList.toArray(new String[0]),
                null, null,
                "id ASC, epoch_seconds DESC")) {
            Map<String, BuoyStdMetData> result = new HashMap<>();
            int idIndex = cursor.getColumnIndexOrThrow("id");
            while (cursor.moveToNext()) {
                String id = cursor.getString(idIndex);
                if (!result.containsKey(id)) {
                    result.put(id, fromCursor(cursor));
                }
            }
            return result;
        }
    }

    /**
     * Returns the most recent observation for a single station.
     *
     * @param stationId the station ID to query
     * @return the latest observation for the station, or null if no data exists
     */
    public BuoyStdMetData queryLatestByStation(String stationId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                OpenSurfcastDbHelper.TABLE_BUOY_STD_MET_DATA,
                null,
                "id = ? AND wave_height IS NOT NULL AND dominant_wave_period IS NOT NULL",
                new String[]{stationId},
                null, null,
                "epoch_seconds DESC",
                "1")) {
            if (cursor.moveToFirst()) {
                return fromCursor(cursor);
            }
            return null;
        }
    }

    private ContentValues toContentValues(String stationId, BuoyStdMetData data) {
        ContentValues values = new ContentValues();
        values.put("id", stationId);
        values.put("epoch_seconds", data.getEpochSeconds());
        values.put("year", data.getYear());
        values.put("month", data.getMonth());
        values.put("day", data.getDay());
        values.put("hour", data.getHour());
        values.put("minute", data.getMinute());
        values.put("wind_direction", data.getWindDirection());
        values.put("wind_speed", data.getWindSpeed());
        values.put("gust_speed", data.getGustSpeed());
        values.put("wave_height", data.getWaveHeight());
        values.put("dominant_wave_period", data.getDominantWavePeriod());
        values.put("average_wave_period", data.getAverageWavePeriod());
        values.put("mean_wave_direction", data.getMeanWaveDirection());
        values.put("pressure", data.getPressure());
        values.put("air_temperature", data.getAirTemperature());
        values.put("water_temperature", data.getWaterTemperature());
        values.put("dew_point", data.getDewPoint());
        values.put("visibility", data.getVisibility());
        values.put("pressure_tendency", data.getPressureTendency());
        values.put("tide", data.getTide());
        return values;
    }

    private BuoyStdMetData fromCursor(Cursor cursor) {
        BuoyStdMetData data = new BuoyStdMetData();
        data.setEpochSeconds(cursor.getLong(cursor.getColumnIndexOrThrow("epoch_seconds")));
        data.setYear(cursor.getInt(cursor.getColumnIndexOrThrow("year")));
        data.setMonth(cursor.getInt(cursor.getColumnIndexOrThrow("month")));
        data.setDay(cursor.getInt(cursor.getColumnIndexOrThrow("day")));
        data.setHour(cursor.getInt(cursor.getColumnIndexOrThrow("hour")));
        data.setMinute(cursor.getInt(cursor.getColumnIndexOrThrow("minute")));
        int windDirIndex = cursor.getColumnIndexOrThrow("wind_direction");
        data.setWindDirection(cursor.isNull(windDirIndex) ? null : cursor.getInt(windDirIndex));
        int windSpeedIndex = cursor.getColumnIndexOrThrow("wind_speed");
        data.setWindSpeed(cursor.isNull(windSpeedIndex) ? null : cursor.getDouble(windSpeedIndex));
        int gustSpeedIndex = cursor.getColumnIndexOrThrow("gust_speed");
        data.setGustSpeed(cursor.isNull(gustSpeedIndex) ? null : cursor.getDouble(gustSpeedIndex));
        int waveHeightIndex = cursor.getColumnIndexOrThrow("wave_height");
        data.setWaveHeight(cursor.isNull(waveHeightIndex) ? null : cursor.getDouble(waveHeightIndex));
        int domWavePeriodIndex = cursor.getColumnIndexOrThrow("dominant_wave_period");
        data.setDominantWavePeriod(cursor.isNull(domWavePeriodIndex) ? null : cursor.getDouble(domWavePeriodIndex));
        int avgWavePeriodIndex = cursor.getColumnIndexOrThrow("average_wave_period");
        data.setAverageWavePeriod(cursor.isNull(avgWavePeriodIndex) ? null : cursor.getDouble(avgWavePeriodIndex));
        int meanWaveDirIndex = cursor.getColumnIndexOrThrow("mean_wave_direction");
        data.setMeanWaveDirection(cursor.isNull(meanWaveDirIndex) ? null : cursor.getInt(meanWaveDirIndex));
        int pressureIndex = cursor.getColumnIndexOrThrow("pressure");
        data.setPressure(cursor.isNull(pressureIndex) ? null : cursor.getDouble(pressureIndex));
        int airTempIndex = cursor.getColumnIndexOrThrow("air_temperature");
        data.setAirTemperature(cursor.isNull(airTempIndex) ? null : cursor.getDouble(airTempIndex));
        int waterTempIndex = cursor.getColumnIndexOrThrow("water_temperature");
        data.setWaterTemperature(cursor.isNull(waterTempIndex) ? null : cursor.getDouble(waterTempIndex));
        int dewPointIndex = cursor.getColumnIndexOrThrow("dew_point");
        data.setDewPoint(cursor.isNull(dewPointIndex) ? null : cursor.getDouble(dewPointIndex));
        int visibilityIndex = cursor.getColumnIndexOrThrow("visibility");
        data.setVisibility(cursor.isNull(visibilityIndex) ? null : cursor.getDouble(visibilityIndex));
        int pressureTendencyIndex = cursor.getColumnIndexOrThrow("pressure_tendency");
        data.setPressureTendency(cursor.isNull(pressureTendencyIndex) ? null : cursor.getDouble(pressureTendencyIndex));
        int tideIndex = cursor.getColumnIndexOrThrow("tide");
        data.setTide(cursor.isNull(tideIndex) ? null : cursor.getDouble(tideIndex));
        return data;
    }
}

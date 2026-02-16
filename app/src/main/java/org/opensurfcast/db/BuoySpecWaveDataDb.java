package org.opensurfcast.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.opensurfcast.buoy.BuoySpecWaveData;

import java.util.Collections;
import java.util.List;

/**
 * Database operations for buoy spectral wave data.
 */
public class BuoySpecWaveDataDb {

    private final OpenSurfcastDbHelper dbHelper;

    public BuoySpecWaveDataDb(OpenSurfcastDbHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * Replaces all spectral wave data for a station with the provided list.
     * This deletes all existing data for the station and inserts the new records.
     *
     * @param stationId the station ID
     * @param dataList  the new list of data records
     */
    public void replaceAllForStation(String stationId, List<BuoySpecWaveData> dataList) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(OpenSurfcastDbHelper.TABLE_BUOY_SPEC_WAVE_DATA,
                    "id = ?", new String[]{stationId});
            for (BuoySpecWaveData data : dataList) {
                db.insertOrThrow(OpenSurfcastDbHelper.TABLE_BUOY_SPEC_WAVE_DATA, null,
                        toContentValues(stationId, data));
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Returns all spectral wave data for a station.
     *
     * @param stationId the station ID
     * @return list of data records for the station
     */
    public List<BuoySpecWaveData> queryByStation(String stationId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                OpenSurfcastDbHelper.TABLE_BUOY_SPEC_WAVE_DATA,
                null,
                "id = ?",
                new String[]{stationId},
                null, null,
                "epoch_seconds ASC")) {
            return SqlUtils.map(cursor, this::fromCursor);
        }
    }

    /**
     * Returns all spectral wave data for a list of stations.
     *
     * @param stationIds the list of station IDs
     * @return list of data records for all specified stations
     */
    public List<BuoySpecWaveData> queryByStations(List<String> stationIds) {
        if (stationIds.isEmpty()) {
            return Collections.emptyList();
        }
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String placeholders = SqlUtils.buildPlaceholders(stationIds.size());
        try (Cursor cursor = db.query(
                OpenSurfcastDbHelper.TABLE_BUOY_SPEC_WAVE_DATA,
                null,
                "id IN (" + placeholders + ")",
                stationIds.toArray(new String[0]),
                null, null,
                "id ASC, epoch_seconds ASC")) {
            return SqlUtils.map(cursor, this::fromCursor);
        }
    }

    private ContentValues toContentValues(String stationId, BuoySpecWaveData data) {
        ContentValues values = new ContentValues();
        values.put("id", stationId);
        values.put("epoch_seconds", data.getEpochSeconds());
        values.put("year", data.getYear());
        values.put("month", data.getMonth());
        values.put("day", data.getDay());
        values.put("hour", data.getHour());
        values.put("minute", data.getMinute());
        values.put("wave_height", data.getWaveHeight());
        values.put("swell_height", data.getSwellHeight());
        values.put("swell_period", data.getSwellPeriod());
        values.put("wind_wave_height", data.getWindWaveHeight());
        values.put("wind_wave_period", data.getWindWavePeriod());
        values.put("swell_direction", data.getSwellDirection());
        values.put("wind_wave_direction", data.getWindWaveDirection());
        values.put("steepness", data.getSteepness());
        values.put("average_wave_period", data.getAverageWavePeriod());
        values.put("mean_wave_direction", data.getMeanWaveDirection());
        return values;
    }

    private BuoySpecWaveData fromCursor(Cursor cursor) {
        BuoySpecWaveData data = new BuoySpecWaveData();
        data.setEpochSeconds(cursor.getLong(cursor.getColumnIndexOrThrow("epoch_seconds")));
        data.setYear(cursor.getInt(cursor.getColumnIndexOrThrow("year")));
        data.setMonth(cursor.getInt(cursor.getColumnIndexOrThrow("month")));
        data.setDay(cursor.getInt(cursor.getColumnIndexOrThrow("day")));
        data.setHour(cursor.getInt(cursor.getColumnIndexOrThrow("hour")));
        data.setMinute(cursor.getInt(cursor.getColumnIndexOrThrow("minute")));
        int waveHeightIndex = cursor.getColumnIndexOrThrow("wave_height");
        data.setWaveHeight(cursor.isNull(waveHeightIndex) ? null : cursor.getDouble(waveHeightIndex));
        int swellHeightIndex = cursor.getColumnIndexOrThrow("swell_height");
        data.setSwellHeight(cursor.isNull(swellHeightIndex) ? null : cursor.getDouble(swellHeightIndex));
        int swellPeriodIndex = cursor.getColumnIndexOrThrow("swell_period");
        data.setSwellPeriod(cursor.isNull(swellPeriodIndex) ? null : cursor.getDouble(swellPeriodIndex));
        int windWaveHeightIndex = cursor.getColumnIndexOrThrow("wind_wave_height");
        data.setWindWaveHeight(cursor.isNull(windWaveHeightIndex) ? null : cursor.getDouble(windWaveHeightIndex));
        int windWavePeriodIndex = cursor.getColumnIndexOrThrow("wind_wave_period");
        data.setWindWavePeriod(cursor.isNull(windWavePeriodIndex) ? null : cursor.getDouble(windWavePeriodIndex));
        data.setSwellDirection(cursor.getString(cursor.getColumnIndexOrThrow("swell_direction")));
        data.setWindWaveDirection(cursor.getString(cursor.getColumnIndexOrThrow("wind_wave_direction")));
        data.setSteepness(cursor.getString(cursor.getColumnIndexOrThrow("steepness")));
        int avgWavePeriodIndex = cursor.getColumnIndexOrThrow("average_wave_period");
        data.setAverageWavePeriod(cursor.isNull(avgWavePeriodIndex) ? null : cursor.getDouble(avgWavePeriodIndex));
        int meanWaveDirIndex = cursor.getColumnIndexOrThrow("mean_wave_direction");
        data.setMeanWaveDirection(cursor.isNull(meanWaveDirIndex) ? null : cursor.getInt(meanWaveDirIndex));
        return data;
    }
}

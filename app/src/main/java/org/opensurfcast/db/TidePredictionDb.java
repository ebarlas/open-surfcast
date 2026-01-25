package org.opensurfcast.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.opensurfcast.tide.TidePrediction;

import java.util.Collections;
import java.util.List;

/**
 * Database operations for tide predictions.
 */
public class TidePredictionDb {

    private final OpenSurfcastDbHelper dbHelper;

    public TidePredictionDb(OpenSurfcastDbHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * Replaces all tide predictions for a station with the provided list.
     * This deletes all existing data for the station and inserts the new records.
     *
     * @param stationId the station ID
     * @param dataList  the new list of predictions
     */
    public void replaceAllForStation(String stationId, List<TidePrediction> dataList) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(OpenSurfcastDbHelper.TABLE_TIDE_PREDICTION,
                    "id = ?", new String[]{stationId});
            for (TidePrediction data : dataList) {
                db.insert(OpenSurfcastDbHelper.TABLE_TIDE_PREDICTION, null,
                        toContentValues(stationId, data));
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Returns all tide predictions for a station.
     *
     * @param stationId the station ID
     * @return list of predictions for the station
     */
    public List<TidePrediction> queryByStation(String stationId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                OpenSurfcastDbHelper.TABLE_TIDE_PREDICTION,
                null,
                "id = ?",
                new String[]{stationId},
                null, null,
                "epoch_seconds ASC")) {
            return SqlUtils.map(cursor, this::fromCursor);
        }
    }

    /**
     * Returns all tide predictions for a list of stations.
     *
     * @param stationIds the list of station IDs
     * @return list of predictions for all specified stations
     */
    public List<TidePrediction> queryByStations(List<String> stationIds) {
        if (stationIds.isEmpty()) {
            return Collections.emptyList();
        }
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String placeholders = SqlUtils.buildPlaceholders(stationIds.size());
        try (Cursor cursor = db.query(
                OpenSurfcastDbHelper.TABLE_TIDE_PREDICTION,
                null,
                "id IN (" + placeholders + ")",
                stationIds.toArray(new String[0]),
                null, null,
                "id ASC, epoch_seconds ASC")) {
            return SqlUtils.map(cursor, this::fromCursor);
        }
    }

    private ContentValues toContentValues(String stationId, TidePrediction data) {
        ContentValues values = new ContentValues();
        values.put("id", stationId);
        values.put("epoch_seconds", data.epochSeconds);
        values.put("timestamp", data.timestamp);
        values.put("value", data.value);
        values.put("type", data.type);
        return values;
    }

    private TidePrediction fromCursor(Cursor cursor) {
        TidePrediction data = new TidePrediction();
        data.epochSeconds = cursor.getLong(cursor.getColumnIndexOrThrow("epoch_seconds"));
        data.timestamp = cursor.getString(cursor.getColumnIndexOrThrow("timestamp"));
        data.value = cursor.getDouble(cursor.getColumnIndexOrThrow("value"));
        data.type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
        return data;
    }
}

package org.opensurfcast.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.opensurfcast.tide.CurrentPrediction;

import java.util.Collections;
import java.util.List;

/**
 * Database operations for current predictions.
 */
public class CurrentPredictionDb {

    private final OpenSurfcastDbHelper dbHelper;

    public CurrentPredictionDb(OpenSurfcastDbHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * Replaces all current predictions for a station with the provided list.
     * This deletes all existing data for the station and inserts the new records.
     *
     * @param stationId the station ID
     * @param dataList  the new list of predictions
     */
    public void replaceAllForStation(String stationId, List<CurrentPrediction> dataList) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(OpenSurfcastDbHelper.TABLE_CURRENT_PREDICTION,
                    "id = ?", new String[]{stationId});
            for (CurrentPrediction data : dataList) {
                db.insert(OpenSurfcastDbHelper.TABLE_CURRENT_PREDICTION, null,
                        toContentValues(stationId, data));
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Returns all current predictions for a station.
     *
     * @param stationId the station ID
     * @return list of predictions for the station
     */
    public List<CurrentPrediction> queryByStation(String stationId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                OpenSurfcastDbHelper.TABLE_CURRENT_PREDICTION,
                null,
                "id = ?",
                new String[]{stationId},
                null, null,
                "epoch_seconds ASC")) {
            return SqlUtils.map(cursor, this::fromCursor);
        }
    }

    /**
     * Returns all current predictions for a list of stations.
     *
     * @param stationIds the list of station IDs
     * @return list of predictions for all specified stations
     */
    public List<CurrentPrediction> queryByStations(List<String> stationIds) {
        if (stationIds.isEmpty()) {
            return Collections.emptyList();
        }
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String placeholders = SqlUtils.buildPlaceholders(stationIds.size());
        try (Cursor cursor = db.query(
                OpenSurfcastDbHelper.TABLE_CURRENT_PREDICTION,
                null,
                "id IN (" + placeholders + ")",
                stationIds.toArray(new String[0]),
                null, null,
                "id ASC, epoch_seconds ASC")) {
            return SqlUtils.map(cursor, this::fromCursor);
        }
    }

    private ContentValues toContentValues(String stationId, CurrentPrediction data) {
        ContentValues values = new ContentValues();
        values.put("id", stationId);
        values.put("epoch_seconds", data.epochSeconds);
        values.put("timestamp", data.timestamp);
        values.put("type", data.type);
        values.put("velocity_major", data.velocityMajor);
        values.put("mean_flood_direction", data.meanFloodDirection);
        values.put("mean_ebb_direction", data.meanEbbDirection);
        values.put("bin", data.bin);
        values.put("depth", data.depth);
        return values;
    }

    private CurrentPrediction fromCursor(Cursor cursor) {
        CurrentPrediction data = new CurrentPrediction();
        data.epochSeconds = cursor.getLong(cursor.getColumnIndexOrThrow("epoch_seconds"));
        data.timestamp = cursor.getString(cursor.getColumnIndexOrThrow("timestamp"));
        data.type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
        data.velocityMajor = cursor.getDouble(cursor.getColumnIndexOrThrow("velocity_major"));
        data.meanFloodDirection = cursor.getDouble(cursor.getColumnIndexOrThrow("mean_flood_direction"));
        data.meanEbbDirection = cursor.getDouble(cursor.getColumnIndexOrThrow("mean_ebb_direction"));
        data.bin = cursor.getString(cursor.getColumnIndexOrThrow("bin"));
        int depthIndex = cursor.getColumnIndexOrThrow("depth");
        data.depth = cursor.isNull(depthIndex) ? null : cursor.getDouble(depthIndex);
        return data;
    }
}

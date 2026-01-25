package org.opensurfcast.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.opensurfcast.tide.CurrentStation;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Database operations for current stations.
 */
public class CurrentStationDb {

    private final OpenSurfcastDbHelper dbHelper;

    public CurrentStationDb(OpenSurfcastDbHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * Replaces the current station catalog using a diff strategy.
     * Inserts missing stations and deletes stations that are no longer present.
     *
     * @param stations the new list of stations
     */
    public void replaceAll(List<CurrentStation> stations) {
        List<CurrentStation> incomingStations = stations == null ? Collections.emptyList() : stations;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            Map<String, CurrentStation> existingStations = queryExistingStations(db);
            Set<String> incomingIds = incomingStations.stream()
                    .map(s -> s.id)
                    .collect(Collectors.toSet());

            Set<String> idsToDelete = new HashSet<>(existingStations.keySet());
            idsToDelete.removeAll(incomingIds);
            deleteByIds(db, idsToDelete);

            for (CurrentStation station : incomingStations) {
                CurrentStation existing = existingStations.get(station.id);
                if (existing == null) {
                    db.insert(OpenSurfcastDbHelper.TABLE_CURRENT_STATION, null, toContentValues(station));
                } else {
                    ContentValues existingValues = toContentValues(existing);
                    ContentValues incomingValues = toContentValues(station);
                    if (!existingValues.equals(incomingValues)) {
                        db.update(OpenSurfcastDbHelper.TABLE_CURRENT_STATION, incomingValues,
                                "id = ?", new String[]{station.id});
                    }
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Returns all current stations.
     *
     * @return list of all current stations
     */
    public List<CurrentStation> queryAll() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                OpenSurfcastDbHelper.TABLE_CURRENT_STATION,
                null, null, null, null, null, null)) {
            return SqlUtils.map(cursor, this::fromCursor);
        }
    }

    private ContentValues toContentValues(CurrentStation station) {
        ContentValues values = new ContentValues();
        values.put("id", station.id);
        values.put("name", station.name);
        values.put("latitude", station.latitude);
        values.put("longitude", station.longitude);
        values.put("type", station.type);
        values.put("current_bin", station.currentBin);
        values.put("depth", station.depth);
        values.put("depth_type", station.depthType);
        values.put("timezone_offset", station.timezoneOffset);
        values.put("affiliations", station.affiliations);
        values.put("ports_code", station.portsCode);
        values.put("tide_type", station.tideType);
        values.put("self_url", station.selfUrl);
        values.put("expand", station.expand);
        values.put("current_prediction_offsets_url", station.currentPredictionOffsetsUrl);
        values.put("harmonic_constituents_url", station.harmonicConstituentsUrl);
        return values;
    }

    private Map<String, CurrentStation> queryExistingStations(SQLiteDatabase db) {
        Map<String, CurrentStation> stations = new HashMap<>();
        try (Cursor cursor = db.query(OpenSurfcastDbHelper.TABLE_CURRENT_STATION,
                null, null, null, null, null, null)) {
            for (CurrentStation station : SqlUtils.map(cursor, this::fromCursor)) {
                stations.put(station.id, station);
            }
        }
        return stations;
    }

    private void deleteByIds(SQLiteDatabase db, Set<String> ids) {
        if (ids.isEmpty()) {
            return;
        }
        String placeholders = SqlUtils.buildPlaceholders(ids.size());
        db.delete(OpenSurfcastDbHelper.TABLE_CURRENT_STATION,
                "id IN (" + placeholders + ")", ids.toArray(new String[0]));
    }

    private CurrentStation fromCursor(Cursor cursor) {
        CurrentStation station = new CurrentStation();
        station.id = cursor.getString(cursor.getColumnIndexOrThrow("id"));
        station.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
        station.latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude"));
        station.longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"));
        station.type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
        int currentBinIndex = cursor.getColumnIndexOrThrow("current_bin");
        station.currentBin = cursor.isNull(currentBinIndex) ? null : cursor.getInt(currentBinIndex);
        int depthIndex = cursor.getColumnIndexOrThrow("depth");
        station.depth = cursor.isNull(depthIndex) ? null : cursor.getDouble(depthIndex);
        station.depthType = cursor.getString(cursor.getColumnIndexOrThrow("depth_type"));
        station.timezoneOffset = cursor.getString(cursor.getColumnIndexOrThrow("timezone_offset"));
        station.affiliations = cursor.getString(cursor.getColumnIndexOrThrow("affiliations"));
        station.portsCode = cursor.getString(cursor.getColumnIndexOrThrow("ports_code"));
        station.tideType = cursor.getString(cursor.getColumnIndexOrThrow("tide_type"));
        station.selfUrl = cursor.getString(cursor.getColumnIndexOrThrow("self_url"));
        station.expand = cursor.getString(cursor.getColumnIndexOrThrow("expand"));
        station.currentPredictionOffsetsUrl = cursor.getString(cursor.getColumnIndexOrThrow("current_prediction_offsets_url"));
        station.harmonicConstituentsUrl = cursor.getString(cursor.getColumnIndexOrThrow("harmonic_constituents_url"));
        return station;
    }
}

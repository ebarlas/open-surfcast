package org.opensurfcast.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.opensurfcast.tide.TideStation;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Database operations for tide stations.
 */
public class TideStationDb {

    private final OpenSurfcastDbHelper dbHelper;

    public TideStationDb(OpenSurfcastDbHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * Replaces the tide station catalog using a diff strategy.
     * Inserts missing stations and deletes stations that are no longer present.
     *
     * @param stations the new list of stations
     */
    public void replaceAll(List<TideStation> stations) {
        List<TideStation> incomingStations = stations == null ? Collections.emptyList() : stations;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            Map<String, TideStation> existingStations = queryExistingStations(db);
            Set<String> incomingIds = incomingStations.stream()
                    .map(s -> s.id)
                    .collect(Collectors.toSet());

            Set<String> idsToDelete = new HashSet<>(existingStations.keySet());
            idsToDelete.removeAll(incomingIds);
            deleteByIds(db, idsToDelete);

            for (TideStation station : incomingStations) {
                TideStation existing = existingStations.get(station.id);
                if (existing == null) {
                    db.insert(OpenSurfcastDbHelper.TABLE_TIDE_STATION, null, toContentValues(station));
                } else {
                    ContentValues existingValues = toContentValues(existing);
                    ContentValues incomingValues = toContentValues(station);
                    if (!existingValues.equals(incomingValues)) {
                        db.update(OpenSurfcastDbHelper.TABLE_TIDE_STATION, incomingValues,
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
     * Returns all tide stations.
     *
     * @return list of all tide stations
     */
    public List<TideStation> queryAll() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                OpenSurfcastDbHelper.TABLE_TIDE_STATION,
                null, null, null, null, null, null)) {
            return SqlUtils.map(cursor, this::fromCursor);
        }
    }

    private ContentValues toContentValues(TideStation station) {
        ContentValues values = new ContentValues();
        values.put("id", station.id);
        values.put("name", station.name);
        values.put("state", station.state);
        values.put("latitude", station.latitude);
        values.put("longitude", station.longitude);
        values.put("type", station.type);
        values.put("reference_id", station.referenceId);
        values.put("time_meridian", station.timeMeridian);
        values.put("time_zone_correction", station.timeZoneCorrection);
        values.put("tide_type", station.tideType);
        values.put("affiliations", station.affiliations);
        values.put("ports_code", station.portsCode);
        values.put("timezone", station.timezone);
        values.put("observes_dst", station.observesDst == null ? null : (station.observesDst ? 1 : 0));
        values.put("tidal", station.tidal == null ? null : (station.tidal ? 1 : 0));
        values.put("great_lakes", station.greatLakes == null ? null : (station.greatLakes ? 1 : 0));
        return values;
    }

    private Map<String, TideStation> queryExistingStations(SQLiteDatabase db) {
        Map<String, TideStation> stations = new HashMap<>();
        try (Cursor cursor = db.query(OpenSurfcastDbHelper.TABLE_TIDE_STATION,
                null, null, null, null, null, null)) {
            for (TideStation station : SqlUtils.map(cursor, this::fromCursor)) {
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
        db.delete(OpenSurfcastDbHelper.TABLE_TIDE_STATION,
                "id IN (" + placeholders + ")", ids.toArray(new String[0]));
    }

    private TideStation fromCursor(Cursor cursor) {
        TideStation station = new TideStation();
        station.id = cursor.getString(cursor.getColumnIndexOrThrow("id"));
        station.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
        station.state = cursor.getString(cursor.getColumnIndexOrThrow("state"));
        station.latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude"));
        station.longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"));
        station.type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
        station.referenceId = cursor.getString(cursor.getColumnIndexOrThrow("reference_id"));
        int timeMeridianIndex = cursor.getColumnIndexOrThrow("time_meridian");
        station.timeMeridian = cursor.isNull(timeMeridianIndex) ? null : cursor.getInt(timeMeridianIndex);
        station.timeZoneCorrection = cursor.getInt(cursor.getColumnIndexOrThrow("time_zone_correction"));
        station.tideType = cursor.getString(cursor.getColumnIndexOrThrow("tide_type"));
        station.affiliations = cursor.getString(cursor.getColumnIndexOrThrow("affiliations"));
        station.portsCode = cursor.getString(cursor.getColumnIndexOrThrow("ports_code"));
        station.timezone = cursor.getString(cursor.getColumnIndexOrThrow("timezone"));
        int observesDstIndex = cursor.getColumnIndexOrThrow("observes_dst");
        station.observesDst = cursor.isNull(observesDstIndex) ? null : cursor.getInt(observesDstIndex) == 1;
        int tidalIndex = cursor.getColumnIndexOrThrow("tidal");
        station.tidal = cursor.isNull(tidalIndex) ? null : cursor.getInt(tidalIndex) == 1;
        int greatLakesIndex = cursor.getColumnIndexOrThrow("great_lakes");
        station.greatLakes = cursor.isNull(greatLakesIndex) ? null : cursor.getInt(greatLakesIndex) == 1;
        return station;
    }

    // No helper for comparing stations; inline in replaceAll.
}

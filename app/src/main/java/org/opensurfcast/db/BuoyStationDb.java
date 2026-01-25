package org.opensurfcast.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.opensurfcast.buoy.BuoyStation;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Database operations for buoy stations.
 */
public class BuoyStationDb {

    private final OpenSurfcastDbHelper dbHelper;

    public BuoyStationDb(OpenSurfcastDbHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * Replaces the buoy station catalog using a diff strategy.
     * Inserts missing stations and deletes stations that are no longer present.
     *
     * @param stations the new list of stations
     */
    public void replaceAll(List<BuoyStation> stations) {
        List<BuoyStation> incomingStations = stations == null ? Collections.emptyList() : stations;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            Map<String, BuoyStation> existingStations = queryExistingStations(db);
            Set<String> incomingIds = incomingStations.stream()
                    .map(BuoyStation::getId)
                    .collect(Collectors.toSet());

            Set<String> idsToDelete = new HashSet<>(existingStations.keySet());
            idsToDelete.removeAll(incomingIds);
            deleteByIds(db, idsToDelete);

            for (BuoyStation station : incomingStations) {
                BuoyStation existing = existingStations.get(station.getId());
                if (existing == null) {
                    db.insert(OpenSurfcastDbHelper.TABLE_BUOY_STATION, null, toContentValues(station));
                } else {
                    ContentValues existingValues = toContentValues(existing);
                    ContentValues incomingValues = toContentValues(station);
                    if (!existingValues.equals(incomingValues)) {
                        db.update(OpenSurfcastDbHelper.TABLE_BUOY_STATION, incomingValues,
                                "id = ?", new String[]{station.getId()});
                    }
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Returns all buoy stations.
     *
     * @return list of all buoy stations
     */
    public List<BuoyStation> queryAll() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                OpenSurfcastDbHelper.TABLE_BUOY_STATION,
                null, null, null, null, null, null)) {
            return SqlUtils.map(cursor, this::fromCursor);
        }
    }

    private ContentValues toContentValues(BuoyStation station) {
        ContentValues values = new ContentValues();
        values.put("id", station.getId());
        values.put("latitude", station.getLatitude());
        values.put("longitude", station.getLongitude());
        values.put("elevation", station.getElevation());
        values.put("name", station.getName());
        values.put("owner", station.getOwner());
        values.put("program", station.getProgram());
        values.put("type", station.getType());
        values.put("has_met", station.hasMet() ? 1 : 0);
        values.put("has_currents", station.hasCurrents() ? 1 : 0);
        values.put("has_water_quality", station.hasWaterQuality() ? 1 : 0);
        values.put("has_dart", station.hasDart() ? 1 : 0);
        return values;
    }

    private Map<String, BuoyStation> queryExistingStations(SQLiteDatabase db) {
        Map<String, BuoyStation> stations = new HashMap<>();
        try (Cursor cursor = db.query(OpenSurfcastDbHelper.TABLE_BUOY_STATION,
                null, null, null, null, null, null)) {
            for (BuoyStation station : SqlUtils.map(cursor, this::fromCursor)) {
                stations.put(station.getId(), station);
            }
        }
        return stations;
    }

    private void deleteByIds(SQLiteDatabase db, Set<String> ids) {
        if (ids.isEmpty()) {
            return;
        }
        String placeholders = SqlUtils.buildPlaceholders(ids.size());
        db.delete(OpenSurfcastDbHelper.TABLE_BUOY_STATION,
                "id IN (" + placeholders + ")", ids.toArray(new String[0]));
    }

    private BuoyStation fromCursor(Cursor cursor) {
        BuoyStation station = new BuoyStation();
        station.setId(cursor.getString(cursor.getColumnIndexOrThrow("id")));
        station.setLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")));
        station.setLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")));
        int elevationIndex = cursor.getColumnIndexOrThrow("elevation");
        station.setElevation(cursor.isNull(elevationIndex) ? null : cursor.getDouble(elevationIndex));
        station.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
        station.setOwner(cursor.getString(cursor.getColumnIndexOrThrow("owner")));
        station.setProgram(cursor.getString(cursor.getColumnIndexOrThrow("program")));
        station.setType(cursor.getString(cursor.getColumnIndexOrThrow("type")));
        station.setHasMet(cursor.getInt(cursor.getColumnIndexOrThrow("has_met")) == 1);
        station.setHasCurrents(cursor.getInt(cursor.getColumnIndexOrThrow("has_currents")) == 1);
        station.setHasWaterQuality(cursor.getInt(cursor.getColumnIndexOrThrow("has_water_quality")) == 1);
        station.setHasDart(cursor.getInt(cursor.getColumnIndexOrThrow("has_dart")) == 1);
        return station;
    }
}

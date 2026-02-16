package org.opensurfcast.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * SQLite database helper for OpenSurfcast.
 * <p>
 * Manages the creation and upgrade of the database schema for station catalogs
 * and observational/prediction data.
 */
public class OpenSurfcastDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "opensurfcast.db";
    private static final int DATABASE_VERSION = 3;

    // Station tables
    public static final String TABLE_BUOY_STATION = "buoy_station";
    public static final String TABLE_TIDE_STATION = "tide_station";
    public static final String TABLE_CURRENT_STATION = "current_station";

    // Data tables
    public static final String TABLE_BUOY_SPEC_WAVE_DATA = "buoy_spec_wave_data";
    public static final String TABLE_BUOY_STD_MET_DATA = "buoy_std_met_data";
    public static final String TABLE_TIDE_PREDICTION = "tide_prediction";
    public static final String TABLE_CURRENT_PREDICTION = "current_prediction";
    public static final String TABLE_LOG = "logs";

    private static final String CREATE_BUOY_STATION = String.format("""
            CREATE TABLE %s (
                id TEXT PRIMARY KEY,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                elevation REAL,
                name TEXT,
                owner TEXT,
                program TEXT,
                type TEXT,
                has_met INTEGER NOT NULL,
                has_currents INTEGER NOT NULL,
                has_water_quality INTEGER NOT NULL,
                has_dart INTEGER NOT NULL
            )
            """, TABLE_BUOY_STATION);

    private static final String CREATE_TIDE_STATION = String.format("""
            CREATE TABLE %s (
                id TEXT PRIMARY KEY,
                name TEXT,
                state TEXT,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                type TEXT,
                reference_id TEXT,
                time_meridian INTEGER,
                time_zone_correction INTEGER NOT NULL,
                tide_type TEXT,
                affiliations TEXT,
                ports_code TEXT,
                timezone TEXT,
                observes_dst INTEGER,
                tidal INTEGER,
                great_lakes INTEGER
            )
            """, TABLE_TIDE_STATION);

    private static final String CREATE_CURRENT_STATION = String.format("""
            CREATE TABLE %s (
                id TEXT PRIMARY KEY,
                name TEXT,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                type TEXT,
                current_bin INTEGER,
                depth REAL,
                depth_type TEXT,
                timezone_offset TEXT,
                affiliations TEXT,
                ports_code TEXT,
                tide_type TEXT,
                self_url TEXT,
                expand TEXT,
                current_prediction_offsets_url TEXT,
                harmonic_constituents_url TEXT
            )
            """, TABLE_CURRENT_STATION);

    private static final String CREATE_BUOY_SPEC_WAVE_DATA = String.format("""
            CREATE TABLE %s (
                id TEXT NOT NULL,
                epoch_seconds INTEGER NOT NULL,
                year INTEGER NOT NULL,
                month INTEGER NOT NULL,
                day INTEGER NOT NULL,
                hour INTEGER NOT NULL,
                minute INTEGER NOT NULL,
                wave_height REAL,
                swell_height REAL,
                swell_period REAL,
                wind_wave_height REAL,
                wind_wave_period REAL,
                swell_direction TEXT,
                wind_wave_direction TEXT,
                steepness TEXT,
                average_wave_period REAL,
                mean_wave_direction INTEGER,
                PRIMARY KEY (id, epoch_seconds),
                FOREIGN KEY (id) REFERENCES %s(id) ON DELETE CASCADE
            )
            """, TABLE_BUOY_SPEC_WAVE_DATA, TABLE_BUOY_STATION);

    private static final String CREATE_BUOY_STD_MET_DATA = String.format("""
            CREATE TABLE %s (
                id TEXT NOT NULL,
                epoch_seconds INTEGER NOT NULL,
                year INTEGER NOT NULL,
                month INTEGER NOT NULL,
                day INTEGER NOT NULL,
                hour INTEGER NOT NULL,
                minute INTEGER NOT NULL,
                wind_direction INTEGER,
                wind_speed REAL,
                gust_speed REAL,
                wave_height REAL,
                dominant_wave_period REAL,
                average_wave_period REAL,
                mean_wave_direction INTEGER,
                pressure REAL,
                air_temperature REAL,
                water_temperature REAL,
                dew_point REAL,
                visibility REAL,
                pressure_tendency REAL,
                tide REAL,
                PRIMARY KEY (id, epoch_seconds),
                FOREIGN KEY (id) REFERENCES %s(id) ON DELETE CASCADE
            )
            """, TABLE_BUOY_STD_MET_DATA, TABLE_BUOY_STATION);

    private static final String CREATE_TIDE_PREDICTION = String.format("""
            CREATE TABLE %s (
                id TEXT NOT NULL,
                epoch_seconds INTEGER NOT NULL,
                timestamp TEXT,
                value REAL NOT NULL,
                type TEXT,
                PRIMARY KEY (id, epoch_seconds),
                FOREIGN KEY (id) REFERENCES %s(id) ON DELETE CASCADE
            )
            """, TABLE_TIDE_PREDICTION, TABLE_TIDE_STATION);

    private static final String CREATE_CURRENT_PREDICTION = String.format("""
            CREATE TABLE %s (
                id TEXT NOT NULL,
                epoch_seconds INTEGER NOT NULL,
                timestamp TEXT,
                type TEXT,
                velocity_major REAL NOT NULL,
                mean_flood_direction REAL,
                mean_ebb_direction REAL,
                bin TEXT,
                depth REAL,
                PRIMARY KEY (id, epoch_seconds),
                FOREIGN KEY (id) REFERENCES %s(id) ON DELETE CASCADE
            )
            """, TABLE_CURRENT_PREDICTION, TABLE_CURRENT_STATION);

    private static final String CREATE_LOG = String.format("""
            CREATE TABLE %s (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp INTEGER NOT NULL,
                level TEXT NOT NULL,
                message TEXT NOT NULL,
                stack_trace TEXT
            )
            """, TABLE_LOG);

    public OpenSurfcastDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create station tables first (referenced by data tables)
        db.execSQL(CREATE_BUOY_STATION);
        db.execSQL(CREATE_TIDE_STATION);
        db.execSQL(CREATE_CURRENT_STATION);

        // Create data tables with foreign keys
        db.execSQL(CREATE_BUOY_SPEC_WAVE_DATA);
        db.execSQL(CREATE_BUOY_STD_MET_DATA);
        db.execSQL(CREATE_TIDE_PREDICTION);
        db.execSQL(CREATE_CURRENT_PREDICTION);

        // Create log table
        db.execSQL(CREATE_LOG);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop all tables and recreate on upgrade
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOG);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CURRENT_PREDICTION);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TIDE_PREDICTION);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BUOY_STD_MET_DATA);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BUOY_SPEC_WAVE_DATA);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CURRENT_STATION);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TIDE_STATION);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BUOY_STATION);
        onCreate(db);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        // Enable foreign key constraints
        db.setForeignKeyConstraintsEnabled(true);
        db.enableWriteAheadLogging();
    }
}

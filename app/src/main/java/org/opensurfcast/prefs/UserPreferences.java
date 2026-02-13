package org.opensurfcast.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Manages user preferences for the OpenSurfcast application.
 * <p>
 * Provides type-safe access to user settings including preferred stations,
 * home coordinates, units, and other configuration options.
 */
public class UserPreferences {

    private static final String PREFS_NAME = "opensurfcast_prefs";

    // Station preference keys
    private static final String KEY_PREFERRED_BUOY_STATIONS = "preferred_buoy_stations";
    private static final String KEY_PREFERRED_TIDE_STATIONS = "preferred_tide_stations";
    private static final String KEY_PREFERRED_CURRENT_STATIONS = "preferred_current_stations";

    // Future: Location preference keys
    private static final String KEY_HOME_LATITUDE = "home_latitude";
    private static final String KEY_HOME_LONGITUDE = "home_longitude";

    // Units preference keys
    private static final String KEY_USE_METRIC = "use_metric";

    // Theme preference keys
    private static final String KEY_THEME_MODE = "theme_mode";

    // Sentinel value for unset coordinates
    private static final float COORDINATE_NOT_SET = Float.NaN;

    // Default home coordinates: Petaluma, CA
    private static final double DEFAULT_HOME_LATITUDE = 38.115307;
    private static final double DEFAULT_HOME_LONGITUDE = -122.50567;

    private final SharedPreferences prefs;

    public UserPreferences(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ========== Buoy Station Preferences ==========

    /**
     * Returns the set of preferred buoy station IDs.
     *
     * @return immutable set of station IDs (empty if none configured)
     */
    public Set<String> getPreferredBuoyStations() {
        Set<String> stations = prefs.getStringSet(KEY_PREFERRED_BUOY_STATIONS, null);
        if (stations == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new HashSet<>(stations));
    }

    /**
     * Sets the preferred buoy station IDs.
     *
     * @param ids set of station IDs to save
     */
    public void setPreferredBuoyStations(Set<String> ids) {
        prefs.edit()
                .putStringSet(KEY_PREFERRED_BUOY_STATIONS, new HashSet<>(ids))
                .apply();
    }

    /**
     * Adds a buoy station to the preferred list.
     *
     * @param id station ID to add
     */
    public void addPreferredBuoyStation(String id) {
        Set<String> stations = new HashSet<>(getPreferredBuoyStations());
        stations.add(id);
        setPreferredBuoyStations(stations);
    }

    /**
     * Removes a buoy station from the preferred list.
     *
     * @param id station ID to remove
     */
    public void removePreferredBuoyStation(String id) {
        Set<String> stations = new HashSet<>(getPreferredBuoyStations());
        stations.remove(id);
        setPreferredBuoyStations(stations);
    }

    /**
     * Checks if a buoy station is in the preferred list.
     *
     * @param id station ID to check
     * @return true if the station is preferred
     */
    public boolean isPreferredBuoyStation(String id) {
        return getPreferredBuoyStations().contains(id);
    }

    // ========== Tide Station Preferences ==========

    /**
     * Returns the set of preferred tide station IDs.
     *
     * @return immutable set of station IDs (empty if none configured)
     */
    public Set<String> getPreferredTideStations() {
        Set<String> stations = prefs.getStringSet(KEY_PREFERRED_TIDE_STATIONS, null);
        if (stations == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new HashSet<>(stations));
    }

    /**
     * Sets the preferred tide station IDs.
     *
     * @param ids set of station IDs to save
     */
    public void setPreferredTideStations(Set<String> ids) {
        prefs.edit()
                .putStringSet(KEY_PREFERRED_TIDE_STATIONS, new HashSet<>(ids))
                .apply();
    }

    /**
     * Adds a tide station to the preferred list.
     *
     * @param id station ID to add
     */
    public void addPreferredTideStation(String id) {
        Set<String> stations = new HashSet<>(getPreferredTideStations());
        stations.add(id);
        setPreferredTideStations(stations);
    }

    /**
     * Removes a tide station from the preferred list.
     *
     * @param id station ID to remove
     */
    public void removePreferredTideStation(String id) {
        Set<String> stations = new HashSet<>(getPreferredTideStations());
        stations.remove(id);
        setPreferredTideStations(stations);
    }

    /**
     * Checks if a tide station is in the preferred list.
     *
     * @param id station ID to check
     * @return true if the station is preferred
     */
    public boolean isPreferredTideStation(String id) {
        return getPreferredTideStations().contains(id);
    }

    // ========== Current Station Preferences ==========

    /**
     * Returns the set of preferred current station IDs.
     *
     * @return immutable set of station IDs (empty if none configured)
     */
    public Set<String> getPreferredCurrentStations() {
        Set<String> stations = prefs.getStringSet(KEY_PREFERRED_CURRENT_STATIONS, null);
        if (stations == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new HashSet<>(stations));
    }

    /**
     * Sets the preferred current station IDs.
     *
     * @param ids set of station IDs to save
     */
    public void setPreferredCurrentStations(Set<String> ids) {
        prefs.edit()
                .putStringSet(KEY_PREFERRED_CURRENT_STATIONS, new HashSet<>(ids))
                .apply();
    }

    /**
     * Adds a current station to the preferred list.
     *
     * @param id station ID to add
     */
    public void addPreferredCurrentStation(String id) {
        Set<String> stations = new HashSet<>(getPreferredCurrentStations());
        stations.add(id);
        setPreferredCurrentStations(stations);
    }

    /**
     * Removes a current station from the preferred list.
     *
     * @param id station ID to remove
     */
    public void removePreferredCurrentStation(String id) {
        Set<String> stations = new HashSet<>(getPreferredCurrentStations());
        stations.remove(id);
        setPreferredCurrentStations(stations);
    }

    /**
     * Checks if a current station is in the preferred list.
     *
     * @param id station ID to check
     * @return true if the station is preferred
     */
    public boolean isPreferredCurrentStation(String id) {
        return getPreferredCurrentStations().contains(id);
    }

    // ========== Home Location Preferences (Future) ==========

    /**
     * Returns the home latitude coordinate.
     * Defaults to Petaluma, CA if not explicitly set.
     *
     * @return latitude value (default: 38.115307)
     */
    public Double getHomeLatitude() {
        float value = prefs.getFloat(KEY_HOME_LATITUDE, COORDINATE_NOT_SET);
        return Float.isNaN(value) ? DEFAULT_HOME_LATITUDE : (double) value;
    }

    /**
     * Sets the home latitude coordinate.
     *
     * @param latitude latitude value (null to clear)
     */
    public void setHomeLatitude(Double latitude) {
        float value = (latitude != null) ? latitude.floatValue() : COORDINATE_NOT_SET;
        prefs.edit()
                .putFloat(KEY_HOME_LATITUDE, value)
                .apply();
    }

    /**
     * Returns the home longitude coordinate.
     * Defaults to Petaluma, CA if not explicitly set.
     *
     * @return longitude value (default: -122.50567)
     */
    public Double getHomeLongitude() {
        float value = prefs.getFloat(KEY_HOME_LONGITUDE, COORDINATE_NOT_SET);
        return Float.isNaN(value) ? DEFAULT_HOME_LONGITUDE : (double) value;
    }

    /**
     * Sets the home longitude coordinate.
     *
     * @param longitude longitude value (null to clear)
     */
    public void setHomeLongitude(Double longitude) {
        float value = (longitude != null) ? longitude.floatValue() : COORDINATE_NOT_SET;
        prefs.edit()
                .putFloat(KEY_HOME_LONGITUDE, value)
                .apply();
    }

    // ========== Units Preferences (Future) ==========

    /**
     * Returns whether metric units are preferred.
     *
     * @return true for metric, false for imperial (default)
     */
    public boolean isMetric() {
        return prefs.getBoolean(KEY_USE_METRIC, false);
    }

    /**
     * Sets the preferred unit system.
     *
     * @param useMetric true for metric, false for imperial
     */
    public void setMetric(boolean useMetric) {
        prefs.edit()
                .putBoolean(KEY_USE_METRIC, useMetric)
                .apply();
    }

    // ========== Theme Preferences ==========

    /**
     * Returns the persisted theme mode.
     * <p>
     * Valid values correspond to {@link AppCompatDelegate} night-mode constants:
     * {@link AppCompatDelegate#MODE_NIGHT_FOLLOW_SYSTEM},
     * {@link AppCompatDelegate#MODE_NIGHT_NO},
     * {@link AppCompatDelegate#MODE_NIGHT_YES}.
     *
     * @return the stored night-mode constant (default: follow system)
     */
    public int getThemeMode() {
        return prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    /**
     * Persists the chosen theme mode.
     *
     * @param mode one of {@link AppCompatDelegate#MODE_NIGHT_FOLLOW_SYSTEM},
     *             {@link AppCompatDelegate#MODE_NIGHT_NO}, or
     *             {@link AppCompatDelegate#MODE_NIGHT_YES}
     */
    public void setThemeMode(int mode) {
        prefs.edit()
                .putInt(KEY_THEME_MODE, mode)
                .apply();
    }

    /**
     * Applies the given theme mode to the running application via
     * {@link AppCompatDelegate#setDefaultNightMode(int)}.
     *
     * @param mode night-mode constant to apply
     */
    public static void applyThemeMode(int mode) {
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    // ========== Utility Methods ==========

    /**
     * Clears all user preferences.
     */
    public void clear() {
        prefs.edit().clear().apply();
    }
}

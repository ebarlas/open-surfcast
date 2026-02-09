package org.opensurfcast.http;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Persists HTTP Last-Modified header values in SharedPreferences.
 * <p>
 * Used to send {@code If-Modified-Since} headers on subsequent requests,
 * allowing servers to return {@code 304 Not Modified} when data hasn't changed.
 */
public class HttpCache {

    private static final String PREFS_NAME = "http_cache";

    private final SharedPreferences prefs;

    public HttpCache(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Returns the stored Last-Modified value for the given key.
     *
     * @param key cache key (typically the task key)
     * @return the Last-Modified header string, or {@code null} if none stored
     */
    public String get(String key) {
        return prefs.getString(key, null);
    }

    /**
     * Stores a Last-Modified value for the given key.
     * <p>
     * If {@code lastModified} is null, the call is ignored.
     *
     * @param key          cache key (typically the task key)
     * @param lastModified the Last-Modified header value from the server
     */
    public void put(String key, String lastModified) {
        if (lastModified == null) {
            return;
        }
        prefs.edit()
                .putString(key, lastModified)
                .apply();
    }

    /**
     * Removes the stored value for the given key.
     *
     * @param key cache key to remove
     */
    public void remove(String key) {
        prefs.edit()
                .remove(key)
                .apply();
    }

    /**
     * Clears all stored Last-Modified values.
     */
    public void clear() {
        prefs.edit().clear().apply();
    }
}

package org.opensurfcast.buoy;

/**
 * Marks types that expose a Unix epoch timestamp in seconds.
 * Used for uniform filtering by lookback window across buoy data types.
 */
public interface HasEpochSeconds {

    /**
     * Returns the Unix epoch timestamp in seconds (seconds since 1970-01-01 00:00:00 UTC).
     */
    long getEpochSeconds();
}

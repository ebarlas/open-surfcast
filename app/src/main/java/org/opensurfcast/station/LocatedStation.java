package org.opensurfcast.station;

/**
 * Unifies the concept of a station with a display name and geographic position.
 * Implemented by {@link org.opensurfcast.buoy.BuoyStation}, {@link org.opensurfcast.tide.TideStation},
 * and {@link org.opensurfcast.tide.CurrentStation} to enable consistent sorting across station types.
 */
public interface LocatedStation {

    /**
     * Returns the display name for sorting and display.
     * Typically the station name when available, otherwise the station ID.
     *
     * @return non-null display name
     */
    String getDisplayName();

    /**
     * Returns the unique station identifier.
     *
     * @return non-null station ID
     */
    String getId();

    /**
     * Returns the latitude in decimal degrees.
     *
     * @return latitude (positive = northern hemisphere)
     */
    double getLatitude();

    /**
     * Returns the longitude in decimal degrees.
     *
     * @return longitude (negative = western hemisphere)
     */
    double getLongitude();
}

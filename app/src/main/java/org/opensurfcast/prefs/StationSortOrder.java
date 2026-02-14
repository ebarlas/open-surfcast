package org.opensurfcast.prefs;

import org.opensurfcast.station.LocatedStation;

import java.util.Comparator;

/**
 * User-selectable sort order for station lists.
 * Provides comparators that work with any {@link LocatedStation} implementation.
 */
public enum StationSortOrder {

    ALPHABETICAL,
    LATITUDINAL,
    PROXIMAL;

    /**
     * Returns a comparator for the given sort order.
     * For {@link #PROXIMAL}, home coordinates are used to compute distance.
     *
     * @param homeLat home latitude (decimal degrees)
     * @param homeLon home longitude (decimal degrees)
     * @return comparator for sorting LocatedStation instances
     */
    public Comparator<LocatedStation> getComparator(double homeLat, double homeLon) {
        return switch (this) {
            case ALPHABETICAL -> Comparator
                    .comparing(LocatedStation::getDisplayName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(LocatedStation::getId);
            case LATITUDINAL -> Comparator.comparingDouble(LocatedStation::getLatitude).reversed();
            case PROXIMAL -> Comparator.comparingDouble(s ->
                    haversineKm(homeLat, homeLon, s.getLatitude(), s.getLongitude()));
        };
    }

    /**
     * Great-circle distance in km between two lat/lon points (Haversine formula).
     */
    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }
}

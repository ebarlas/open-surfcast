package org.opensurfcast.tide;

/**
 * Represents a single high or low tide prediction from the NOAA CO-OPS Data API.
 * <p>
 * This class expects data requested with:
 * <ul>
 *   <li>{@code units=metric} - Water levels in meters</li>
 *   <li>{@code time_zone=gmt} - Timestamps in UTC</li>
 * </ul>
 * <p>
 * API Documentation: <a href="https://api.tidesandcurrents.noaa.gov/api/prod/">CO-OPS Data API</a>
 * <p>
 * Example JSON element from predictions response:
 * <pre>
 * {
 *   "t": "2026-01-02 10:57",
 *   "v": "2.314",
 *   "type": "H"
 * }
 * </pre>
 * <p>
 * Request URL example:
 * <pre>
 * https://api.tidesandcurrents.noaa.gov/api/prod/datagetter
 *   ?station=9414290
 *   &amp;product=predictions
 *   &amp;datum=MLLW
 *   &amp;interval=hilo
 *   &amp;units=metric
 *   &amp;time_zone=gmt
 *   &amp;format=json
 *   &amp;date=today
 * </pre>
 */
public class TidePrediction {

    /**
     * Type value for high tide.
     */
    public static final String TYPE_HIGH = "H";

    /**
     * Type value for low tide.
     */
    public static final String TYPE_LOW = "L";

    /**
     * Timestamp (t field) - Date and time of the predicted tide in UTC.
     * Format: "YYYY-MM-DD HH:mm"
     * Example: "2026-01-02 10:57"
     * <p>
     * This value is in GMT/UTC (requested with {@code time_zone=gmt}).
     */
    public String timestamp;

    /**
     * Unix epoch timestamp in seconds.
     * Represents the prediction time as seconds since January 1, 1970 00:00:00 UTC.
     * <p>
     * Derived from the {@link #timestamp} field during parsing.
     */
    public long epochSeconds;

    /**
     * Value (v field) - Predicted water level height in meters.
     * <p>
     * This value is in metric units (requested with {@code units=metric}).
     * The value is relative to the datum specified in the request (e.g., MLLW, MSL).
     * Negative values indicate water level below the datum.
     */
    public double value;

    /**
     * Type (type field) - Indicates whether this is a high or low tide.
     * <ul>
     *   <li>"H" - High tide</li>
     *   <li>"L" - Low tide</li>
     * </ul>
     *
     * @see #TYPE_HIGH
     * @see #TYPE_LOW
     */
    public String type;

    /**
     * Returns true if this is a high tide prediction.
     */
    public boolean isHighTide() {
        return TYPE_HIGH.equals(type);
    }

    /**
     * Returns true if this is a low tide prediction.
     */
    public boolean isLowTide() {
        return TYPE_LOW.equals(type);
    }

    @Override
    public String toString() {
        return "TidePrediction{" +
                "timestamp='" + timestamp + '\'' +
                ", epochSeconds=" + epochSeconds +
                ", value=" + value +
                ", type='" + type + '\'' +
                '}';
    }
}

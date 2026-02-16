package org.opensurfcast.tide;

/**
 * Represents a single current prediction from the NOAA CO-OPS Data API.
 * <p>
 * This class expects data requested with:
 * <ul>
 *   <li>{@code units=metric} - Velocity in cm/s, depth in meters</li>
 *   <li>{@code time_zone=gmt} - Timestamps in UTC</li>
 * </ul>
 * <p>
 * API Documentation: <a href="https://api.tidesandcurrents.noaa.gov/api/prod/">CO-OPS Data API</a>
 * <p>
 * Example JSON element from current_predictions response:
 * <pre>
 * {
 *   "Type": "flood",
 *   "Time": "2026-01-05 02:47",
 *   "Velocity_Major": 160.7,
 *   "meanFloodDir": 210,
 *   "meanEbbDir": 40,
 *   "Bin": "1",
 *   "Depth": null
 * }
 * </pre>
 * <p>
 * Request URL example:
 * <pre>
 * https://api.tidesandcurrents.noaa.gov/api/prod/datagetter
 *   ?station=ACT0091
 *   &amp;product=currents_predictions
 *   &amp;units=metric
 *   &amp;time_zone=gmt
 *   &amp;format=json
 *   &amp;date=today
 * </pre>
 */
public class CurrentPrediction {

    /**
     * Type value for flood current (incoming tide, water flowing toward shore).
     */
    public static final String TYPE_FLOOD = "flood";

    /**
     * Type value for ebb current (outgoing tide, water flowing away from shore).
     */
    public static final String TYPE_EBB = "ebb";

    /**
     * Type value for slack water (minimal or no current flow between flood and ebb).
     */
    public static final String TYPE_SLACK = "slack";

    /**
     * Timestamp (Time field) - Date and time of the predicted current in UTC.
     * Format: "YYYY-MM-DD HH:mm"
     * Example: "2026-01-05 02:47"
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
     * Type (Type field) - Indicates the current phase.
     * <ul>
     *   <li>"flood" - Incoming tide, water flowing toward shore</li>
     *   <li>"ebb" - Outgoing tide, water flowing away from shore</li>
     *   <li>"slack" - Minimal or no current flow</li>
     * </ul>
     *
     * @see #TYPE_FLOOD
     * @see #TYPE_EBB
     * @see #TYPE_SLACK
     */
    public String type;

    /**
     * Velocity major (Velocity_Major field) - The predicted current velocity along the
     * major axis of the current ellipse.
     * <p>
     * Unit: centimeters per second (cm/s) when requested with {@code units=metric}.
     * <p>
     * Sign convention:
     * <ul>
     *   <li>Positive values indicate flood current</li>
     *   <li>Negative values indicate ebb current</li>
     *   <li>Zero (or near zero) indicates slack water</li>
     * </ul>
     */
    public double velocityMajor;

    /**
     * Mean flood direction (meanFloodDir field) - The average direction of the flood current.
     * <p>
     * Unit: degrees (0-360, where 0/360 = North, 90 = East, 180 = South, 270 = West)
     * <p>
     * This is the direction the current is flowing TOWARD during flood tide.
     * May be null for some stations (e.g. subordinate stations) that do not provide this data.
     */
    public Double meanFloodDirection;

    /**
     * Mean ebb direction (meanEbbDir field) - The average direction of the ebb current.
     * <p>
     * Unit: degrees (0-360, where 0/360 = North, 90 = East, 180 = South, 270 = West)
     * <p>
     * This is the direction the current is flowing TOWARD during ebb tide.
     * Typically approximately 180 degrees opposite of the flood direction.
     * May be null for some stations (e.g. subordinate stations) that do not provide this data.
     */
    public Double meanEbbDirection;

    /**
     * Bin number (Bin field) - The depth bin for this prediction.
     * Current meters often measure at multiple depths. This indicates which
     * depth level this prediction applies to.
     */
    public String bin;

    /**
     * Depth (Depth field) - The depth of the measurement bin.
     * <p>
     * Unit: meters when requested with {@code units=metric}.
     * May be null if not specified.
     */
    public Double depth;

    /**
     * Returns true if this is a flood current prediction.
     * Flood currents flow toward shore during incoming tide.
     */
    public boolean isFlood() {
        return TYPE_FLOOD.equals(type);
    }

    /**
     * Returns true if this is an ebb current prediction.
     * Ebb currents flow away from shore during outgoing tide.
     */
    public boolean isEbb() {
        return TYPE_EBB.equals(type);
    }

    /**
     * Returns true if this is a slack water prediction.
     * Slack water occurs at the transition between flood and ebb.
     */
    public boolean isSlack() {
        return TYPE_SLACK.equals(type);
    }

    /**
     * Returns the current direction based on the current type.
     * <ul>
     *   <li>For flood: returns {@link #meanFloodDirection}</li>
     *   <li>For ebb: returns {@link #meanEbbDirection}</li>
     *   <li>For slack: returns {@link #meanFloodDirection} (direction is less meaningful at slack)</li>
     * </ul>
     *
     * @return the current direction in degrees, or null if not available
     */
    public Double getCurrentDirection() {
        if (isEbb()) {
            return meanEbbDirection;
        }
        return meanFloodDirection;
    }

    /**
     * Returns the absolute velocity (speed) regardless of direction.
     *
     * @return the absolute value of {@link #velocityMajor} in cm/s
     */
    public double getSpeed() {
        return Math.abs(velocityMajor);
    }

    @Override
    public String toString() {
        return "CurrentPrediction{" +
                "timestamp='" + timestamp + '\'' +
                ", epochSeconds=" + epochSeconds +
                ", type='" + type + '\'' +
                ", velocityMajor=" + velocityMajor +
                ", meanFloodDirection=" + meanFloodDirection +
                ", meanEbbDirection=" + meanEbbDirection +
                ", bin='" + bin + '\'' +
                ", depth=" + depth +
                '}';
    }
}


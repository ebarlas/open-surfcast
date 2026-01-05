package org.opensurfcast.tide;

/**
 * Represents a NOAA CO-OPS tide prediction station.
 * Contains station metadata as returned by the CO-OPS Metadata API (MDAPI).
 * <p>
 * API Documentation: <a href="https://api.tidesandcurrents.noaa.gov/mdapi/prod/">CO-OPS Metadata API v1.0</a>
 * <p>
 * Data source: <a href="https://api.tidesandcurrents.noaa.gov/mdapi/prod/webapi/stations.json?type=tidepredictions">Tide Prediction Stations</a>
 * <p>
 * Example station entry from JSON (list response):
 * <pre>
 * {
 *   "id": "9415252",
 *   "name": "Petaluma River entrance",
 *   "state": "CA",
 *   "lat": 38.115307,
 *   "lng": -122.50567,
 *   "type": "S",
 *   "reference_id": "9414290",
 *   "timemeridian": -120,
 *   "timezonecorr": -8,
 *   "tideType": "Mixed",
 *   "affiliations": "",
 *   "portscode": ""
 * }
 * </pre>
 */
public class TideStation {

    /**
     * Station ID (id field) - Unique identifier for the station.
     * Typically a 7-digit numeric string for water level stations (e.g., "9414290").
     * May also be alphanumeric for some station types (e.g., "TPT2693").
     */
    public String id;

    /**
     * Station name (name field) - Descriptive name or location of the station.
     * Example: "PETALUMA RIVER ENTRANCE", "SAN FRANCISCO"
     */
    public String name;

    /**
     * State (state field) - US state or territory abbreviation where the station is located.
     * Example: "CA", "HI", "FL"
     * May be empty for stations outside US states (e.g., Pacific islands).
     */
    public String state;

    /**
     * Latitude (lat field) - Latitude of the station location.
     * Positive values indicate northern hemisphere.
     * Unit: decimal degrees
     */
    public double latitude;

    /**
     * Longitude (lng field) - Longitude of the station location.
     * Negative values indicate western hemisphere.
     * Unit: decimal degrees
     */
    public double longitude;

    /**
     * Station type (type field) - Indicates whether this is a reference or subordinate station.
     * <ul>
     *   <li>"R" - Reference station: Has measured harmonic constituents for tide predictions</li>
     *   <li>"S" - Subordinate station: Predictions are derived from a reference station using offsets</li>
     * </ul>
     */
    public String type;

    /**
     * Reference station ID (reference_id field) - For subordinate stations, the ID of the
     * reference station used to compute predictions.
     * Empty string for reference stations.
     * Example: "9414290" (San Francisco is reference for many Bay Area subordinate stations)
     */
    public String referenceId;

    /**
     * Time meridian (timemeridian field) - The standard meridian used for time calculations.
     * Used in conjunction with timezonecorr for local time calculations.
     * Unit: degrees (negative for west of Prime Meridian)
     * Example: -120 for Pacific Standard Time meridian
     * May be null.
     */
    public Integer timeMeridian;

    /**
     * Time zone correction (timezonecorr field) - Hours offset from UTC/GMT.
     * Example: -8 for Pacific Standard Time, -10 for Hawaii Standard Time
     * Unit: hours
     */
    public int timeZoneCorrection;

    /**
     * Tide type (tideType field) - Classification of the tidal pattern at this station.
     * <ul>
     *   <li>"Diurnal" - One high and one low tide per day</li>
     *   <li>"Semidiurnal" - Two nearly equal high and low tides per day</li>
     *   <li>"Mixed" - Two unequal high and low tides per day</li>
     * </ul>
     * May be empty in list responses; populated in single station responses.
     */
    public String tideType;

    /**
     * Affiliations (affiliations field) - Program or network affiliations for the station.
     * Example: "PORTS", "NWLON"
     * Often empty string.
     */
    public String affiliations;

    /**
     * PORTS code (portscode field) - Physical Oceanographic Real-Time System code
     * if the station is part of a PORTS network.
     * Example: "sf" for San Francisco PORTS
     * May be null or empty.
     */
    public String portsCode;

    /**
     * Timezone abbreviation (timezone field) - Standard timezone abbreviation.
     * Example: "PST", "HST", "EST"
     * Only present in single station responses, not in list responses.
     */
    public String timezone;

    /**
     * Observes daylight saving time (observedst field) - Whether the station
     * location observes daylight saving time.
     * Only present in single station responses.
     */
    public Boolean observesDst;

    /**
     * Tidal station (tidal field) - Whether this is a tidal station.
     * Only present in single station responses.
     */
    public Boolean tidal;

    /**
     * Great Lakes station (greatlakes field) - Whether this station is located
     * in the Great Lakes region. Great Lakes stations do not have tide predictions.
     * Only present in single station responses.
     */
    public Boolean greatLakes;

    /**
     * Returns true if this is a reference station (type "R").
     * Reference stations have measured harmonic constituents.
     */
    public boolean isReferenceStation() {
        return "R".equals(type);
    }

    /**
     * Returns true if this is a subordinate station (type "S").
     * Subordinate stations derive predictions from a reference station.
     */
    public boolean isSubordinateStation() {
        return "S".equals(type);
    }

    @Override
    public String toString() {
        return "TideStation{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", state='" + state + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", type='" + type + '\'' +
                ", referenceId='" + referenceId + '\'' +
                ", timeMeridian=" + timeMeridian +
                ", timeZoneCorrection=" + timeZoneCorrection +
                ", tideType='" + tideType + '\'' +
                ", affiliations='" + affiliations + '\'' +
                ", portsCode='" + portsCode + '\'' +
                ", timezone='" + timezone + '\'' +
                ", observesDst=" + observesDst +
                ", tidal=" + tidal +
                ", greatLakes=" + greatLakes +
                '}';
    }
}

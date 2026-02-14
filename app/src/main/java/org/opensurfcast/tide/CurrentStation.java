package org.opensurfcast.tide;

import org.opensurfcast.station.LocatedStation;

/**
 * Represents a NOAA CO-OPS current prediction station.
 * Contains station metadata as returned by the CO-OPS Metadata API (MDAPI).
 * <p>
 * API Documentation: <a href="https://api.tidesandcurrents.noaa.gov/mdapi/prod/">CO-OPS Metadata API v1.0</a>
 * <p>
 * Data source: <a href="https://api.tidesandcurrents.noaa.gov/mdapi/prod/webapi/stations.json?type=currentpredictions&units=metric">Current Prediction Stations</a>
 * <p>
 * Note: All measurements assume metric units (meters for depth).
 * <p>
 * Example station entry from JSON (list response):
 * <pre>
 * {
 *   "currentpredictionoffsets": {
 *     "self": "https://api.tidesandcurrents.noaa.gov/mdapi/prod/webapi/stations/ACT0091_1/currentpredictionoffsets.json"
 *   },
 *   "currbin": 1,
 *   "type": "S",
 *   "depth": null,
 *   "depthType": "U",
 *   "timezone_offset": "",
 *   "harmonicConstituents": {
 *     "self": "https://api.tidesandcurrents.noaa.gov/mdapi/prod/webapi/stations/ACT0091/harcon.json"
 *   },
 *   "id": "ACT0091",
 *   "name": "Eastport, Friar Roads",
 *   "lat": 44.9,
 *   "lng": -66.98333,
 *   "affiliations": "",
 *   "portscode": "",
 *   "products": null,
 *   "disclaimers": null,
 *   "notices": null,
 *   "self": "https://api.tidesandcurrents.noaa.gov/mdapi/prod/webapi/stations/ACT0091.json",
 *   "expand": "currentpredictionoffsets,harcon",
 *   "tideType": ""
 * }
 * </pre>
 */
public class CurrentStation implements LocatedStation {

    /**
     * Station ID (id field) - Unique identifier for the station.
     * Alphanumeric format for current prediction stations (e.g., "ACT0091", "EPT0003", "PCT1291").
     */
    public String id;

    /**
     * Station name (name field) - Descriptive name or location of the station.
     * Example: "Eastport, Friar Roads", "Western Passage, off Kendall Head"
     */
    public String name;

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
     *   <li>"R" - Reference station: Has measured harmonic constituents for current predictions</li>
     *   <li>"S" - Subordinate station: Predictions are derived from a reference station using offsets</li>
     * </ul>
     */
    public String type;

    /**
     * Current bin number (currbin field) - The bin number for this current prediction station.
     * Current meters often measure at multiple depths (bins). This indicates which bin
     * this station data represents.
     * Example: 1, 11, 14
     */
    public Integer currentBin;

    /**
     * Depth (depth field) - The depth at which current measurements are made.
     * May be null if depth is not specified.
     * Unit: meters
     */
    public Double depth;

    /**
     * Depth type (depthType field) - Indicates the reference point for depth measurements.
     * <ul>
     *   <li>"S" - Surface: Depth measured from the water surface</li>
     *   <li>"B" - Bottom: Depth measured from the sea floor</li>
     *   <li>"U" - Unknown or unspecified</li>
     * </ul>
     */
    public String depthType;

    /**
     * Timezone offset (timezone_offset field) - Timezone offset information.
     * Often empty string in responses.
     */
    public String timezoneOffset;

    /**
     * Affiliations (affiliations field) - Program or network affiliations for the station.
     * Example: "PORTS"
     * Often empty string.
     */
    public String affiliations;

    /**
     * PORTS code (portscode field) - Physical Oceanographic Real-Time System code
     * if the station is part of a PORTS network.
     * May be null or empty.
     */
    public String portsCode;

    /**
     * Tide type (tideType field) - Classification of the tidal pattern at this station.
     * <ul>
     *   <li>"Diurnal" - One flood and one ebb per day</li>
     *   <li>"Semidiurnal" - Two nearly equal flood and ebb cycles per day</li>
     *   <li>"Mixed" - Two unequal flood and ebb cycles per day</li>
     * </ul>
     * Often empty for current prediction stations.
     */
    public String tideType;

    /**
     * Self URL (self field) - URL to fetch this station's full details.
     * Example: "https://api.tidesandcurrents.noaa.gov/mdapi/prod/webapi/stations/ACT0091.json"
     */
    public String selfUrl;

    /**
     * Expand options (expand field) - Available expansion options for this station.
     * Comma-separated list of resources that can be expanded.
     * Example: "currentpredictionoffsets,harcon"
     */
    public String expand;

    /**
     * Current prediction offsets URL - URL to fetch current prediction offsets.
     * Present when station is a subordinate station.
     */
    public String currentPredictionOffsetsUrl;

    /**
     * Harmonic constituents URL - URL to fetch harmonic constituents for this station.
     */
    public String harmonicConstituentsUrl;

    @Override
    public String getDisplayName() {
        return (name != null && !name.isEmpty()) ? name : (id != null ? id : "");
    }

    @Override
    public String getId() {
        return id != null ? id : "";
    }

    @Override
    public double getLatitude() {
        return latitude;
    }

    @Override
    public double getLongitude() {
        return longitude;
    }

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

    /**
     * Returns true if the depth is measured from the surface.
     */
    public boolean isDepthFromSurface() {
        return "S".equals(depthType);
    }

    /**
     * Returns true if the depth is measured from the bottom.
     */
    public boolean isDepthFromBottom() {
        return "B".equals(depthType);
    }

    @Override
    public String toString() {
        return "CurrentStation{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", type='" + type + '\'' +
                ", currentBin=" + currentBin +
                ", depth=" + depth +
                ", depthType='" + depthType + '\'' +
                ", timezoneOffset='" + timezoneOffset + '\'' +
                ", affiliations='" + affiliations + '\'' +
                ", portsCode='" + portsCode + '\'' +
                ", tideType='" + tideType + '\'' +
                ", selfUrl='" + selfUrl + '\'' +
                ", expand='" + expand + '\'' +
                '}';
    }
}


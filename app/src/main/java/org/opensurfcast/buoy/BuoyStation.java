package org.opensurfcast.buoy;

import org.opensurfcast.station.LocatedStation;

/**
 * Represents an NDBC (National Data Buoy Center) buoy station.
 * Contains station metadata as defined at:
 * <a href="https://www.ndbc.noaa.gov/faq/activestations.shtml">NDBC Active Stations FAQ</a>
 * <p>
 * Data source: <a href="https://www.ndbc.noaa.gov/activestations.xml">Active Stations XML</a>
 * <p>
 * Example station entry from XML:
 * <pre>
 * &lt;station id="44060" lat="41.263" lon="-72.067" elev="0"
 *          name="Eastern Long Island Sound" owner="MYSOUND" pgm="IOOS Partners"
 *          type="fixed" met="y" currents="n" waterquality="n" dart="n"/&gt;
 * </pre>
 */
public class BuoyStation implements LocatedStation {

    /**
     * Station ID (id attribute) - WMO ID of the station.
     * May contain alphanumeric characters (e.g., "44060", "0y2w3", "wplf1").
     */
    private String id;

    /**
     * Latitude (lat attribute) - Signed decimal degree value of the latitude.
     * Negative numbers represent the southern hemisphere.
     * Unit: decimal degrees
     */
    private double latitude;

    /**
     * Longitude (lon attribute) - Signed decimal degree value of the longitude.
     * Negative numbers represent the western hemisphere.
     * Unit: decimal degrees
     */
    private double longitude;

    /**
     * Elevation (elev attribute) - Station elevation above mean sea level.
     * Unit: meters (m)
     * Optional: May be null if elevation data is not available.
     */
    private Double elevation;

    /**
     * Station name (name attribute) - Descriptive name of the station.
     * Example: "Eastern Long Island Sound"
     */
    private String name;

    /**
     * Owner (owner attribute) - Name of the station owner/operator.
     * Example: "NDBC", "NOS", "GLERL", "National Estuarine Research Reserve System"
     */
    private String owner;

    /**
     * Program (pgm attribute) - The program or network the station belongs to.
     * Example: "NDBC Meteorological/Ocean", "NOS/CO-OPS", "IOOS Partners", "NERRS", "Tsunami"
     */
    private String program;

    /**
     * Station type (type attribute) - The type of observation platform.
     * Possible values: "buoy", "fixed", "dart", "usv", "other"
     * <ul>
     *   <li>buoy - Moored buoy</li>
     *   <li>fixed - Fixed platform (e.g., C-MAN station, pier)</li>
     *   <li>dart - DART (Deep-ocean Assessment and Reporting of Tsunamis) buoy</li>
     *   <li>usv - Unmanned Surface Vehicle (e.g., Saildrone)</li>
     *   <li>other - Other platform types</li>
     * </ul>
     */
    private String type;

    /**
     * Meteorological data available (met attribute) - Indicates whether the station
     * has reported meteorological data in the past 8 hours.
     * Derived from "y"/"n" in XML.
     */
    private boolean hasMet;

    /**
     * Currents data available (currents attribute) - Indicates whether the station
     * has reported single point or profile currents data in the past 8 hours.
     * Derived from "y"/"n" in XML.
     */
    private boolean hasCurrents;

    /**
     * Water quality data available (waterquality attribute) - Indicates whether the station
     * has reported water temperature, salinity and/or ocean chemistry data in the past 8 hours.
     * Derived from "y"/"n" in XML.
     */
    private boolean hasWaterQuality;

    /**
     * DART data available (dart attribute) - Indicates whether the station
     * has reported water level (tsunami) data in the past 24 hours.
     * Derived from "y"/"n" in XML.
     */
    private boolean hasDart;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Double getElevation() {
        return elevation;
    }

    public void setElevation(Double elevation) {
        this.elevation = elevation;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getDisplayName() {
        return name != null && !name.isEmpty() ? name : (id != null ? id : "");
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean hasMet() {
        return hasMet;
    }

    public void setHasMet(boolean hasMet) {
        this.hasMet = hasMet;
    }

    public boolean hasCurrents() {
        return hasCurrents;
    }

    public void setHasCurrents(boolean hasCurrents) {
        this.hasCurrents = hasCurrents;
    }

    public boolean hasWaterQuality() {
        return hasWaterQuality;
    }

    public void setHasWaterQuality(boolean hasWaterQuality) {
        this.hasWaterQuality = hasWaterQuality;
    }

    public boolean hasDart() {
        return hasDart;
    }

    public void setHasDart(boolean hasDart) {
        this.hasDart = hasDart;
    }

    @Override
    public String toString() {
        return "BuoyStation{" +
                "id='" + id + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", elevation=" + elevation +
                ", name='" + name + '\'' +
                ", owner='" + owner + '\'' +
                ", program='" + program + '\'' +
                ", type='" + type + '\'' +
                ", hasMet=" + hasMet +
                ", hasCurrents=" + hasCurrents +
                ", hasWaterQuality=" + hasWaterQuality +
                ", hasDart=" + hasDart +
                '}';
    }
}


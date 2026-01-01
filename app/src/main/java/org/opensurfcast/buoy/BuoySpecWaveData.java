package org.opensurfcast.buoy;

/**
 * Represents a single Detailed Wave Summary observation from an NDBC (National Data Buoy Center) buoy station.
 * Contains spectral wave data as defined at:
 * https://www.ndbc.noaa.gov/faq/measdes.shtml#swden
 * <p>
 * Data file format (.spec): https://www.ndbc.noaa.gov/data/realtime2/{stationId}.spec
 * <p>
 * Missing data in realtime files is denoted by "MM".
 * All measurements are in metric units (SI) unless otherwise noted.
 * Times are in UTC.
 * <p>
 * Data format:
 * <pre>
 * #YY  MM DD hh mm WVHT  SwH  SwP  WWH  WWP SwD WWD  STEEPNESS  APD MWD
 * #yr  mo dy hr mn    m    m  sec    m  sec  -  degT     -      sec degT
 * </pre>
 */
public class BuoySpecWaveData {

    /**
     * Year of observation (YY) - Four-digit year.
     * Example: 2025
     */
    private int year;

    /**
     * Month of observation (MM) - 1-12.
     */
    private int month;

    /**
     * Day of observation (DD) - 1-31.
     */
    private int day;

    /**
     * Hour of observation (hh) - 0-23, in UTC.
     */
    private int hour;

    /**
     * Minute of observation (mm) - 0-59.
     */
    private int minute;

    /**
     * Unix epoch timestamp in seconds.
     * Represents the observation time as seconds since January 1, 1970 00:00:00 UTC.
     */
    private long epochSeconds;

    /**
     * Significant wave height (WVHT) - Calculated as the average of the highest
     * one-third of all wave heights during the 20-minute sampling period.
     * Unit: meters (m)
     * Optional: May be null if sensor data is unavailable.
     */
    private Double waveHeight;

    /**
     * Swell height (SwH) - Height of the swell waves.
     * Unit: meters (m)
     * Optional: May be null if sensor data is unavailable.
     */
    private Double swellHeight;

    /**
     * Swell period (SwP) - Period of the swell waves.
     * Unit: seconds (sec)
     * Optional: May be null if sensor data is unavailable.
     */
    private Double swellPeriod;

    /**
     * Wind wave height (WWH) - Height of the wind-generated waves.
     * Unit: meters (m)
     * Optional: May be null if sensor data is unavailable.
     */
    private Double windWaveHeight;

    /**
     * Wind wave period (WWP) - Period of the wind-generated waves.
     * Unit: seconds (sec)
     * Optional: May be null if sensor data is unavailable.
     */
    private Double windWavePeriod;

    /**
     * Swell direction (SwD) - The direction from which the swell waves are coming.
     * Expressed as a cardinal direction (e.g., N, NE, E, SE, S, SW, W, NW, NNE, ENE, etc.).
     * Optional: May be null if sensor data is unavailable.
     */
    private String swellDirection;

    /**
     * Wind wave direction (WWD) - The direction from which the wind waves are coming.
     * Expressed as a cardinal direction (e.g., N, NE, E, SE, S, SW, W, NW, NNE, ENE, etc.).
     * Optional: May be null if sensor data is unavailable.
     */
    private String windWaveDirection;

    /**
     * Wave steepness (STEEPNESS) - A categorical description of wave steepness.
     * Possible values: SWELL, AVERAGE, STEEP, VERY_STEEP
     * <p>
     * Steepness is calculated as the ratio of wave height to wavelength.
     * - SWELL: Low steepness, typically long-period ocean swell
     * - AVERAGE: Moderate steepness
     * - STEEP: High steepness
     * - VERY_STEEP: Very high steepness, typically short-period wind waves
     * <p>
     * Optional: May be null if sensor data is unavailable.
     */
    private String steepness;

    /**
     * Average wave period (APD) - Average period of all waves during the
     * 20-minute sampling period.
     * Unit: seconds (sec)
     * Optional: May be null if sensor data is unavailable.
     */
    private Double averageWavePeriod;

    /**
     * Mean wave direction (MWD) - The direction from which the waves at the
     * dominant period are coming. Degrees from true North, increasing
     * clockwise, with North as 0 degrees and East as 90 degrees.
     * Unit: degrees (degT)
     * Optional: May be null if sensor data is unavailable.
     */
    private Integer meanWaveDirection;

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public long getEpochSeconds() {
        return epochSeconds;
    }

    public void setEpochSeconds(long epochSeconds) {
        this.epochSeconds = epochSeconds;
    }

    public Double getWaveHeight() {
        return waveHeight;
    }

    public void setWaveHeight(Double waveHeight) {
        this.waveHeight = waveHeight;
    }

    public Double getSwellHeight() {
        return swellHeight;
    }

    public void setSwellHeight(Double swellHeight) {
        this.swellHeight = swellHeight;
    }

    public Double getSwellPeriod() {
        return swellPeriod;
    }

    public void setSwellPeriod(Double swellPeriod) {
        this.swellPeriod = swellPeriod;
    }

    public Double getWindWaveHeight() {
        return windWaveHeight;
    }

    public void setWindWaveHeight(Double windWaveHeight) {
        this.windWaveHeight = windWaveHeight;
    }

    public Double getWindWavePeriod() {
        return windWavePeriod;
    }

    public void setWindWavePeriod(Double windWavePeriod) {
        this.windWavePeriod = windWavePeriod;
    }

    public String getSwellDirection() {
        return swellDirection;
    }

    public void setSwellDirection(String swellDirection) {
        this.swellDirection = swellDirection;
    }

    public String getWindWaveDirection() {
        return windWaveDirection;
    }

    public void setWindWaveDirection(String windWaveDirection) {
        this.windWaveDirection = windWaveDirection;
    }

    public String getSteepness() {
        return steepness;
    }

    public void setSteepness(String steepness) {
        this.steepness = steepness;
    }

    public Double getAverageWavePeriod() {
        return averageWavePeriod;
    }

    public void setAverageWavePeriod(Double averageWavePeriod) {
        this.averageWavePeriod = averageWavePeriod;
    }

    public Integer getMeanWaveDirection() {
        return meanWaveDirection;
    }

    public void setMeanWaveDirection(Integer meanWaveDirection) {
        this.meanWaveDirection = meanWaveDirection;
    }

    @Override
    public String toString() {
        return "BuoySpecWaveData{" +
                "year=" + year +
                ", month=" + month +
                ", day=" + day +
                ", hour=" + hour +
                ", minute=" + minute +
                ", epochSeconds=" + epochSeconds +
                ", waveHeight=" + waveHeight +
                ", swellHeight=" + swellHeight +
                ", swellPeriod=" + swellPeriod +
                ", windWaveHeight=" + windWaveHeight +
                ", windWavePeriod=" + windWavePeriod +
                ", swellDirection='" + swellDirection + '\'' +
                ", windWaveDirection='" + windWaveDirection + '\'' +
                ", steepness='" + steepness + '\'' +
                ", averageWavePeriod=" + averageWavePeriod +
                ", meanWaveDirection=" + meanWaveDirection +
                '}';
    }
}


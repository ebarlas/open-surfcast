package org.opensurfcast.buoy;

/**
 * Represents a single observation from an NDBC (National Data Buoy Center) buoy station.
 * Contains standard meteorological data as defined at:
 * https://www.ndbc.noaa.gov/faq/measdes.shtml#stdmet
 * <p>
 * Missing data in realtime files is denoted by "MM".
 * All measurements are in metric units (SI) unless otherwise noted.
 * Times are in UTC.
 */
public class BuoyStdMetData implements HasEpochSeconds {

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
     * Wind direction (WDIR) - The direction the wind is coming from in degrees
     * clockwise from true North (0-360°).
     * Unit: degrees (degT)
     * Optional: May be null if sensor data is unavailable.
     */
    private Integer windDirection;

    /**
     * Wind speed (WSPD) - Wind speed averaged over an eight-minute period for buoys
     * and a two-minute period for land stations. Reported hourly.
     * Unit: meters per second (m/s)
     * Optional: May be null if sensor data is unavailable.
     */
    private Double windSpeed;

    /**
     * Gust speed (GST) - Peak 5 or 8 second gust speed measured during the
     * eight-minute or two-minute period.
     * Unit: meters per second (m/s)
     * Optional: May be null if sensor data is unavailable.
     */
    private Double gustSpeed;

    /**
     * Significant wave height (WVHT) - Calculated as the average of the highest
     * one-third of all wave heights during the 20-minute sampling period.
     * Unit: meters (m)
     * Optional: May be null if sensor data is unavailable.
     */
    private Double waveHeight;

    /**
     * Dominant wave period (DPD) - The period with the maximum wave energy.
     * Unit: seconds (sec)
     * Optional: May be null if sensor data is unavailable.
     */
    private Double dominantWavePeriod;

    /**
     * Average wave period (APD) - Average period of all waves during the
     * 20-minute sampling period.
     * Unit: seconds (sec)
     * Optional: May be null if sensor data is unavailable.
     */
    private Double averageWavePeriod;

    /**
     * Mean wave direction (MWD) - The direction from which the waves at the
     * dominant period (DPD) are coming. Degrees from true North, increasing
     * clockwise, with North as 0 degrees and East as 90 degrees.
     * Unit: degrees (degT)
     * Optional: May be null if sensor data is unavailable.
     */
    private Integer meanWaveDirection;

    /**
     * Sea level pressure (PRES) - Atmospheric pressure reduced to sea level.
     * For C-MAN sites and Great Lakes buoys, the recorded pressure is reduced
     * to sea level using NWS Technical Procedures Bulletin 291.
     * Unit: hectopascals (hPa)
     * Optional: May be null if sensor data is unavailable.
     */
    private Double pressure;

    /**
     * Air temperature (ATMP) - Measured at sensor height on buoys.
     * Unit: degrees Celsius (degC)
     * Optional: May be null if sensor data is unavailable.
     */
    private Double airTemperature;

    /**
     * Sea surface temperature (WTMP) - For buoys, the depth is referenced to
     * the hull's waterline. For fixed platforms it varies with tide but is
     * referenced to, or near, Mean Lower Low Water (MLLW).
     * Unit: degrees Celsius (degC)
     * Optional: May be null if sensor data is unavailable.
     */
    private Double waterTemperature;

    /**
     * Dew point temperature (DEWP) - Taken at the same height as the air
     * temperature measurement.
     * Unit: degrees Celsius (degC)
     * Optional: May be null if sensor data is unavailable.
     */
    private Double dewPoint;

    /**
     * Visibility (VIS) - Station visibility. Note that buoy stations are
     * limited to reports from 0 to 1.6 nautical miles.
     * Unit: nautical miles (nmi)
     * Optional: May be null if sensor data is unavailable. Rarely reported.
     */
    private Double visibility;

    /**
     * Pressure tendency (PTDY) - The direction (plus or minus) and the amount
     * of pressure change for a three hour period ending at the time of observation.
     * Not included in historical files.
     * Unit: hectopascals (hPa)
     * Optional: May be null if sensor data is unavailable. Rarely reported.
     */
    private Double pressureTendency;

    /**
     * Tide (TIDE) - The water level above or below Mean Lower Low Water (MLLW).
     * Unit: feet (ft)
     * Optional: May be null if sensor data is unavailable. Rarely reported.
     */
    private Double tide;

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

    public Integer getWindDirection() {
        return windDirection;
    }

    public void setWindDirection(Integer windDirection) {
        this.windDirection = windDirection;
    }

    public Double getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(Double windSpeed) {
        this.windSpeed = windSpeed;
    }

    public Double getGustSpeed() {
        return gustSpeed;
    }

    public void setGustSpeed(Double gustSpeed) {
        this.gustSpeed = gustSpeed;
    }

    public Double getWaveHeight() {
        return waveHeight;
    }

    public void setWaveHeight(Double waveHeight) {
        this.waveHeight = waveHeight;
    }

    public Double getDominantWavePeriod() {
        return dominantWavePeriod;
    }

    public void setDominantWavePeriod(Double dominantWavePeriod) {
        this.dominantWavePeriod = dominantWavePeriod;
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

    public Double getPressure() {
        return pressure;
    }

    public void setPressure(Double pressure) {
        this.pressure = pressure;
    }

    public Double getAirTemperature() {
        return airTemperature;
    }

    public void setAirTemperature(Double airTemperature) {
        this.airTemperature = airTemperature;
    }

    public Double getWaterTemperature() {
        return waterTemperature;
    }

    public void setWaterTemperature(Double waterTemperature) {
        this.waterTemperature = waterTemperature;
    }

    public Double getDewPoint() {
        return dewPoint;
    }

    public void setDewPoint(Double dewPoint) {
        this.dewPoint = dewPoint;
    }

    public Double getVisibility() {
        return visibility;
    }

    public void setVisibility(Double visibility) {
        this.visibility = visibility;
    }

    public Double getPressureTendency() {
        return pressureTendency;
    }

    public void setPressureTendency(Double pressureTendency) {
        this.pressureTendency = pressureTendency;
    }

    public Double getTide() {
        return tide;
    }

    public void setTide(Double tide) {
        this.tide = tide;
    }

    @Override
    public String toString() {
        return "BuoyObs{" +
                "year=" + year +
                ", month=" + month +
                ", day=" + day +
                ", hour=" + hour +
                ", minute=" + minute +
                ", epochSeconds=" + epochSeconds +
                ", windDirection=" + windDirection +
                ", windSpeed=" + windSpeed +
                ", gustSpeed=" + gustSpeed +
                ", waveHeight=" + waveHeight +
                ", dominantWavePeriod=" + dominantWavePeriod +
                ", averageWavePeriod=" + averageWavePeriod +
                ", meanWaveDirection=" + meanWaveDirection +
                ", pressure=" + pressure +
                ", airTemperature=" + airTemperature +
                ", waterTemperature=" + waterTemperature +
                ", dewPoint=" + dewPoint +
                ", visibility=" + visibility +
                ", pressureTendency=" + pressureTendency +
                ", tide=" + tide +
                '}';
    }
}

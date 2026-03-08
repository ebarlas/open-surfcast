package org.opensurfcast.buoy;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Averages buoy observation data into one-hour bins.
 * <p>
 * Observations are grouped by their {@code (year, month, day, hour)} fields.
 * Numeric {@link Double} fields are averaged (arithmetic mean of non-null values).
 * {@link Integer} direction fields and {@link String} categorical fields use
 * the value from the most recent observation within the hour.
 * The output observation has {@code minute = 0} and {@code epochSeconds}
 * recomputed from the calendar fields.
 */
public final class BuoyDataHourlyAverager {

    private BuoyDataHourlyAverager() {
    }

    private record HourKey(int year, int month, int day, int hour) {
    }

    /**
     * Averages standard meteorological data into hourly bins.
     *
     * @param dataList observations sorted by time (any order)
     * @return one observation per hour with averaged numeric fields
     */
    public static List<BuoyStdMetData> averageStdMetByHour(List<BuoyStdMetData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return new ArrayList<>();
        }

        Map<HourKey, List<BuoyStdMetData>> bins = new LinkedHashMap<>();
        for (BuoyStdMetData d : dataList) {
            HourKey key = new HourKey(d.getYear(), d.getMonth(), d.getDay(), d.getHour());
            bins.computeIfAbsent(key, k -> new ArrayList<>()).add(d);
        }

        List<BuoyStdMetData> result = new ArrayList<>(bins.size());
        for (Map.Entry<HourKey, List<BuoyStdMetData>> entry : bins.entrySet()) {
            List<BuoyStdMetData> group = entry.getValue();
            BuoyStdMetData first = group.get(0);
            BuoyStdMetData mostRecent = findMostRecent(group, BuoyStdMetData::getEpochSeconds);

            BuoyStdMetData avg = new BuoyStdMetData();
            avg.setYear(first.getYear());
            avg.setMonth(first.getMonth());
            avg.setDay(first.getDay());
            avg.setHour(first.getHour());
            avg.setMinute(0);
            avg.setEpochSeconds(computeEpochSeconds(
                    first.getYear(), first.getMonth(), first.getDay(), first.getHour()));

            avg.setWindSpeed(averageDouble(group, BuoyStdMetData::getWindSpeed));
            avg.setGustSpeed(averageDouble(group, BuoyStdMetData::getGustSpeed));
            avg.setWaveHeight(averageDouble(group, BuoyStdMetData::getWaveHeight));
            avg.setDominantWavePeriod(averageDouble(group, BuoyStdMetData::getDominantWavePeriod));
            avg.setAverageWavePeriod(averageDouble(group, BuoyStdMetData::getAverageWavePeriod));
            avg.setPressure(averageDouble(group, BuoyStdMetData::getPressure));
            avg.setAirTemperature(averageDouble(group, BuoyStdMetData::getAirTemperature));
            avg.setWaterTemperature(averageDouble(group, BuoyStdMetData::getWaterTemperature));
            avg.setDewPoint(averageDouble(group, BuoyStdMetData::getDewPoint));
            avg.setVisibility(averageDouble(group, BuoyStdMetData::getVisibility));
            avg.setPressureTendency(averageDouble(group, BuoyStdMetData::getPressureTendency));
            avg.setTide(averageDouble(group, BuoyStdMetData::getTide));

            avg.setWindDirection(mostRecent.getWindDirection());
            avg.setMeanWaveDirection(mostRecent.getMeanWaveDirection());

            result.add(avg);
        }
        return result;
    }

    /**
     * Averages spectral wave data into hourly bins.
     *
     * @param dataList observations sorted by time (any order)
     * @return one observation per hour with averaged numeric fields
     */
    public static List<BuoySpecWaveData> averageSpecWaveByHour(List<BuoySpecWaveData> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            return new ArrayList<>();
        }

        Map<HourKey, List<BuoySpecWaveData>> bins = new LinkedHashMap<>();
        for (BuoySpecWaveData d : dataList) {
            HourKey key = new HourKey(d.getYear(), d.getMonth(), d.getDay(), d.getHour());
            bins.computeIfAbsent(key, k -> new ArrayList<>()).add(d);
        }

        List<BuoySpecWaveData> result = new ArrayList<>(bins.size());
        for (Map.Entry<HourKey, List<BuoySpecWaveData>> entry : bins.entrySet()) {
            List<BuoySpecWaveData> group = entry.getValue();
            BuoySpecWaveData first = group.get(0);
            BuoySpecWaveData mostRecent = findMostRecent(group, BuoySpecWaveData::getEpochSeconds);

            BuoySpecWaveData avg = new BuoySpecWaveData();
            avg.setYear(first.getYear());
            avg.setMonth(first.getMonth());
            avg.setDay(first.getDay());
            avg.setHour(first.getHour());
            avg.setMinute(0);
            avg.setEpochSeconds(computeEpochSeconds(
                    first.getYear(), first.getMonth(), first.getDay(), first.getHour()));

            avg.setWaveHeight(averageDouble(group, BuoySpecWaveData::getWaveHeight));
            avg.setSwellHeight(averageDouble(group, BuoySpecWaveData::getSwellHeight));
            avg.setSwellPeriod(averageDouble(group, BuoySpecWaveData::getSwellPeriod));
            avg.setWindWaveHeight(averageDouble(group, BuoySpecWaveData::getWindWaveHeight));
            avg.setWindWavePeriod(averageDouble(group, BuoySpecWaveData::getWindWavePeriod));
            avg.setAverageWavePeriod(averageDouble(group, BuoySpecWaveData::getAverageWavePeriod));

            avg.setMeanWaveDirection(mostRecent.getMeanWaveDirection());
            avg.setSwellDirection(mostRecent.getSwellDirection());
            avg.setWindWaveDirection(mostRecent.getWindWaveDirection());
            avg.setSteepness(mostRecent.getSteepness());

            result.add(avg);
        }
        return result;
    }

    /**
     * Computes the arithmetic mean of a nullable Double field across a group,
     * ignoring null values. Returns null if all values are null.
     */
    static <T> Double averageDouble(List<T> group, Function<T, Double> getter) {
        double sum = 0;
        int count = 0;
        for (T item : group) {
            Double val = getter.apply(item);
            if (val != null) {
                sum += val;
                count++;
            }
        }
        return count > 0 ? sum / count : null;
    }

    private static <T> T findMostRecent(List<T> group, Function<T, Long> epochGetter) {
        T best = group.get(0);
        long bestEpoch = epochGetter.apply(best);
        for (int i = 1; i < group.size(); i++) {
            T item = group.get(i);
            long epoch = epochGetter.apply(item);
            if (epoch > bestEpoch) {
                best = item;
                bestEpoch = epoch;
            }
        }
        return best;
    }

    private static long computeEpochSeconds(int year, int month, int day, int hour) {
        return ZonedDateTime.of(year, month, day, hour, 0, 0, 0, ZoneOffset.UTC)
                .toEpochSecond();
    }
}

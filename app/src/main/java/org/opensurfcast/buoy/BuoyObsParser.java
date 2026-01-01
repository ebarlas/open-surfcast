package org.opensurfcast.buoy;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser for NDBC standard meteorological data format.
 * Format specification: https://www.ndbc.noaa.gov/faq/measdes.shtml#stdmet
 */
public class BuoyObsParser {

    private static final String MISSING_VALUE = "MM";

    private static final int EXPECTED_COLUMNS = 19;

    public static List<BuoyObs> parse(String text) {
        try {
            return parse(new ByteArrayInputStream(text.getBytes()));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static List<BuoyObs> parse(InputStream inputStream) throws IOException {
        List<BuoyObs> observations = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            BuoyObs obs = parseLine(line);
            observations.add(obs);
        }

        return observations;
    }

    private static BuoyObs parseLine(String line) throws IOException {
        String[] parts = line.trim().split("\\s+");
        if (parts.length < EXPECTED_COLUMNS) {
            throw new IOException("Expected " + EXPECTED_COLUMNS + " columns but found " + parts.length);
        }

        ColumnScanner scanner = new ColumnScanner(parts);
        BuoyObs obs = new BuoyObs();

        int year = scanner.nextInt();
        int month = scanner.nextInt();
        int day = scanner.nextInt();
        int hour = scanner.nextInt();
        int minute = scanner.nextInt();

        obs.setYear(year);
        obs.setMonth(month);
        obs.setDay(day);
        obs.setHour(hour);
        obs.setMinute(minute);

        ZonedDateTime dateTime = ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneOffset.UTC);
        obs.setEpochSeconds(dateTime.toEpochSecond());

        obs.setWindDirection(scanner.nextOptionalInteger());
        obs.setWindSpeed(scanner.nextOptionalDouble());
        obs.setGustSpeed(scanner.nextOptionalDouble());
        obs.setWaveHeight(scanner.nextOptionalDouble());
        obs.setDominantWavePeriod(scanner.nextOptionalDouble());
        obs.setAverageWavePeriod(scanner.nextOptionalDouble());
        obs.setMeanWaveDirection(scanner.nextOptionalInteger());
        obs.setPressure(scanner.nextOptionalDouble());
        obs.setAirTemperature(scanner.nextOptionalDouble());
        obs.setWaterTemperature(scanner.nextOptionalDouble());
        obs.setDewPoint(scanner.nextOptionalDouble());
        obs.setVisibility(scanner.nextOptionalDouble());
        obs.setPressureTendency(scanner.nextOptionalDouble());
        obs.setTide(scanner.nextOptionalDouble());

        return obs;
    }

    private static class ColumnScanner {
        private final String[] parts;
        private int index = 0;

        ColumnScanner(String[] parts) {
            this.parts = parts;
        }

        int nextInt() {
            return Integer.parseInt(parts[index++]);
        }

        Double nextOptionalDouble() {
            String value = parts[index++];
            if (MISSING_VALUE.equals(value)) {
                return null;
            }
            return Double.parseDouble(value);
        }

        Integer nextOptionalInteger() {
            String value = parts[index++];
            if (MISSING_VALUE.equals(value)) {
                return null;
            }
            return Integer.parseInt(value);
        }
    }
}

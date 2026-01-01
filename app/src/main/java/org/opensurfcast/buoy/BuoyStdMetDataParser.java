package org.opensurfcast.buoy;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Parser for NDBC standard meteorological data format.
 * Format specification: https://www.ndbc.noaa.gov/faq/measdes.shtml#stdmet
 */
public class BuoyStdMetDataParser {

    private static final int EXPECTED_COLUMNS = 19;

    private static final NdbcNoaaParser<BuoyStdMetData> PARSER =
            new NdbcNoaaParser<>(EXPECTED_COLUMNS, BuoyStdMetDataParser::parseRow);

    public static List<BuoyStdMetData> parse(InputStream inputStream) throws IOException {
        return PARSER.parse(inputStream);
    }

    private static BuoyStdMetData parseRow(ColumnScanner scanner) {
        BuoyStdMetData obs = new BuoyStdMetData();

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
}

package org.opensurfcast.buoy;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Parser for NDBC Detailed Wave Summary (spectral) data format.
 * Format specification: https://www.ndbc.noaa.gov/faq/measdes.shtml
 * <p>
 * Data file format:
 * <pre>
 * #YY  MM DD hh mm WVHT  SwH  SwP  WWH  WWP SwD WWD  STEEPNESS  APD MWD
 * #yr  mo dy hr mn    m    m  sec    m  sec  -  degT     -      sec degT
 * </pre>
 */
public class BuoySpecWaveDataParser {

    private static final int EXPECTED_COLUMNS = 15;

    private static final NdbcNoaaParser<BuoySpecWaveData> PARSER =
            new NdbcNoaaParser<>(EXPECTED_COLUMNS, BuoySpecWaveDataParser::parseRow);

    public static List<BuoySpecWaveData> parse(InputStream inputStream) throws IOException {
        return PARSER.parse(inputStream);
    }

    private static BuoySpecWaveData parseRow(ColumnScanner scanner) {
        BuoySpecWaveData obs = new BuoySpecWaveData();

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

        obs.setWaveHeight(scanner.nextOptionalDouble());
        obs.setSwellHeight(scanner.nextOptionalDouble());
        obs.setSwellPeriod(scanner.nextOptionalDouble());
        obs.setWindWaveHeight(scanner.nextOptionalDouble());
        obs.setWindWavePeriod(scanner.nextOptionalDouble());
        obs.setSwellDirection(scanner.nextOptionalString());
        obs.setWindWaveDirection(scanner.nextOptionalString());
        obs.setSteepness(scanner.nextOptionalString());
        obs.setAverageWavePeriod(scanner.nextOptionalDouble());
        obs.setMeanWaveDirection(scanner.nextOptionalInteger());

        return obs;
    }
}

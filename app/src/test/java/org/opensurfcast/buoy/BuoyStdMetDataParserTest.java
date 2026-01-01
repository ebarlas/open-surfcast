package org.opensurfcast.buoy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class BuoyStdMetDataParserTest {

    private static final String TEST_DATA = """
            #YY  MM DD hh mm WDIR WSPD GST  WVHT   DPD   APD MWD   PRES  ATMP  WTMP  DEWP  VIS PTDY  TIDE
            #yr  mo dy hr mn degT m/s  m/s     m   sec   sec degT   hPa  degC  degC  degC  nmi  hPa    ft
            2026 01 01 00 50  80 12.0 14.0   1.5     4   4.4  98 1011.9  11.1  13.4   9.1   MM   MM    MM
            2026 01 01 00 40  80 12.0 14.0    MM    MM    MM  MM 1012.0  11.2  13.4   9.1   MM   MM    MM
            2026 01 01 00 30  80 12.0 14.0    MM    MM    MM  MM 1012.0  11.1  13.4   9.1   MM   MM    MM
            2026 01 01 00 20  80 11.0 14.0   1.3    11   4.5 283 1012.2  11.2  13.4   9.3   MM   MM    MM
            2026 01 01 00 10  80 12.0 14.0   1.3    MM   4.5 283 1012.4  11.2  13.4   9.1   MM   MM    MM
            2026 01 01 00 00  80 11.0 13.0    MM    MM    MM  MM 1012.5  11.4  13.4   9.2   MM -1.5    MM
            """;

    private static InputStream toInputStream(String text) {
        return new ByteArrayInputStream(text.getBytes());
    }

    private static List<BuoyStdMetData> parse(String text) throws IOException {
        return BuoyStdMetDataParser.parse(toInputStream(text));
    }

    @Test
    public void testParseReturnsCorrectCount() throws IOException {
        List<BuoyStdMetData> observations = parse(TEST_DATA);
        assertEquals(6, observations.size());
    }

    @Test
    public void testParseDateTime() throws IOException {
        List<BuoyStdMetData> observations = parse(TEST_DATA);
        BuoyStdMetData first = observations.get(0);

        assertEquals(2026, first.getYear());
        assertEquals(1, first.getMonth());
        assertEquals(1, first.getDay());
        assertEquals(0, first.getHour());
        assertEquals(50, first.getMinute());
        assertEquals(1767228600L, first.getEpochSeconds());
    }

    @Test
    public void testParseCompleteObservation() throws IOException {
        List<BuoyStdMetData> observations = parse(TEST_DATA);
        BuoyStdMetData first = observations.get(0);

        assertEquals(Integer.valueOf(80), first.getWindDirection());
        assertEquals(12.0, first.getWindSpeed(), 0.001);
        assertEquals(14.0, first.getGustSpeed(), 0.001);
        assertEquals(1.5, first.getWaveHeight(), 0.001);
        assertEquals(4.0, first.getDominantWavePeriod(), 0.001);
        assertEquals(4.4, first.getAverageWavePeriod(), 0.001);
        assertEquals(Integer.valueOf(98), first.getMeanWaveDirection());
        assertEquals(1011.9, first.getPressure(), 0.001);
        assertEquals(11.1, first.getAirTemperature(), 0.001);
        assertEquals(13.4, first.getWaterTemperature(), 0.001);
        assertEquals(9.1, first.getDewPoint(), 0.001);
        assertNull(first.getVisibility());
        assertNull(first.getPressureTendency());
        assertNull(first.getTide());
    }

    @Test
    public void testParseMissingWaveData() throws IOException {
        List<BuoyStdMetData> observations = parse(TEST_DATA);
        BuoyStdMetData second = observations.get(1); // Row with MM wave data

        assertEquals(Integer.valueOf(80), second.getWindDirection());
        assertNull(second.getWaveHeight());
        assertNull(second.getDominantWavePeriod());
        assertNull(second.getAverageWavePeriod());
        assertNull(second.getMeanWaveDirection());
        assertEquals(1012.0, second.getPressure(), 0.001);
    }

    @Test
    public void testParseNegativeValue() throws IOException {
        List<BuoyStdMetData> observations = parse(TEST_DATA);
        BuoyStdMetData last = observations.get(5);

        assertEquals(-1.5, last.getPressureTendency(), 0.001);
    }

    @Test
    public void testParseEmptyInput() throws IOException {
        List<BuoyStdMetData> observations = parse("");
        assertEquals(0, observations.size());
    }

    @Test
    public void testParseHeadersOnly() throws IOException {
        String headersOnly = """
                #YY  MM DD hh mm WDIR WSPD GST  WVHT   DPD   APD MWD   PRES  ATMP  WTMP  DEWP  VIS PTDY  TIDE
                #yr  mo dy hr mn degT m/s  m/s     m   sec   sec degT   hPa  degC  degC  degC  nmi  hPa    ft
                """;
        List<BuoyStdMetData> observations = parse(headersOnly);
        assertEquals(0, observations.size());
    }

    @Test
    public void testParseMalformedInputThrows() {
        String malformed = "2026 01 01 00 50  80 12.0 14.0";
        IOException ex = assertThrows(IOException.class, () -> parse(malformed));
        assertTrue(ex.getMessage().contains("Expected 19 columns"));
    }
}

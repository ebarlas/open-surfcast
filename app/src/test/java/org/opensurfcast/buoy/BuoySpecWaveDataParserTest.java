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

public class BuoySpecWaveDataParserTest {

    /**
     * Sample data based on real NDBC .spec file format from:
     * https://www.ndbc.noaa.gov/data/realtime2/46013.spec
     */
    private static final String TEST_DATA = """
            #YY  MM DD hh mm WVHT  SwH  SwP  WWH  WWP SwD WWD  STEEPNESS  APD MWD
            #yr  mo dy hr mn    m    m  sec    m  sec  -  degT     -      sec degT
            2026 01 01 04 10  1.3  0.9 11.4  1.0  4.0 WNW   E    AVERAGE  4.6 296
            2026 01 01 03 40  1.3  0.8 10.8  1.1  4.2 WNW   E    AVERAGE  4.5 287
            2026 01 01 03 10  1.4  0.8 12.1  1.1  4.3   W   E VERY_STEEP  4.5  90
            2026 01 01 02 40  1.4  0.8 11.4  1.1  4.3 WNW   E    AVERAGE  4.5 285
            2026 01 01 02 10  1.3  0.8 10.8  1.1  4.0   W   E    AVERAGE  4.4 274
            2025 12 31 23 40  1.3  0.9 11.4  0.9  4.2   W   E      SWELL  4.7 280
            """;

    private static final String TEST_DATA_WITH_MISSING = """
            #YY  MM DD hh mm WVHT  SwH  SwP  WWH  WWP SwD WWD  STEEPNESS  APD MWD
            #yr  mo dy hr mn    m    m  sec    m  sec  -  degT     -      sec degT
            2026 01 01 04 10  1.3  0.9 11.4  1.0  4.0 WNW   E    AVERAGE  4.6 296
            2026 01 01 03 40   MM   MM   MM   MM   MM  MM  MM         MM   MM  MM
            2026 01 01 03 10  1.4   MM 12.1   MM  4.3  MM   E       STEEP  4.5  90
            """;

    private static InputStream toInputStream(String text) {
        return new ByteArrayInputStream(text.getBytes());
    }

    private static List<BuoySpecWaveData> parse(String text) throws IOException {
        return BuoySpecWaveDataParser.parse(toInputStream(text));
    }

    @Test
    public void testParseReturnsCorrectCount() throws IOException {
        List<BuoySpecWaveData> observations = parse(TEST_DATA);
        assertEquals(6, observations.size());
    }

    @Test
    public void testParseDateTime() throws IOException {
        List<BuoySpecWaveData> observations = parse(TEST_DATA);
        BuoySpecWaveData first = observations.get(0);

        assertEquals(2026, first.getYear());
        assertEquals(1, first.getMonth());
        assertEquals(1, first.getDay());
        assertEquals(4, first.getHour());
        assertEquals(10, first.getMinute());
        assertEquals(1767240600L, first.getEpochSeconds());
    }

    @Test
    public void testParseCompleteObservation() throws IOException {
        List<BuoySpecWaveData> observations = parse(TEST_DATA);
        BuoySpecWaveData first = observations.get(0);

        assertEquals(1.3, first.getWaveHeight(), 0.001);
        assertEquals(0.9, first.getSwellHeight(), 0.001);
        assertEquals(11.4, first.getSwellPeriod(), 0.001);
        assertEquals(1.0, first.getWindWaveHeight(), 0.001);
        assertEquals(4.0, first.getWindWavePeriod(), 0.001);
        assertEquals("WNW", first.getSwellDirection());
        assertEquals("E", first.getWindWaveDirection());
        assertEquals("AVERAGE", first.getSteepness());
        assertEquals(4.6, first.getAverageWavePeriod(), 0.001);
        assertEquals(Integer.valueOf(296), first.getMeanWaveDirection());
    }

    @Test
    public void testParseDifferentDirections() throws IOException {
        List<BuoySpecWaveData> observations = parse(TEST_DATA);

        // First row: WNW
        assertEquals("WNW", observations.get(0).getSwellDirection());
        // Third row: W
        assertEquals("W", observations.get(2).getSwellDirection());
    }

    @Test
    public void testParseDifferentSteepnessValues() throws IOException {
        List<BuoySpecWaveData> observations = parse(TEST_DATA);

        assertEquals("AVERAGE", observations.get(0).getSteepness());
        assertEquals("VERY_STEEP", observations.get(2).getSteepness());
        assertEquals("SWELL", observations.get(5).getSteepness());
    }

    @Test
    public void testParseAllMissingValues() throws IOException {
        List<BuoySpecWaveData> observations = parse(TEST_DATA_WITH_MISSING);
        BuoySpecWaveData allMissing = observations.get(1);

        // Date/time should still be parsed
        assertEquals(2026, allMissing.getYear());
        assertEquals(1, allMissing.getMonth());
        assertEquals(1, allMissing.getDay());
        assertEquals(3, allMissing.getHour());
        assertEquals(40, allMissing.getMinute());

        // All wave data should be null
        assertNull(allMissing.getWaveHeight());
        assertNull(allMissing.getSwellHeight());
        assertNull(allMissing.getSwellPeriod());
        assertNull(allMissing.getWindWaveHeight());
        assertNull(allMissing.getWindWavePeriod());
        assertNull(allMissing.getSwellDirection());
        assertNull(allMissing.getWindWaveDirection());
        assertNull(allMissing.getSteepness());
        assertNull(allMissing.getAverageWavePeriod());
        assertNull(allMissing.getMeanWaveDirection());
    }

    @Test
    public void testParsePartialMissingValues() throws IOException {
        List<BuoySpecWaveData> observations = parse(TEST_DATA_WITH_MISSING);
        BuoySpecWaveData partialMissing = observations.get(2);

        assertEquals(1.4, partialMissing.getWaveHeight(), 0.001);
        assertNull(partialMissing.getSwellHeight());
        assertEquals(12.1, partialMissing.getSwellPeriod(), 0.001);
        assertNull(partialMissing.getWindWaveHeight());
        assertEquals(4.3, partialMissing.getWindWavePeriod(), 0.001);
        assertNull(partialMissing.getSwellDirection());
        assertEquals("E", partialMissing.getWindWaveDirection());
        assertEquals("STEEP", partialMissing.getSteepness());
        assertEquals(4.5, partialMissing.getAverageWavePeriod(), 0.001);
        assertEquals(Integer.valueOf(90), partialMissing.getMeanWaveDirection());
    }

    @Test
    public void testParsePreviousYear() throws IOException {
        List<BuoySpecWaveData> observations = parse(TEST_DATA);
        BuoySpecWaveData last = observations.get(5);

        assertEquals(2025, last.getYear());
        assertEquals(12, last.getMonth());
        assertEquals(31, last.getDay());
        assertEquals(23, last.getHour());
        assertEquals(40, last.getMinute());
    }

    @Test
    public void testParseEmptyInput() throws IOException {
        List<BuoySpecWaveData> observations = parse("");
        assertEquals(0, observations.size());
    }

    @Test
    public void testParseHeadersOnly() throws IOException {
        String headersOnly = """
                #YY  MM DD hh mm WVHT  SwH  SwP  WWH  WWP SwD WWD  STEEPNESS  APD MWD
                #yr  mo dy hr mn    m    m  sec    m  sec  -  degT     -      sec degT
                """;
        List<BuoySpecWaveData> observations = parse(headersOnly);
        assertEquals(0, observations.size());
    }

    @Test
    public void testParseMalformedInputThrows() {
        String malformed = "2026 01 01 04 10  1.3  0.9 11.4";
        IOException ex = assertThrows(IOException.class, () -> parse(malformed));
        assertTrue(ex.getMessage().contains("Expected 15 columns"));
    }

    @Test
    public void testToString() throws IOException {
        List<BuoySpecWaveData> observations = parse(TEST_DATA);
        BuoySpecWaveData first = observations.get(0);
        String str = first.toString();

        assertTrue(str.contains("BuoySpecWaveData"));
        assertTrue(str.contains("waveHeight=1.3"));
        assertTrue(str.contains("swellDirection='WNW'"));
        assertTrue(str.contains("steepness='AVERAGE'"));
    }
}

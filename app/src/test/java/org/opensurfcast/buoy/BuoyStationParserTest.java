package org.opensurfcast.buoy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class BuoyStationParserTest {

    private static final String SINGLE_STATION = """
            <?xml version="1.0" encoding="UTF-8"?>
            <stations created="2026-01-01T12:00:00UTC" count="1">
              <station id="46013" lat="38.253" lon="-123.303" elev="0"
                       name="Bodega Bay" owner="NDBC" pgm="NDBC Meteorological/Ocean"
                       type="buoy" met="y" currents="n" waterquality="n" dart="n"/>
            </stations>
            """;

    private static final String MULTIPLE_STATIONS = """
            <?xml version="1.0" encoding="UTF-8"?>
            <stations created="2026-01-01T12:00:00UTC" count="3">
              <station id="46013" lat="38.253" lon="-123.303" elev="0"
                       name="Bodega Bay" owner="NDBC" pgm="NDBC Meteorological/Ocean"
                       type="buoy" met="y" currents="n" waterquality="n" dart="n"/>
              <station id="44060" lat="41.263" lon="-72.067" elev="5"
                       name="Eastern Long Island Sound" owner="MYSOUND" pgm="IOOS Partners"
                       type="fixed" met="y" currents="y" waterquality="y" dart="n"/>
              <station id="21418" lat="38.711" lon="148.839" elev="0"
                       name="" owner="NDBC" pgm="Tsunami"
                       type="dart" met="n" currents="n" waterquality="n" dart="y"/>
            </stations>
            """;

    private static InputStream toInputStream(String text) {
        return new ByteArrayInputStream(text.getBytes());
    }

    private static List<BuoyStation> parse(String text) throws IOException {
        return BuoyStationParser.parse(toInputStream(text));
    }

    @Test
    public void testParseSingleStation() throws IOException {
        List<BuoyStation> stations = parse(SINGLE_STATION);

        assertEquals(1, stations.size());
        BuoyStation station = stations.get(0);
        assertEquals("46013", station.getId());
        assertEquals(38.253, station.getLatitude(), 0.001);
        assertEquals(-123.303, station.getLongitude(), 0.001);
        assertEquals(0.0, station.getElevation(), 0.001);
        assertEquals("Bodega Bay", station.getName());
        assertEquals("NDBC", station.getOwner());
        assertEquals("NDBC Meteorological/Ocean", station.getProgram());
        assertEquals("buoy", station.getType());
    }

    @Test
    public void testParseMultipleStations() throws IOException {
        List<BuoyStation> stations = parse(MULTIPLE_STATIONS);

        assertEquals(3, stations.size());
        assertEquals("46013", stations.get(0).getId());
        assertEquals("44060", stations.get(1).getId());
        assertEquals("21418", stations.get(2).getId());
    }

    @Test
    public void testParseBooleanAttributes() throws IOException {
        List<BuoyStation> stations = parse(MULTIPLE_STATIONS);

        // First station: met=y, others=n
        BuoyStation buoy = stations.get(0);
        assertTrue(buoy.hasMet());
        assertFalse(buoy.hasCurrents());
        assertFalse(buoy.hasWaterQuality());
        assertFalse(buoy.hasDart());

        // Second station: met=y, currents=y, waterquality=y, dart=n
        BuoyStation fixed = stations.get(1);
        assertTrue(fixed.hasMet());
        assertTrue(fixed.hasCurrents());
        assertTrue(fixed.hasWaterQuality());
        assertFalse(fixed.hasDart());

        // Third station: dart=y, others=n
        BuoyStation dart = stations.get(2);
        assertFalse(dart.hasMet());
        assertTrue(dart.hasDart());
    }

    @Test
    public void testParseMissingElevation() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <stations count="1">
                  <station id="test1" lat="40.0" lon="-70.0"
                           name="Test" owner="Test" pgm="Test"
                           type="buoy" met="n" currents="n" waterquality="n" dart="n"/>
                </stations>
                """;
        List<BuoyStation> stations = parse(xml);

        assertNull(stations.get(0).getElevation());
    }

    @Test
    public void testParseMissingLatitudeThrows() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <stations count="1">
                  <station id="test1" lon="-70.0"
                           name="Test" owner="Test" pgm="Test"
                           type="buoy" met="n" currents="n" waterquality="n" dart="n"/>
                </stations>
                """;
        assertThrows(IllegalArgumentException.class, () -> parse(xml));
    }

    @Test
    public void testParseEmptyStations() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <stations created="2026-01-01T12:00:00UTC" count="0">
                </stations>
                """;
        List<BuoyStation> stations = parse(xml);

        assertEquals(0, stations.size());
    }

    @Test
    public void testParseMalformedXmlThrows() {
        String malformed = "not valid xml <<<<";
        assertThrows(IOException.class, () -> parse(malformed));
    }
}


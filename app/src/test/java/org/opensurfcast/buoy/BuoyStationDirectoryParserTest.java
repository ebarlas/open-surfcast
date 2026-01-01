package org.opensurfcast.buoy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

public class BuoyStationDirectoryParserTest {

    private static final String SAMPLE_HTML = """
            <html>
            <body>
            <table>
            <tr><td><a href="41002.txt">41002.txt</a></td><td>Standard Meteorological Data</td></tr>
            <tr><td><a href="41002.spec">41002.spec</a></td><td>Spectral Wave Summary Data</td></tr>
            <tr><td><a href="41004.txt">41004.txt</a></td><td>Standard Meteorological Data</td></tr>
            <tr><td><a href="41004.spec">41004.spec</a></td><td>Spectral Wave Summary Data</td></tr>
            <tr><td><a href="46013.txt">46013.txt</a></td><td>Standard Meteorological Data</td></tr>
            <tr><td><a href="21413.dart">21413.dart</a></td><td>Water Column Height (DART) Data</td></tr>
            </table>
            </body>
            </html>
            """;

    private static InputStream toInputStream(String text) {
        return new ByteArrayInputStream(text.getBytes());
    }

    @Test
    public void testParseStationsWithBothDataTypes() throws IOException {
        Set<String> stations = BuoyStationDirectoryParser
                .parseStationsWithStdMetAndSpecWave(toInputStream(SAMPLE_HTML));

        assertEquals(2, stations.size());
        assertTrue(stations.contains("41002"));
        assertTrue(stations.contains("41004"));
    }

    @Test
    public void testExcludesStationsWithOnlyStdMet() throws IOException {
        Set<String> stations = BuoyStationDirectoryParser
                .parseStationsWithStdMetAndSpecWave(toInputStream(SAMPLE_HTML));

        // 46013 has only .txt, not .spec
        assertTrue(!stations.contains("46013"));
    }

    @Test
    public void testExcludesOtherDataTypes() throws IOException {
        Set<String> stations = BuoyStationDirectoryParser
                .parseStationsWithStdMetAndSpecWave(toInputStream(SAMPLE_HTML));

        // 21413 has only .dart
        assertTrue(!stations.contains("21413"));
    }

    @Test
    public void testEmptyHtml() throws IOException {
        Set<String> stations = BuoyStationDirectoryParser
                .parseStationsWithStdMetAndSpecWave(toInputStream("<html></html>"));

        assertEquals(0, stations.size());
    }
}


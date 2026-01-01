package org.opensurfcast.buoy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the NDBC realtime2 directory listing to find stations with specific data types.
 * Data source: <a href="https://www.ndbc.noaa.gov/data/realtime2/">NDBC Realtime2 Directory</a>
 */
class BuoyStationDirectoryParser {

    private static final Pattern HREF_PATTERN = Pattern.compile("href=\"([^\"]+)\"");
    private static final String EXT_STD_MET = ".txt";
    private static final String EXT_SPEC_WAVE = ".spec";

    /**
     * Parses the directory listing HTML and returns station IDs that have both
     * Standard Meteorological Data (.txt) and Spectral Wave Summary Data (.spec).
     *
     * @param inputStream the input stream containing the HTML directory listing
     * @return set of station IDs with both data types available
     * @throws IOException if an error occurs during parsing
     */
    static Set<String> parseStationsWithStdMetAndSpecWave(InputStream inputStream)
            throws IOException {
        Set<String> stdMetStations = new HashSet<>();
        Set<String> specWaveStations = new HashSet<>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;

        while ((line = reader.readLine()) != null) {
            Matcher matcher = HREF_PATTERN.matcher(line);
            while (matcher.find()) {
                String href = matcher.group(1);
                if (href.endsWith(EXT_STD_MET)) {
                    stdMetStations.add(extractStationId(href, EXT_STD_MET));
                } else if (href.endsWith(EXT_SPEC_WAVE)) {
                    specWaveStations.add(extractStationId(href, EXT_SPEC_WAVE));
                }
            }
        }

        stdMetStations.retainAll(specWaveStations);
        return stdMetStations;
    }

    private static String extractStationId(String filename, String extension) {
        return filename.substring(0, filename.length() - extension.length());
    }
}

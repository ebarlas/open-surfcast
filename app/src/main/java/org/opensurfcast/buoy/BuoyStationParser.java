package org.opensurfcast.buoy;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Parser for NDBC active stations XML feed.
 * Data source: <a href="https://www.ndbc.noaa.gov/activestations.xml">Active Stations XML</a>
 * Format specification: <a href="https://www.ndbc.noaa.gov/faq/activestations.shtml">Active Stations FAQ</a>
 * <p>
 * Example XML structure:
 * <pre>
 * &lt;stations created="2026-01-01T21:15:01UTC" count="1365"&gt;
 *   &lt;station id="44060" lat="41.263" lon="-72.067" elev="0"
 *            name="Eastern Long Island Sound" owner="MYSOUND" pgm="IOOS Partners"
 *            type="fixed" met="y" currents="n" waterquality="n" dart="n"/&gt;
 *   ...
 * &lt;/stations&gt;
 * </pre>
 */
public class BuoyStationParser {

    private static final String TAG_STATION = "station";
    private static final String ATTR_ID = "id";
    private static final String ATTR_LAT = "lat";
    private static final String ATTR_LON = "lon";
    private static final String ATTR_ELEV = "elev";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_OWNER = "owner";
    private static final String ATTR_PGM = "pgm";
    private static final String ATTR_TYPE = "type";
    private static final String ATTR_MET = "met";
    private static final String ATTR_CURRENTS = "currents";
    private static final String ATTR_WATERQUALITY = "waterquality";
    private static final String ATTR_DART = "dart";

    /**
     * Parses the NDBC active stations XML feed from the given input stream.
     *
     * @param inputStream the input stream containing the XML data
     * @return list of parsed BuoyStation objects
     * @throws IOException if an error occurs during parsing
     */
    public static List<BuoyStation> parse(InputStream inputStream) throws IOException {
        List<BuoyStation> stations = new ArrayList<>();

        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();

            parser.parse(inputStream, new DefaultHandler() {
                @Override
                public void startElement(String uri, String localName, String qName,
                                          Attributes attributes) {
                    if (TAG_STATION.equals(qName)) {
                        BuoyStation station = parseStation(attributes);
                        stations.add(station);
                    }
                }
            });
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("Failed to parse stations XML", e);
        }

        return stations;
    }

    private static BuoyStation parseStation(Attributes attributes) {
        BuoyStation station = new BuoyStation();

        station.setId(attributes.getValue(ATTR_ID));
        station.setLatitude(parseRequiredDouble(attributes.getValue(ATTR_LAT), ATTR_LAT));
        station.setLongitude(parseRequiredDouble(attributes.getValue(ATTR_LON), ATTR_LON));
        station.setElevation(parseOptionalDouble(attributes.getValue(ATTR_ELEV)));
        station.setName(attributes.getValue(ATTR_NAME));
        station.setOwner(attributes.getValue(ATTR_OWNER));
        station.setProgram(attributes.getValue(ATTR_PGM));
        station.setType(attributes.getValue(ATTR_TYPE));
        station.setHasMet(parseYesNo(attributes.getValue(ATTR_MET)));
        station.setHasCurrents(parseYesNo(attributes.getValue(ATTR_CURRENTS)));
        station.setHasWaterQuality(parseYesNo(attributes.getValue(ATTR_WATERQUALITY)));
        station.setHasDart(parseYesNo(attributes.getValue(ATTR_DART)));

        return station;
    }

    private static double parseRequiredDouble(String value, String attrName) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Missing required attribute: " + attrName);
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid value for " + attrName + ": " + value);
        }
    }

    private static Double parseOptionalDouble(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean parseYesNo(String value) {
        return "y".equalsIgnoreCase(value);
    }
}

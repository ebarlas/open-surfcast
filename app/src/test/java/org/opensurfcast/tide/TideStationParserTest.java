package org.opensurfcast.tide;

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

public class TideStationParserTest {

    private static final String SINGLE_STATION = """
            {
              "count": 1,
              "units": null,
              "stations": [
                {
                  "id": "9415252",
                  "name": "Petaluma River entrance",
                  "state": "CA",
                  "lat": 38.115307,
                  "lng": -122.50567,
                  "type": "S",
                  "reference_id": "9414290",
                  "timemeridian": -120,
                  "timezonecorr": -8,
                  "tideType": "Mixed",
                  "affiliations": "",
                  "portscode": null
                }
              ]
            }
            """;

    private static final String MULTIPLE_STATIONS = """
            {
              "count": 3,
              "units": null,
              "stations": [
                {
                  "id": "9414290",
                  "name": "San Francisco",
                  "state": "CA",
                  "lat": 37.8063,
                  "lng": -122.4659,
                  "type": "R",
                  "reference_id": "",
                  "timemeridian": null,
                  "timezonecorr": -8,
                  "tideType": "Mixed",
                  "affiliations": "PORTS",
                  "portscode": "sf"
                },
                {
                  "id": "9415252",
                  "name": "Petaluma River entrance",
                  "state": "CA",
                  "lat": 38.115307,
                  "lng": -122.50567,
                  "type": "S",
                  "reference_id": "9414290",
                  "timemeridian": -120,
                  "timezonecorr": -8,
                  "tideType": "Mixed",
                  "affiliations": "",
                  "portscode": null
                },
                {
                  "id": "1611400",
                  "name": "NAWILIWILI",
                  "state": "HI",
                  "lat": 21.9544,
                  "lng": -159.3561,
                  "type": "R",
                  "reference_id": "",
                  "timemeridian": null,
                  "timezonecorr": -10,
                  "tideType": "Semidiurnal",
                  "affiliations": "",
                  "portscode": ""
                }
              ]
            }
            """;

    private static final String STATION_WITH_EXTENDED_FIELDS = """
            {
              "count": 1,
              "units": null,
              "stations": [
                {
                  "id": "9414290",
                  "name": "San Francisco",
                  "state": "CA",
                  "lat": 37.8063,
                  "lng": -122.4659,
                  "type": "R",
                  "reference_id": "",
                  "timemeridian": null,
                  "timezonecorr": -8,
                  "tideType": "Mixed",
                  "affiliations": "PORTS",
                  "portscode": "sf",
                  "timezone": "PST",
                  "observedst": true,
                  "tidal": true,
                  "greatlakes": false
                }
              ]
            }
            """;

    private static InputStream toInputStream(String text) {
        return new ByteArrayInputStream(text.getBytes());
    }

    private static List<TideStation> parse(String text) throws IOException {
        return TideStationParser.parseStationList(toInputStream(text));
    }

    @Test
    public void testParseSingleStation() throws IOException {
        List<TideStation> stations = parse(SINGLE_STATION);

        assertEquals(1, stations.size());
        TideStation station = stations.get(0);
        assertEquals("9415252", station.id);
        assertEquals("Petaluma River entrance", station.name);
        assertEquals("CA", station.state);
        assertEquals(38.115307, station.latitude, 0.000001);
        assertEquals(-122.50567, station.longitude, 0.000001);
        assertEquals("S", station.type);
        assertEquals("9414290", station.referenceId);
        assertEquals(Integer.valueOf(-120), station.timeMeridian);
        assertEquals(-8, station.timeZoneCorrection);
        assertEquals("Mixed", station.tideType);
        assertEquals("", station.affiliations);
        assertNull(station.portsCode);
    }

    @Test
    public void testParseMultipleStations() throws IOException {
        List<TideStation> stations = parse(MULTIPLE_STATIONS);

        assertEquals(3, stations.size());
        assertEquals("9414290", stations.get(0).id);
        assertEquals("9415252", stations.get(1).id);
        assertEquals("1611400", stations.get(2).id);
    }

    @Test
    public void testParseReferenceStation() throws IOException {
        List<TideStation> stations = parse(MULTIPLE_STATIONS);

        TideStation sanFrancisco = stations.get(0);
        assertEquals("R", sanFrancisco.type);
        assertTrue(sanFrancisco.isReferenceStation());
        assertFalse(sanFrancisco.isSubordinateStation());
        assertEquals("", sanFrancisco.referenceId);
        assertNull(sanFrancisco.timeMeridian);
    }

    @Test
    public void testParseSubordinateStation() throws IOException {
        List<TideStation> stations = parse(MULTIPLE_STATIONS);

        TideStation petaluma = stations.get(1);
        assertEquals("S", petaluma.type);
        assertTrue(petaluma.isSubordinateStation());
        assertFalse(petaluma.isReferenceStation());
        assertEquals("9414290", petaluma.referenceId);
        assertEquals(Integer.valueOf(-120), petaluma.timeMeridian);
    }

    @Test
    public void testParseTimezoneCorrection() throws IOException {
        List<TideStation> stations = parse(MULTIPLE_STATIONS);

        assertEquals(-8, stations.get(0).timeZoneCorrection);  // San Francisco (PST)
        assertEquals(-8, stations.get(1).timeZoneCorrection);  // Petaluma (PST)
        assertEquals(-10, stations.get(2).timeZoneCorrection); // Nawiliwili (HST)
    }

    @Test
    public void testParseTideType() throws IOException {
        List<TideStation> stations = parse(MULTIPLE_STATIONS);

        assertEquals("Mixed", stations.get(0).tideType);
        assertEquals("Mixed", stations.get(1).tideType);
        assertEquals("Semidiurnal", stations.get(2).tideType);
    }

    @Test
    public void testParseExtendedFields() throws IOException {
        List<TideStation> stations = parse(STATION_WITH_EXTENDED_FIELDS);

        TideStation station = stations.get(0);
        assertEquals("PST", station.timezone);
        assertEquals(Boolean.TRUE, station.observesDst);
        assertEquals(Boolean.TRUE, station.tidal);
        assertEquals(Boolean.FALSE, station.greatLakes);
    }

    @Test
    public void testParseEmptyStations() throws IOException {
        String json = """
                {
                  "count": 0,
                  "units": null,
                  "stations": []
                }
                """;
        List<TideStation> stations = parse(json);

        assertEquals(0, stations.size());
    }

    @Test
    public void testParseMissingOptionalFields() throws IOException {
        String json = """
                {
                  "count": 1,
                  "stations": [
                    {
                      "id": "TEST001",
                      "lat": 40.0,
                      "lng": -70.0
                    }
                  ]
                }
                """;
        List<TideStation> stations = parse(json);

        TideStation station = stations.get(0);
        assertEquals("TEST001", station.id);
        assertEquals(40.0, station.latitude, 0.001);
        assertEquals(-70.0, station.longitude, 0.001);
        assertEquals("", station.name);
        assertEquals("", station.state);
        assertEquals("", station.type);
        assertNull(station.timeMeridian);
        assertNull(station.timezone);
        assertNull(station.observesDst);
        assertNull(station.tidal);
        assertNull(station.greatLakes);
    }

    @Test
    public void testParseMalformedJsonThrows() {
        String malformed = "not valid json {{{";
        assertThrows(IOException.class, () -> parse(malformed));
    }

    @Test
    public void testParseMissingStationsArrayThrows() {
        String json = """
                {
                  "count": 0
                }
                """;
        assertThrows(IOException.class, () -> parse(json));
    }

    @Test
    public void testParseMissingRequiredIdThrows() {
        String json = """
                {
                  "stations": [
                    {
                      "lat": 40.0,
                      "lng": -70.0
                    }
                  ]
                }
                """;
        assertThrows(IOException.class, () -> parse(json));
    }
}


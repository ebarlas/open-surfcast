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

public class CurrentStationParserTest {

    private static final String SINGLE_STATION = """
            {
              "count": 1,
              "units": "metric",
              "stations": [
                {
                  "currentpredictionoffsets": {
                    "self": "https://api.tidesandcurrents.noaa.gov/mdapi/prod/webapi/stations/ACT0091_1/currentpredictionoffsets.json"
                  },
                  "currbin": 1,
                  "type": "S",
                  "depth": null,
                  "depthType": "U",
                  "timezone_offset": "",
                  "harmonicConstituents": {
                    "self": "https://api.tidesandcurrents.noaa.gov/mdapi/prod/webapi/stations/ACT0091/harcon.json"
                  },
                  "id": "ACT0091",
                  "name": "Eastport, Friar Roads",
                  "lat": 44.9,
                  "lng": -66.98333,
                  "affiliations": "",
                  "portscode": "",
                  "products": null,
                  "disclaimers": null,
                  "notices": null,
                  "self": "https://api.tidesandcurrents.noaa.gov/mdapi/prod/webapi/stations/ACT0091.json",
                  "expand": "currentpredictionoffsets,harcon",
                  "tideType": ""
                }
              ]
            }
            """;

    private static final String MULTIPLE_STATIONS = """
            {
              "count": 3,
              "units": "metric",
              "stations": [
                {
                  "currentpredictionoffsets": {
                    "self": "https://api.tidesandcurrents.noaa.gov/mdapi/prod/webapi/stations/ACT0091_1/currentpredictionoffsets.json"
                  },
                  "currbin": 1,
                  "type": "S",
                  "depth": null,
                  "depthType": "U",
                  "timezone_offset": "",
                  "harmonicConstituents": {
                    "self": "https://api.tidesandcurrents.noaa.gov/mdapi/prod/webapi/stations/ACT0091/harcon.json"
                  },
                  "id": "ACT0091",
                  "name": "Eastport, Friar Roads",
                  "lat": 44.9,
                  "lng": -66.98333,
                  "affiliations": "",
                  "portscode": "",
                  "self": "https://api.tidesandcurrents.noaa.gov/mdapi/prod/webapi/stations/ACT0091.json",
                  "expand": "currentpredictionoffsets,harcon",
                  "tideType": ""
                },
                {
                  "currentpredictionoffsets": null,
                  "currbin": 11,
                  "type": "R",
                  "depth": 5.5,
                  "depthType": "S",
                  "timezone_offset": "-5",
                  "harmonicConstituents": {
                    "self": "https://api.tidesandcurrents.noaa.gov/mdapi/prod/webapi/stations/EPT0003/harcon.json"
                  },
                  "id": "EPT0003",
                  "name": "The Race",
                  "lat": 41.2267,
                  "lng": -72.0633,
                  "affiliations": "PORTS",
                  "portscode": "nb",
                  "self": "https://api.tidesandcurrents.noaa.gov/mdapi/prod/webapi/stations/EPT0003.json",
                  "expand": "harcon",
                  "tideType": "Semidiurnal"
                },
                {
                  "currbin": 14,
                  "type": "S",
                  "depth": 10.2,
                  "depthType": "B",
                  "timezone_offset": "",
                  "id": "PCT1291",
                  "name": "Golden Gate Bridge",
                  "lat": 37.8117,
                  "lng": -122.4783,
                  "affiliations": "",
                  "portscode": "sf",
                  "tideType": "Mixed"
                }
              ]
            }
            """;

    private static InputStream toInputStream(String text) {
        return new ByteArrayInputStream(text.getBytes());
    }

    private static List<CurrentStation> parse(String text) throws IOException {
        return CurrentStationParser.parseStationList(toInputStream(text));
    }

    @Test
    public void testParseSingleStation() throws IOException {
        List<CurrentStation> stations = parse(SINGLE_STATION);

        assertEquals(1, stations.size());
        CurrentStation station = stations.get(0);
        assertEquals("ACT0091", station.id);
        assertEquals("Eastport, Friar Roads", station.name);
        assertEquals(44.9, station.latitude, 0.0001);
        assertEquals(-66.98333, station.longitude, 0.00001);
        assertEquals("S", station.type);
        assertEquals(Integer.valueOf(1), station.currentBin);
        assertNull(station.depth);
        assertEquals("U", station.depthType);
        assertEquals("", station.timezoneOffset);
        assertEquals("", station.affiliations);
        assertEquals("", station.portsCode);
        assertEquals("", station.tideType);
        assertEquals("https://api.tidesandcurrents.noaa.gov/mdapi/prod/webapi/stations/ACT0091.json", station.selfUrl);
        assertEquals("currentpredictionoffsets,harcon", station.expand);
        assertEquals("https://api.tidesandcurrents.noaa.gov/mdapi/prod/webapi/stations/ACT0091_1/currentpredictionoffsets.json",
                station.currentPredictionOffsetsUrl);
        assertEquals("https://api.tidesandcurrents.noaa.gov/mdapi/prod/webapi/stations/ACT0091/harcon.json",
                station.harmonicConstituentsUrl);
    }

    @Test
    public void testParseMultipleStations() throws IOException {
        List<CurrentStation> stations = parse(MULTIPLE_STATIONS);

        assertEquals(3, stations.size());
        assertEquals("ACT0091", stations.get(0).id);
        assertEquals("EPT0003", stations.get(1).id);
        assertEquals("PCT1291", stations.get(2).id);
    }

    @Test
    public void testParseReferenceStation() throws IOException {
        List<CurrentStation> stations = parse(MULTIPLE_STATIONS);

        CurrentStation reference = stations.get(1);
        assertEquals("R", reference.type);
        assertTrue(reference.isReferenceStation());
        assertFalse(reference.isSubordinateStation());
    }

    @Test
    public void testParseSubordinateStation() throws IOException {
        List<CurrentStation> stations = parse(MULTIPLE_STATIONS);

        CurrentStation subordinate = stations.get(0);
        assertEquals("S", subordinate.type);
        assertTrue(subordinate.isSubordinateStation());
        assertFalse(subordinate.isReferenceStation());
    }

    @Test
    public void testParseDepthFromSurface() throws IOException {
        List<CurrentStation> stations = parse(MULTIPLE_STATIONS);

        CurrentStation station = stations.get(1);
        assertEquals("S", station.depthType);
        assertTrue(station.isDepthFromSurface());
        assertFalse(station.isDepthFromBottom());
        assertEquals(Double.valueOf(5.5), station.depth);
    }

    @Test
    public void testParseDepthFromBottom() throws IOException {
        List<CurrentStation> stations = parse(MULTIPLE_STATIONS);

        CurrentStation station = stations.get(2);
        assertEquals("B", station.depthType);
        assertTrue(station.isDepthFromBottom());
        assertFalse(station.isDepthFromSurface());
        assertEquals(Double.valueOf(10.2), station.depth);
    }

    @Test
    public void testParseCurrentBin() throws IOException {
        List<CurrentStation> stations = parse(MULTIPLE_STATIONS);

        assertEquals(Integer.valueOf(1), stations.get(0).currentBin);
        assertEquals(Integer.valueOf(11), stations.get(1).currentBin);
        assertEquals(Integer.valueOf(14), stations.get(2).currentBin);
    }

    @Test
    public void testParseTideType() throws IOException {
        List<CurrentStation> stations = parse(MULTIPLE_STATIONS);

        assertEquals("", stations.get(0).tideType);
        assertEquals("Semidiurnal", stations.get(1).tideType);
        assertEquals("Mixed", stations.get(2).tideType);
    }

    @Test
    public void testParseAffiliationsAndPortsCode() throws IOException {
        List<CurrentStation> stations = parse(MULTIPLE_STATIONS);

        assertEquals("", stations.get(0).affiliations);
        assertEquals("", stations.get(0).portsCode);

        assertEquals("PORTS", stations.get(1).affiliations);
        assertEquals("nb", stations.get(1).portsCode);

        assertEquals("", stations.get(2).affiliations);
        assertEquals("sf", stations.get(2).portsCode);
    }

    @Test
    public void testParseNullCurrentPredictionOffsets() throws IOException {
        List<CurrentStation> stations = parse(MULTIPLE_STATIONS);

        CurrentStation station = stations.get(1);
        assertNull(station.currentPredictionOffsetsUrl);
    }

    @Test
    public void testParseEmptyStations() throws IOException {
        String json = """
                {
                  "count": 0,
                  "units": "metric",
                  "stations": []
                }
                """;
        List<CurrentStation> stations = parse(json);

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
        List<CurrentStation> stations = parse(json);

        CurrentStation station = stations.get(0);
        assertEquals("TEST001", station.id);
        assertEquals(40.0, station.latitude, 0.001);
        assertEquals(-70.0, station.longitude, 0.001);
        assertEquals("", station.name);
        assertEquals("", station.type);
        assertNull(station.currentBin);
        assertNull(station.depth);
        assertEquals("", station.depthType);
        assertNull(station.selfUrl);
        assertNull(station.expand);
        assertNull(station.currentPredictionOffsetsUrl);
        assertNull(station.harmonicConstituentsUrl);
    }

    @Test
    public void testParseFromString() throws IOException {
        List<CurrentStation> stations = CurrentStationParser.parseStationList(SINGLE_STATION);

        assertEquals(1, stations.size());
        assertEquals("ACT0091", stations.get(0).id);
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

    @Test
    public void testParseMissingRequiredLatThrows() {
        String json = """
                {
                  "stations": [
                    {
                      "id": "TEST001",
                      "lng": -70.0
                    }
                  ]
                }
                """;
        assertThrows(IOException.class, () -> parse(json));
    }
}


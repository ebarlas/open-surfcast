package org.opensurfcast.buoy;

import org.opensurfcast.http.HttpClient;
import org.opensurfcast.http.Modified;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class NdbcNoaaGovService {

    private static final String DATA_URL = "https://www.ndbc.noaa.gov/data/realtime2/%s.%s";
    private static final String ACTIVE_STATIONS_URL = "https://www.ndbc.noaa.gov/activestations.xml";
    private static final String DIRECTORY_URL = "https://www.ndbc.noaa.gov/data/realtime2/";

    public static Modified<List<BuoyStdMetData>> fetchBuoyStdMetData(
            String stationId,
            String lastModified) throws IOException {
        String url = String.format(DATA_URL, stationId, "txt");
        return HttpClient.send(url, lastModified, BuoyStdMetDataParser::parse);
    }

    public static Modified<List<BuoySpecWaveData>> fetchBuoySpecWaveData(
            String stationId,
            String lastModified) throws IOException {
        String url = String.format(DATA_URL, stationId, "spec");
        return HttpClient.send(url, lastModified, BuoySpecWaveDataParser::parse);
    }

    public static Modified<List<BuoyStation>> fetchBuoyStations(
            String lastModified) throws IOException {
        return HttpClient.send(ACTIVE_STATIONS_URL, lastModified, BuoyStationParser::parse);
    }

    public static Set<String> fetchStationIdsWithStdMetAndSpecWave() throws IOException {
        return HttpClient.send(DIRECTORY_URL, BuoyStationDirectoryParser::parseStationsWithStdMetAndSpecWave);
    }

}

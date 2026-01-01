package org.opensurfcast.buoy;

import org.opensurfcast.http.HttpClient;
import org.opensurfcast.http.HttpRequest;
import org.opensurfcast.http.HttpResponse;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class NdbcNoaaGovService {

    private static final String DATA_URL = "https://www.ndbc.noaa.gov/data/realtime2/%s.%s";
    private static final String ACTIVE_STATIONS_URL = "https://www.ndbc.noaa.gov/activestations.xml";
    private static final String DIRECTORY_URL = "https://www.ndbc.noaa.gov/data/realtime2/";

    public static HttpResponse<List<BuoyStdMetData>> fetchBuoyStdMetData(
            String stationId,
            String lastModified,
            String eTag) throws IOException {
        String url = String.format(DATA_URL, stationId, "txt");
        return HttpClient.send(new HttpRequest(url, lastModified, eTag), BuoyStdMetDataParser::parse);
    }

    public static HttpResponse<List<BuoySpecWaveData>> fetchBuoySpecWaveData(
            String stationId,
            String lastModified,
            String eTag) throws IOException {
        String url = String.format(DATA_URL, stationId, "spec");
        return HttpClient.send(new HttpRequest(url, lastModified, eTag), BuoySpecWaveDataParser::parse);
    }

    public static HttpResponse<List<BuoyStation>> fetchBuoyStations(
            String lastModified,
            String eTag) throws IOException {
        return HttpClient.send(
                new HttpRequest(ACTIVE_STATIONS_URL, lastModified, eTag),
                BuoyStationParser::parse);
    }

    public static Set<String> fetchStationIdsWithStdMetAndSpecWave() throws IOException {
        return HttpClient.send(
                new HttpRequest(DIRECTORY_URL, null, null),
                BuoyStationDirectoryParser::parseStationsWithStdMetAndSpecWave).body();
    }

}

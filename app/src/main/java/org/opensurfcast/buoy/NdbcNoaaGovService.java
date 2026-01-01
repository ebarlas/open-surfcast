package org.opensurfcast.buoy;

import org.opensurfcast.http.HttpClient;
import org.opensurfcast.http.HttpRequest;
import org.opensurfcast.http.HttpResponse;

import java.io.IOException;
import java.util.List;

public class NdbcNoaaGovService {

    private static final String BASE_URL = "https://www.ndbc.noaa.gov/data/realtime2/%s.%s";

    public static HttpResponse<List<BuoyStdMetData>> fetchBuoyStdMetData(
            String stationId,
            String lastModified,
            String eTag) throws IOException {
        String url = String.format(BASE_URL, stationId, "txt");
        return HttpClient.send(new HttpRequest(url, lastModified, eTag), BuoyStdMetDataParser::parse);
    }

    public static HttpResponse<List<BuoySpecWaveData>> fetchBuoySpecWaveData(
            String stationId,
            String lastModified,
            String eTag) throws IOException {
        String url = String.format(BASE_URL, stationId, "spec");
        return HttpClient.send(new HttpRequest(url, lastModified, eTag), BuoySpecWaveDataParser::parse);
    }

}

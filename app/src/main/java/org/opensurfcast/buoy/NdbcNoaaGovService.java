package org.opensurfcast.buoy;

import org.opensurfcast.http.HttpClient;
import org.opensurfcast.http.HttpRequest;
import org.opensurfcast.http.HttpResponse;

import java.io.IOException;
import java.util.List;

public class NdbcNoaaGovService {

    private static final String URL_FMT = "https://www.ndbc.noaa.gov/data/realtime2/%s.txt";

    public static HttpResponse<List<BuoyObs>> buoyObservations(
            String stationId,
            String lastModified,
            String eTag) throws IOException {
        String url = String.format(URL_FMT, stationId);
        return HttpClient.send(new HttpRequest(url, lastModified, eTag), BuoyObsParser::parse);
    }

}

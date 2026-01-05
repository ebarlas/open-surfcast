package org.opensurfcast.tide;

import org.opensurfcast.http.HttpClient;

import java.io.IOException;
import java.util.List;

/**
 * Service for fetching tide data from NOAA CO-OPS (Center for Operational
 * Oceanographic Products and Services) web services.
 * <p>
 * API Documentation:
 * <ul>
 *   <li><a href="https://api.tidesandcurrents.noaa.gov/mdapi/prod/">Metadata API (MDAPI)</a></li>
 *   <li><a href="https://api.tidesandcurrents.noaa.gov/api/prod/">Data API</a></li>
 * </ul>
 */
public class CoOpsNoaaGovService {

    private static final String MDAPI_BASE_URL = "https://api.tidesandcurrents.noaa.gov/mdapi/prod/webapi";
    private static final String DATA_API_BASE_URL = "https://api.tidesandcurrents.noaa.gov/api/prod/datagetter";

    private static final String STATIONS_URL = MDAPI_BASE_URL + "/stations.json?type=tidepredictions";

    /**
     * URL template for fetching high/low tide predictions.
     * Uses metric units (meters) and GMT timezone.
     * Parameters: station ID, begin date (yyyyMMdd), end date (yyyyMMdd)
     */
    private static final String PREDICTIONS_URL = DATA_API_BASE_URL +
            "?station=%s" +
            "&product=predictions" +
            "&datum=MLLW" +
            "&interval=hilo" +
            "&units=metric" +
            "&time_zone=gmt" +
            "&format=json" +
            "&begin_date=%s" +
            "&end_date=%s" +
            "&application=OpenSurfcast";

    /**
     * Fetches all tide prediction stations from the CO-OPS Metadata API.
     *
     * @return list of all tide prediction stations
     * @throws IOException if a network or parsing error occurs
     */
    public static List<TideStation> fetchTideStations() throws IOException {
        return HttpClient.send(STATIONS_URL, TideStationParser::parseStationList);
    }

    /**
     * Fetches high/low tide predictions for a station within a date range.
     * <p>
     * Predictions are returned in metric units (meters) relative to MLLW datum,
     * with timestamps in GMT/UTC.
     *
     * @param stationId the station ID (e.g., "9414290")
     * @param beginDate start date in yyyyMMdd format (e.g., "20260101")
     * @param endDate   end date in yyyyMMdd format (e.g., "20260107")
     * @return list of high/low tide predictions
     * @throws IOException if a network or parsing error occurs
     */
    public static List<TidePrediction> fetchTidePredictions(
            String stationId,
            String beginDate,
            String endDate) throws IOException {
        String url = String.format(PREDICTIONS_URL, stationId, beginDate, endDate);
        return HttpClient.send(url, TidePredictionParser::parse);
    }

}

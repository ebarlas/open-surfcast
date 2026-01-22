package org.opensurfcast.tide;

import org.opensurfcast.http.HttpClient;

import java.io.IOException;
import java.util.List;

/**
 * Service for fetching tide and current data from NOAA CO-OPS (Center for Operational
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

    private static final String TIDE_STATIONS_URL = MDAPI_BASE_URL + "/stations.json?type=tidepredictions";

    private static final String CURRENT_STATIONS_URL = MDAPI_BASE_URL + "/stations.json?type=currentpredictions&units=metric";

    /**
     * URL template for fetching high/low tide predictions.
     * Uses metric units (meters) and GMT timezone.
     * Parameters: station ID, begin date (yyyyMMdd), end date (yyyyMMdd)
     */
    private static final String TIDE_PREDICTIONS_URL = DATA_API_BASE_URL +
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
     * URL template for fetching current predictions (flood, ebb, slack).
     * Uses metric units (cm/s for velocity, meters for depth) and GMT timezone.
     * Uses MAX_SLACK interval for flood/ebb max and slack times.
     * Parameters: station ID, begin date (yyyyMMdd), end date (yyyyMMdd)
     */
    private static final String CURRENT_PREDICTIONS_URL = DATA_API_BASE_URL +
            "?station=%s" +
            "&product=currents_predictions" +
            "&interval=MAX_SLACK" +
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
        return HttpClient.send(TIDE_STATIONS_URL, TideStationParser::parseStationList);
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
        String url = String.format(TIDE_PREDICTIONS_URL, stationId, beginDate, endDate);
        return HttpClient.send(url, TidePredictionParser::parse);
    }

    /**
     * Fetches all current prediction stations from the CO-OPS Metadata API.
     *
     * @return list of all current prediction stations
     * @throws IOException if a network or parsing error occurs
     */
    public static List<CurrentStation> fetchCurrentStations() throws IOException {
        return HttpClient.send(CURRENT_STATIONS_URL, CurrentStationParser::parseStationList);
    }

    /**
     * Fetches current predictions (flood, ebb, slack) for a station within a date range.
     * <p>
     * Predictions are returned in metric units (cm/s for velocity, meters for depth),
     * with timestamps in GMT/UTC. Uses MAX_SLACK interval to get flood/ebb maximums
     * and slack water times.
     *
     * @param stationId the station ID (e.g., "ACT0091")
     * @param beginDate start date in yyyyMMdd format (e.g., "20260101")
     * @param endDate   end date in yyyyMMdd format (e.g., "20260107")
     * @return list of current predictions
     * @throws IOException if a network or parsing error occurs
     */
    public static List<CurrentPrediction> fetchCurrentPredictions(
            String stationId,
            String beginDate,
            String endDate) throws IOException {
        String url = String.format(CURRENT_PREDICTIONS_URL, stationId, beginDate, endDate);
        return HttpClient.send(url, CurrentPredictionParser::parse);
    }

}

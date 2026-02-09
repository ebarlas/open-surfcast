package org.opensurfcast.sync;

import org.opensurfcast.buoy.BuoyStation;
import org.opensurfcast.buoy.NdbcNoaaGovService;
import org.opensurfcast.db.BuoyStationDb;
import org.opensurfcast.http.HttpCache;
import org.opensurfcast.http.Modified;
import org.opensurfcast.log.Logger;
import org.opensurfcast.tasks.BaseTask;
import org.opensurfcast.timer.Timer;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * Task to fetch the buoy station catalog from NDBC.
 * <p>
 * Fetches all active buoy stations and updates the local database.
 * Uses {@link HttpCache} to send {@code If-Modified-Since} headers
 * and skip redundant downloads.
 */
public class FetchBuoyStationsTask extends BaseTask {
    private static final String KEY = "FETCH_BUOY_STATIONS";
    private static final Duration COOLDOWN_PERIOD = Duration.ofHours(1);

    private final BuoyStationDb stationDb;
    private final HttpCache httpCache;
    private final Logger logger;

    public FetchBuoyStationsTask(BuoyStationDb stationDb, HttpCache httpCache, Logger logger) {
        super(KEY, COOLDOWN_PERIOD);
        this.stationDb = stationDb;
        this.httpCache = httpCache;
        this.logger = logger;
    }

    @Override
    protected void execute() throws IOException {
        Timer timer = new Timer();
        String lastModified = httpCache.get(KEY);
        Modified<List<BuoyStation>> result = NdbcNoaaGovService.fetchBuoyStations(lastModified);
        long elapsed = timer.elapsed();
        if (result == null) {
            logger.info("Buoy stations not modified (" + elapsed + "ms)");
            return;
        }
        Set<String> ids = NdbcNoaaGovService.fetchStationIdsWithStdMetAndSpecWave();
        elapsed = timer.elapsed();
        if (result.value() != null) {
            List<BuoyStation> stations = result.value();
            List<BuoyStation> retained = stations.stream()
                    .filter(station -> ids.contains(station.getId()))
                    .toList();
            stationDb.replaceAll(retained);
            httpCache.put(KEY, result.lastModified());
            logger.info("Fetched " + stations.size() + " buoy stations and retained " + retained.size() + " (" + elapsed + "ms)");
        }
    }
}

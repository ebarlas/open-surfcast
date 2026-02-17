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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Task to fetch the buoy station catalog from NDBC.
 * <p>
 * Fetches all active buoy stations and updates the local database.
 * Uses {@link HttpCache} to send {@code If-Modified-Since} headers
 * and skip redundant downloads. Returns the set of station IDs written
 * to the DB, or null when not modified (304).
 */
public class FetchBuoyStationsTask extends BaseTask {
    private static final Duration COOLDOWN_PERIOD = Duration.ofDays(1);

    private final BuoyStationDb stationDb;
    private final HttpCache httpCache;
    private final Logger logger;

    public FetchBuoyStationsTask(BuoyStationDb stationDb, HttpCache httpCache, Logger logger) {
        super(COOLDOWN_PERIOD);
        this.stationDb = stationDb;
        this.httpCache = httpCache;
        this.logger = logger;
    }

    @Override
    public Object call() throws IOException {
        Timer timer = new Timer();
        String lastModified = httpCache.get(getKey());
        Modified<List<BuoyStation>> result = NdbcNoaaGovService.fetchBuoyStations(lastModified);
        long elapsed = timer.elapsed();
        if (result == null) {
            logger.info("Buoy stations not modified (" + elapsed + "ms)");
            return null;
        }
        Set<String> ids = NdbcNoaaGovService.fetchStationIdsWithStdMetAndSpecWave();
        elapsed = timer.elapsed();
        if (result.value() != null) {
            List<BuoyStation> stations = result.value();
            List<BuoyStation> retained = stations.stream()
                    .filter(station -> ids.contains(station.getId()))
                    .collect(Collectors.toList());
            logger.info("Fetched " + stations.size() + " buoy stations and retained " + retained.size() + " (" + elapsed + "ms)");
            Timer t = new Timer();
            stationDb.replaceAll(retained);
            logger.info("Replaced " + retained.size() + " buoy stations (" + t.elapsed() + " ms)");
            httpCache.put(getKey(), result.lastModified());
            return retained.stream().map(BuoyStation::getId).collect(Collectors.toCollection(HashSet::new));
        }
        return null;
    }
}

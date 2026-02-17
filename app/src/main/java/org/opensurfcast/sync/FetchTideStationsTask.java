package org.opensurfcast.sync;

import org.opensurfcast.db.TideStationDb;
import org.opensurfcast.log.Logger;
import org.opensurfcast.tasks.BaseTask;
import org.opensurfcast.tide.CoOpsNoaaGovService;
import org.opensurfcast.tide.TideStation;
import org.opensurfcast.timer.Timer;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Task to fetch the tide station catalog from NOAA CO-OPS.
 * <p>
 * Fetches all tide prediction stations and updates the local database.
 * Returns the set of station IDs written to the DB.
 */
public class FetchTideStationsTask extends BaseTask {
    private static final Duration COOLDOWN_PERIOD = Duration.ofDays(1);

    private final TideStationDb stationDb;
    private final Logger logger;

    public FetchTideStationsTask(TideStationDb stationDb, Logger logger) {
        super(COOLDOWN_PERIOD);
        this.stationDb = stationDb;
        this.logger = logger;
    }

    @Override
    public Object call() throws IOException {
        Timer timer = new Timer();
        List<TideStation> stations = CoOpsNoaaGovService.fetchTideStations();
        long elapsed = timer.elapsed();
        logger.info("Fetched " + stations.size() + " tide stations (" + elapsed + "ms)");
        Timer t = new Timer();
        stationDb.replaceAll(stations);
        logger.info("Replaced " + stations.size() + " tide stations (" + t.elapsed() + " ms)");
        return stations.stream().map(s -> s.id).collect(Collectors.toSet());
    }
}

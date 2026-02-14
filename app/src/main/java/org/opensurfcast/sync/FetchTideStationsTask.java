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

/**
 * Task to fetch the tide station catalog from NOAA CO-OPS.
 * <p>
 * Fetches all tide prediction stations and updates the local database.
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
    protected void execute() throws IOException {
        Timer timer = new Timer();
        List<TideStation> stations = CoOpsNoaaGovService.fetchTideStations();
        long elapsed = timer.elapsed();
        logger.info("Fetched " + stations.size() + " tide stations (" + elapsed + "ms)");
        Timer t = new Timer();
        stationDb.replaceAll(stations);
        logger.info("Replaced " + stations.size() + " tide stations (" + t.elapsed() + " ms)");
    }
}

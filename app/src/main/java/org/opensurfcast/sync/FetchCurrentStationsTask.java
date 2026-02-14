package org.opensurfcast.sync;

import org.opensurfcast.db.CurrentStationDb;
import org.opensurfcast.log.Logger;
import org.opensurfcast.tasks.BaseTask;
import org.opensurfcast.tide.CoOpsNoaaGovService;
import org.opensurfcast.tide.CurrentStation;
import org.opensurfcast.timer.Timer;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * Task to fetch the current station catalog from NOAA CO-OPS.
 * <p>
 * Fetches all current prediction stations and updates the local database.
 */
public class FetchCurrentStationsTask extends BaseTask {
    private static final Duration COOLDOWN_PERIOD = Duration.ofDays(1);

    private final CurrentStationDb stationDb;
    private final Logger logger;

    public FetchCurrentStationsTask(CurrentStationDb stationDb, Logger logger) {
        super(COOLDOWN_PERIOD);
        this.stationDb = stationDb;
        this.logger = logger;
    }

    @Override
    protected void execute() throws IOException {
        Timer timer = new Timer();
        List<CurrentStation> stations = CoOpsNoaaGovService.fetchCurrentStations();
        long elapsed = timer.elapsed();
        logger.info("Fetched " + stations.size() + " current stations (" + elapsed + "ms)");
        Timer t = new Timer();
        stationDb.replaceAll(stations);
        logger.info("Replaced " + stations.size() + " current stations (" + t.elapsed() + " ms)");
    }
}

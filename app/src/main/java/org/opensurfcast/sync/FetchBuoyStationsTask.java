package org.opensurfcast.sync;

import org.opensurfcast.buoy.BuoyStation;
import org.opensurfcast.buoy.NdbcNoaaGovService;
import org.opensurfcast.db.BuoyStationDb;
import org.opensurfcast.http.Modified;
import org.opensurfcast.log.Logger;
import org.opensurfcast.tasks.BaseTask;
import org.opensurfcast.timer.Timer;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * Task to fetch the buoy station catalog from NDBC.
 * <p>
 * Fetches all active buoy stations and updates the local database.
 */
public class FetchBuoyStationsTask extends BaseTask {
    private static final String KEY = "FETCH_BUOY_STATIONS";
    private static final Duration COOLDOWN_PERIOD = Duration.ofHours(1);

    private final BuoyStationDb stationDb;
    private final Logger logger;

    public FetchBuoyStationsTask(BuoyStationDb stationDb, Logger logger) {
        super(KEY, COOLDOWN_PERIOD);
        this.stationDb = stationDb;
        this.logger = logger;
    }

    @Override
    protected void execute() throws IOException {
        Timer timer = new Timer();
        Modified<List<BuoyStation>> result = NdbcNoaaGovService.fetchBuoyStations(null);
        long elapsed = timer.elapsed();
        if (result.value() != null) {
            List<BuoyStation> stations = result.value();
            stationDb.replaceAll(stations);
            logger.info("Fetched " + stations.size() + " buoy stations (" + elapsed + "ms)");
        } else {
            logger.info("Buoy stations not modified (" + elapsed + "ms)");
        }
    }
}

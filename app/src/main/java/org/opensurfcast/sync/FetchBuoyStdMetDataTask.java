package org.opensurfcast.sync;

import org.opensurfcast.buoy.BuoyStdMetData;
import org.opensurfcast.buoy.NdbcNoaaGovService;
import org.opensurfcast.db.BuoyStdMetDataDb;
import org.opensurfcast.http.Modified;
import org.opensurfcast.log.Logger;
import org.opensurfcast.tasks.BaseTask;
import org.opensurfcast.timer.Timer;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * Task to fetch standard meteorological data for a specific buoy station.
 * <p>
 * Fetches recent observations and updates the local database.
 */
public class FetchBuoyStdMetDataTask extends BaseTask {
    private static final String KEY_PREFIX = "FETCH_BUOY_STD_MET_DATA:";
    private static final Duration COOLDOWN_PERIOD = Duration.ofMinutes(5);

    private final BuoyStdMetDataDb dataDb;
    private final String stationId;
    private final Logger logger;

    public FetchBuoyStdMetDataTask(BuoyStdMetDataDb dataDb, String stationId, Logger logger) {
        super(KEY_PREFIX + stationId, COOLDOWN_PERIOD);
        this.dataDb = dataDb;
        this.stationId = stationId;
        this.logger = logger;
    }

    @Override
    protected void execute() throws IOException {
        Timer timer = new Timer();
        Modified<List<BuoyStdMetData>> result = NdbcNoaaGovService.fetchBuoyStdMetData(stationId, null);
        long elapsed = timer.elapsed();
        if (result.value() != null) {
            List<BuoyStdMetData> data = result.value();
            dataDb.replaceAllForStation(stationId, data);
            logger.info("Fetched " + data.size() + " std met observations for station " + stationId + " (" + elapsed + "ms)");
        } else {
            logger.info("Std met data not modified for station " + stationId + " (" + elapsed + "ms)");
        }
    }
}

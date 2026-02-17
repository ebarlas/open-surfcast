package org.opensurfcast.sync;

import org.opensurfcast.buoy.BuoyStdMetData;
import org.opensurfcast.buoy.NdbcNoaaGovService;
import org.opensurfcast.db.BuoyStdMetDataDb;
import org.opensurfcast.http.HttpCache;
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
 * Uses {@link HttpCache} to send {@code If-Modified-Since} headers
 * and skip redundant downloads.
 */
public class FetchBuoyStdMetDataTask extends BaseTask {
    private static final Duration COOLDOWN_PERIOD = Duration.ofMinutes(5);

    private final BuoyStdMetDataDb dataDb;
    private final String stationId;
    private final HttpCache httpCache;
    private final Logger logger;

    public FetchBuoyStdMetDataTask(BuoyStdMetDataDb dataDb, String stationId, HttpCache httpCache, Logger logger) {
        super(COOLDOWN_PERIOD);
        this.dataDb = dataDb;
        this.stationId = stationId;
        this.httpCache = httpCache;
        this.logger = logger;
    }

    public String stationId() {
        return stationId;
    }

    @Override
    protected String getKeySuffix() {
        return stationId;
    }

    @Override
    public Object call() throws IOException {
        Timer timer = new Timer();
        String lastModified = httpCache.get(getKey());
        Modified<List<BuoyStdMetData>> result = NdbcNoaaGovService.fetchBuoyStdMetData(stationId, lastModified);
        long elapsed = timer.elapsed();
        if (result == null) {
            logger.info("Std met data not modified for station " + stationId + " (" + elapsed + "ms)");
            return null;
        }
        if (result.value() != null) {
            List<BuoyStdMetData> data = result.value();
            logger.info("Fetched " + data.size() + " std met observations for station " + stationId + " (" + elapsed + "ms)");
            Timer t = new Timer();
            dataDb.replaceAllForStation(stationId, data);
            logger.info("Replaced std met observations for station " + stationId + " (" + t.elapsed() + " ms)");
            httpCache.put(getKey(), result.lastModified());
        }
        return null;
    }
}

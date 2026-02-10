package org.opensurfcast.sync;

import org.opensurfcast.buoy.BuoySpecWaveData;
import org.opensurfcast.buoy.NdbcNoaaGovService;
import org.opensurfcast.db.BuoySpecWaveDataDb;
import org.opensurfcast.http.HttpCache;
import org.opensurfcast.http.Modified;
import org.opensurfcast.log.Logger;
import org.opensurfcast.tasks.BaseTask;
import org.opensurfcast.timer.Timer;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * Task to fetch spectral wave data for a specific buoy station.
 * <p>
 * Fetches recent wave observations and updates the local database.
 * Uses {@link HttpCache} to send {@code If-Modified-Since} headers
 * and skip redundant downloads.
 */
public class FetchBuoySpecWaveDataTask extends BaseTask {
    private static final Duration COOLDOWN_PERIOD = Duration.ofMinutes(5);

    private final BuoySpecWaveDataDb dataDb;
    private final String stationId;
    private final HttpCache httpCache;
    private final Logger logger;

    public FetchBuoySpecWaveDataTask(BuoySpecWaveDataDb dataDb, String stationId, HttpCache httpCache, Logger logger) {
        super(COOLDOWN_PERIOD);
        this.dataDb = dataDb;
        this.stationId = stationId;
        this.httpCache = httpCache;
        this.logger = logger;
    }

    @Override
    protected String getKeySuffix() {
        return stationId;
    }

    @Override
    protected void execute() throws IOException {
        Timer timer = new Timer();
        String lastModified = httpCache.get(getKey());
        Modified<List<BuoySpecWaveData>> result = NdbcNoaaGovService.fetchBuoySpecWaveData(stationId, lastModified);
        long elapsed = timer.elapsed();
        if (result == null) {
            logger.info("Spec wave data not modified for station " + stationId + " (" + elapsed + "ms)");
            return;
        }
        if (result.value() != null) {
            List<BuoySpecWaveData> data = result.value();
            logger.info("Fetched " + data.size() + " spec wave observations for station " + stationId + " (" + elapsed + "ms)");
            Timer t = new Timer();
            dataDb.replaceAllForStation(stationId, data);
            logger.info("Replaced spec wave observations for station " + stationId + " (" + t.elapsed() + " ms)");
            httpCache.put(getKey(), result.lastModified());
        }
    }
}

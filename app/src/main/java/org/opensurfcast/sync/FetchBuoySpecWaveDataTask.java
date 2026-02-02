package org.opensurfcast.sync;

import org.opensurfcast.buoy.BuoySpecWaveData;
import org.opensurfcast.buoy.NdbcNoaaGovService;
import org.opensurfcast.db.BuoySpecWaveDataDb;
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
 */
public class FetchBuoySpecWaveDataTask extends BaseTask {
    private static final String KEY_PREFIX = "FETCH_BUOY_SPEC_WAVE_DATA:";
    private static final Duration COOLDOWN_PERIOD = Duration.ofMinutes(5);

    private final BuoySpecWaveDataDb dataDb;
    private final String stationId;
    private final Logger logger;

    public FetchBuoySpecWaveDataTask(BuoySpecWaveDataDb dataDb, String stationId, Logger logger) {
        super(KEY_PREFIX + stationId, COOLDOWN_PERIOD);
        this.dataDb = dataDb;
        this.stationId = stationId;
        this.logger = logger;
    }

    @Override
    protected void execute() throws IOException {
        Timer timer = new Timer();
        Modified<List<BuoySpecWaveData>> result = NdbcNoaaGovService.fetchBuoySpecWaveData(stationId, null);
        long elapsed = timer.elapsed();
        if (result.value() != null) {
            List<BuoySpecWaveData> data = result.value();
            dataDb.replaceAllForStation(stationId, data);
            logger.info("Fetched " + data.size() + " spec wave observations for station " + stationId + " (" + elapsed + "ms)");
        } else {
            logger.info("Spec wave data not modified for station " + stationId + " (" + elapsed + "ms)");
        }
    }
}

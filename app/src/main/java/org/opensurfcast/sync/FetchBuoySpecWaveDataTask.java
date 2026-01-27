package org.opensurfcast.sync;

import org.opensurfcast.buoy.BuoySpecWaveData;
import org.opensurfcast.buoy.NdbcNoaaGovService;
import org.opensurfcast.db.BuoySpecWaveDataDb;
import org.opensurfcast.http.Modified;
import org.opensurfcast.tasks.BaseTask;

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

    public FetchBuoySpecWaveDataTask(BuoySpecWaveDataDb dataDb, String stationId) {
        super(KEY_PREFIX + stationId, COOLDOWN_PERIOD);
        this.dataDb = dataDb;
        this.stationId = stationId;
    }

    @Override
    protected void execute() throws IOException {
        Modified<List<BuoySpecWaveData>> result = NdbcNoaaGovService.fetchBuoySpecWaveData(stationId, null);
        if (result.value() != null) {
            dataDb.replaceAllForStation(stationId, result.value());
        }
    }
}

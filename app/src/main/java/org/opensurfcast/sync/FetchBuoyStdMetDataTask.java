package org.opensurfcast.sync;

import org.opensurfcast.buoy.BuoyStdMetData;
import org.opensurfcast.buoy.NdbcNoaaGovService;
import org.opensurfcast.db.BuoyStdMetDataDb;
import org.opensurfcast.http.Modified;
import org.opensurfcast.tasks.BaseTask;

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

    public FetchBuoyStdMetDataTask(BuoyStdMetDataDb dataDb, String stationId) {
        super(KEY_PREFIX + stationId, COOLDOWN_PERIOD);
        this.dataDb = dataDb;
        this.stationId = stationId;
    }

    @Override
    protected void execute() throws IOException {
        Modified<List<BuoyStdMetData>> result = NdbcNoaaGovService.fetchBuoyStdMetData(stationId, null);
        if (result.value() != null) {
            dataDb.replaceAllForStation(stationId, result.value());
        }
    }
}

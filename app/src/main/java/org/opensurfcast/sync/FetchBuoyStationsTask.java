package org.opensurfcast.sync;

import org.opensurfcast.buoy.BuoyStation;
import org.opensurfcast.buoy.NdbcNoaaGovService;
import org.opensurfcast.db.BuoyStationDb;
import org.opensurfcast.http.Modified;
import org.opensurfcast.tasks.BaseTask;

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

    public FetchBuoyStationsTask(BuoyStationDb stationDb) {
        super(KEY, COOLDOWN_PERIOD);
        this.stationDb = stationDb;
    }

    @Override
    protected void execute() throws IOException {
        Modified<List<BuoyStation>> result = NdbcNoaaGovService.fetchBuoyStations(null);
        if (result.value() != null) {
            stationDb.replaceAll(result.value());
        }
    }
}

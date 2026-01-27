package org.opensurfcast.sync;

import org.opensurfcast.db.TideStationDb;
import org.opensurfcast.tasks.BaseTask;
import org.opensurfcast.tide.CoOpsNoaaGovService;
import org.opensurfcast.tide.TideStation;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * Task to fetch the tide station catalog from NOAA CO-OPS.
 * <p>
 * Fetches all tide prediction stations and updates the local database.
 */
public class FetchTideStationsTask extends BaseTask {
    private static final String KEY = "FETCH_TIDE_STATIONS";
    private static final Duration COOLDOWN_PERIOD = Duration.ofHours(1);

    private final TideStationDb stationDb;

    public FetchTideStationsTask(TideStationDb stationDb) {
        super(KEY, COOLDOWN_PERIOD);
        this.stationDb = stationDb;
    }

    @Override
    protected void execute() throws IOException {
        List<TideStation> stations = CoOpsNoaaGovService.fetchTideStations();
        stationDb.replaceAll(stations);
    }
}

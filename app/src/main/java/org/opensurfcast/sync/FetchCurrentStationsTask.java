package org.opensurfcast.sync;

import org.opensurfcast.db.CurrentStationDb;
import org.opensurfcast.tasks.BaseTask;
import org.opensurfcast.tide.CoOpsNoaaGovService;
import org.opensurfcast.tide.CurrentStation;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * Task to fetch the current station catalog from NOAA CO-OPS.
 * <p>
 * Fetches all current prediction stations and updates the local database.
 */
public class FetchCurrentStationsTask extends BaseTask {
    private static final String KEY = "FETCH_CURRENT_STATIONS";
    private static final Duration COOLDOWN_PERIOD = Duration.ofHours(1);

    private final CurrentStationDb stationDb;

    public FetchCurrentStationsTask(CurrentStationDb stationDb) {
        super(KEY, COOLDOWN_PERIOD);
        this.stationDb = stationDb;
    }

    @Override
    protected void execute() throws IOException {
        List<CurrentStation> stations = CoOpsNoaaGovService.fetchCurrentStations();
        stationDb.replaceAll(stations);
    }
}

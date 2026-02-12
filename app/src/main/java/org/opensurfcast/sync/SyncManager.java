package org.opensurfcast.sync;

import org.opensurfcast.db.BuoySpecWaveDataDb;
import org.opensurfcast.db.BuoyStationDb;
import org.opensurfcast.db.BuoyStdMetDataDb;
import org.opensurfcast.db.CurrentPredictionDb;
import org.opensurfcast.db.CurrentStationDb;
import org.opensurfcast.db.TidePredictionDb;
import org.opensurfcast.db.TideStationDb;
import org.opensurfcast.http.HttpCache;
import org.opensurfcast.log.Logger;
import org.opensurfcast.prefs.UserPreferences;
import org.opensurfcast.tasks.TaskScheduler;

/**
 * Manages synchronization of station catalogs and observational data.
 * <p>
 * Provides convenience methods for fetching stations and data,
 * delegating task execution to the underlying {@link TaskScheduler}.
 */
public class SyncManager {
    private final TaskScheduler scheduler;
    private final BuoyStationDb buoyStationDb;
    private final TideStationDb tideStationDb;
    private final CurrentStationDb currentStationDb;
    private final BuoyStdMetDataDb buoyStdMetDataDb;
    private final BuoySpecWaveDataDb buoySpecWaveDataDb;
    private final TidePredictionDb tidePredictionDb;
    private final CurrentPredictionDb currentPredictionDb;
    private final HttpCache httpCache;
    private final Logger logger;

    /**
     * Creates a new sync manager.
     *
     * @param scheduler             task scheduler for background execution
     * @param buoyStationDb         buoy station database
     * @param tideStationDb         tide station database
     * @param currentStationDb      current station database
     * @param buoyStdMetDataDb      buoy standard met data database
     * @param buoySpecWaveDataDb    buoy spectral wave data database
     * @param tidePredictionDb      tide prediction database
     * @param currentPredictionDb   current prediction database
     * @param httpCache             HTTP cache for Last-Modified headers
     * @param logger                logger for task logging
     */
    public SyncManager(
            TaskScheduler scheduler,
            BuoyStationDb buoyStationDb,
            TideStationDb tideStationDb,
            CurrentStationDb currentStationDb,
            BuoyStdMetDataDb buoyStdMetDataDb,
            BuoySpecWaveDataDb buoySpecWaveDataDb,
            TidePredictionDb tidePredictionDb,
            CurrentPredictionDb currentPredictionDb,
            HttpCache httpCache,
            Logger logger) {
        this.scheduler = scheduler;
        this.buoyStationDb = buoyStationDb;
        this.tideStationDb = tideStationDb;
        this.currentStationDb = currentStationDb;
        this.buoyStdMetDataDb = buoyStdMetDataDb;
        this.buoySpecWaveDataDb = buoySpecWaveDataDb;
        this.tidePredictionDb = tidePredictionDb;
        this.currentPredictionDb = currentPredictionDb;
        this.httpCache = httpCache;
        this.logger = logger;
    }

    /**
     * Returns the underlying task scheduler.
     *
     * @return the task scheduler
     */
    public TaskScheduler getScheduler() {
        return scheduler;
    }

    /**
     * Fetches the tide station catalog only.
     * <p>
     * Use for pull-to-refresh on the tide list without fetching buoy/current catalogs.
     */
    public void fetchTideStationsOnly() {
        scheduler.submit(new FetchTideStationsTask(tideStationDb, logger));
    }

    /**
     * Fetches all station catalogs.
     * <p>
     * Submits tasks to fetch buoy, tide, and current station catalogs.
     */
    public void fetchAllStations() {
        scheduler.submit(new FetchBuoyStationsTask(buoyStationDb, httpCache, logger));
        scheduler.submit(new FetchTideStationsTask(tideStationDb, logger));
        scheduler.submit(new FetchCurrentStationsTask(currentStationDb, logger));
    }

    /**
     * Fetches data for all preferred stations.
     * <p>
     * Queries user preferences and submits tasks to fetch data
     * for each preferred station.
     *
     * @param prefs user preferences containing preferred station IDs
     */
    public void fetchPreferredStationData(UserPreferences prefs) {
        fetchPreferredBuoyStationData(prefs);
        fetchPreferredTideStationData(prefs);

        // Fetch current predictions
        for (String stationId : prefs.getPreferredCurrentStations()) {
            scheduler.submit(new FetchCurrentPredictionsTask(currentPredictionDb, stationId, logger));
        }
    }

    public void fetchPreferredBuoyStationData(UserPreferences prefs) {
        for (String stationId : prefs.getPreferredBuoyStations()) {
            scheduler.submit(new FetchBuoyStdMetDataTask(buoyStdMetDataDb, stationId, httpCache, logger));
            scheduler.submit(new FetchBuoySpecWaveDataTask(buoySpecWaveDataDb, stationId, httpCache, logger));
        }
    }

    public void fetchPreferredTideStationData(UserPreferences prefs) {
        for (String stationId : prefs.getPreferredTideStations()) {
            scheduler.submit(new FetchTidePredictionsTask(tidePredictionDb, stationId, logger));
        }
    }
}

package org.opensurfcast.sync;

import org.opensurfcast.db.CurrentPredictionDb;
import org.opensurfcast.log.Logger;
import org.opensurfcast.tasks.BaseTask;
import org.opensurfcast.tide.CoOpsNoaaGovService;
import org.opensurfcast.tide.CurrentPrediction;
import org.opensurfcast.timer.Timer;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Task to fetch current predictions for a specific station.
 * <p>
 * Fetches predictions from 7 days ago to 30 days ahead and updates the
 * local database.
 */
public class FetchCurrentPredictionsTask extends BaseTask {
    private static final Duration COOLDOWN_PERIOD = Duration.ofHours(12);

    private final CurrentPredictionDb dataDb;
    private final String stationId;
    private final Logger logger;

    public FetchCurrentPredictionsTask(CurrentPredictionDb dataDb, String stationId, Logger logger) {
        super(COOLDOWN_PERIOD);
        this.dataDb = dataDb;
        this.stationId = stationId;
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
    protected void execute() throws IOException {
        // Generate date range: 7 days ago to 30 days from now
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.US);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        String beginDate = dateFormat.format(calendar.getTime());
        calendar.add(Calendar.DAY_OF_YEAR, 37); // -7 + 37 = +30
        String endDate = dateFormat.format(calendar.getTime());

        Timer timer = new Timer();
        List<CurrentPrediction> predictions = CoOpsNoaaGovService.fetchCurrentPredictions(
                stationId, beginDate, endDate);
        long elapsed = timer.elapsed();
        logger.info("Fetched " + predictions.size() + " current predictions for station " + stationId + " (" + elapsed + "ms)");
        Timer t = new Timer();
        dataDb.replaceAllForStation(stationId, predictions);
        logger.info("Replaced current predictions for station " + stationId + " (" + t.elapsed() + " ms)");
    }
}

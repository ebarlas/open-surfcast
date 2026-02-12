package org.opensurfcast.sync;

import org.opensurfcast.db.TidePredictionDb;
import org.opensurfcast.log.Logger;
import org.opensurfcast.tasks.BaseTask;
import org.opensurfcast.tide.CoOpsNoaaGovService;
import org.opensurfcast.tide.TidePrediction;
import org.opensurfcast.timer.Timer;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Task to fetch tide predictions for a specific station.
 * <p>
 * Fetches predictions from 7 days back to 30 days forward and updates the local database.
 */
public class FetchTidePredictionsTask extends BaseTask {
    private static final Duration COOLDOWN_PERIOD = Duration.ofDays(1);

    private final TidePredictionDb dataDb;
    private final String stationId;
    private final Logger logger;

    public FetchTidePredictionsTask(TidePredictionDb dataDb, String stationId, Logger logger) {
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
        // Generate date range: 7 days back to 30 days forward
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.US);
        Calendar beginCal = Calendar.getInstance();
        beginCal.add(Calendar.DAY_OF_YEAR, -7);
        String beginDate = dateFormat.format(beginCal.getTime());
        Calendar endCal = Calendar.getInstance();
        endCal.add(Calendar.DAY_OF_YEAR, 30);
        String endDate = dateFormat.format(endCal.getTime());

        Timer timer = new Timer();
        List<TidePrediction> predictions = CoOpsNoaaGovService.fetchTidePredictions(
                stationId, beginDate, endDate);
        long elapsed = timer.elapsed();
        logger.info("Fetched " + predictions.size() + " tide predictions for station " + stationId + " (" + elapsed + "ms)");
        Timer t = new Timer();
        dataDb.replaceAllForStation(stationId, predictions);
        logger.info("Replaced tide predictions for station " + stationId + " (" + t.elapsed() + " ms)");
    }
}

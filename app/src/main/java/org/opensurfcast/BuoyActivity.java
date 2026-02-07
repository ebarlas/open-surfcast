package org.opensurfcast;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import org.opensurfcast.db.BuoySpecWaveDataDb;
import org.opensurfcast.db.BuoyStationDb;
import org.opensurfcast.db.BuoyStdMetDataDb;
import org.opensurfcast.db.CurrentPredictionDb;
import org.opensurfcast.db.CurrentStationDb;
import org.opensurfcast.db.LogDb;
import org.opensurfcast.db.OpenSurfcastDbHelper;
import org.opensurfcast.db.TidePredictionDb;
import org.opensurfcast.db.TideStationDb;
import org.opensurfcast.log.AppLogger;
import org.opensurfcast.log.AsyncLogDb;
import org.opensurfcast.log.Logger;
import org.opensurfcast.prefs.UserPreferences;
import org.opensurfcast.sync.SyncManager;
import org.opensurfcast.tasks.TaskCooldowns;
import org.opensurfcast.tasks.TaskScheduler;
import org.opensurfcast.ui.BuoyListFragment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BuoyActivity extends AppCompatActivity {

    private OpenSurfcastDbHelper dbHelper;
    private BuoyStationDb buoyStationDb;
    private UserPreferences userPreferences;
    private SyncManager syncManager;
    private TaskScheduler taskScheduler;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fragment_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initDependencies();

        // Load the default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new BuoyListFragment())
                    .commit();
        }

        // Sync station catalogs on launch
        syncManager.fetchAllStations();
    }

    private void initDependencies() {
        executorService = Executors.newFixedThreadPool(4);
        Handler mainHandler = new Handler(Looper.getMainLooper());

        dbHelper = new OpenSurfcastDbHelper(this);
        buoyStationDb = new BuoyStationDb(dbHelper);
        TideStationDb tideStationDb = new TideStationDb(dbHelper);
        CurrentStationDb currentStationDb = new CurrentStationDb(dbHelper);
        BuoyStdMetDataDb buoyStdMetDataDb = new BuoyStdMetDataDb(dbHelper);
        BuoySpecWaveDataDb buoySpecWaveDataDb = new BuoySpecWaveDataDb(dbHelper);
        TidePredictionDb tidePredictionDb = new TidePredictionDb(dbHelper);
        CurrentPredictionDb currentPredictionDb = new CurrentPredictionDb(dbHelper);

        LogDb logDb = new LogDb(dbHelper);
        AsyncLogDb asyncLogDb = new AsyncLogDb(logDb, executorService);
        Logger logger = new AppLogger(asyncLogDb);

        userPreferences = new UserPreferences(this);

        TaskCooldowns cooldowns = new TaskCooldowns(this);
        taskScheduler = new TaskScheduler(executorService, mainHandler::post, cooldowns);

        syncManager = new SyncManager(
                taskScheduler,
                buoyStationDb,
                tideStationDb,
                currentStationDb,
                buoyStdMetDataDb,
                buoySpecWaveDataDb,
                tidePredictionDb,
                currentPredictionDb,
                logger
        );
    }

    /**
     * Returns the buoy station database for use by fragments.
     */
    public BuoyStationDb getBuoyStationDb() {
        return buoyStationDb;
    }

    /**
     * Returns user preferences for use by fragments.
     */
    public UserPreferences getUserPreferences() {
        return userPreferences;
    }

    /**
     * Returns the sync manager for use by fragments.
     */
    public SyncManager getSyncManager() {
        return syncManager;
    }

    /**
     * Returns the background executor for use by fragments.
     */
    public ExecutorService getExecutorService() {
        return executorService;
    }

    /**
     * Navigates to the given fragment, adding to the back stack.
     */
    public void navigateTo(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (taskScheduler != null) {
            taskScheduler.shutdown();
        }
    }
}

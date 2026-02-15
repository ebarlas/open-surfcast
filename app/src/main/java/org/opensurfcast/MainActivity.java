package org.opensurfcast;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.opensurfcast.db.BuoySpecWaveDataDb;
import org.opensurfcast.db.BuoyStationDb;
import org.opensurfcast.db.BuoyStdMetDataDb;
import org.opensurfcast.db.CurrentPredictionDb;
import org.opensurfcast.db.CurrentStationDb;
import org.opensurfcast.db.LogDb;
import org.opensurfcast.db.OpenSurfcastDbHelper;
import org.opensurfcast.db.TidePredictionDb;
import org.opensurfcast.db.TideStationDb;
import org.opensurfcast.http.HttpCache;
import org.opensurfcast.log.AppLogger;
import org.opensurfcast.log.AsyncLogDb;
import org.opensurfcast.log.Logger;
import org.opensurfcast.prefs.UserPreferences;
import org.opensurfcast.sync.SyncManager;
import org.opensurfcast.tasks.TaskCooldowns;
import org.opensurfcast.tasks.TaskScheduler;
import org.opensurfcast.ui.BuoyListFragment;
import org.opensurfcast.ui.CurrentListFragment;
import org.opensurfcast.ui.LogListFragment;
import org.opensurfcast.ui.SettingsFragment;
import org.opensurfcast.ui.TideListFragment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG_BUOYS = "TAG_BUOYS";
    private static final String TAG_TIDES = "TAG_TIDES";
    private static final String TAG_CURRENTS = "TAG_CURRENTS";
    private static final String TAG_LOGS = "TAG_LOGS";
    private static final String TAG_SETTINGS = "TAG_SETTINGS";
    private static final String KEY_CURRENT_TAG = "current_tag";

    private OpenSurfcastDbHelper dbHelper;
    private BuoyStationDb buoyStationDb;
    private TideStationDb tideStationDb;
    private TidePredictionDb tidePredictionDb;
    private CurrentStationDb currentStationDb;
    private CurrentPredictionDb currentPredictionDb;
    private BuoyStdMetDataDb buoyStdMetDataDb;
    private BuoySpecWaveDataDb buoySpecWaveDataDb;
    private AsyncLogDb asyncLogDb;
    private UserPreferences userPreferences;
    private SyncManager syncManager;
    private TaskScheduler taskScheduler;
    private ExecutorService executorService;
    private ExecutorService dbExecutor;

    private BottomNavigationView bottomNavigation;
    private String currentTag = TAG_BUOYS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply persisted theme before the activity inflates its layout
        userPreferences = new UserPreferences(this);
        UserPreferences.applyThemeMode(userPreferences.getThemeMode());

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Edge-to-edge insets: top goes to fragment container, bottom goes to nav bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fragment_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        bottomNavigation = findViewById(R.id.bottom_navigation);
        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);
            return insets;
        });

        initDependencies();

        // Set up bottom navigation
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_buoys) {
                switchToFragment(TAG_BUOYS);
                return true;
            } else if (id == R.id.nav_tides) {
                switchToFragment(TAG_TIDES);
                return true;
            } else if (id == R.id.nav_currents) {
                switchToFragment(TAG_CURRENTS);
                return true;
            } else if (id == R.id.nav_logs) {
                switchToFragment(TAG_LOGS);
                return true;
            } else if (id == R.id.nav_settings) {
                switchToFragment(TAG_SETTINGS);
                return true;
            }
            return false;
        });

        // Load the default fragment, or restore the active tag after recreation
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new BuoyListFragment(), TAG_BUOYS)
                    .commit();
        } else {
            currentTag = savedInstanceState.getString(KEY_CURRENT_TAG, TAG_BUOYS);
        }

        // Sync station catalogs on launch
        syncManager.fetchAllStations();
    }

    /**
     * Switches the fragment container to the top-level fragment for the given tag.
     * Clears the back stack to avoid stacking catalog/detail fragments under tabs.
     */
    private void switchToFragment(String tag) {
        if (tag.equals(currentTag)) {
            return;
        }
        currentTag = tag;

        // Pop entire back stack so sub-fragments (e.g. catalog) are removed
        getSupportFragmentManager().popBackStackImmediate(null,
                androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);

        Fragment fragment;
        if (TAG_LOGS.equals(tag)) {
            fragment = new LogListFragment();
        } else if (TAG_SETTINGS.equals(tag)) {
            fragment = new SettingsFragment();
        } else if (TAG_TIDES.equals(tag)) {
            fragment = new TideListFragment();
        } else if (TAG_CURRENTS.equals(tag)) {
            fragment = new CurrentListFragment();
        } else {
            fragment = new BuoyListFragment();
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, tag)
                .commit();
    }

    private void initDependencies() {
        executorService = Executors.newFixedThreadPool(5);
        dbExecutor = Executors.newFixedThreadPool(5);
        Handler mainHandler = new Handler(Looper.getMainLooper());

        dbHelper = new OpenSurfcastDbHelper(this);
        buoyStationDb = new BuoyStationDb(dbHelper);
        tideStationDb = new TideStationDb(dbHelper);
        currentStationDb = new CurrentStationDb(dbHelper);
        buoyStdMetDataDb = new BuoyStdMetDataDb(dbHelper);
        buoySpecWaveDataDb = new BuoySpecWaveDataDb(dbHelper);
        tidePredictionDb = new TidePredictionDb(dbHelper);
        currentPredictionDb = new CurrentPredictionDb(dbHelper);

        LogDb logDb = new LogDb(dbHelper);
        asyncLogDb = new AsyncLogDb(logDb, dbExecutor);
        Logger logger = new AppLogger(asyncLogDb);

        // userPreferences is initialized early in onCreate() for theme application
        HttpCache httpCache = new HttpCache(this);

        TaskCooldowns cooldowns = new TaskCooldowns(this);
        taskScheduler = new TaskScheduler(executorService, mainHandler::post, cooldowns, logger);

        syncManager = new SyncManager(
                taskScheduler,
                buoyStationDb,
                tideStationDb,
                currentStationDb,
                buoyStdMetDataDb,
                buoySpecWaveDataDb,
                tidePredictionDb,
                currentPredictionDb,
                httpCache,
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
     * Returns the tide station database for use by fragments.
     */
    public TideStationDb getTideStationDb() {
        return tideStationDb;
    }

    /**
     * Returns the tide prediction database for use by fragments.
     */
    public TidePredictionDb getTidePredictionDb() {
        return tidePredictionDb;
    }

    /**
     * Returns the current station database for use by fragments.
     */
    public CurrentStationDb getCurrentStationDb() {
        return currentStationDb;
    }

    /**
     * Returns the current prediction database for use by fragments.
     */
    public CurrentPredictionDb getCurrentPredictionDb() {
        return currentPredictionDb;
    }

    /**
     * Returns the buoy standard meteorological data database for use by fragments.
     */
    public BuoyStdMetDataDb getBuoyStdMetDataDb() {
        return buoyStdMetDataDb;
    }

    /**
     * Returns the buoy spectral wave data database for use by fragments.
     */
    public BuoySpecWaveDataDb getBuoySpecWaveDataDb() {
        return buoySpecWaveDataDb;
    }

    /**
     * Returns the async log database for use by fragments.
     */
    public AsyncLogDb getAsyncLogDb() {
        return asyncLogDb;
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
     * Returns the dedicated database executor for local DB queries.
     * <p>
     * Use this instead of {@link #getExecutorService()} for database operations
     * to avoid being blocked by long-running network tasks.
     */
    public ExecutorService getDbExecutor() {
        return dbExecutor;
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
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_CURRENT_TAG, currentTag);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (taskScheduler != null) {
            taskScheduler.shutdown();
        }
        if (dbExecutor != null) {
            dbExecutor.shutdown();
        }
    }
}

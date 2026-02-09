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
import org.opensurfcast.ui.LogListFragment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BuoyActivity extends AppCompatActivity {

    private static final String TAG_BUOYS = "TAG_BUOYS";
    private static final String TAG_LOGS = "TAG_LOGS";

    private OpenSurfcastDbHelper dbHelper;
    private BuoyStationDb buoyStationDb;
    private BuoyStdMetDataDb buoyStdMetDataDb;
    private AsyncLogDb asyncLogDb;
    private UserPreferences userPreferences;
    private SyncManager syncManager;
    private TaskScheduler taskScheduler;
    private ExecutorService executorService;

    private BottomNavigationView bottomNavigation;
    private String currentTag = TAG_BUOYS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
            } else if (id == R.id.nav_logs) {
                switchToFragment(TAG_LOGS);
                return true;
            }
            return false;
        });

        // Load the default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new BuoyListFragment(), TAG_BUOYS)
                    .commit();
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
        } else {
            fragment = new BuoyListFragment();
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, tag)
                .commit();
    }

    private void initDependencies() {
        executorService = Executors.newFixedThreadPool(4);
        Handler mainHandler = new Handler(Looper.getMainLooper());

        dbHelper = new OpenSurfcastDbHelper(this);
        buoyStationDb = new BuoyStationDb(dbHelper);
        TideStationDb tideStationDb = new TideStationDb(dbHelper);
        CurrentStationDb currentStationDb = new CurrentStationDb(dbHelper);
        buoyStdMetDataDb = new BuoyStdMetDataDb(dbHelper);
        BuoySpecWaveDataDb buoySpecWaveDataDb = new BuoySpecWaveDataDb(dbHelper);
        TidePredictionDb tidePredictionDb = new TidePredictionDb(dbHelper);
        CurrentPredictionDb currentPredictionDb = new CurrentPredictionDb(dbHelper);

        LogDb logDb = new LogDb(dbHelper);
        asyncLogDb = new AsyncLogDb(logDb, executorService);
        Logger logger = new AppLogger(asyncLogDb);

        userPreferences = new UserPreferences(this);
        HttpCache httpCache = new HttpCache(this);

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
     * Returns the buoy standard meteorological data database for use by fragments.
     */
    public BuoyStdMetDataDb getBuoyStdMetDataDb() {
        return buoyStdMetDataDb;
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

package org.opensurfcast.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import org.opensurfcast.MainActivity;
import org.opensurfcast.R;
import org.opensurfcast.db.TidePredictionDb;
import org.opensurfcast.db.TideStationDb;
import org.opensurfcast.prefs.UserPreferences;
import org.opensurfcast.sync.FetchTidePredictionsTask;
import org.opensurfcast.sync.FetchTideStationsTask;
import org.opensurfcast.sync.SyncManager;
import org.opensurfcast.tasks.Task;
import org.opensurfcast.tasks.TaskListener;
import org.opensurfcast.tasks.TaskScheduler;
import org.opensurfcast.tide.TideLevelInterpolator;
import org.opensurfcast.tide.TidePrediction;
import org.opensurfcast.tide.TideStation;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Fragment displaying the user's preferred tide stations.
 * <p>
 * Provides swipe-to-delete with undo, pull-to-refresh, and a FAB
 * to navigate to the tide catalog. Displays interpolated current tide level
 * for each station.
 */
public class TideListFragment extends Fragment {

    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private SwipeRefreshLayout swipeRefresh;
    private LinearProgressIndicator syncProgress;
    private TideListAdapter adapter;

    private TideStationDb tideStationDb;
    private TidePredictionDb tidePredictionDb;
    private UserPreferences userPreferences;
    private SyncManager syncManager;
    private ExecutorService dbExecutor;

    private final TaskListener taskListener = new TaskListener() {
        @Override
        public void onTaskStarted(Task task) {
            if (task instanceof FetchTideStationsTask || task instanceof FetchTidePredictionsTask) {
                showSyncProgress(true);
            }
        }

        @Override
        public void onTaskCompleted(Task task) {
            if (task instanceof FetchTideStationsTask || task instanceof FetchTidePredictionsTask) {
                loadPreferredStations();
                updateSyncState();
            }
        }

        @Override
        public void onTaskFailed(Task task, Exception error) {
            if (task instanceof FetchTideStationsTask || task instanceof FetchTidePredictionsTask) {
                updateSyncState();
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tide_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MainActivity activity = (MainActivity) requireActivity();
        tideStationDb = activity.getTideStationDb();
        tidePredictionDb = activity.getTidePredictionDb();
        userPreferences = activity.getUserPreferences();
        syncManager = activity.getSyncManager();
        dbExecutor = activity.getDbExecutor();

        recyclerView = view.findViewById(R.id.tide_list);
        emptyState = view.findViewById(R.id.empty_state);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        syncProgress = view.findViewById(R.id.sync_progress);
        FloatingActionButton fab = view.findViewById(R.id.fab_add_tide);

        // RecyclerView setup
        adapter = new TideListAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        // Swipe-to-delete
        ItemTouchHelper touchHelper = new ItemTouchHelper(new SwipeToDeleteCallback());
        touchHelper.attachToRecyclerView(recyclerView);

        // Pull-to-refresh: sync tide station catalog and fetch tide predictions
        swipeRefresh.setColorSchemeResources(
                R.color.md_theme_light_primary,
                R.color.md_theme_light_secondary);
        swipeRefresh.setOnRefreshListener(() -> {
            syncManager.fetchTideStationsOnly();
            syncManager.fetchPreferredStationData(userPreferences);
            if (!hasTideTasksRunning()) {
                swipeRefresh.setRefreshing(false);
            }
        });

        // FAB -> catalog
        fab.setOnClickListener(v ->
                activity.navigateTo(new TideCatalogFragment()));

        // Register task listener
        syncManager.getScheduler().addListener(taskListener);

        if (hasTideTasksRunning()) {
            showSyncProgress(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.setUseMetric(userPreferences.isMetric());
        loadPreferredStations();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        syncManager.getScheduler().removeListener(taskListener);
    }

    /**
     * Loads the preferred tide stations from the database on a background thread,
     * fetches predictions per station, interpolates current levels, and updates
     * the UI on the main thread.
     */
    private void loadPreferredStations() {
        Set<String> preferredIds = userPreferences.getPreferredTideStations();
        if (preferredIds.isEmpty()) {
            updateEmptyState(true);
            adapter.submitList(null);
            return;
        }

        dbExecutor.execute(() -> {
            List<TideStation> stations = tideStationDb.queryByIds(preferredIds);
            Map<String, Double> levels = new HashMap<>();
            long nowEpochSeconds = System.currentTimeMillis() / 1000;

            for (TideStation station : stations) {
                List<TidePrediction> predictions =
                        tidePredictionDb.queryByStation(station.id);
                Double level = TideLevelInterpolator.interpolate(predictions, nowEpochSeconds);
                if (level != null) {
                    levels.put(station.id, level);
                }
            }

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    adapter.setUseMetric(userPreferences.isMetric());
                    adapter.submitList(stations, levels);
                    updateEmptyState(stations.isEmpty());
                });
            }
        });
    }

    private void updateEmptyState(boolean empty) {
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private boolean hasTideTasksRunning() {
        TaskScheduler scheduler = syncManager.getScheduler();
        Collection<Task> running = scheduler.getRunningTasks();
        for (Task task : running) {
            if (task instanceof FetchTideStationsTask || task instanceof FetchTidePredictionsTask) {
                return true;
            }
        }
        return false;
    }

    private void showSyncProgress(boolean show) {
        syncProgress.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void updateSyncState() {
        boolean running = hasTideTasksRunning();
        showSyncProgress(running);
        if (!running) {
            swipeRefresh.setRefreshing(false);
        }
    }

    /**
     * ItemTouchHelper callback for swipe-to-delete with undo via Snackbar.
     */
    private class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {

        SwipeToDeleteCallback() {
            super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView,
                              @NonNull RecyclerView.ViewHolder viewHolder,
                              @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getBindingAdapterPosition();
            TideStation removed = adapter.getStationAt(position);
            adapter.removeAt(position);
            userPreferences.removePreferredTideStation(removed.id);
            updateEmptyState(adapter.getItemCount() == 0);

            Snackbar.make(requireView(), R.string.station_removed, Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo, v -> {
                        userPreferences.addPreferredTideStation(removed.id);
                        adapter.insertAt(position, removed);
                        updateEmptyState(false);
                    })
                    .show();
        }
    }
}

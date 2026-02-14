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
import org.opensurfcast.db.CurrentPredictionDb;
import org.opensurfcast.db.CurrentStationDb;
import org.opensurfcast.prefs.UserPreferences;
import org.opensurfcast.sync.FetchCurrentPredictionsTask;
import org.opensurfcast.sync.SyncManager;
import org.opensurfcast.tasks.Task;
import org.opensurfcast.tasks.TaskListener;
import org.opensurfcast.tide.CurrentPrediction;
import org.opensurfcast.tide.CurrentStation;
import org.opensurfcast.tide.CurrentVelocityInterpolator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Fragment displaying the user's preferred current stations.
 * <p>
 * Provides swipe-to-delete with undo, pull-to-refresh, and a FAB
 * to navigate to the current station catalog. Displays interpolated
 * current velocity for each station.
 */
public class CurrentListFragment extends Fragment {

    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private SwipeRefreshLayout swipeRefresh;
    private LinearProgressIndicator syncProgress;
    private CurrentListAdapter adapter;

    private CurrentStationDb currentStationDb;
    private CurrentPredictionDb currentPredictionDb;
    private UserPreferences userPreferences;
    private SyncManager syncManager;
    private ExecutorService dbExecutor;

    private final TaskListener taskListener = new TaskListener() {
        @Override
        public void onTaskStarted(Task task) {
            if (task instanceof FetchCurrentPredictionsTask) {
                showSyncProgress(true);
            }
        }

        @Override
        public void onTaskCompleted(Task task) {
            if (task instanceof FetchCurrentPredictionsTask t) {
                updateCurrentRowForStation(t.stationId());
                updateSyncState();
            }
        }

        @Override
        public void onTaskFailed(Task task, Exception error) {
            if (task instanceof FetchCurrentPredictionsTask) {
                updateSyncState();
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_current_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MainActivity activity = (MainActivity) requireActivity();
        currentStationDb = activity.getCurrentStationDb();
        currentPredictionDb = activity.getCurrentPredictionDb();
        userPreferences = activity.getUserPreferences();
        syncManager = activity.getSyncManager();
        dbExecutor = activity.getDbExecutor();

        recyclerView = view.findViewById(R.id.current_list);
        emptyState = view.findViewById(R.id.empty_state);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        syncProgress = view.findViewById(R.id.sync_progress);
        FloatingActionButton fab = view.findViewById(R.id.fab_add_current);

        // RecyclerView setup
        adapter = new CurrentListAdapter();
        adapter.setOnStationClickListener(station -> {
            CurrentDetailFragment fragment = CurrentDetailFragment.newInstance(station.id);
            activity.navigateTo(fragment);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        // Swipe-to-delete
        ItemTouchHelper touchHelper = new ItemTouchHelper(new SwipeToDeleteCallback());
        touchHelper.attachToRecyclerView(recyclerView);

        // Pull-to-refresh
        swipeRefresh.setColorSchemeResources(
                R.color.md_theme_light_primary,
                R.color.md_theme_light_secondary);
        swipeRefresh.setOnRefreshListener(() -> {
            loadPreferredStations();
            swipeRefresh.setRefreshing(false);
        });

        // FAB -> catalog
        fab.setOnClickListener(v ->
                activity.navigateTo(new CurrentCatalogFragment()));

        // Register task listener
        syncManager.getScheduler().addListener(taskListener);

        if (hasCurrentTasksRunning()) {
            showSyncProgress(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.setUseMetric(userPreferences.isMetric());
        loadPreferredStations();
        // Auto-fetch latest predictions from the network
        syncManager.fetchPreferredCurrentStationData(userPreferences);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        syncManager.getScheduler().removeListener(taskListener);
    }

    /**
     * Loads the preferred current stations from the database on a background thread,
     * fetches predictions per station, interpolates current velocity, and updates
     * the UI on the main thread.
     */
    private void loadPreferredStations() {
        Set<String> preferredIds = userPreferences.getPreferredCurrentStations();
        if (preferredIds.isEmpty()) {
            updateEmptyState(true);
            adapter.submitList(null);
            return;
        }

        dbExecutor.execute(() -> {
            List<CurrentStation> stations = currentStationDb.queryByIds(preferredIds);
            Map<String, CurrentListAdapter.CurrentProgress> progressMap = new HashMap<>();
            long nowEpochSeconds = System.currentTimeMillis() / 1000;

            for (CurrentStation station : stations) {
                List<CurrentPrediction> predictions =
                        currentPredictionDb.queryByStation(station.id);
                CurrentVelocityInterpolator.Result result =
                        CurrentVelocityInterpolator.interpolateWithProgress(predictions, nowEpochSeconds);
                if (result != null) {
                    progressMap.put(station.id, new CurrentListAdapter.CurrentProgress(
                            result.velocityCmPerSec, result.progressFraction,
                            result.upcomingType, result.upcomingEpochSeconds));
                }
            }

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    adapter.setUseMetric(userPreferences.isMetric());
                    adapter.submitList(stations, progressMap);
                    updateEmptyState(stations.isEmpty());
                });
            }
        });
    }

    private void updateEmptyState(boolean empty) {
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    /**
     * Performs a targeted update of the current row for the given station.
     */
    private void updateCurrentRowForStation(String stationId) {
        if (adapter.hasCurrentProgress(stationId)) {
            return;
        }

        dbExecutor.execute(() -> {
            List<CurrentPrediction> predictions = currentPredictionDb.queryByStation(stationId);
            long nowEpochSeconds = System.currentTimeMillis() / 1000;
            CurrentVelocityInterpolator.Result result =
                    CurrentVelocityInterpolator.interpolateWithProgress(predictions, nowEpochSeconds);

            CurrentListAdapter.CurrentProgress newProgress = result != null
                    ? new CurrentListAdapter.CurrentProgress(
                    result.velocityCmPerSec, result.progressFraction,
                    result.upcomingType, result.upcomingEpochSeconds)
                    : null;

            if (isAdded()) {
                requireActivity().runOnUiThread(() ->
                        adapter.updateProgress(stationId, newProgress));
            }
        });
    }

    private boolean hasCurrentTasksRunning() {
        return syncManager.getScheduler()
                .getRunningTasks()
                .stream()
                .anyMatch(t -> t instanceof FetchCurrentPredictionsTask);
    }

    private void showSyncProgress(boolean show) {
        syncProgress.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void updateSyncState() {
        boolean running = hasCurrentTasksRunning();
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
            CurrentStation removed = adapter.getStationAt(position);
            adapter.removeAt(position);
            userPreferences.removePreferredCurrentStation(removed.id);
            updateEmptyState(adapter.getItemCount() == 0);

            Snackbar.make(requireView(), R.string.station_removed, Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo, v -> {
                        userPreferences.addPreferredCurrentStation(removed.id);
                        adapter.insertAt(position, removed);
                        updateEmptyState(false);
                    })
                    .show();
        }
    }
}

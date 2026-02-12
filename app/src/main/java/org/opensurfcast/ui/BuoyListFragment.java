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
import org.opensurfcast.buoy.BuoyStation;
import org.opensurfcast.buoy.BuoyStdMetData;
import org.opensurfcast.db.BuoyStationDb;
import org.opensurfcast.db.BuoyStdMetDataDb;
import org.opensurfcast.prefs.UserPreferences;
import org.opensurfcast.sync.FetchBuoyStdMetDataTask;
import org.opensurfcast.sync.SyncManager;
import org.opensurfcast.tasks.Task;
import org.opensurfcast.tasks.TaskListener;
import org.opensurfcast.tasks.TaskScheduler;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Fragment displaying the user's preferred buoy stations.
 * <p>
 * Provides swipe-to-delete with undo, pull-to-refresh, and a FAB
 * to navigate to the buoy catalog.
 */
public class BuoyListFragment extends Fragment {

    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private SwipeRefreshLayout swipeRefresh;
    private LinearProgressIndicator syncProgress;
    private BuoyListAdapter adapter;

    private BuoyStationDb buoyStationDb;
    private BuoyStdMetDataDb buoyStdMetDataDb;
    private UserPreferences userPreferences;
    private SyncManager syncManager;
    private ExecutorService dbExecutor;

    private final TaskListener taskListener = new TaskListener() {
        @Override
        public void onTaskStarted(Task task) {
            if (task instanceof FetchBuoyStdMetDataTask) {
                showSyncProgress(true);
            }
        }

        @Override
        public void onTaskCompleted(Task task) {
            if (task instanceof FetchBuoyStdMetDataTask) {
                refreshObservations(task);
                updateSyncState();
            }
        }

        @Override
        public void onTaskFailed(Task task, Exception error) {
            if (task instanceof FetchBuoyStdMetDataTask) {
                updateSyncState();
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_buoy_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MainActivity activity = (MainActivity) requireActivity();
        buoyStationDb = activity.getBuoyStationDb();
        buoyStdMetDataDb = activity.getBuoyStdMetDataDb();
        userPreferences = activity.getUserPreferences();
        syncManager = activity.getSyncManager();
        dbExecutor = activity.getDbExecutor();

        recyclerView = view.findViewById(R.id.buoy_list);
        emptyState = view.findViewById(R.id.empty_state);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        syncProgress = view.findViewById(R.id.sync_progress);
        FloatingActionButton fab = view.findViewById(R.id.fab_add_buoy);

        // RecyclerView setup
        adapter = new BuoyListAdapter();
        adapter.setUseMetric(userPreferences.isMetric());
        adapter.setOnItemClickListener(station ->
                activity.navigateTo(BuoyDetailFragment.newInstance(station.getId())));
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
            syncManager.fetchPreferredStationData(userPreferences);
            // If all buoy tasks were rejected (on cooldown or already running),
            // no callbacks will fire, so clear the refresh indicator immediately.
            if (!hasBuoyTasksRunning()) {
                swipeRefresh.setRefreshing(false);
            }
        });

        // FAB -> catalog
        fab.setOnClickListener(v ->
                activity.navigateTo(new BuoyCatalogFragment()));

        // Register task listener
        syncManager.getScheduler().addListener(taskListener);

        // Show progress if sync tasks are already running
        if (hasBuoyTasksRunning()) {
            showSyncProgress(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Re-apply unit preference (user may have changed it in settings)
        adapter.setUseMetric(userPreferences.isMetric());
        // Refresh when returning from catalog (user may have added stations)
        loadPreferredStations();
        // Auto-fetch latest observations from the network
        syncManager.fetchPreferredBuoyStationData(userPreferences);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        syncManager.getScheduler().removeListener(taskListener);
    }

    /**
     * Loads the preferred buoy stations from the database on a background thread
     * and updates the UI on the main thread.
     */
    private void loadPreferredStations() {
        Set<String> preferredIds = userPreferences.getPreferredBuoyStations();
        if (preferredIds.isEmpty()) {
            updateEmptyState(true);
            adapter.submitList(null);
            return;
        }

        dbExecutor.execute(() -> {
            List<BuoyStation> stations = buoyStationDb.queryByIds(preferredIds);
            Map<String, BuoyStdMetData> latestObs =
                    buoyStdMetDataDb.queryLatestByStations(preferredIds);
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    adapter.submitList(stations);
                    adapter.submitObservations(latestObs);
                    updateEmptyState(stations.isEmpty());
                });
            }
        });
    }

    /**
     * Refreshes only the observation data for already-loaded stations.
     * <p>
     * Unlike {@link #loadPreferredStations()}, this does not re-query the
     * station list. It queries the latest observations and uses the adapter's
     * timestamp-aware {@link BuoyListAdapter#updateObservations} to only
     * invalidate row tiles whose observation time has actually changed.
     */
    private void refreshObservations(Task task) {
        if (!(task instanceof FetchBuoyStdMetDataTask)) {
            return;
        }

        var stationId = ((FetchBuoyStdMetDataTask) task).stationId();

        Set<String> preferredIds = userPreferences.getPreferredBuoyStations();
        if (!preferredIds.contains(stationId)) {
            return;
        }

        dbExecutor.execute(() -> {
            BuoyStdMetData latestObs = buoyStdMetDataDb.queryLatestByStation(stationId);
            if (isAdded()) {
                requireActivity().runOnUiThread(() ->
                        adapter.updateObservations(stationId, latestObs));
            }
        });
    }

    private void updateEmptyState(boolean empty) {
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    /**
     * Returns true if any buoy-related fetch tasks are currently running.
     */
    private boolean hasBuoyTasksRunning() {
        return syncManager
                .getScheduler()
                .getRunningTasks()
                .stream()
                .anyMatch(t -> t instanceof FetchBuoyStdMetDataTask);
    }

    /**
     * Shows or hides the sync progress indicator.
     */
    private void showSyncProgress(boolean show) {
        syncProgress.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /**
     * Updates the sync progress indicator and swipe-to-refresh state
     * based on whether buoy tasks are still running.
     */
    private void updateSyncState() {
        boolean running = hasBuoyTasksRunning();
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
            BuoyStation removed = adapter.getStationAt(position);
            adapter.removeAt(position);
            userPreferences.removePreferredBuoyStation(removed.getId());
            updateEmptyState(adapter.getItemCount() == 0);

            Snackbar.make(requireView(), R.string.station_removed, Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo, v -> {
                        userPreferences.addPreferredBuoyStation(removed.getId());
                        adapter.insertAt(position, removed);
                        updateEmptyState(false);
                    })
                    .show();
        }
    }
}

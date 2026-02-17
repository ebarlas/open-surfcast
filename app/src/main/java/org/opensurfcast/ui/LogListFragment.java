package org.opensurfcast.ui;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;

import org.opensurfcast.MainActivity;
import org.opensurfcast.R;
import org.opensurfcast.log.AsyncLogDb;
import org.opensurfcast.log.LogEntry;
import org.opensurfcast.log.LogLevel;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Fragment displaying application log entries.
 * <p>
 * Provides level filtering via M3 filter chips, pull-to-refresh,
 * and a toolbar action to clear all logs.
 */
public class LogListFragment extends Fragment {

    private static final int MAX_LOG_ENTRIES = 500;

    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private SwipeRefreshLayout swipeRefresh;
    private LogListAdapter adapter;
    private AsyncLogDb asyncLogDb;

    /** Currently selected minimum log level filter, or null for "All". */
    private LogLevel currentFilter = null;

    /** Whether the fragment is in landscape immersive mode. */
    private boolean immersiveMode;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_log_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MainActivity activity = (MainActivity) requireActivity();
        asyncLogDb = activity.getAsyncLogDb();

        recyclerView = view.findViewById(R.id.log_list);
        emptyState = view.findViewById(R.id.empty_state);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(this::onMenuItemClick);

        // RecyclerView setup
        adapter = new LogListAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        // Pull-to-refresh
        swipeRefresh.setColorSchemeResources(
                R.color.md_theme_light_primary,
                R.color.md_theme_light_secondary);
        swipeRefresh.setOnRefreshListener(this::loadLogs);

        ChipGroup chipGroup = view.findViewById(R.id.chip_group_filter);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                currentFilter = null;
            } else {
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chip_all) {
                    currentFilter = null;
                } else if (checkedId == R.id.chip_debug) {
                    currentFilter = LogLevel.DEBUG;
                } else if (checkedId == R.id.chip_info) {
                    currentFilter = LogLevel.INFO;
                } else if (checkedId == R.id.chip_warn) {
                    currentFilter = LogLevel.WARN;
                } else if (checkedId == R.id.chip_error) {
                    currentFilter = LogLevel.ERROR;
                }
            }
            loadLogs();
        });

        loadLogs();

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            enterImmersiveMode(view, toolbar);
        }
    }

    @Override
    public void onDestroyView() {
        if (immersiveMode) {
            exitImmersiveMode();
        }
        super.onDestroyView();
    }

    private void enterImmersiveMode(View view, MaterialToolbar toolbar) {
        immersiveMode = true;

        ((View) toolbar.getParent()).setVisibility(View.GONE);
        view.findViewById(R.id.filter_chip_row).setVisibility(View.GONE);

        ((MainActivity) requireActivity()).setBottomNavigationVisible(false);

        Window window = requireActivity().getWindow();
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, view);
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    private void exitImmersiveMode() {
        if (getActivity() == null) return;

        ((MainActivity) requireActivity()).setBottomNavigationVisible(true);

        Window window = requireActivity().getWindow();
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        controller.show(WindowInsetsCompat.Type.systemBars());
    }

    private boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.action_clear_logs) {
            clearLogs();
            return true;
        }
        return false;
    }

    /**
     * Loads log entries from the database based on the current filter and
     * updates the UI on the main thread.
     */
    private void loadLogs() {
        CompletableFuture<List<LogEntry>> future;

        if (currentFilter != null) {
            future = asyncLogDb.getByMinLevel(currentFilter);
        } else {
            future = asyncLogDb.getRecent(MAX_LOG_ENTRIES);
        }

        future.thenAccept(entries -> {
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    adapter.submitList(entries);
                    updateEmptyState(entries.isEmpty());
                    swipeRefresh.setRefreshing(false);
                });
            }
        }).exceptionally(throwable -> {
            if (isAdded()) {
                requireActivity().runOnUiThread(() ->
                        swipeRefresh.setRefreshing(false));
            }
            return null;
        });
    }

    private void clearLogs() {
        asyncLogDb.deleteAll();
        adapter.submitList(null);
        updateEmptyState(true);
        Snackbar.make(requireView(), R.string.logs_cleared, Snackbar.LENGTH_SHORT).show();
    }

    private void updateEmptyState(boolean empty) {
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }
}

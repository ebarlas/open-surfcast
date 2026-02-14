package org.opensurfcast.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import org.opensurfcast.MainActivity;
import org.opensurfcast.R;
import org.opensurfcast.db.CurrentStationDb;
import org.opensurfcast.prefs.UserPreferences;
import org.opensurfcast.sync.FetchCurrentStationsTask;
import org.opensurfcast.sync.SyncManager;
import org.opensurfcast.tasks.Task;
import org.opensurfcast.tasks.TaskListener;
import org.opensurfcast.tide.CurrentStation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Fragment for browsing and searching the full current station catalog.
 * <p>
 * Allows adding/removing stations from the user's preferred list
 * via tap-to-toggle with a checkmark indicator.
 */
public class CurrentCatalogFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView stationCountText;
    private EditText searchInput;
    private CurrentCatalogAdapter adapter;

    private CurrentStationDb currentStationDb;
    private UserPreferences userPreferences;
    private SyncManager syncManager;
    private ExecutorService dbExecutor;

    /** Full unfiltered list of all catalog stations. */
    private List<CurrentStation> allStations = new ArrayList<>();

    private final TaskListener taskListener = new TaskListener() {
        @Override
        public void onTaskCompleted(Task task) {
            if (task instanceof FetchCurrentStationsTask) {
                loadCatalog();
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_current_catalog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MainActivity activity = (MainActivity) requireActivity();
        currentStationDb = activity.getCurrentStationDb();
        userPreferences = activity.getUserPreferences();
        syncManager = activity.getSyncManager();
        dbExecutor = activity.getDbExecutor();

        // Toolbar with back navigation
        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        searchInput = view.findViewById(R.id.search_input);
        stationCountText = view.findViewById(R.id.station_count);
        recyclerView = view.findViewById(R.id.catalog_list);

        // RecyclerView setup
        adapter = new CurrentCatalogAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        // Toggle listener
        adapter.setOnStationToggleListener((station, added) -> {
            if (added) {
                userPreferences.addPreferredCurrentStation(station.id);
            } else {
                userPreferences.removePreferredCurrentStation(station.id);
            }
        });

        // Search filtering
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStations(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        syncManager.getScheduler().addListener(taskListener);

        stationCountText.setText(R.string.catalog_loading);

        loadCatalog();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        syncManager.getScheduler().removeListener(taskListener);
    }

    /**
     * Loads all current stations from the database on a background thread.
     */
    private void loadCatalog() {
        dbExecutor.execute(() -> {
            List<CurrentStation> stations = currentStationDb.queryAll();
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    allStations = stations;
                    Set<String> addedIds = userPreferences.getPreferredCurrentStations();
                    adapter.setAddedIds(addedIds);
                    String query = searchInput.getText().toString();
                    filterStations(query);
                });
            }
        });
    }

    /**
     * Filters the station list based on the search query and updates the UI.
     */
    private void filterStations(String query) {
        String lowerQuery = query.trim().toLowerCase(Locale.US);

        List<CurrentStation> filtered;
        if (lowerQuery.isEmpty()) {
            filtered = allStations;
        } else {
            filtered = new ArrayList<>();
            for (CurrentStation station : allStations) {
                String name = station.name != null ? station.name.toLowerCase(Locale.US) : "";
                String id = station.id.toLowerCase(Locale.US);
                if (name.contains(lowerQuery) || id.contains(lowerQuery)) {
                    filtered.add(station);
                }
            }
        }

        adapter.submitList(filtered);
        updateStationCount(filtered.size());
    }

    private void updateStationCount(int count) {
        if (count == 0 && allStations.isEmpty()) {
            stationCountText.setText(R.string.catalog_loading);
        } else if (count == 0) {
            stationCountText.setText(R.string.no_stations_found);
        } else if (count == 1) {
            stationCountText.setText(R.string.station_count_one);
        } else {
            stationCountText.setText(getString(R.string.station_count, count));
        }
    }
}

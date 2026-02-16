package org.opensurfcast.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.materialswitch.MaterialSwitch;

import org.opensurfcast.MainActivity;
import org.opensurfcast.R;
import org.opensurfcast.db.TideStationDb;
import org.opensurfcast.prefs.StationSortOrder;
import org.opensurfcast.prefs.UserPreferences;
import org.opensurfcast.tide.TideStation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

/**
 * Fragment displaying application settings.
 * <p>
 * Provides controls for theme mode, metric/imperial units, and home tide station selection.
 */
public class SettingsFragment extends Fragment {

    private UserPreferences userPreferences;
    private TideStationDb tideStationDb;
    private ExecutorService dbExecutor;

    private MaterialButtonToggleGroup themeToggleGroup;
    private MaterialButtonToggleGroup sortToggleGroup;
    private MaterialSwitch chartLabelsSwitch;
    private TextView chartLabelsSummary;
    private MaterialSwitch metricSwitch;
    private TextView metricSummary;
    private AutoCompleteTextView homeStationInput;
    private TextView homeStationSummary;

    /** Full list of all tide stations loaded from the database. */
    private List<TideStation> allStations = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MainActivity activity = (MainActivity) requireActivity();
        userPreferences = activity.getUserPreferences();
        tideStationDb = activity.getTideStationDb();
        dbExecutor = activity.getDbExecutor();

        // --- Theme mode toggle ---
        themeToggleGroup = view.findViewById(R.id.toggle_theme_mode);

        // Select the button matching the persisted mode
        int savedMode = userPreferences.getThemeMode();
        themeToggleGroup.check(themeModeToButtonId(savedMode));

        themeToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return; // only act on the newly checked button
            int mode = buttonIdToThemeMode(checkedId);
            userPreferences.setThemeMode(mode);
            UserPreferences.applyThemeMode(mode);
        });

        // --- Station sort order toggle ---
        sortToggleGroup = view.findViewById(R.id.toggle_station_sort);

        StationSortOrder savedSortOrder = userPreferences.getStationSortOrder();
        sortToggleGroup.check(sortOrderToButtonId(savedSortOrder));

        sortToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            StationSortOrder order = buttonIdToSortOrder(checkedId);
            userPreferences.setStationSortOrder(order);
        });

        // --- Home location selector ---
        homeStationInput = view.findViewById(R.id.home_station_input);
        homeStationSummary = view.findViewById(R.id.home_station_summary);

        // Show current selection
        updateHomeStationSummary();

        // When a station is picked from the dropdown, persist and update summary
        homeStationInput.setOnItemClickListener((parent, v, position, id) -> {
            TideStation station = (TideStation) parent.getItemAtPosition(position);
            userPreferences.setHomeLocation(
                    station.id, station.name, station.latitude, station.longitude);
            homeStationInput.setText("", false);
            updateHomeStationSummary();
        });

        // Load tide stations for autocomplete
        loadTideStations();

        // --- Chart labels toggle ---
        chartLabelsSwitch = view.findViewById(R.id.switch_show_chart_labels);
        chartLabelsSummary = view.findViewById(R.id.chart_labels_summary);
        LinearLayout chartLabelsRow = view.findViewById(R.id.setting_show_chart_labels);

        boolean showLabels = userPreferences.isShowChartLabels();
        chartLabelsSwitch.setChecked(showLabels);
        updateChartLabelsSummary(showLabels);

        chartLabelsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            userPreferences.setShowChartLabels(isChecked);
            updateChartLabelsSummary(isChecked);
        });

        chartLabelsRow.setOnClickListener(v -> chartLabelsSwitch.toggle());

        // --- Metric units toggle ---
        metricSwitch = view.findViewById(R.id.switch_use_metric);
        metricSummary = view.findViewById(R.id.metric_summary);
        LinearLayout metricRow = view.findViewById(R.id.setting_use_metric);

        // Initialize from saved preference
        boolean isMetric = userPreferences.isMetric();
        metricSwitch.setChecked(isMetric);
        updateMetricSummary(isMetric);

        // Persist on toggle and update summary
        metricSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            userPreferences.setMetric(isChecked);
            updateMetricSummary(isChecked);
        });

        // Tapping anywhere on the row toggles the switch
        metricRow.setOnClickListener(v -> metricSwitch.toggle());
    }

    /**
     * Loads all tide stations from the database on a background thread,
     * then sets up the autocomplete adapter on the UI thread.
     */
    private void loadTideStations() {
        dbExecutor.execute(() -> {
            List<TideStation> stations = tideStationDb.queryAll();
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    StationSortOrder sortOrder = userPreferences.getStationSortOrder();
                    double homeLat = userPreferences.getHomeLatitude();
                    double homeLon = userPreferences.getHomeLongitude();
                    stations.sort(sortOrder.getComparator(homeLat, homeLon));
                    allStations = stations;
                    TideStationAdapter adapter = new TideStationAdapter(
                            requireContext(), allStations);
                    homeStationInput.setAdapter(adapter);
                });
            }
        });
    }

    /**
     * Updates the home station summary text from persisted preferences.
     */
    private void updateHomeStationSummary() {
        String name = userPreferences.getHomeStationName();
        if (name != null) {
            String stationId = userPreferences.getHomeStationId();
            homeStationSummary.setText(getString(
                    R.string.settings_home_station_summary, name, stationId));
        } else {
            homeStationSummary.setText(R.string.settings_home_station_none);
        }
    }

    private void updateChartLabelsSummary(boolean showLabels) {
        chartLabelsSummary.setText(showLabels
                ? R.string.settings_show_chart_labels_summary_on
                : R.string.settings_show_chart_labels_summary_off);
    }

    private void updateMetricSummary(boolean isMetric) {
        metricSummary.setText(isMetric
                ? R.string.settings_use_metric_summary_on
                : R.string.settings_use_metric_summary_off);
    }

    /**
     * Maps an {@link AppCompatDelegate} night-mode constant to the corresponding button ID.
     */
    private int themeModeToButtonId(int mode) {
        if (mode == AppCompatDelegate.MODE_NIGHT_NO) {
            return R.id.btn_theme_light;
        } else if (mode == AppCompatDelegate.MODE_NIGHT_YES) {
            return R.id.btn_theme_dark;
        }
        return R.id.btn_theme_system;
    }

    /**
     * Maps a toggle-group button ID to the corresponding {@link AppCompatDelegate} night-mode constant.
     */
    private int buttonIdToThemeMode(int buttonId) {
        if (buttonId == R.id.btn_theme_light) {
            return AppCompatDelegate.MODE_NIGHT_NO;
        } else if (buttonId == R.id.btn_theme_dark) {
            return AppCompatDelegate.MODE_NIGHT_YES;
        }
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }

    /**
     * Maps a {@link StationSortOrder} to the corresponding toggle button ID.
     */
    private int sortOrderToButtonId(StationSortOrder order) {
        return switch (order) {
            case ALPHABETICAL -> R.id.btn_sort_alphabetical;
            case LATITUDINAL -> R.id.btn_sort_latitudinal;
            case PROXIMAL -> R.id.btn_sort_proximal;
        };
    }

    /**
     * Maps a toggle-group button ID to the corresponding {@link StationSortOrder}.
     */
    private StationSortOrder buttonIdToSortOrder(int buttonId) {
        if (buttonId == R.id.btn_sort_alphabetical) {
            return StationSortOrder.ALPHABETICAL;
        } else if (buttonId == R.id.btn_sort_latitudinal) {
            return StationSortOrder.LATITUDINAL;
        } else if (buttonId == R.id.btn_sort_proximal) {
            return StationSortOrder.PROXIMAL;
        }
        return StationSortOrder.ALPHABETICAL;
    }

    // ========== Autocomplete Adapter ==========

    /**
     * Custom {@link ArrayAdapter} for tide station autocomplete.
     * Filters stations by name or ID substring match (case-insensitive),
     * mirroring the search behavior in {@link TideCatalogFragment}.
     */
    private static class TideStationAdapter extends ArrayAdapter<TideStation> {

        private final List<TideStation> allStations;
        private List<TideStation> filtered;

        TideStationAdapter(@NonNull Context context, @NonNull List<TideStation> stations) {
            super(context, android.R.layout.simple_dropdown_item_1line, new ArrayList<>(stations));
            this.allStations = stations;
            this.filtered = new ArrayList<>(stations);
        }

        @Override
        public int getCount() {
            return filtered.size();
        }

        @Nullable
        @Override
        public TideStation getItem(int position) {
            return filtered.get(position);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
            }
            TideStation station = getItem(position);
            TextView text = convertView.findViewById(android.R.id.text1);
            if (station != null) {
                String state = (station.state != null && !station.state.isEmpty())
                        ? ", " + station.state : "";
                text.setText(station.name + state + " (" + station.id + ")");
            }
            return convertView;
        }

        @NonNull
        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    if (constraint == null || constraint.length() == 0) {
                        results.values = new ArrayList<>(allStations);
                        results.count = allStations.size();
                    } else {
                        String query = constraint.toString().trim().toLowerCase(Locale.US);
                        List<TideStation> matched = new ArrayList<>();
                        for (TideStation station : allStations) {
                            String name = station.name != null
                                    ? station.name.toLowerCase(Locale.US) : "";
                            String id = station.id.toLowerCase(Locale.US);
                            if (name.contains(query) || id.contains(query)) {
                                matched.add(station);
                            }
                        }
                        results.values = matched;
                        results.count = matched.size();
                    }
                    return results;
                }

                @Override
                @SuppressWarnings("unchecked")
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    filtered = (List<TideStation>) results.values;
                    if (results.count > 0) {
                        notifyDataSetChanged();
                    } else {
                        notifyDataSetInvalidated();
                    }
                }

                @Override
                public CharSequence convertResultToString(Object resultValue) {
                    TideStation station = (TideStation) resultValue;
                    return station.name != null ? station.name : station.id;
                }
            };
        }
    }
}

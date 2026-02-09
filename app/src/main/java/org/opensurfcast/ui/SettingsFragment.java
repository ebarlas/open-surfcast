package org.opensurfcast.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.materialswitch.MaterialSwitch;

import org.opensurfcast.BuoyActivity;
import org.opensurfcast.R;
import org.opensurfcast.prefs.UserPreferences;

/**
 * Fragment displaying application settings.
 * <p>
 * Currently provides a toggle for metric vs imperial units.
 * Additional settings will be added here in the future.
 */
public class SettingsFragment extends Fragment {

    private UserPreferences userPreferences;
    private MaterialSwitch metricSwitch;
    private TextView metricSummary;

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

        BuoyActivity activity = (BuoyActivity) requireActivity();
        userPreferences = activity.getUserPreferences();

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

    private void updateMetricSummary(boolean isMetric) {
        metricSummary.setText(isMetric
                ? R.string.settings_use_metric_summary_on
                : R.string.settings_use_metric_summary_off);
    }
}

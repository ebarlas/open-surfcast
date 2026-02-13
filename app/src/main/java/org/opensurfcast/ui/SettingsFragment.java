package org.opensurfcast.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import org.opensurfcast.prefs.UserPreferences;

/**
 * Fragment displaying application settings.
 * <p>
 * Currently provides a toggle for metric vs imperial units.
 * Additional settings will be added here in the future.
 */
public class SettingsFragment extends Fragment {

    private UserPreferences userPreferences;
    private MaterialButtonToggleGroup themeToggleGroup;
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

        MainActivity activity = (MainActivity) requireActivity();
        userPreferences = activity.getUserPreferences();

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
}

package org.opensurfcast.ui;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.slider.Slider;

import org.opensurfcast.MainActivity;
import org.opensurfcast.R;
import org.opensurfcast.buoy.BuoySpecWaveData;
import org.opensurfcast.buoy.BuoyStdMetData;
import org.opensurfcast.db.BuoySpecWaveDataDb;
import org.opensurfcast.db.BuoyStdMetDataDb;
import org.opensurfcast.prefs.UserPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Fragment displaying a single metric chart in full-screen view with pan,
 * pinch-zoom, and a lookback-days slider. Opened when a user taps a chart
 * tile in {@link BuoyDetailFragment}.
 */
public class ChartFullScreenFragment extends Fragment {

    // -- Argument keys -----------------------------------------------------------

    private static final String ARG_STATION_ID = "station_id";
    private static final String ARG_METRIC_KEY = "metric_key";

    // -- Metric key constants (std met) ------------------------------------------

    public static final String METRIC_STD_WAVE_HEIGHT = "std_wave_height";
    public static final String METRIC_STD_DOM_WAVE_PERIOD = "std_dom_wave_period";
    public static final String METRIC_STD_AVG_WAVE_PERIOD = "std_avg_wave_period";
    public static final String METRIC_STD_WIND_SPEED_GUST = "std_wind_speed_gust";
    public static final String METRIC_STD_WIND_DIRECTION = "std_wind_direction";
    public static final String METRIC_STD_MEAN_WAVE_DIR = "std_mean_wave_direction";
    public static final String METRIC_STD_PRESSURE = "std_pressure";
    public static final String METRIC_STD_PRESSURE_TENDENCY = "std_pressure_tendency";
    public static final String METRIC_STD_AIR_TEMP = "std_air_temp";
    public static final String METRIC_STD_WATER_TEMP = "std_water_temp";
    public static final String METRIC_STD_DEW_POINT = "std_dew_point";
    public static final String METRIC_STD_VISIBILITY = "std_visibility";
    public static final String METRIC_STD_TIDE = "std_tide";

    // -- Metric key constants (spec wave) ----------------------------------------

    public static final String METRIC_SPEC_SWELL_HEIGHT = "spec_swell_height";
    public static final String METRIC_SPEC_SWELL_PERIOD = "spec_swell_period";
    public static final String METRIC_SPEC_WIND_WAVE_HEIGHT = "spec_wind_wave_height";
    public static final String METRIC_SPEC_WIND_WAVE_PERIOD = "spec_wind_wave_period";
    public static final String METRIC_SPEC_AVG_WAVE_PERIOD = "spec_avg_wave_period";
    public static final String METRIC_SPEC_MEAN_WAVE_DIR = "spec_mean_wave_direction";
    public static final String METRIC_SPEC_STEEPNESS = "spec_steepness";

    // -- Unit conversion constants -----------------------------------------------

    private static final double METERS_TO_FEET = 3.28084;
    private static final double FEET_TO_METERS = 0.3048;
    private static final double MPS_TO_MPH = 2.23694;
    private static final double HPA_TO_INHG = 0.02953;

    // -- Views -------------------------------------------------------------------

    private MaterialToolbar toolbar;
    private LinearProgressIndicator loadingProgress;
    private Slider lookbackSlider;
    private TextView lookbackValue;
    private FrameLayout chartContainer;

    // -- Dependencies & state ----------------------------------------------------

    private BuoyStdMetDataDb buoyStdMetDataDb;
    private BuoySpecWaveDataDb buoySpecWaveDataDb;
    private UserPreferences userPreferences;
    private ExecutorService dbExecutor;

    /** Full (unfiltered) data lists kept in memory for instant slider updates. */
    private List<BuoyStdMetData> allStdMetData;
    private List<BuoySpecWaveData> allSpecWaveData;

    /** The currently displayed chart view (to remove before rebuilding). */
    private View currentChart;

    /** Whether the fragment is in landscape immersive mode. */
    private boolean immersiveMode;

    // ========================================================================
    // Factory
    // ========================================================================

    /**
     * Creates a new instance for the given station and metric.
     *
     * @param stationId the buoy station ID
     * @param metricKey one of the {@code METRIC_*} constants
     * @return a configured fragment instance
     */
    public static ChartFullScreenFragment newInstance(String stationId, String metricKey) {
        ChartFullScreenFragment fragment = new ChartFullScreenFragment();
        Bundle args = new Bundle();
        args.putString(ARG_STATION_ID, stationId);
        args.putString(ARG_METRIC_KEY, metricKey);
        fragment.setArguments(args);
        return fragment;
    }

    // ========================================================================
    // Lifecycle
    // ========================================================================

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chart_fullscreen, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MainActivity activity = (MainActivity) requireActivity();
        buoyStdMetDataDb = activity.getBuoyStdMetDataDb();
        buoySpecWaveDataDb = activity.getBuoySpecWaveDataDb();
        userPreferences = activity.getUserPreferences();
        dbExecutor = activity.getDbExecutor();

        toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_reset_zoom) {
                resetZoom();
                return true;
            }
            return false;
        });

        loadingProgress = view.findViewById(R.id.loading_progress);
        lookbackSlider = view.findViewById(R.id.lookback_slider);
        lookbackValue = view.findViewById(R.id.lookback_value);
        chartContainer = view.findViewById(R.id.chart_container);

        // Initialise the lookback label
        updateLookbackLabel((int) lookbackSlider.getValue());

        lookbackSlider.addOnChangeListener((slider, value, fromUser) -> {
            int days = (int) value;
            updateLookbackLabel(days);
            if (fromUser) {
                rebuildChart();
            }
        });

        String stationId = requireArguments().getString(ARG_STATION_ID);
        String metricKey = requireArguments().getString(ARG_METRIC_KEY);
        toolbar.setTitle(titleForMetric(metricKey));

        loadData(stationId, metricKey);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            enterImmersiveMode(view);
        }
    }

    @Override
    public void onDestroyView() {
        if (immersiveMode) {
            exitImmersiveMode();
        }
        super.onDestroyView();
    }

    // ========================================================================
    // Immersive mode
    // ========================================================================

    private void enterImmersiveMode(View view) {
        immersiveMode = true;

        ((View) toolbar.getParent()).setVisibility(View.GONE);
        view.findViewById(R.id.lookback_row).setVisibility(View.GONE);
        loadingProgress.setVisibility(View.GONE);

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

    // ========================================================================
    // Data loading
    // ========================================================================

    private void loadData(String stationId, String metricKey) {
        loadingProgress.setVisibility(View.VISIBLE);

        dbExecutor.execute(() -> {
            if (metricKey.startsWith("std_")) {
                allStdMetData = buoyStdMetDataDb.queryByStation(stationId);
            } else {
                allSpecWaveData = buoySpecWaveDataDb.queryByStation(stationId);
            }

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    loadingProgress.setVisibility(View.GONE);
                    rebuildChart();
                });
            }
        });
    }

    // ========================================================================
    // Chart building
    // ========================================================================

    /**
     * Rebuilds the chart using the current lookback slider value and the
     * full in-memory data lists. Called on initial load and on slider change.
     */
    private void rebuildChart() {
        if (chartContainer == null) return;

        // Remove previous chart
        if (currentChart != null) {
            chartContainer.removeView(currentChart);
            currentChart = null;
        }

        String metricKey = requireArguments().getString(ARG_METRIC_KEY);
        boolean useMetric = userPreferences.isMetric();
        int lookbackDays = (int) lookbackSlider.getValue();
        long cutoff = System.currentTimeMillis() / 1000L - (long) lookbackDays * 86400L;

        View chart;

        switch (metricKey) {
            // ---- StdMet line charts ----
            case METRIC_STD_WAVE_HEIGHT:
                chart = buildStdMetLineChart(cutoff, "Wave Height",
                        useMetric ? "m" : "ft",
                        d -> d.getWaveHeight() == null ? null
                                : useMetric ? d.getWaveHeight() : d.getWaveHeight() * METERS_TO_FEET);
                break;

            case METRIC_STD_DOM_WAVE_PERIOD:
                chart = buildStdMetLineChart(cutoff, "Dominant Wave Period", "s",
                        d -> d.getDominantWavePeriod());
                break;

            case METRIC_STD_AVG_WAVE_PERIOD:
                chart = buildStdMetLineChart(cutoff, "Average Wave Period", "s",
                        d -> d.getAverageWavePeriod());
                break;

            case METRIC_STD_WIND_DIRECTION:
                chart = buildStdMetLineChart(cutoff, "Wind Direction", "°",
                        d -> d.getWindDirection() == null ? null : (double) d.getWindDirection());
                break;

            case METRIC_STD_MEAN_WAVE_DIR:
                chart = buildStdMetLineChart(cutoff, "Mean Wave Direction", "°",
                        d -> d.getMeanWaveDirection() == null ? null : (double) d.getMeanWaveDirection());
                break;

            case METRIC_STD_PRESSURE:
                chart = buildStdMetLineChart(cutoff, "Sea Level Pressure",
                        useMetric ? "hPa" : "inHg",
                        d -> d.getPressure() == null ? null
                                : useMetric ? d.getPressure() : d.getPressure() * HPA_TO_INHG);
                break;

            case METRIC_STD_AIR_TEMP:
                chart = buildStdMetLineChart(cutoff, "Air Temperature",
                        useMetric ? "°C" : "°F",
                        d -> d.getAirTemperature() == null ? null
                                : useMetric ? d.getAirTemperature()
                                : celsiusToFahrenheit(d.getAirTemperature()));
                break;

            case METRIC_STD_WATER_TEMP:
                chart = buildStdMetLineChart(cutoff, "Water Temperature",
                        useMetric ? "°C" : "°F",
                        d -> d.getWaterTemperature() == null ? null
                                : useMetric ? d.getWaterTemperature()
                                : celsiusToFahrenheit(d.getWaterTemperature()));
                break;

            case METRIC_STD_DEW_POINT:
                chart = buildStdMetLineChart(cutoff, "Dew Point",
                        useMetric ? "°C" : "°F",
                        d -> d.getDewPoint() == null ? null
                                : useMetric ? d.getDewPoint()
                                : celsiusToFahrenheit(d.getDewPoint()));
                break;

            case METRIC_STD_VISIBILITY:
                chart = buildStdMetLineChart(cutoff, "Visibility", "nmi",
                        d -> d.getVisibility());
                break;

            case METRIC_STD_TIDE:
                chart = buildStdMetLineChart(cutoff, "Tide",
                        useMetric ? "m" : "ft",
                        d -> d.getTide() == null ? null
                                : useMetric ? d.getTide() * FEET_TO_METERS : d.getTide());
                break;

            // ---- StdMet special charts ----
            case METRIC_STD_WIND_SPEED_GUST:
                chart = buildWindSpeedGustChart(cutoff, useMetric);
                break;

            case METRIC_STD_PRESSURE_TENDENCY:
                chart = buildPressureTendencyChart(cutoff, useMetric);
                break;

            // ---- SpecWave line charts ----
            case METRIC_SPEC_SWELL_HEIGHT:
                chart = buildSpecWaveLineChart(cutoff, "Swell Height",
                        useMetric ? "m" : "ft",
                        d -> d.getSwellHeight() == null ? null
                                : useMetric ? d.getSwellHeight() : d.getSwellHeight() * METERS_TO_FEET);
                break;

            case METRIC_SPEC_SWELL_PERIOD:
                chart = buildSpecWaveLineChart(cutoff, "Swell Period", "s",
                        d -> d.getSwellPeriod());
                break;

            case METRIC_SPEC_WIND_WAVE_HEIGHT:
                chart = buildSpecWaveLineChart(cutoff, "Wind Wave Height",
                        useMetric ? "m" : "ft",
                        d -> d.getWindWaveHeight() == null ? null
                                : useMetric ? d.getWindWaveHeight()
                                : d.getWindWaveHeight() * METERS_TO_FEET);
                break;

            case METRIC_SPEC_WIND_WAVE_PERIOD:
                chart = buildSpecWaveLineChart(cutoff, "Wind Wave Period", "s",
                        d -> d.getWindWavePeriod());
                break;

            case METRIC_SPEC_AVG_WAVE_PERIOD:
                chart = buildSpecWaveLineChart(cutoff, "Average Wave Period", "s",
                        d -> d.getAverageWavePeriod());
                break;

            case METRIC_SPEC_MEAN_WAVE_DIR:
                chart = buildSpecWaveLineChart(cutoff, "Mean Wave Direction", "°",
                        d -> d.getMeanWaveDirection() == null ? null
                                : (double) d.getMeanWaveDirection());
                break;

            // ---- SpecWave special charts ----
            case METRIC_SPEC_STEEPNESS:
                chart = buildSteepnessChart(cutoff);
                break;

            default:
                return;
        }

        if (chart != null) {
            currentChart = chart;
            chartContainer.addView(chart);
        }
    }

    // ========================================================================
    // StdMet line chart builder
    // ========================================================================

    private View buildStdMetLineChart(long cutoff, String label, String unit,
                                      Function<BuoyStdMetData, Double> extractor) {
        if (allStdMetData == null) return null;

        long baseEpoch = Long.MAX_VALUE;
        for (BuoyStdMetData d : allStdMetData) {
            if (d.getEpochSeconds() < cutoff) continue;
            if (extractor.apply(d) != null && d.getEpochSeconds() < baseEpoch)
                baseEpoch = d.getEpochSeconds();
        }
        if (baseEpoch == Long.MAX_VALUE) return null;

        List<Entry> entries = new ArrayList<>();
        for (BuoyStdMetData d : allStdMetData) {
            if (d.getEpochSeconds() < cutoff) continue;
            Double val = extractor.apply(d);
            if (val != null)
                entries.add(new Entry((float) (d.getEpochSeconds() - baseEpoch), val.floatValue()));
        }
        if (entries.isEmpty()) return null;

        int color = resolveColor(com.google.android.material.R.attr.colorPrimary);
        return createFullScreenLineChart(entries, label, color, null, null, null, unit, baseEpoch);
    }

    // ========================================================================
    // SpecWave line chart builder
    // ========================================================================

    private View buildSpecWaveLineChart(long cutoff, String label, String unit,
                                        Function<BuoySpecWaveData, Double> extractor) {
        if (allSpecWaveData == null) return null;

        long baseEpoch = Long.MAX_VALUE;
        for (BuoySpecWaveData d : allSpecWaveData) {
            if (d.getEpochSeconds() < cutoff) continue;
            if (extractor.apply(d) != null && d.getEpochSeconds() < baseEpoch)
                baseEpoch = d.getEpochSeconds();
        }
        if (baseEpoch == Long.MAX_VALUE) return null;

        List<Entry> entries = new ArrayList<>();
        for (BuoySpecWaveData d : allSpecWaveData) {
            if (d.getEpochSeconds() < cutoff) continue;
            Double val = extractor.apply(d);
            if (val != null)
                entries.add(new Entry((float) (d.getEpochSeconds() - baseEpoch), val.floatValue()));
        }
        if (entries.isEmpty()) return null;

        int color = resolveColor(com.google.android.material.R.attr.colorPrimary);
        return createFullScreenLineChart(entries, label, color, null, null, null, unit, baseEpoch);
    }

    // ========================================================================
    // Wind speed & gust (dual-line) chart builder
    // ========================================================================

    private View buildWindSpeedGustChart(long cutoff, boolean useMetric) {
        if (allStdMetData == null) return null;

        long baseEpoch = Long.MAX_VALUE;
        for (BuoyStdMetData d : allStdMetData) {
            if (d.getEpochSeconds() < cutoff) continue;
            if ((d.getWindSpeed() != null || d.getGustSpeed() != null) && d.getEpochSeconds() < baseEpoch)
                baseEpoch = d.getEpochSeconds();
        }
        if (baseEpoch == Long.MAX_VALUE) return null;

        List<Entry> speedEntries = new ArrayList<>();
        List<Entry> gustEntries = new ArrayList<>();
        for (BuoyStdMetData d : allStdMetData) {
            if (d.getEpochSeconds() < cutoff) continue;
            long epoch = d.getEpochSeconds();
            if (d.getWindSpeed() != null) {
                double speed = useMetric ? d.getWindSpeed() : d.getWindSpeed() * MPS_TO_MPH;
                speedEntries.add(new Entry((float) (epoch - baseEpoch), (float) speed));
            }
            if (d.getGustSpeed() != null) {
                double gust = useMetric ? d.getGustSpeed() : d.getGustSpeed() * MPS_TO_MPH;
                gustEntries.add(new Entry((float) (epoch - baseEpoch), (float) gust));
            }
        }
        if (speedEntries.isEmpty() && gustEntries.isEmpty()) return null;

        int primaryColor = resolveColor(com.google.android.material.R.attr.colorPrimary);
        int tertiaryColor = resolveColor(com.google.android.material.R.attr.colorTertiary);
        String windUnit = useMetric ? "m/s" : "mph";

        return createFullScreenLineChart(
                speedEntries, "Wind Speed", primaryColor,
                gustEntries, "Gust", tertiaryColor, windUnit, baseEpoch);
    }

    // ========================================================================
    // Pressure tendency (bar) chart builder
    // ========================================================================

    private View buildPressureTendencyChart(long cutoff, boolean useMetric) {
        if (allStdMetData == null) return null;

        long baseEpoch = Long.MAX_VALUE;
        for (BuoyStdMetData d : allStdMetData) {
            if (d.getEpochSeconds() < cutoff) continue;
            if (d.getPressureTendency() != null && d.getEpochSeconds() < baseEpoch)
                baseEpoch = d.getEpochSeconds();
        }
        if (baseEpoch == Long.MAX_VALUE) return null;

        List<BarEntry> entries = new ArrayList<>();
        for (BuoyStdMetData d : allStdMetData) {
            if (d.getEpochSeconds() < cutoff) continue;
            if (d.getPressureTendency() != null) {
                long epoch = d.getEpochSeconds();
                double value = useMetric ? d.getPressureTendency()
                        : d.getPressureTendency() * HPA_TO_INHG;
                entries.add(new BarEntry((float) (epoch - baseEpoch), (float) value));
            }
        }
        if (entries.isEmpty()) return null;

        int primaryColor = resolveColor(com.google.android.material.R.attr.colorPrimary);
        int tertiaryColor = resolveColor(com.google.android.material.R.attr.colorTertiary);

        BarChart chart = new BarChart(requireContext());
        chart.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        enableTouchControls(chart);
        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.setExtraBottomOffset(8f);
        chart.setExtraLeftOffset(4f);
        chart.setExtraRightOffset(4f);
        chart.setDrawBarShadow(false);
        chart.setDrawValueAboveBar(false);

        List<Integer> colors = new ArrayList<>();
        for (BarEntry entry : entries) {
            colors.add(entry.getY() >= 0 ? primaryColor : tertiaryColor);
        }

        BarDataSet dataSet = new BarDataSet(entries, "Pressure Tendency");
        dataSet.setColors(colors);
        dataSet.setDrawValues(false);

        BarData barData = new BarData(dataSet);
        if (entries.size() >= 2) {
            float spacing = entries.get(1).getX() - entries.get(0).getX();
            barData.setBarWidth(spacing * 0.9f);
        }

        chart.setData(barData);
        configureXAxis(chart, baseEpoch);
        configureYAxis(chart);

        String pressureUnit = useMetric ? "hPa" : "inHg";
        attachMarker(chart, createSignedValueMarker(pressureUnit, baseEpoch));
        chart.invalidate();

        return chart;
    }

    // ========================================================================
    // Steepness (scatter) chart builder
    // ========================================================================

    private View buildSteepnessChart(long cutoff) {
        if (allSpecWaveData == null) return null;

        long baseEpoch = Long.MAX_VALUE;
        for (BuoySpecWaveData d : allSpecWaveData) {
            if (d.getEpochSeconds() < cutoff) continue;
            if (steepnessToOrdinal(d.getSteepness()) > 0 && d.getEpochSeconds() < baseEpoch)
                baseEpoch = d.getEpochSeconds();
        }
        if (baseEpoch == Long.MAX_VALUE) return null;

        List<Entry> swellEntries = new ArrayList<>();
        List<Entry> averageEntries = new ArrayList<>();
        List<Entry> steepEntries = new ArrayList<>();
        List<Entry> verySteepEntries = new ArrayList<>();

        for (BuoySpecWaveData d : allSpecWaveData) {
            if (d.getEpochSeconds() < cutoff) continue;
            int ordinal = steepnessToOrdinal(d.getSteepness());
            if (ordinal <= 0) continue;
            Entry entry = new Entry((float) (d.getEpochSeconds() - baseEpoch), ordinal);
            switch (ordinal) {
                case 1: swellEntries.add(entry); break;
                case 2: averageEntries.add(entry); break;
                case 3: steepEntries.add(entry); break;
                case 4: verySteepEntries.add(entry); break;
            }
        }

        boolean hasAny = !swellEntries.isEmpty() || !averageEntries.isEmpty()
                || !steepEntries.isEmpty() || !verySteepEntries.isEmpty();
        if (!hasAny) return null;

        int swellColor = resolveColor(com.google.android.material.R.attr.colorPrimary);
        int avgColor = resolveColor(com.google.android.material.R.attr.colorSecondary);
        int steepColor = resolveColor(com.google.android.material.R.attr.colorTertiary);
        int vSteepColor = resolveColor(com.google.android.material.R.attr.colorError);

        ScatterChart chart = new ScatterChart(requireContext());
        chart.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        chart.getDescription().setEnabled(false);
        enableTouchControls(chart);
        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.setExtraBottomOffset(8f);
        chart.setExtraLeftOffset(4f);
        chart.setExtraRightOffset(4f);

        ScatterData scatterData = new ScatterData();
        if (!swellEntries.isEmpty())
            scatterData.addDataSet(createScatterDataSet(swellEntries, "Swell", swellColor));
        if (!averageEntries.isEmpty())
            scatterData.addDataSet(createScatterDataSet(averageEntries, "Average", avgColor));
        if (!steepEntries.isEmpty())
            scatterData.addDataSet(createScatterDataSet(steepEntries, "Steep", steepColor));
        if (!verySteepEntries.isEmpty())
            scatterData.addDataSet(createScatterDataSet(verySteepEntries, "Very Steep", vSteepColor));

        chart.setData(scatterData);

        Legend legend = chart.getLegend();
        legend.setEnabled(true);
        legend.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface));
        legend.setTextSize(12f);
        legend.setWordWrapEnabled(true);

        configureXAxis(chart, baseEpoch);

        // Custom Y-axis: fixed range 1-4 with category labels
        int axisTextColor = resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant);
        int gridColor = resolveColor(com.google.android.material.R.attr.colorOutlineVariant);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(axisTextColor);
        leftAxis.setTextSize(11f);
        leftAxis.setGridColor(gridColor);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setAxisMinimum(0.5f);
        leftAxis.setAxisMaximum(4.5f);
        leftAxis.setLabelCount(4, false);
        leftAxis.setGranularity(1f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return ordinalToSteepness(Math.round(value));
            }
        });

        chart.getAxisRight().setEnabled(false);

        ChartMarkerView marker = new ChartMarkerView(requireContext(),
                yValue -> ordinalToSteepness(Math.round(yValue)), baseEpoch);
        marker.setChartView(chart);
        chart.setMarker(marker);

        chart.invalidate();

        return chart;
    }

    // ========================================================================
    // Shared chart helpers
    // ========================================================================

    /**
     * Creates a full-screen LineChart with optional second dataset.
     * Entry X values are seconds since baseEpochSeconds for float precision.
     */
    private View createFullScreenLineChart(List<Entry> entries, String label, int color,
                                           @Nullable List<Entry> secondEntries,
                                           @Nullable String secondLabel,
                                           @Nullable Integer secondColor,
                                           String unit, long baseEpochSeconds) {
        LineChart chart = new LineChart(requireContext());
        chart.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        enableTouchControls(chart);
        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.setExtraBottomOffset(8f);
        chart.setExtraLeftOffset(4f);
        chart.setExtraRightOffset(4f);

        LineData lineData = new LineData();

        if (entries != null && !entries.isEmpty()) {
            LineDataSet dataSet = createLineDataSet(entries, label, color);
            lineData.addDataSet(dataSet);
        }

        if (secondEntries != null && secondColor != null && !secondEntries.isEmpty()) {
            LineDataSet secondSet = createLineDataSet(secondEntries,
                    secondLabel != null ? secondLabel : label, secondColor);
            secondSet.enableDashedLine(10f, 5f, 0f);
            secondSet.setDrawFilled(false);
            lineData.addDataSet(secondSet);

            // Show legend for dual-line charts
            Legend legend = chart.getLegend();
            legend.setEnabled(true);
            legend.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface));
            legend.setTextSize(12f);
        }

        chart.setData(lineData);
        configureXAxis(chart, baseEpochSeconds);
        configureYAxis(chart);
        attachMarker(chart, createValueMarker(unit, baseEpochSeconds));
        chart.invalidate();

        return chart;
    }

    private void enableTouchControls(BarLineChartBase<?> chart) {
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(false);  // false = independent X/Y axis scaling
        chart.setDoubleTapToZoomEnabled(true);
    }

    private void enableTouchControls(ScatterChart chart) {
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(false);  // false = independent X/Y axis scaling
        chart.setDoubleTapToZoomEnabled(true);
    }

    /**
     * Creates a marker that formats values as "5.2 unit".
     */
    private ChartMarkerView createValueMarker(String unit, long baseEpochSeconds) {
        return new ChartMarkerView(requireContext(), yValue -> {
            String formatted = formatValue(yValue);
            return unit != null && !unit.isEmpty() ? formatted + " " + unit : formatted;
        }, baseEpochSeconds);
    }

    /**
     * Creates a marker that formats signed values as "+0.3 unit".
     */
    private ChartMarkerView createSignedValueMarker(String unit, long baseEpochSeconds) {
        return new ChartMarkerView(requireContext(), yValue -> {
            String formatted = formatSignedValue(yValue);
            return unit != null && !unit.isEmpty() ? formatted + " " + unit : formatted;
        }, baseEpochSeconds);
    }

    /**
     * Attaches a marker view to a BarLineChartBase chart.
     */
    private void attachMarker(BarLineChartBase<?> chart, ChartMarkerView marker) {
        marker.setChartView(chart);
        chart.setMarker(marker);
    }

    private LineDataSet createLineDataSet(List<Entry> entries, String label, int color) {
        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(color);
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.LINEAR);
        dataSet.setDrawFilled(true);

        int fillColor = Color.argb(38, Color.red(color), Color.green(color), Color.blue(color));
        dataSet.setFillColor(fillColor);
        dataSet.setFillAlpha(255);

        dataSet.setHighLightColor(resolveColor(com.google.android.material.R.attr.colorTertiary));
        dataSet.setHighlightLineWidth(1.5f);
        dataSet.setDrawHorizontalHighlightIndicator(false);

        return dataSet;
    }

    private ScatterDataSet createScatterDataSet(List<Entry> entries, String label, int color) {
        ScatterDataSet dataSet = new ScatterDataSet(entries, label);
        dataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        dataSet.setScatterShapeSize(18f);
        dataSet.setColor(color);
        dataSet.setDrawValues(false);
        dataSet.setHighLightColor(resolveColor(com.google.android.material.R.attr.colorTertiary));
        return dataSet;
    }

    private void configureXAxis(BarLineChartBase<?> chart, long baseEpochSeconds) {
        int axisTextColor = resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant);
        int gridColor = resolveColor(com.google.android.material.R.attr.colorOutlineVariant);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(axisTextColor);
        xAxis.setTextSize(11f);
        xAxis.setGridColor(gridColor);
        xAxis.setDrawAxisLine(false);

        float xMin = chart.getData() != null ? chart.getData().getXMin() : 0;
        float xMax = chart.getData() != null ? chart.getData().getXMax() : 0;
        float rangeSeconds = xMax - xMin;
        boolean shortRange = rangeSeconds <= 2 * 24 * 3600;

        xAxis.setLabelCount(shortRange ? 3 : 5, false);
        String pattern = shortRange ? "M/d h:mm a" : "MMM d";
        xAxis.setGranularity(shortRange ? 3600f : 86400f);

        final long base = baseEpochSeconds;
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat fmt = new SimpleDateFormat(pattern, Locale.getDefault());

            {
                fmt.setTimeZone(TimeZone.getDefault());
            }

            @Override
            public String getFormattedValue(float value) {
                return fmt.format(new Date((base + (long) value) * 1000L));
            }
        });
    }

    private void configureYAxis(BarLineChartBase<?> chart) {
        int axisTextColor = resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant);
        int gridColor = resolveColor(com.google.android.material.R.attr.colorOutlineVariant);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(axisTextColor);
        leftAxis.setTextSize(11f);
        leftAxis.setGridColor(gridColor);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setLabelCount(6, false);

        chart.getAxisRight().setEnabled(false);
    }

    // ========================================================================
    // Steepness helpers
    // ========================================================================

    private static int steepnessToOrdinal(String steepness) {
        if (steepness == null) return -1;
        switch (steepness) {
            case "SWELL":      return 1;
            case "AVERAGE":    return 2;
            case "STEEP":      return 3;
            case "VERY_STEEP": return 4;
            default:           return -1;
        }
    }

    private static String ordinalToSteepness(int ordinal) {
        switch (ordinal) {
            case 1: return "Swell";
            case 2: return "Average";
            case 3: return "Steep";
            case 4: return "Very Steep";
            default: return "";
        }
    }

    // ========================================================================
    // Reset zoom
    // ========================================================================

    private void resetZoom() {
        if (currentChart instanceof BarLineChartBase) {
            ((BarLineChartBase<?>) currentChart).fitScreen();
        } else if (currentChart instanceof ScatterChart) {
            ((ScatterChart) currentChart).fitScreen();
        }
    }

    // ========================================================================
    // Lookback helpers
    // ========================================================================

    private void updateLookbackLabel(int days) {
        lookbackValue.setText(getString(R.string.chart_fullscreen_lookback_days, days));
    }

    // ========================================================================
    // Metric key → display title
    // ========================================================================

    private String titleForMetric(String metricKey) {
        switch (metricKey) {
            case METRIC_STD_WAVE_HEIGHT:      return "Wave Height";
            case METRIC_STD_DOM_WAVE_PERIOD:  return "Dominant Wave Period";
            case METRIC_STD_AVG_WAVE_PERIOD:  return "Average Wave Period";
            case METRIC_STD_WIND_SPEED_GUST:  return "Wind Speed & Gust";
            case METRIC_STD_WIND_DIRECTION:   return "Wind Direction";
            case METRIC_STD_MEAN_WAVE_DIR:    return "Mean Wave Direction";
            case METRIC_STD_PRESSURE:         return "Sea Level Pressure";
            case METRIC_STD_PRESSURE_TENDENCY: return "Pressure Tendency";
            case METRIC_STD_AIR_TEMP:         return "Air Temperature";
            case METRIC_STD_WATER_TEMP:       return "Water Temperature";
            case METRIC_STD_DEW_POINT:        return "Dew Point";
            case METRIC_STD_VISIBILITY:       return "Visibility";
            case METRIC_STD_TIDE:             return "Tide";
            case METRIC_SPEC_SWELL_HEIGHT:    return "Swell Height";
            case METRIC_SPEC_SWELL_PERIOD:    return "Swell Period";
            case METRIC_SPEC_WIND_WAVE_HEIGHT: return "Wind Wave Height";
            case METRIC_SPEC_WIND_WAVE_PERIOD: return "Wind Wave Period";
            case METRIC_SPEC_AVG_WAVE_PERIOD: return "Average Wave Period";
            case METRIC_SPEC_MEAN_WAVE_DIR:   return "Mean Wave Direction";
            case METRIC_SPEC_STEEPNESS:       return "Steepness";
            default:                          return "Chart";
        }
    }

    // ========================================================================
    // Utility
    // ========================================================================

    private static double celsiusToFahrenheit(double celsius) {
        return celsius * 9.0 / 5.0 + 32.0;
    }

    private int resolveColor(int attr) {
        return MaterialColors.getColor(requireView(), attr);
    }

    private static String formatValue(float value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.US, "%.1f", value);
    }

    private static String formatSignedValue(float value) {
        String formatted = formatValue(value);
        if (value > 0) {
            return "+" + formatted;
        }
        return formatted;
    }
}

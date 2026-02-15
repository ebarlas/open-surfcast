package org.opensurfcast.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.CombinedData;
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
import org.opensurfcast.db.CurrentPredictionDb;
import org.opensurfcast.db.CurrentStationDb;
import org.opensurfcast.prefs.UserPreferences;
import org.opensurfcast.tide.CurrentPrediction;
import org.opensurfcast.tide.CurrentStation;
import org.opensurfcast.tide.CurrentVelocityInterpolator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

/**
 * Fragment displaying a detailed current velocity chart for a current station.
 * <p>
 * Shows an interpolated velocity curve sampled at 20-minute intervals,
 * with visible markers at each max-flood and max-ebb prediction point.
 * Supports viewing up to 7 days into the future.
 */
public class CurrentDetailFragment extends Fragment {

    // -- Argument keys -----------------------------------------------------------

    private static final String ARG_STATION_ID = "station_id";

    // -- Unit conversion constants -----------------------------------------------

    private static final double CM_PER_SEC_TO_KNOTS = 0.0194384;

    // -- Sampling interval -------------------------------------------------------

    private static final long SAMPLE_INTERVAL_SECONDS = 1200; // 20 minutes

    // -- Views -------------------------------------------------------------------

    private MaterialToolbar toolbar;
    private LinearProgressIndicator loadingProgress;
    private Slider windowSizeSlider;
    private TextView windowSizeValue;
    private Slider centerDateSlider;
    private TextView centerDateValue;
    private FrameLayout chartContainer;

    // -- Dependencies & state ----------------------------------------------------

    private CurrentStationDb currentStationDb;
    private CurrentPredictionDb currentPredictionDb;
    private UserPreferences userPreferences;
    private ExecutorService dbExecutor;

    /** Full (unfiltered) current predictions kept in memory for instant slider updates. */
    private List<CurrentPrediction> allPredictions;

    /** The currently displayed chart view (to remove before rebuilding). */
    private View currentChart;

    /** Station ID kept as field for reference. */
    private String stationId;

    // ========================================================================
    // Factory
    // ========================================================================

    /**
     * Creates a new instance for the given current station.
     *
     * @param stationId the current station ID
     * @return a configured fragment instance
     */
    public static CurrentDetailFragment newInstance(String stationId) {
        CurrentDetailFragment fragment = new CurrentDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_STATION_ID, stationId);
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
        return inflater.inflate(R.layout.fragment_current_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MainActivity activity = (MainActivity) requireActivity();
        currentStationDb = activity.getCurrentStationDb();
        currentPredictionDb = activity.getCurrentPredictionDb();
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
        windowSizeSlider = view.findViewById(R.id.window_size_slider);
        windowSizeValue = view.findViewById(R.id.window_size_value);
        centerDateSlider = view.findViewById(R.id.center_date_slider);
        centerDateValue = view.findViewById(R.id.center_date_value);
        chartContainer = view.findViewById(R.id.chart_container);

        // Initialize the labels
        updateWindowSizeLabel((int) windowSizeSlider.getValue());
        updateCenterDateLabel((int) centerDateSlider.getValue());

        windowSizeSlider.addOnChangeListener((slider, value, fromUser) -> {
            int days = (int) value;
            updateWindowSizeLabel(days);
            if (fromUser) {
                rebuildChart();
            }
        });

        centerDateSlider.addOnChangeListener((slider, value, fromUser) -> {
            int days = (int) value;
            updateCenterDateLabel(days);
            if (fromUser) {
                rebuildChart();
            }
        });

        stationId = requireArguments().getString(ARG_STATION_ID);

        loadData(stationId);
    }

    // ========================================================================
    // Data loading
    // ========================================================================

    private void loadData(String stationId) {
        loadingProgress.setVisibility(View.VISIBLE);

        dbExecutor.execute(() -> {
            CurrentStation station = currentStationDb.queryById(stationId);
            allPredictions = currentPredictionDb.queryByStation(stationId);

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    loadingProgress.setVisibility(View.GONE);

                    // Set toolbar title to station name
                    if (station != null && station.name != null) {
                        toolbar.setTitle(station.name);
                        toolbar.setSubtitle(stationId);
                    } else {
                        toolbar.setTitle(stationId);
                    }

                    rebuildChart();
                });
            }
        });
    }

    // ========================================================================
    // Chart building
    // ========================================================================

    /**
     * Rebuilds the chart using the current time range slider value and the
     * full in-memory predictions list. Called on initial load and on slider change.
     */
    private void rebuildChart() {
        if (chartContainer == null || allPredictions == null) return;

        // Remove previous chart
        if (currentChart != null) {
            chartContainer.removeView(currentChart);
            currentChart = null;
        }

        if (allPredictions.isEmpty()) {
            showEmptyState();
            return;
        }

        boolean useMetric = userPreferences.isMetric();
        int windowSizeDays = (int) windowSizeSlider.getValue();
        int centerDaysOffset = (int) centerDateSlider.getValue();

        // Calculate time window centered on the specified date
        long nowEpochSeconds = System.currentTimeMillis() / 1000L;
        long centerEpochSeconds = nowEpochSeconds + (long) centerDaysOffset * 86400L;
        long halfWindowSeconds = (long) (windowSizeDays / 2.0 * 86400L);

        long startEpochSeconds = centerEpochSeconds - halfWindowSeconds;
        long endEpochSeconds = centerEpochSeconds + halfWindowSeconds;

        // Generate interpolated samples
        List<Entry> interpolatedEntries = generateInterpolatedSamples(
                startEpochSeconds, endEpochSeconds, useMetric);

        // Collect max-flood, max-ebb, and slack markers within the time window
        List<Entry> floodEntries = new ArrayList<>();
        List<Entry> ebbEntries = new ArrayList<>();
        List<Entry> slackEntries = new ArrayList<>();

        for (CurrentPrediction pred : allPredictions) {
            if (pred.epochSeconds >= startEpochSeconds && pred.epochSeconds <= endEpochSeconds) {
                if (pred.isFlood()) {
                    double displayValue = useMetric ? pred.velocityMajor
                            : pred.velocityMajor * CM_PER_SEC_TO_KNOTS;
                    floodEntries.add(new Entry(pred.epochSeconds, (float) displayValue));
                } else if (pred.isEbb()) {
                    double displayValue = useMetric ? pred.velocityMajor
                            : pred.velocityMajor * CM_PER_SEC_TO_KNOTS;
                    ebbEntries.add(new Entry(pred.epochSeconds, (float) displayValue));
                } else if (pred.isSlack()) {
                    // Curve now passes through zero exactly at slack time
                    slackEntries.add(new Entry(pred.epochSeconds, 0f));
                }
            }
        }

        if (interpolatedEntries.isEmpty() && floodEntries.isEmpty()
                && ebbEntries.isEmpty() && slackEntries.isEmpty()) {
            showEmptyState();
            return;
        }

        View chart = createCombinedChart(interpolatedEntries, floodEntries, ebbEntries,
                slackEntries, useMetric);

        if (chart != null) {
            currentChart = chart;
            chartContainer.addView(chart);
        }
    }

    /**
     * Generates interpolated current velocity samples at 20-minute intervals.
     */
    private List<Entry> generateInterpolatedSamples(long startEpoch, long endEpoch,
                                                     boolean useMetric) {
        List<Entry> entries = new ArrayList<>();

        for (long t = startEpoch; t <= endEpoch; t += SAMPLE_INTERVAL_SECONDS) {
            Double velocity = CurrentVelocityInterpolator.interpolate(allPredictions, t);
            if (velocity != null) {
                double displayValue = useMetric ? velocity : velocity * CM_PER_SEC_TO_KNOTS;
                entries.add(new Entry(t, (float) displayValue));
            }
        }

        return entries;
    }

    /**
     * Creates a CombinedChart with interpolated velocity line,
     * max-flood / max-ebb scatter markers, and slack time markers.
     */
    private View createCombinedChart(List<Entry> lineEntries,
                                     List<Entry> floodEntries,
                                     List<Entry> ebbEntries,
                                     List<Entry> slackEntries,
                                     boolean useMetric) {
        CombinedChart chart = new CombinedChart(requireContext());
        chart.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        chart.getDescription().setEnabled(false);
        chart.setDrawOrder(new CombinedChart.DrawOrder[]{
                CombinedChart.DrawOrder.LINE,
                CombinedChart.DrawOrder.SCATTER
        });

        enableTouchControls(chart);
        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.setExtraBottomOffset(8f);
        chart.setExtraLeftOffset(4f);
        chart.setExtraRightOffset(4f);

        CombinedData combinedData = new CombinedData();

        // Add line data (interpolated curve)
        if (!lineEntries.isEmpty()) {
            LineData lineData = new LineData();
            LineDataSet lineSet = createLineDataSet(lineEntries, "");
            lineData.addDataSet(lineSet);
            combinedData.setData(lineData);
        }

        // Add scatter data (max-flood, max-ebb, and slack markers)
        ScatterData scatterData = new ScatterData();
        boolean hasScatter = false;

        if (!floodEntries.isEmpty()) {
            int floodColor = resolveColor(com.google.android.material.R.attr.colorPrimary);
            ScatterDataSet floodSet = createScatterDataSet(floodEntries, "", floodColor);
            scatterData.addDataSet(floodSet);
            hasScatter = true;
        }

        if (!ebbEntries.isEmpty()) {
            int ebbColor = resolveColor(com.google.android.material.R.attr.colorTertiary);
            ScatterDataSet ebbSet = createScatterDataSet(ebbEntries, "", ebbColor);
            scatterData.addDataSet(ebbSet);
            hasScatter = true;
        }

        if (!slackEntries.isEmpty()) {
            int slackColor = resolveColor(com.google.android.material.R.attr.colorOutline);
            ScatterDataSet slackSet = createScatterDataSet(
                    slackEntries, "", slackColor);
            slackSet.setScatterShapeSize(10f);
            slackSet.setScatterShape(ScatterChart.ScatterShape.SQUARE);
            scatterData.addDataSet(slackSet);
            hasScatter = true;
        }

        if (hasScatter) {
            combinedData.setData(scatterData);
        }

        chart.setData(combinedData);

        // Disable legend (no labels to show)
        Legend legend = chart.getLegend();
        legend.setEnabled(false);

        configureXAxis(chart);
        addCurrentTimeLine(chart);
        configureYAxis(chart);

        String unit = useMetric ? "cm/s" : "kn";
        attachMarker(chart, createValueMarker(unit));

        chart.invalidate();

        return chart;
    }

    private LineDataSet createLineDataSet(List<Entry> entries, String label) {
        int lineColor = resolveColor(com.google.android.material.R.attr.colorPrimary);

        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(lineColor);
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.15f);
        dataSet.setDrawFilled(false);

        dataSet.setHighLightColor(resolveColor(com.google.android.material.R.attr.colorTertiary));
        dataSet.setHighlightLineWidth(1.5f);
        dataSet.setDrawHorizontalHighlightIndicator(false);

        return dataSet;
    }

    private ScatterDataSet createScatterDataSet(List<Entry> entries, String label, int color) {
        ScatterDataSet dataSet = new ScatterDataSet(entries, label);
        dataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        dataSet.setScatterShapeSize(12f);
        dataSet.setColor(color);
        dataSet.setDrawValues(false);
        dataSet.setHighLightColor(resolveColor(com.google.android.material.R.attr.colorTertiary));
        return dataSet;
    }

    private void configureXAxis(CombinedChart chart) {
        int axisTextColor = resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant);
        int gridColor = resolveColor(com.google.android.material.R.attr.colorOutlineVariant);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(axisTextColor);
        xAxis.setTextSize(11f);
        xAxis.setGridColor(gridColor);
        xAxis.setDrawAxisLine(false);
        xAxis.setLabelCount(5, false);

        float xMin = chart.getData() != null ? chart.getData().getXMin() : 0;
        float xMax = chart.getData() != null ? chart.getData().getXMax() : 0;
        float rangeSeconds = xMax - xMin;
        boolean shortRange = rangeSeconds <= 2 * 24 * 3600;

        String pattern = shortRange ? "MMM d HH:mm" : "MMM d";
        xAxis.setGranularity(shortRange ? 3600f : 86400f);

        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat fmt = new SimpleDateFormat(pattern, Locale.US);

            @Override
            public String getFormattedValue(float value) {
                return fmt.format(new Date((long) value * 1000L));
            }
        });
    }

    private void configureYAxis(CombinedChart chart) {
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

    /**
     * Adds a vertical dashed line at the current time on the X-axis.
     */
    private void addCurrentTimeLine(CombinedChart chart) {
        float nowEpochSeconds = System.currentTimeMillis() / 1000f;

        int lineColor = resolveColor(com.google.android.material.R.attr.colorError);

        LimitLine nowLine = new LimitLine(nowEpochSeconds);
        nowLine.setLineColor(lineColor);
        nowLine.setLineWidth(1.5f);
        nowLine.enableDashedLine(10f, 6f, 0f);
        nowLine.setLabel(null);

        chart.getXAxis().addLimitLine(nowLine);
    }

    private void enableTouchControls(CombinedChart chart) {
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(false);
        chart.setDoubleTapToZoomEnabled(true);
    }

    /**
     * Creates a marker that formats values as "5.2 unit".
     */
    private ChartMarkerView createValueMarker(String unit) {
        return new ChartMarkerView(requireContext(), yValue -> {
            String formatted = formatValue(yValue);
            return unit != null && !unit.isEmpty() ? formatted + " " + unit : formatted;
        });
    }

    /**
     * Attaches a marker view to the chart.
     */
    private void attachMarker(CombinedChart chart, ChartMarkerView marker) {
        marker.setChartView(chart);
        chart.setMarker(marker);
    }

    // ========================================================================
    // Empty state
    // ========================================================================

    private void showEmptyState() {
        TextView empty = new TextView(requireContext());
        empty.setText(R.string.no_data_available);
        empty.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
        empty.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
        empty.setGravity(android.view.Gravity.CENTER);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        empty.setLayoutParams(params);

        currentChart = empty;
        chartContainer.addView(empty);
    }

    // ========================================================================
    // Reset zoom
    // ========================================================================

    private void resetZoom() {
        if (currentChart instanceof CombinedChart) {
            ((CombinedChart) currentChart).fitScreen();
        }
    }

    // ========================================================================
    // Slider helpers
    // ========================================================================

    private void updateWindowSizeLabel(int days) {
        if (days == 1) {
            windowSizeValue.setText(getString(R.string.current_detail_window_day));
        } else {
            windowSizeValue.setText(getString(R.string.current_detail_window_days, days));
        }
    }

    private void updateCenterDateLabel(int days) {
        if (days == 0) {
            centerDateValue.setText(getString(R.string.current_detail_time_now));
        } else if (days < 0) {
            int absDays = Math.abs(days);
            if (absDays == 1) {
                centerDateValue.setText(getString(R.string.current_detail_time_day_ago));
            } else {
                centerDateValue.setText(getString(R.string.current_detail_time_days_ago, absDays));
            }
        } else {
            if (days == 1) {
                centerDateValue.setText(getString(R.string.current_detail_time_day_ahead));
            } else {
                centerDateValue.setText(getString(R.string.current_detail_time_days_ahead, days));
            }
        }
    }

    // ========================================================================
    // Utility methods
    // ========================================================================

    private int resolveColor(int attr) {
        return MaterialColors.getColor(requireView(), attr);
    }

    private static String formatValue(float value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.US, "%.1f", value);
    }
}

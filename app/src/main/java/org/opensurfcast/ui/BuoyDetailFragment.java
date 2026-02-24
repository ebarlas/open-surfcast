package org.opensurfcast.ui;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.AxisBase;
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
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.opensurfcast.MainActivity;
import org.opensurfcast.R;
import org.opensurfcast.buoy.BuoySpecWaveData;
import org.opensurfcast.buoy.BuoyStation;
import org.opensurfcast.buoy.BuoyStdMetData;
import org.opensurfcast.db.BuoySpecWaveDataDb;
import org.opensurfcast.db.BuoyStationDb;
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
 * Fragment displaying detailed metric tiles with time-series charts for a
 * single buoy station. Each available metric from both standard meteorological
 * data and spectral wave data is shown as an individual card with an
 * MPAndroidChart visualization.
 */
public class BuoyDetailFragment extends Fragment {

    private static final String ARG_STATION_ID = "station_id";
    /** Default visible X range in seconds (2 days). */
    private static final float DEFAULT_VISIBLE_X_RANGE_SECONDS = 2 * 86400f;

    private static final double METERS_TO_FEET = 3.28084;
    private static final double FEET_TO_METERS = 0.3048;
    private static final double MPS_TO_MPH = 2.23694;
    private static final double HPA_TO_INHG = 0.02953;

    private LinearLayout metricContainer;
    private LinearProgressIndicator loadingProgress;
    private MaterialToolbar toolbar;

    private BuoyStationDb buoyStationDb;
    private BuoyStdMetDataDb buoyStdMetDataDb;
    private BuoySpecWaveDataDb buoySpecWaveDataDb;
    private UserPreferences userPreferences;
    private ExecutorService dbExecutor;

    /** Station ID kept as a field so tile click listeners can reference it. */
    private String stationId;

    /**
     * Creates a new instance of BuoyDetailFragment for the given station.
     *
     * @param stationId the buoy station ID to display
     * @return a configured fragment instance
     */
    public static BuoyDetailFragment newInstance(String stationId) {
        BuoyDetailFragment fragment = new BuoyDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_STATION_ID, stationId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_buoy_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MainActivity activity = (MainActivity) requireActivity();
        buoyStationDb = activity.getBuoyStationDb();
        buoyStdMetDataDb = activity.getBuoyStdMetDataDb();
        buoySpecWaveDataDb = activity.getBuoySpecWaveDataDb();
        userPreferences = activity.getUserPreferences();
        dbExecutor = activity.getDbExecutor();

        toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        loadingProgress = view.findViewById(R.id.loading_progress);
        metricContainer = view.findViewById(R.id.metric_container);

        stationId = requireArguments().getString(ARG_STATION_ID);
        toolbar.setTitle(stationId);

        loadData(stationId);
    }

    /**
     * Loads station info, std met data, and spec wave data on a background thread,
     * then builds all metric tiles on the main thread.
     */
    private void loadData(String stationId) {
        loadingProgress.setVisibility(View.VISIBLE);

        dbExecutor.execute(() -> {
            BuoyStation station = buoyStationDb.queryById(stationId);
            List<BuoyStdMetData> stdMetList = buoyStdMetDataDb.queryByStation(stationId);
            List<BuoySpecWaveData> specWaveList = buoySpecWaveDataDb.queryByStation(stationId);

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    loadingProgress.setVisibility(View.GONE);

                    // Set toolbar title to station name
                    if (station != null && station.getName() != null) {
                        toolbar.setTitle(station.getName());
                        toolbar.setSubtitle(stationId);
                    }

                    boolean useMetric = userPreferences.isMetric();
                    buildStdMetSection(stdMetList, useMetric);
                    buildSpecWaveSection(specWaveList, useMetric);

                    if (stdMetList.isEmpty() && specWaveList.isEmpty()) {
                        addEmptyState();
                    }
                });
            }
        });
    }

    // ========================================================================
    // Standard Meteorological Data section
    // ========================================================================

    private void buildStdMetSection(List<BuoyStdMetData> dataList, boolean useMetric) {
        if (dataList.isEmpty()) return;

        addSectionHeader(getString(R.string.section_std_met));

        // Wave Height
        addStdMetLineChart(dataList, "Wave Height",
                useMetric ? "m" : "ft",
                d -> {
                    if (d.getWaveHeight() == null) return null;
                    return useMetric ? d.getWaveHeight() : d.getWaveHeight() * METERS_TO_FEET;
                },
                BuoyStdMetData::getEpochSeconds, 0,
                ChartFullScreenFragment.METRIC_STD_WAVE_HEIGHT);

        // Dominant Wave Period
        addStdMetLineChart(dataList, "Dominant Wave Period", "s",
                d -> d.getDominantWavePeriod() == null ? null : d.getDominantWavePeriod(),
                BuoyStdMetData::getEpochSeconds, 0,
                ChartFullScreenFragment.METRIC_STD_DOM_WAVE_PERIOD);

        // Average Wave Period
        addStdMetLineChart(dataList, "Average Wave Period", "s",
                d -> d.getAverageWavePeriod() == null ? null : d.getAverageWavePeriod(),
                BuoyStdMetData::getEpochSeconds, 0,
                ChartFullScreenFragment.METRIC_STD_AVG_WAVE_PERIOD);

        // Wind Speed & Gust (dual-line chart)
        addWindSpeedGustChart(dataList, useMetric);

        // Wind Direction
        addStdMetLineChart(dataList, "Wind Direction", "°",
                d -> d.getWindDirection() == null ? null : (double) d.getWindDirection(),
                BuoyStdMetData::getEpochSeconds, 0,
                ChartFullScreenFragment.METRIC_STD_WIND_DIRECTION);

        // Mean Wave Direction
        addStdMetLineChart(dataList, "Mean Wave Direction", "°",
                d -> d.getMeanWaveDirection() == null ? null : (double) d.getMeanWaveDirection(),
                BuoyStdMetData::getEpochSeconds, 0,
                ChartFullScreenFragment.METRIC_STD_MEAN_WAVE_DIR);

        // Sea Level Pressure
        addStdMetLineChart(dataList, "Sea Level Pressure",
                useMetric ? "hPa" : "inHg",
                d -> {
                    if (d.getPressure() == null) return null;
                    return useMetric ? d.getPressure() : d.getPressure() * HPA_TO_INHG;
                },
                BuoyStdMetData::getEpochSeconds, 0,
                ChartFullScreenFragment.METRIC_STD_PRESSURE);

        // Pressure Tendency (BarChart)
        addPressureTendencyChart(dataList, useMetric);

        // Air Temperature
        addStdMetLineChart(dataList, "Air Temperature",
                useMetric ? "°C" : "°F",
                d -> {
                    if (d.getAirTemperature() == null) return null;
                    return useMetric ? d.getAirTemperature() : celsiusToFahrenheit(d.getAirTemperature());
                },
                BuoyStdMetData::getEpochSeconds, 0,
                ChartFullScreenFragment.METRIC_STD_AIR_TEMP);

        // Water Temperature
        addStdMetLineChart(dataList, "Water Temperature",
                useMetric ? "°C" : "°F",
                d -> {
                    if (d.getWaterTemperature() == null) return null;
                    return useMetric ? d.getWaterTemperature() : celsiusToFahrenheit(d.getWaterTemperature());
                },
                BuoyStdMetData::getEpochSeconds, 0,
                ChartFullScreenFragment.METRIC_STD_WATER_TEMP);

        // Dew Point
        addStdMetLineChart(dataList, "Dew Point",
                useMetric ? "°C" : "°F",
                d -> {
                    if (d.getDewPoint() == null) return null;
                    return useMetric ? d.getDewPoint() : celsiusToFahrenheit(d.getDewPoint());
                },
                BuoyStdMetData::getEpochSeconds, 0,
                ChartFullScreenFragment.METRIC_STD_DEW_POINT);

        // Visibility (rarely reported)
        addStdMetLineChart(dataList, "Visibility", "nmi",
                d -> d.getVisibility() == null ? null : d.getVisibility(),
                BuoyStdMetData::getEpochSeconds, 0,
                ChartFullScreenFragment.METRIC_STD_VISIBILITY);

        // Tide (rarely reported; source data is in feet)
        addStdMetLineChart(dataList, "Tide",
                useMetric ? "m" : "ft",
                d -> {
                    if (d.getTide() == null) return null;
                    return useMetric ? d.getTide() * FEET_TO_METERS : d.getTide();
                },
                BuoyStdMetData::getEpochSeconds, 0,
                ChartFullScreenFragment.METRIC_STD_TIDE);
    }

    /**
     * Generic helper to create a LineChart tile from std met data.
     * Skips the tile entirely if no non-null values exist.
     */
    private void addStdMetLineChart(List<BuoyStdMetData> dataList, String title, String unit,
                                    Function<BuoyStdMetData, Double> valueExtractor,
                                    Function<BuoyStdMetData, Long> epochExtractor,
                                    int colorIndex, String metricKey) {
        List<Entry> entries = new ArrayList<>();
        Double latestValue = null;

        for (BuoyStdMetData d : dataList) {
            Double val = valueExtractor.apply(d);
            if (val != null) {
                entries.add(new Entry(epochExtractor.apply(d), val.floatValue()));
                latestValue = val;
            }
        }

        if (entries.isEmpty()) return;

        int color = getChartColor(colorIndex);
        addLineChartTile(title, unit, latestValue, entries, color, null, null, metricKey);
    }

    /**
     * Creates a dual-line chart for wind speed and gust speed.
     */
    private void addWindSpeedGustChart(List<BuoyStdMetData> dataList, boolean useMetric) {
        List<Entry> speedEntries = new ArrayList<>();
        List<Entry> gustEntries = new ArrayList<>();
        Double latestSpeed = null;
        Double latestGust = null;

        for (BuoyStdMetData d : dataList) {
            float epoch = d.getEpochSeconds();
            if (d.getWindSpeed() != null) {
                double speed = useMetric ? d.getWindSpeed() : d.getWindSpeed() * MPS_TO_MPH;
                speedEntries.add(new Entry(epoch, (float) speed));
                latestSpeed = speed;
            }
            if (d.getGustSpeed() != null) {
                double gust = useMetric ? d.getGustSpeed() : d.getGustSpeed() * MPS_TO_MPH;
                gustEntries.add(new Entry(epoch, (float) gust));
                latestGust = gust;
            }
        }

        if (speedEntries.isEmpty() && gustEntries.isEmpty()) return;

        String windUnit = useMetric ? "m/s" : "mph";
        int primaryColor = resolveColor(com.google.android.material.R.attr.colorPrimary);
        int secondaryColor = resolveColor(com.google.android.material.R.attr.colorTertiary);

        // Build latest value string
        StringBuilder latestStr = new StringBuilder();
        if (latestSpeed != null) {
            latestStr.append(formatValue(latestSpeed));
        }
        if (latestGust != null) {
            if (latestStr.length() > 0) latestStr.append(" / ");
            latestStr.append(formatValue(latestGust)).append(" gust");
        }

        MaterialCardView card = createTileCard();
        LinearLayout cardContent = createCardContent();
        addTitleRow(cardContent, "Wind Speed & Gust", windUnit);
        addValueText(cardContent, latestStr.toString());

        LineChart chart = createLineChart();

        LineData lineData = new LineData();

        if (!speedEntries.isEmpty()) {
            LineDataSet speedSet = createLineDataSet(speedEntries, "Wind Speed", primaryColor);
            lineData.addDataSet(speedSet);
        }

        if (!gustEntries.isEmpty()) {
            LineDataSet gustSet = createLineDataSet(gustEntries, "Gust", secondaryColor);
            gustSet.enableDashedLine(10f, 5f, 0f);
            gustSet.setDrawFilled(false);
            lineData.addDataSet(gustSet);
        }

        chart.setData(lineData);

        // Show legend for multi-dataset chart
        Legend legend = chart.getLegend();
        legend.setEnabled(true);
        legend.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface));
        legend.setTextSize(11f);

        configureXAxis(chart);
        configureYAxis(chart);
        setDefaultViewport(chart);
        chart.invalidate();

        cardContent.addView(chart);
        card.addView(cardContent);
        setTileClickListener(card, ChartFullScreenFragment.METRIC_STD_WIND_SPEED_GUST);
        metricContainer.addView(card);
    }

    /**
     * Creates a BarChart tile for pressure tendency.
     */
    private void addPressureTendencyChart(List<BuoyStdMetData> dataList, boolean useMetric) {
        List<BarEntry> entries = new ArrayList<>();
        Double latestValue = null;

        for (BuoyStdMetData d : dataList) {
            if (d.getPressureTendency() != null) {
                double value = useMetric ? d.getPressureTendency()
                        : d.getPressureTendency() * HPA_TO_INHG;
                entries.add(new BarEntry(d.getEpochSeconds(), (float) value));
                latestValue = value;
            }
        }

        if (entries.isEmpty()) return;

        String pressureUnit = useMetric ? "hPa" : "inHg";
        int primaryColor = resolveColor(com.google.android.material.R.attr.colorPrimary);
        int tertiaryColor = resolveColor(com.google.android.material.R.attr.colorTertiary);

        MaterialCardView card = createTileCard();
        LinearLayout cardContent = createCardContent();
        addTitleRow(cardContent, "Pressure Tendency", pressureUnit);
        addValueText(cardContent, latestValue != null ? formatSignedValue(latestValue) : "--");

        BarChart chart = createBarChart();

        // Color positive/negative bars differently
        List<Integer> colors = new ArrayList<>();
        for (BarEntry entry : entries) {
            colors.add(entry.getY() >= 0 ? primaryColor : tertiaryColor);
        }

        BarDataSet dataSet = new BarDataSet(entries, "Pressure Tendency");
        dataSet.setColors(colors);
        dataSet.setDrawValues(false);

        BarData barData = new BarData(dataSet);

        // Calculate bar width from actual data spacing. Default width (0.85)
        // is designed for index-based X values and produces invisible bars
        // when X values are epoch seconds in the billions.
        if (entries.size() >= 2) {
            float spacing = entries.get(1).getX() - entries.get(0).getX();
            barData.setBarWidth(spacing * 0.9f);
        }

        chart.setData(barData);

        configureXAxis(chart);
        configureYAxis(chart);
        setDefaultViewport(chart);
        chart.invalidate();

        cardContent.addView(chart);
        card.addView(cardContent);
        setTileClickListener(card, ChartFullScreenFragment.METRIC_STD_PRESSURE_TENDENCY);
        metricContainer.addView(card);
    }

    // ========================================================================
    // Spectral Wave Data section
    // ========================================================================

    private void buildSpecWaveSection(List<BuoySpecWaveData> dataList, boolean useMetric) {
        if (dataList.isEmpty()) return;

        addSectionHeader(getString(R.string.section_spec_wave));

        // Swell Height
        addSpecWaveLineChart(dataList, "Swell Height",
                useMetric ? "m" : "ft",
                d -> {
                    if (d.getSwellHeight() == null) return null;
                    return useMetric ? d.getSwellHeight() : d.getSwellHeight() * METERS_TO_FEET;
                },
                BuoySpecWaveData::getEpochSeconds, 0,
                ChartFullScreenFragment.METRIC_SPEC_SWELL_HEIGHT);

        // Swell Period
        addSpecWaveLineChart(dataList, "Swell Period", "s",
                d -> d.getSwellPeriod() == null ? null : d.getSwellPeriod(),
                BuoySpecWaveData::getEpochSeconds, 0,
                ChartFullScreenFragment.METRIC_SPEC_SWELL_PERIOD);

        // Wind Wave Height
        addSpecWaveLineChart(dataList, "Wind Wave Height",
                useMetric ? "m" : "ft",
                d -> {
                    if (d.getWindWaveHeight() == null) return null;
                    return useMetric ? d.getWindWaveHeight() : d.getWindWaveHeight() * METERS_TO_FEET;
                },
                BuoySpecWaveData::getEpochSeconds, 0,
                ChartFullScreenFragment.METRIC_SPEC_WIND_WAVE_HEIGHT);

        // Wind Wave Period
        addSpecWaveLineChart(dataList, "Wind Wave Period", "s",
                d -> d.getWindWavePeriod() == null ? null : d.getWindWavePeriod(),
                BuoySpecWaveData::getEpochSeconds, 0,
                ChartFullScreenFragment.METRIC_SPEC_WIND_WAVE_PERIOD);

        // Average Wave Period
        addSpecWaveLineChart(dataList, "Average Wave Period", "s",
                d -> d.getAverageWavePeriod() == null ? null : d.getAverageWavePeriod(),
                BuoySpecWaveData::getEpochSeconds, 0,
                ChartFullScreenFragment.METRIC_SPEC_AVG_WAVE_PERIOD);

        // Mean Wave Direction
        addSpecWaveLineChart(dataList, "Mean Wave Direction", "°",
                d -> d.getMeanWaveDirection() == null ? null : (double) d.getMeanWaveDirection(),
                BuoySpecWaveData::getEpochSeconds, 0,
                ChartFullScreenFragment.METRIC_SPEC_MEAN_WAVE_DIR);

        // Steepness (categorical → stepped line chart)
        addSteepnessChart(dataList);

        // Text-only tiles for cardinal direction data
        addLatestTextTile(dataList, "Swell Direction",
                d -> d.getSwellDirection() != null ? d.getSwellDirection() : null);
        addLatestTextTile(dataList, "Wind Wave Direction",
                d -> d.getWindWaveDirection() != null ? d.getWindWaveDirection() : null);
    }

    /**
     * Generic helper to create a LineChart tile from spec wave data.
     */
    private void addSpecWaveLineChart(List<BuoySpecWaveData> dataList, String title, String unit,
                                      Function<BuoySpecWaveData, Double> valueExtractor,
                                      Function<BuoySpecWaveData, Long> epochExtractor,
                                      int colorIndex, String metricKey) {
        List<Entry> entries = new ArrayList<>();
        Double latestValue = null;

        for (BuoySpecWaveData d : dataList) {
            Double val = valueExtractor.apply(d);
            if (val != null) {
                entries.add(new Entry(epochExtractor.apply(d), val.floatValue()));
                latestValue = val;
            }
        }

        if (entries.isEmpty()) return;

        int color = getChartColor(colorIndex);
        addLineChartTile(title, unit, latestValue, entries, color, null, null, metricKey);
    }

    /**
     * Maps a steepness category string to an ordinal value for charting.
     */
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

    /**
     * Maps an ordinal value back to its steepness category label.
     */
    private static String ordinalToSteepness(int ordinal) {
        switch (ordinal) {
            case 1: return "Swell";
            case 2: return "Average";
            case 3: return "Steep";
            case 4: return "Very Steep";
            default: return "";
        }
    }

    /**
     * Creates a scatter chart for the steepness categorical metric.
     * Each category gets its own colored dataset so the legend maps
     * colors to categories. Points are plotted at ordinal Y-values (1-4)
     * with category names on the Y-axis.
     */
    private void addSteepnessChart(List<BuoySpecWaveData> dataList) {
        // Bin entries by category
        List<Entry> swellEntries = new ArrayList<>();
        List<Entry> averageEntries = new ArrayList<>();
        List<Entry> steepEntries = new ArrayList<>();
        List<Entry> verySteepEntries = new ArrayList<>();
        String latestLabel = null;

        for (BuoySpecWaveData d : dataList) {
            int ordinal = steepnessToOrdinal(d.getSteepness());
            if (ordinal <= 0) continue;
            Entry entry = new Entry(d.getEpochSeconds(), ordinal);
            latestLabel = ordinalToSteepness(ordinal);
            switch (ordinal) {
                case 1: swellEntries.add(entry); break;
                case 2: averageEntries.add(entry); break;
                case 3: steepEntries.add(entry); break;
                case 4: verySteepEntries.add(entry); break;
            }
        }

        boolean hasAny = !swellEntries.isEmpty() || !averageEntries.isEmpty()
                || !steepEntries.isEmpty() || !verySteepEntries.isEmpty();
        if (!hasAny) return;

        // Resolve M3 colors for each category
        int swellColor = resolveColor(com.google.android.material.R.attr.colorPrimary);
        int avgColor = resolveColor(com.google.android.material.R.attr.colorSecondary);
        int steepColor = resolveColor(com.google.android.material.R.attr.colorTertiary);
        int vSteepColor = resolveColor(com.google.android.material.R.attr.colorError);

        MaterialCardView card = createTileCard();
        LinearLayout cardContent = createCardContent();
        addTitleRow(cardContent, "Steepness", "");
        addValueText(cardContent, latestLabel != null ? latestLabel : "--");

        ScatterChart chart = new ScatterChart(requireContext());
        LinearLayout.LayoutParams chartParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(180));
        chart.setLayoutParams(chartParams);
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setScaleYEnabled(false);
        chart.setPinchZoom(true);
        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.setExtraBottomOffset(4f);

        ScatterData scatterData = new ScatterData();

        if (!swellEntries.isEmpty()) {
            scatterData.addDataSet(createScatterDataSet(swellEntries, "Swell", swellColor));
        }
        if (!averageEntries.isEmpty()) {
            scatterData.addDataSet(createScatterDataSet(averageEntries, "Average", avgColor));
        }
        if (!steepEntries.isEmpty()) {
            scatterData.addDataSet(createScatterDataSet(steepEntries, "Steep", steepColor));
        }
        if (!verySteepEntries.isEmpty()) {
            scatterData.addDataSet(createScatterDataSet(verySteepEntries, "Very Steep", vSteepColor));
        }

        chart.setData(scatterData);

        // Legend
        Legend legend = chart.getLegend();
        legend.setEnabled(true);
        legend.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface));
        legend.setTextSize(11f);
        legend.setWordWrapEnabled(true);

        configureXAxis(chart);

        // Custom Y-axis: fixed range 1-4 with category labels
        int axisTextColor = resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant);
        int gridColor = resolveColor(com.google.android.material.R.attr.colorOutlineVariant);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(axisTextColor);
        leftAxis.setTextSize(10f);
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
        setDefaultViewport(chart);
        chart.invalidate();

        cardContent.addView(chart);
        card.addView(cardContent);
        setTileClickListener(card, ChartFullScreenFragment.METRIC_SPEC_STEEPNESS);
        metricContainer.addView(card);
    }

    /**
     * Creates a ScatterDataSet with circle markers for the steepness chart.
     */
    private ScatterDataSet createScatterDataSet(List<Entry> entries, String label, int color) {
        ScatterDataSet dataSet = new ScatterDataSet(entries, label);
        dataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        dataSet.setScatterShapeSize(16f);
        dataSet.setColor(color);
        dataSet.setDrawValues(false);
        dataSet.setHighLightColor(resolveColor(com.google.android.material.R.attr.colorTertiary));
        return dataSet;
    }

    /**
     * Creates a text-only tile for cardinal direction data.
     * Shows the latest non-null value.
     */
    private void addLatestTextTile(List<BuoySpecWaveData> dataList, String title,
                                   Function<BuoySpecWaveData, String> valueExtractor) {
        String latestValue = null;
        for (BuoySpecWaveData d : dataList) {
            String val = valueExtractor.apply(d);
            if (val != null) {
                latestValue = val;
            }
        }

        if (latestValue == null) return;

        MaterialCardView card = createTileCard();
        LinearLayout cardContent = createCardContent();
        addTitleRow(cardContent, title, "");
        addValueText(cardContent, latestValue);
        card.addView(cardContent);
        metricContainer.addView(card);
    }

    // ========================================================================
    // Tile & chart construction helpers
    // ========================================================================

    /**
     * Adds a complete LineChart tile (card + title + value + chart) to the container.
     * Optionally supports a second dataset for dual-line charts.
     *
     * @param metricKey the metric key used to open the full-screen chart on tap
     */
    private void addLineChartTile(String title, String unit, Double latestValue,
                                  List<Entry> entries, int lineColor,
                                  @Nullable List<Entry> secondEntries,
                                  @Nullable Integer secondColor,
                                  String metricKey) {
        MaterialCardView card = createTileCard();
        LinearLayout cardContent = createCardContent();

        addTitleRow(cardContent, title, unit);
        addValueText(cardContent, latestValue != null ? formatValue(latestValue) + " " + unit : "--");

        LineChart chart = createLineChart();
        LineData lineData = new LineData();

        LineDataSet dataSet = createLineDataSet(entries, title, lineColor);
        lineData.addDataSet(dataSet);

        if (secondEntries != null && secondColor != null && !secondEntries.isEmpty()) {
            LineDataSet secondSet = createLineDataSet(secondEntries, title + " (2)", secondColor);
            secondSet.setDrawFilled(false);
            lineData.addDataSet(secondSet);
        }

        chart.setData(lineData);
        configureXAxis(chart);
        configureYAxis(chart);
        setDefaultViewport(chart);
        chart.invalidate();

        cardContent.addView(chart);
        card.addView(cardContent);
        setTileClickListener(card, metricKey);
        metricContainer.addView(card);
    }

    /**
     * Adds a section header (e.g. "Standard Meteorological Data") to the container.
     */
    private void addSectionHeader(String text) {
        TextView header = new TextView(requireContext());
        header.setText(text);
        header.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleSmall);
        header.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(dpToPx(4), dpToPx(16), dpToPx(4), dpToPx(4));
        header.setLayoutParams(params);

        metricContainer.addView(header);
    }

    /**
     * Adds a prominent "no data" message when both data sources are empty.
     */
    private void addEmptyState() {
        TextView empty = new TextView(requireContext());
        empty.setText(R.string.no_data_available);
        empty.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
        empty.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
        empty.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(dpToPx(16), dpToPx(48), dpToPx(16), dpToPx(48));
        empty.setLayoutParams(params);

        metricContainer.addView(empty);
    }

    /**
     * Configures a tile card to open the full-screen chart when tapped.
     */
    private void setTileClickListener(MaterialCardView card, String metricKey) {
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> {
            ChartFullScreenFragment fragment =
                    ChartFullScreenFragment.newInstance(stationId, metricKey);
            ((MainActivity) requireActivity()).navigateTo(fragment);
        });
    }

    /**
     * Creates a MaterialCardView styled as an outlined M3 card.
     */
    private MaterialCardView createTileCard() {
        MaterialCardView card = new MaterialCardView(requireContext());
        card.setStrokeWidth(dpToPx(1));
        card.setStrokeColor(resolveColor(com.google.android.material.R.attr.colorOutlineVariant));
        card.setCardElevation(0);
        card.setRadius(dpToPx(12));
        card.setUseCompatPadding(false);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dpToPx(4), 0, dpToPx(4));
        card.setLayoutParams(params);

        return card;
    }

    /**
     * Creates the inner LinearLayout for a tile card.
     */
    private LinearLayout createCardContent() {
        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
        content.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        return content;
    }

    /**
     * Adds a title row with metric name and unit label.
     */
    private void addTitleRow(LinearLayout parent, String title, String unit) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView titleView = new TextView(requireContext());
        titleView.setText(title);
        titleView.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        titleView.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleView.setLayoutParams(titleParams);
        row.addView(titleView);

        if (unit != null && !unit.isEmpty()) {
            TextView unitView = new TextView(requireContext());
            unitView.setText(unit);
            unitView.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelMedium);
            unitView.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
            row.addView(unitView);
        }

        parent.addView(row);
    }

    /**
     * Adds a large value display text.
     */
    private void addValueText(LinearLayout parent, String value) {
        TextView valueView = new TextView(requireContext());
        valueView.setText(value);
        valueView.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_HeadlineMedium);
        valueView.setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dpToPx(2), 0, dpToPx(8));
        valueView.setLayoutParams(params);

        parent.addView(valueView);
    }

    // ========================================================================
    // Chart creation & configuration
    // ========================================================================

    private LineChart createLineChart() {
        LineChart chart = new LineChart(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(180));
        chart.setLayoutParams(params);

        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setScaleYEnabled(false);
        chart.setPinchZoom(true);
        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.setExtraBottomOffset(4f);

        return chart;
    }

    private BarChart createBarChart() {
        BarChart chart = new BarChart(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(180));
        chart.setLayoutParams(params);

        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setScaleYEnabled(false);
        chart.setPinchZoom(true);
        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.setExtraBottomOffset(4f);
        chart.setDrawBarShadow(false);
        chart.setDrawValueAboveBar(false);

        return chart;
    }

    private LineDataSet createLineDataSet(List<Entry> entries, String label, int color) {
        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(color);
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.LINEAR);
        dataSet.setDrawFilled(true);

        // Fill with 15% alpha of the line color
        int fillColor = Color.argb(38, Color.red(color), Color.green(color), Color.blue(color));
        dataSet.setFillColor(fillColor);
        dataSet.setFillAlpha(255); // alpha is baked into fillColor

        dataSet.setHighLightColor(resolveColor(com.google.android.material.R.attr.colorTertiary));
        dataSet.setHighlightLineWidth(1f);
        dataSet.setDrawHorizontalHighlightIndicator(false);

        return dataSet;
    }

    private void configureXAxis(BarLineChartBase<?> chart) {
        int axisTextColor = resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant);
        int gridColor = resolveColor(com.google.android.material.R.attr.colorOutlineVariant);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(axisTextColor);
        xAxis.setTextSize(10f);
        xAxis.setGridColor(gridColor);
        xAxis.setDrawAxisLine(false);

        // Adapt format to the visible data range:
        // >2 days  → "Jan 9"  (date only, readable at multi-week scale)
        // ≤2 days  → "M/d h:mm a" (date + time, AM/PM, consistent with list views)
        float xMin = chart.getData() != null ? chart.getData().getXMin() : 0;
        float xMax = chart.getData() != null ? chart.getData().getXMax() : 0;
        float rangeSeconds = xMax - xMin;
        boolean shortRange = rangeSeconds <= 2 * 24 * 3600;

        xAxis.setLabelCount(shortRange ? 3 : 4, false);
        String pattern = shortRange ? "M/d h:mm a" : "MMM d";
        xAxis.setGranularity(shortRange ? 3600f : 86400f);

        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat fmt = new SimpleDateFormat(pattern, Locale.getDefault());

            {
                fmt.setTimeZone(TimeZone.getDefault());
            }

            @Override
            public String getFormattedValue(float value) {
                return fmt.format(new Date((long) value * 1000L));
            }
        });
    }

    private void configureYAxis(BarLineChartBase<?> chart) {
        int axisTextColor = resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant);
        int gridColor = resolveColor(com.google.android.material.R.attr.colorOutlineVariant);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(axisTextColor);
        leftAxis.setTextSize(10f);
        leftAxis.setGridColor(gridColor);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setLabelCount(5, false);

        chart.getAxisRight().setEnabled(false);
    }

    // ========================================================================
    // Default viewport
    // ========================================================================

    /**
     * Zooms the chart to show the last 2 days by default, allowing pan/zoom
     * across the full data range. Uses the metricContainer width for the zoom
     * calculation (the container is already laid out by this point).
     */
    private void setDefaultViewport(BarLineChartBase<?> chart) {
        if (chart.getData() == null) return;
        float xMin = chart.getData().getXMin();
        float xMax = chart.getData().getXMax();
        float fullRange = xMax - xMin;
        if (fullRange <= 0f) return;
        chart.setVisibleXRangeMaximum(fullRange);
        chart.setVisibleXRangeMinimum(Math.min(3600f, fullRange));
        if (fullRange > DEFAULT_VISIBLE_X_RANGE_SECONDS) {
            int w = metricContainer.getWidth();
            int h = dpToPx(180);
            if (w > 0) {
                chart.zoom(fullRange / DEFAULT_VISIBLE_X_RANGE_SECONDS, 1f, w, h / 2f);
                chart.moveViewToX(xMax - DEFAULT_VISIBLE_X_RANGE_SECONDS);
            }
        }
    }

    // ========================================================================
    // Utility methods
    // ========================================================================

    /**
     * Returns an M3 chart color by index.
     * Index 0 = colorPrimary, 1 = colorSecondary, 2 = colorTertiary.
     */
    private int getChartColor(int index) {
        switch (index) {
            case 1:
                return resolveColor(com.google.android.material.R.attr.colorSecondary);
            case 2:
                return resolveColor(com.google.android.material.R.attr.colorTertiary);
            default:
                return resolveColor(com.google.android.material.R.attr.colorPrimary);
        }
    }

    /**
     * Resolves a Material theme attribute to its current color value.
     */
    private int resolveColor(int attr) {
        return MaterialColors.getColor(requireView(), attr);
    }

    private static double celsiusToFahrenheit(double celsius) {
        return celsius * 9.0 / 5.0 + 32.0;
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                requireContext().getResources().getDisplayMetrics());
    }

    private String formatValue(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.US, "%.1f", value);
    }

    private String formatSignedValue(double value) {
        String formatted = formatValue(value);
        if (value > 0) {
            return "+" + formatted;
        }
        return formatted;
    }
}

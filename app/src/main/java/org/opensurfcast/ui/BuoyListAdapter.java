package org.opensurfcast.ui;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;

import org.opensurfcast.R;
import org.opensurfcast.buoy.BuoyStdMetData;
import org.opensurfcast.buoy.BuoyStation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * RecyclerView adapter for displaying preferred buoy stations.
 */
public class BuoyListAdapter extends RecyclerView.Adapter<BuoyListAdapter.ViewHolder> {

    private static final double METERS_TO_FEET = 3.28084;

    private final List<BuoyStation> stations = new ArrayList<>();
    private Map<String, BuoyStdMetData> observations = Collections.emptyMap();
    private boolean useMetric;

    /**
     * Replaces the adapter data with the given list and refreshes the view.
     */
    public void submitList(List<BuoyStation> newStations) {
        stations.clear();
        if (newStations != null) {
            stations.addAll(newStations);
        }
        notifyDataSetChanged();
    }

    /**
     * Sets the latest observation data for each station and refreshes the view.
     *
     * @param observations map of station ID to its latest observation
     */
    public void submitObservations(Map<String, BuoyStdMetData> observations) {
        this.observations = observations != null ? observations : Collections.emptyMap();
        notifyDataSetChanged();
    }

    /**
     * Sets the preferred unit system and refreshes the view.
     *
     * @param useMetric true for metric (m), false for imperial (ft)
     */
    public void setUseMetric(boolean useMetric) {
        if (this.useMetric != useMetric) {
            this.useMetric = useMetric;
            notifyDataSetChanged();
        }
    }

    /**
     * Returns the station at the given adapter position.
     */
    public BuoyStation getStationAt(int position) {
        return stations.get(position);
    }

    /**
     * Removes the station at the given position and notifies the adapter.
     */
    public void removeAt(int position) {
        stations.remove(position);
        notifyItemRemoved(position);
    }

    /**
     * Inserts a station at the given position and notifies the adapter.
     */
    public void insertAt(int position, BuoyStation station) {
        stations.add(position, station);
        notifyItemInserted(position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_buoy_station, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BuoyStation station = stations.get(position);
        BuoyStdMetData obs = observations.get(station.getId());
        holder.bind(station, obs, useMetric);
    }

    @Override
    public int getItemCount() {
        return stations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView stationName;
        private final LinearLayout supportingRow;
        private final TextView stationWaveSummary;
        private final TextView observationAge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            stationName = itemView.findViewById(R.id.station_name);
            supportingRow = itemView.findViewById(R.id.supporting_row);
            stationWaveSummary = itemView.findViewById(R.id.station_wave_summary);
            observationAge = itemView.findViewById(R.id.observation_age);
        }

        void bind(BuoyStation station, BuoyStdMetData obs, boolean useMetric) {
            // Build station name with ID suffix: "Station Name · 46025"
            String name = station.getName() != null ? station.getName() : station.getId();
            String suffix = " \u00B7 " + station.getId();
            SpannableString spannable = new SpannableString(name + suffix);
            int suffixStart = name.length();
            int suffixEnd = spannable.length();
            int variantColor = MaterialColors.getColor(itemView,
                    com.google.android.material.R.attr.colorOnSurfaceVariant);
            spannable.setSpan(new ForegroundColorSpan(variantColor),
                    suffixStart, suffixEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new RelativeSizeSpan(0.85f),
                    suffixStart, suffixEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            stationName.setText(spannable);

            boolean hasWaveData = obs != null
                    && obs.getWaveHeight() != null
                    && obs.getDominantWavePeriod() != null;
            boolean hasObservation = obs != null && obs.getEpochSeconds() > 0;

            if (hasWaveData || hasObservation) {
                supportingRow.setVisibility(View.VISIBLE);

                // Wave summary
                if (hasWaveData) {
                    double waveHeight = obs.getWaveHeight();
                    if (!useMetric) {
                        waveHeight *= METERS_TO_FEET;
                    }
                    int summaryRes = useMetric
                            ? R.string.buoy_wave_summary_metric
                            : R.string.buoy_wave_summary_imperial;
                    stationWaveSummary.setText(
                            itemView.getContext().getString(summaryRes,
                                    waveHeight, obs.getDominantWavePeriod()));
                    stationWaveSummary.setVisibility(View.VISIBLE);
                } else {
                    stationWaveSummary.setVisibility(View.GONE);
                }

                // Observation age
                if (hasObservation) {
                    long obsMillis = obs.getEpochSeconds() * 1000L;
                    long nowMillis = System.currentTimeMillis();
                    CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                            obsMillis, nowMillis, DateUtils.MINUTE_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_RELATIVE);
                    observationAge.setText(relativeTime);
                    observationAge.setVisibility(View.VISIBLE);
                } else {
                    observationAge.setVisibility(View.GONE);
                }
            } else {
                supportingRow.setVisibility(View.GONE);
            }
        }
    }
}

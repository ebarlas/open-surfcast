package org.opensurfcast.ui;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;

import org.opensurfcast.R;
import org.opensurfcast.tide.TideStation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * RecyclerView adapter for displaying preferred tide stations.
 * <p>
 * Displays station name, ID, and interpolated current tide level.
 */
public class TideListAdapter extends RecyclerView.Adapter<TideListAdapter.ViewHolder> {

    private static final double METERS_TO_FEET = 3.28084;

    private final List<TideStation> stations = new ArrayList<>();
    private final Map<String, Double> currentLevels = new HashMap<>();
    private boolean useMetric;

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tide_station, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TideStation station = stations.get(position);
        Double levelMeters = currentLevels.get(station.id);
        holder.bind(station, levelMeters, useMetric);
    }

    @Override
    public int getItemCount() {
        return stations.size();
    }

    /**
     * Replaces the adapter data with the given list and refreshes the view.
     */
    public void submitList(List<TideStation> newStations) {
        submitList(newStations, null);
    }

    /**
     * Replaces the adapter data with the given list and current tide levels.
     *
     * @param newStations list of stations (null to clear)
     * @param levels      map of station ID to interpolated level in meters (null for empty)
     */
    public void submitList(List<TideStation> newStations, Map<String, Double> levels) {
        stations.clear();
        currentLevels.clear();
        if (newStations != null) {
            stations.addAll(newStations);
        }
        if (levels != null) {
            currentLevels.putAll(levels);
        }
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
    public TideStation getStationAt(int position) {
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
    public void insertAt(int position, TideStation station) {
        stations.add(position, station);
        notifyItemInserted(position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView stationName;
        private final TextView tideLevel;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            stationName = itemView.findViewById(R.id.station_name);
            tideLevel = itemView.findViewById(R.id.tide_level);
        }

        void bind(TideStation station, Double levelMeters, boolean useMetric) {
            // Build station name with ID suffix: "Station Name · 9415252"
            String name = station.name != null ? station.name : station.id;
            String suffix = " \u00B7 " + station.id;
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

            // Current tide level
            if (levelMeters != null) {
                double displayValue = useMetric ? levelMeters : levelMeters * METERS_TO_FEET;
                int formatRes = useMetric ? R.string.tide_level_metric : R.string.tide_level_imperial;
                tideLevel.setText(String.format(Locale.US,
                        itemView.getContext().getString(formatRes), displayValue));
                tideLevel.setVisibility(View.VISIBLE);
            } else {
                tideLevel.setText(R.string.tide_level_unknown);
                tideLevel.setVisibility(View.VISIBLE);
            }
        }
    }
}

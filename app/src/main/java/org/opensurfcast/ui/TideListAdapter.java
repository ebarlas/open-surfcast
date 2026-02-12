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
import java.util.List;

/**
 * RecyclerView adapter for displaying preferred tide stations.
 * <p>
 * Displays station name and ID. No tide predictions or observation data.
 */
public class TideListAdapter extends RecyclerView.Adapter<TideListAdapter.ViewHolder> {

    private final List<TideStation> stations = new ArrayList<>();

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
        holder.bind(station);
    }

    @Override
    public int getItemCount() {
        return stations.size();
    }

    /**
     * Replaces the adapter data with the given list and refreshes the view.
     */
    public void submitList(List<TideStation> newStations) {
        stations.clear();
        if (newStations != null) {
            stations.addAll(newStations);
        }
        notifyDataSetChanged();
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

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            stationName = itemView.findViewById(R.id.station_name);
        }

        void bind(TideStation station) {
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
        }
    }
}

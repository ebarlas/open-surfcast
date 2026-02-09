package org.opensurfcast.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.opensurfcast.R;
import org.opensurfcast.buoy.BuoyStdMetData;
import org.opensurfcast.buoy.BuoyStation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * RecyclerView adapter for displaying preferred buoy stations.
 */
public class BuoyListAdapter extends RecyclerView.Adapter<BuoyListAdapter.ViewHolder> {

    private final List<BuoyStation> stations = new ArrayList<>();
    private Map<String, BuoyStdMetData> observations = Collections.emptyMap();

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
        holder.bind(station, obs);
    }

    @Override
    public int getItemCount() {
        return stations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView stationName;
        private final TextView stationIdType;
        private final TextView stationCoordinates;
        private final TextView stationWaveSummary;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            stationName = itemView.findViewById(R.id.station_name);
            stationIdType = itemView.findViewById(R.id.station_id_type);
            stationCoordinates = itemView.findViewById(R.id.station_coordinates);
            stationWaveSummary = itemView.findViewById(R.id.station_wave_summary);
        }

        void bind(BuoyStation station, BuoyStdMetData obs) {
            stationName.setText(station.getName() != null ? station.getName() : station.getId());

            String type = station.getType() != null ? station.getType() : "";
            stationIdType.setText(
                    itemView.getContext().getString(R.string.station_id_type, station.getId(), type));

            stationCoordinates.setText(
                    String.format(Locale.US, "%.4f, %.4f",
                            station.getLatitude(), station.getLongitude()));

            if (obs != null && obs.getWaveHeight() != null && obs.getDominantWavePeriod() != null) {
                stationWaveSummary.setText(
                        itemView.getContext().getString(R.string.buoy_wave_summary,
                                obs.getWaveHeight(), obs.getDominantWavePeriod()));
                stationWaveSummary.setVisibility(View.VISIBLE);
            } else {
                stationWaveSummary.setVisibility(View.GONE);
            }
        }
    }
}

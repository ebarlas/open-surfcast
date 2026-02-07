package org.opensurfcast.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.opensurfcast.R;
import org.opensurfcast.buoy.BuoyStation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for displaying preferred buoy stations.
 */
public class BuoyListAdapter extends RecyclerView.Adapter<BuoyListAdapter.ViewHolder> {

    private final List<BuoyStation> stations = new ArrayList<>();

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
        holder.bind(station);
    }

    @Override
    public int getItemCount() {
        return stations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView stationName;
        private final TextView stationIdType;
        private final TextView stationCoordinates;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            stationName = itemView.findViewById(R.id.station_name);
            stationIdType = itemView.findViewById(R.id.station_id_type);
            stationCoordinates = itemView.findViewById(R.id.station_coordinates);
        }

        void bind(BuoyStation station) {
            stationName.setText(station.getName() != null ? station.getName() : station.getId());

            String type = station.getType() != null ? station.getType() : "";
            stationIdType.setText(
                    itemView.getContext().getString(R.string.station_id_type, station.getId(), type));

            stationCoordinates.setText(
                    String.format(Locale.US, "%.4f, %.4f",
                            station.getLatitude(), station.getLongitude()));
        }
    }
}

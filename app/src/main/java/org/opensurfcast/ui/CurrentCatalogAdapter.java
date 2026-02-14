package org.opensurfcast.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.opensurfcast.R;
import org.opensurfcast.tide.CurrentStation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * RecyclerView adapter for the current station catalog.
 * <p>
 * Shows all stations with a checkmark for stations already added
 * to the user's preferred list. Supports click-to-toggle.
 */
public class CurrentCatalogAdapter extends RecyclerView.Adapter<CurrentCatalogAdapter.ViewHolder> {

    /**
     * Callback for station toggle events.
     */
    public interface OnStationToggleListener {
        void onStationToggled(CurrentStation station, boolean added);
    }

    private final List<CurrentStation> stations = new ArrayList<>();
    private final Set<String> addedIds = new HashSet<>();
    private OnStationToggleListener listener;

    public void setOnStationToggleListener(OnStationToggleListener listener) {
        this.listener = listener;
    }

    /**
     * Replaces the displayed station list.
     */
    public void submitList(List<CurrentStation> newStations) {
        stations.clear();
        if (newStations != null) {
            stations.addAll(newStations);
        }
        notifyDataSetChanged();
    }

    /**
     * Updates the set of already-added station IDs and refreshes the view.
     */
    public void setAddedIds(Set<String> ids) {
        addedIds.clear();
        if (ids != null) {
            addedIds.addAll(ids);
        }
        notifyDataSetChanged();
    }

    /**
     * Toggles a single station's added state and refreshes its row.
     */
    public void toggleStation(String stationId) {
        if (addedIds.contains(stationId)) {
            addedIds.remove(stationId);
        } else {
            addedIds.add(stationId);
        }
        for (int i = 0; i < stations.size(); i++) {
            if (stations.get(i).id.equals(stationId)) {
                notifyItemChanged(i);
                break;
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_catalog_station, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CurrentStation station = stations.get(position);
        boolean isAdded = addedIds.contains(station.id);
        holder.bind(station, isAdded);

        holder.itemView.setOnClickListener(v -> {
            boolean wasAdded = addedIds.contains(station.id);
            toggleStation(station.id);
            if (listener != null) {
                listener.onStationToggled(station, !wasAdded);
            }
        });
    }

    @Override
    public int getItemCount() {
        return stations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView stationName;
        private final TextView stationIdType;
        private final ImageView checkIcon;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            stationName = itemView.findViewById(R.id.station_name);
            stationIdType = itemView.findViewById(R.id.station_id_type);
            checkIcon = itemView.findViewById(R.id.check_icon);
        }

        void bind(CurrentStation station, boolean isAdded) {
            stationName.setText(station.name != null ? station.name : station.id);

            String type = station.type != null ? station.type : "";
            stationIdType.setText(
                    itemView.getContext().getString(R.string.station_id_type, station.id, type));

            checkIcon.setVisibility(isAdded ? View.VISIBLE : View.GONE);
        }
    }
}

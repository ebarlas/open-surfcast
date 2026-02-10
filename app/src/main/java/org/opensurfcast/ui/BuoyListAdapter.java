package org.opensurfcast.ui;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;

import org.opensurfcast.R;
import org.opensurfcast.buoy.BuoyStation;
import org.opensurfcast.buoy.BuoyStdMetData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RecyclerView adapter for displaying preferred buoy stations.
 */
public class BuoyListAdapter extends RecyclerView.Adapter<BuoyListAdapter.ViewHolder> {

    private static final double METERS_TO_FEET = 3.28084;
    private static final double MAX_WAVE_HEIGHT_METERS = 6.0;
    private static final long BAR_ANIM_DURATION_MS = 600;

    private final List<BuoyStation> stations = new ArrayList<>();
    private final Map<String, BuoyStdMetData> observations = new HashMap<>();
    private boolean useMetric;

    /**
     * Replaces the adapter data with the given list and refreshes the view.
     */
    public void submitList(List<BuoyStation> newStations) {
        stations.clear();
        stations.addAll(newStations);
        notifyDataSetChanged();
    }

    /**
     * Sets the latest observation data for each station and refreshes the view.
     *
     * @param newObservations map of station ID to its latest observation
     */
    public void submitObservations(Map<String, BuoyStdMetData> newObservations) {
        observations.clear();
        observations.putAll(newObservations);
        notifyDataSetChanged();
    }

    /**
     * Updates observations, only invalidating row tiles whose observation
     * timestamp has actually changed. Rows with unchanged timestamps are
     * left untouched, avoiding unnecessary rebinds and animation replays.
     *
     * @param newObservations map of station ID to its latest observation
     */
    public void updateObservations(Map<String, BuoyStdMetData> newObservations) {
        for (int i = 0; i < stations.size(); i++) {
            String id = stations.get(i).getId();
            BuoyStdMetData prev = observations.get(id);
            BuoyStdMetData next = newObservations.get(id);
            if (requiresRefresh(prev, next)) {
                notifyItemChanged(i);
            }
        }
        observations.clear();
        observations.putAll(newObservations);
    }

    public void updateObservations(String stationId, BuoyStdMetData latestObs) {
        for (int i = 0; i < stations.size(); i++) {
            String id = stations.get(i).getId();
            if (id.equals(stationId)) {
                BuoyStdMetData prev = observations.get(id);
                if (requiresRefresh(prev, latestObs)) {
                    notifyItemChanged(i);
                    break;
                }
            }
        }
        observations.put(stationId, latestObs);
    }

    /**
     * Returns {@code true} if the row tile for a station needs to be
     * refreshed because its observation timestamp has changed.
     * <p>
     * Mirrors the pattern from the reference BuoyListActivity.
     */
    static boolean requiresRefresh(BuoyStdMetData prev, BuoyStdMetData next) {
        if (prev == null && next == null) {
            return false;
        }
        if (prev == null || next == null) {
            return true;
        }
        return prev.getEpochSeconds() != next.getEpochSeconds();
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
        private final FrameLayout waveBarTrack;
        private final View waveBarFill;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            stationName = itemView.findViewById(R.id.station_name);
            supportingRow = itemView.findViewById(R.id.supporting_row);
            stationWaveSummary = itemView.findViewById(R.id.station_wave_summary);
            observationAge = itemView.findViewById(R.id.observation_age);
            waveBarTrack = itemView.findViewById(R.id.wave_bar_track);
            waveBarFill = itemView.findViewById(R.id.wave_bar_fill);
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

                // Wave height magnitude bar
                if (hasWaveData) {
                    waveBarTrack.setVisibility(View.VISIBLE);
                    waveBarFill.clearAnimation();

                    // Defer width calculation until after layout so that
                    // parent.getWidth() returns the real measured width.
                    // Without this, the first bind after data appears would
                    // see width == 0 and produce an invisible bar.
                    double waveHeightM = obs.getWaveHeight();
                    waveBarFill.post(() -> {
                        ViewGroup parent = (ViewGroup) waveBarTrack.getParent();
                        int availableWidth = parent.getWidth()
                                - parent.getPaddingStart() - parent.getPaddingEnd();
                        if (availableWidth <= 0) return;

                        double fraction = Math.min(
                                waveHeightM / MAX_WAVE_HEIGHT_METERS, 1.0);
                        int targetWidth = Math.max(1,
                                (int) (availableWidth * fraction));

                        ViewGroup.LayoutParams fillParams =
                                waveBarFill.getLayoutParams();
                        fillParams.width = targetWidth;
                        waveBarFill.setLayoutParams(fillParams);

                        // Animate: scale X from 0 to 1, anchored at the left edge
                        ScaleAnimation scaleAnim = new ScaleAnimation(
                                0f, 1f, 1f, 1f,
                                Animation.RELATIVE_TO_SELF, 0f,
                                Animation.RELATIVE_TO_SELF, 0f);
                        scaleAnim.setDuration(BAR_ANIM_DURATION_MS);
                        scaleAnim.setInterpolator(new DecelerateInterpolator());
                        waveBarFill.startAnimation(scaleAnim);
                    });
                } else {
                    waveBarTrack.setVisibility(View.GONE);
                    waveBarFill.clearAnimation();
                    ViewGroup.LayoutParams fillParams = waveBarFill.getLayoutParams();
                    fillParams.width = 0;
                    waveBarFill.setLayoutParams(fillParams);
                }
            } else {
                supportingRow.setVisibility(View.GONE);
                waveBarTrack.setVisibility(View.GONE);
                waveBarFill.clearAnimation();
                ViewGroup.LayoutParams fillParams = waveBarFill.getLayoutParams();
                fillParams.width = 0;
                waveBarFill.setLayoutParams(fillParams);
            }
        }
    }
}

package org.opensurfcast.ui;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;

import org.opensurfcast.R;
import org.opensurfcast.tide.CurrentPrediction;
import org.opensurfcast.tide.CurrentStation;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * RecyclerView adapter for displaying preferred current stations.
 * <p>
 * Displays station name, ID, interpolated current velocity, and a progress bar
 * showing position in the current cycle (slack to peak).
 */
public class CurrentListAdapter extends RecyclerView.Adapter<CurrentListAdapter.ViewHolder> {

    /**
     * Listener for station row clicks.
     */
    public interface OnStationClickListener {
        void onStationClick(CurrentStation station);
    }

    private static final double CM_PER_SEC_TO_KNOTS = 0.0194384;
    private static final long BAR_ANIM_DURATION_MS = 600;

    /**
     * Holds interpolated velocity, progress fraction, and upcoming event.
     *
     * @param velocityCmPerSec           Current velocity in cm/s.
     * @param progressFraction           Progress between peaks: 0 = previous peak, 1 = next peak.
     * @param upcomingType               Type of the upcoming event ("flood" or "ebb").
     * @param upcomingVelocityCmPerSec   Velocity of the upcoming event in cm/s.
     * @param upcomingEpochSeconds       Epoch seconds of the upcoming event.
     */
    public record CurrentProgress(double velocityCmPerSec, float progressFraction,
                                  String upcomingType, double upcomingVelocityCmPerSec,
                                  long upcomingEpochSeconds) {
    }

    private final List<CurrentStation> stations = new ArrayList<>();
    private final Map<String, CurrentProgress> currentProgress = new HashMap<>();
    private boolean useMetric;
    private OnStationClickListener clickListener;

    public boolean hasCurrentProgress(String stationId) {
        return currentProgress.containsKey(stationId);
    }

    /**
     * Sets the click listener for station rows.
     *
     * @param listener the listener to be notified of clicks
     */
    public void setOnStationClickListener(OnStationClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_current_station, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CurrentStation station = stations.get(position);
        CurrentProgress progress = currentProgress.get(station.id);
        holder.bind(station, progress, useMetric);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onStationClick(station);
            }
        });
    }

    @Override
    public int getItemCount() {
        return stations.size();
    }

    /**
     * Replaces the adapter data with the given list and refreshes the view.
     */
    public void submitList(List<CurrentStation> newStations) {
        submitList(newStations, null);
    }

    /**
     * Replaces the adapter data with the given list and current progress.
     *
     * @param newStations list of stations (null to clear)
     * @param progressMap map of station ID to CurrentProgress (null for empty)
     */
    public void submitList(List<CurrentStation> newStations, Map<String, CurrentProgress> progressMap) {
        stations.clear();
        currentProgress.clear();
        if (newStations != null) {
            stations.addAll(newStations);
        }
        if (progressMap != null) {
            currentProgress.putAll(progressMap);
        }
        notifyDataSetChanged();
    }

    /**
     * Sets the preferred unit system and refreshes the view.
     *
     * @param useMetric true for metric (cm/s), false for imperial (knots)
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
    public CurrentStation getStationAt(int position) {
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
    public void insertAt(int position, CurrentStation station) {
        stations.add(position, station);
        notifyItemInserted(position);
    }

    /**
     * Updates progress for a single station and notifies only if data changed.
     *
     * @param stationId station to update
     * @param progress  new progress data (null to clear)
     * @return true if the item was updated and a change was made
     */
    public boolean updateProgress(String stationId, CurrentProgress progress) {
        int position = indexOfStation(stationId);
        if (position < 0) return false;

        CurrentProgress existing = currentProgress.get(stationId);
        if (progress == null && existing == null) return false;
        if (progress != null && progress.equals(existing)) return false;

        if (progress != null) {
            currentProgress.put(stationId, progress);
        } else {
            currentProgress.remove(stationId);
        }
        notifyItemChanged(position);
        return true;
    }

    private int indexOfStation(String stationId) {
        for (int i = 0; i < stations.size(); i++) {
            if (stationId.equals(stations.get(i).id)) return i;
        }
        return -1;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView stationName;
        private final TextView currentVelocity;
        private final LinearLayout upcomingEventRow;
        private final TextView upcomingEventLabel;
        private final ImageView upcomingEventIndicator;
        private final TextView upcomingEventTime;
        private final FrameLayout currentBarTrack;
        private final View currentBarFill;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            stationName = itemView.findViewById(R.id.station_name);
            currentVelocity = itemView.findViewById(R.id.current_velocity);
            upcomingEventRow = itemView.findViewById(R.id.upcoming_event_row);
            upcomingEventLabel = itemView.findViewById(R.id.upcoming_event_label);
            upcomingEventIndicator = itemView.findViewById(R.id.upcoming_event_indicator);
            upcomingEventTime = itemView.findViewById(R.id.upcoming_event_time);
            currentBarTrack = itemView.findViewById(R.id.current_bar_track);
            currentBarFill = itemView.findViewById(R.id.current_bar_fill);
        }

        void bind(CurrentStation station, CurrentProgress progress, boolean useMetric) {
            // Build station name with ID suffix: "Station Name · ACT0091"
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

            // Current velocity
            if (progress != null) {
                double displayValue = useMetric ? progress.velocityCmPerSec
                        : progress.velocityCmPerSec * CM_PER_SEC_TO_KNOTS;
                int formatRes = useMetric ? R.string.current_velocity_metric
                        : R.string.current_velocity_imperial;
                currentVelocity.setText(String.format(Locale.US,
                        itemView.getContext().getString(formatRes), displayValue));
                currentVelocity.setVisibility(View.VISIBLE);
            } else {
                currentVelocity.setText(R.string.current_velocity_unknown);
                currentVelocity.setVisibility(View.VISIBLE);
            }

            // Upcoming event (next flood or ebb): <magnitude> <arrow> <date>
            if (progress != null) {
                boolean isFlood = CurrentPrediction.TYPE_FLOOD.equals(progress.upcomingType);

                double upcomingDisplay = useMetric ? progress.upcomingVelocityCmPerSec
                        : progress.upcomingVelocityCmPerSec * CM_PER_SEC_TO_KNOTS;
                int formatRes = useMetric ? R.string.current_upcoming_metric
                        : R.string.current_upcoming_imperial;
                upcomingEventLabel.setText(String.format(Locale.US,
                        itemView.getContext().getString(formatRes), upcomingDisplay));

                upcomingEventIndicator.setImageResource(isFlood
                        ? R.drawable.ic_tide_high : R.drawable.ic_tide_low);
                upcomingEventIndicator.setContentDescription(isFlood
                        ? itemView.getContext().getString(R.string.current_upcoming_flood)
                        : itemView.getContext().getString(R.string.current_upcoming_ebb));
                int arrowColor = isFlood
                        ? ContextCompat.getColor(itemView.getContext(), R.color.tide_indicator_up)
                        : MaterialColors.getColor(itemView, com.google.android.material.R.attr.colorError);
                upcomingEventIndicator.setColorFilter(arrowColor);

                upcomingEventTime.setText(formatUpcomingTime(progress.upcomingEpochSeconds));
                upcomingEventTime.setVisibility(View.VISIBLE);

                upcomingEventRow.setVisibility(View.VISIBLE);
            } else {
                upcomingEventRow.setVisibility(View.GONE);
            }

            // Current progress bar
            if (progress != null) {
                currentBarTrack.setVisibility(View.VISIBLE);
                currentBarFill.clearAnimation();

                float fraction = Math.max(0f, Math.min(progress.progressFraction, 1f));
                currentBarFill.post(() -> {
                    ViewGroup parent = (ViewGroup) currentBarTrack.getParent();
                    int availableWidth = parent.getWidth()
                            - parent.getPaddingStart() - parent.getPaddingEnd();
                    if (availableWidth <= 0) return;

                    int targetWidth = Math.max(1, (int) (availableWidth * fraction));

                    ViewGroup.LayoutParams fillParams = currentBarFill.getLayoutParams();
                    fillParams.width = targetWidth;
                    currentBarFill.setLayoutParams(fillParams);

                    ScaleAnimation scaleAnim = new ScaleAnimation(
                            0f, 1f, 1f, 1f,
                            Animation.RELATIVE_TO_SELF, 0f,
                            Animation.RELATIVE_TO_SELF, 0f);
                    scaleAnim.setDuration(BAR_ANIM_DURATION_MS);
                    scaleAnim.setInterpolator(new DecelerateInterpolator());
                    currentBarFill.startAnimation(scaleAnim);
                });
            } else {
                currentBarTrack.setVisibility(View.GONE);
                currentBarFill.clearAnimation();
                ViewGroup.LayoutParams fillParams = currentBarFill.getLayoutParams();
                fillParams.width = 0;
                currentBarFill.setLayoutParams(fillParams);
            }
        }

        /**
         * Formats the upcoming event time compactly:
         * - Today: "@ 2:35 PM"
         * - Other day: "@ Feb 12, 2:35 PM"
         */
        private String formatUpcomingTime(long epochSeconds) {
            TimeZone tz = TimeZone.getDefault();
            Date date = new Date(epochSeconds * 1000L);

            Calendar now = Calendar.getInstance(tz);
            Calendar target = Calendar.getInstance(tz);
            target.setTime(date);

            boolean isToday = now.get(Calendar.YEAR) == target.get(Calendar.YEAR)
                    && now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR);

            DateFormat fmt;
            if (isToday) {
                fmt = new SimpleDateFormat("h:mm a", Locale.getDefault());
            } else {
                fmt = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());
            }
            fmt.setTimeZone(tz);
            return itemView.getContext().getString(R.string.current_upcoming_time,
                    fmt.format(date));
        }
    }
}

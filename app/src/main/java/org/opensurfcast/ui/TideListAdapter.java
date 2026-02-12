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
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;

import org.opensurfcast.R;
import org.opensurfcast.tide.TideStation;

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
 * RecyclerView adapter for displaying preferred tide stations.
 * <p>
 * Displays station name, ID, interpolated current tide level, and a progress bar
 * showing position in the current tide cycle (low to high).
 */
public class TideListAdapter extends RecyclerView.Adapter<TideListAdapter.ViewHolder> {

    private static final double METERS_TO_FEET = 3.28084;
    private static final long BAR_ANIM_DURATION_MS = 600;

    /**
     * Holds interpolated level, progress fraction, and upcoming tide.
     *
     * @param upcomingTideMeters        Upcoming tide level in meters (next high or low).
     * @param upcomingTideIsHigh        True if upcoming tide is high, false if low.
     * @param upcomingTideEpochSeconds  Epoch seconds of the upcoming tide shift.
     */
    public record TideProgress(double levelMeters, float progressFraction,
                               double upcomingTideMeters, boolean upcomingTideIsHigh,
                               long upcomingTideEpochSeconds) {
    }

    private final List<TideStation> stations = new ArrayList<>();
    private final Map<String, TideProgress> currentProgress = new HashMap<>();
    private boolean useMetric;

    public boolean hasCurrentProgress(String stationId) {
        return currentProgress.containsKey(stationId);
    }

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
        TideProgress progress = currentProgress.get(station.id);
        holder.bind(station, progress, useMetric);
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
     * Replaces the adapter data with the given list and current tide progress.
     *
     * @param newStations list of stations (null to clear)
     * @param progressMap map of station ID to TideProgress (null for empty)
     */
    public void submitList(List<TideStation> newStations, Map<String, TideProgress> progressMap) {
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

    /**
     * Updates progress for a single station and notifies only if data changed.
     *
     * @param stationId station to update
     * @param progress  new progress data (null to clear)
     * @return true if the item was updated and a change was made
     */
    public boolean updateProgress(String stationId, TideProgress progress) {
        int position = indexOfStation(stationId);
        if (position < 0) return false;

        TideProgress existing = currentProgress.get(stationId);
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
        private final TextView tideLevel;
        private final LinearLayout upcomingTideRow;
        private final TextView upcomingTideLevel;
        private final ImageView upcomingTideIndicator;
        private final TextView upcomingTideTime;
        private final FrameLayout tideBarTrack;
        private final View tideBarFill;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            stationName = itemView.findViewById(R.id.station_name);
            tideLevel = itemView.findViewById(R.id.tide_level);
            upcomingTideRow = itemView.findViewById(R.id.upcoming_tide_row);
            upcomingTideLevel = itemView.findViewById(R.id.upcoming_tide_level);
            upcomingTideIndicator = itemView.findViewById(R.id.upcoming_tide_indicator);
            upcomingTideTime = itemView.findViewById(R.id.upcoming_tide_time);
            tideBarTrack = itemView.findViewById(R.id.tide_bar_track);
            tideBarFill = itemView.findViewById(R.id.tide_bar_fill);
        }

        void bind(TideStation station, TideProgress progress, boolean useMetric) {
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
            if (progress != null) {
                double displayValue = useMetric ? progress.levelMeters : progress.levelMeters * METERS_TO_FEET;
                int formatRes = useMetric ? R.string.tide_level_metric : R.string.tide_level_imperial;
                tideLevel.setText(String.format(Locale.US,
                        itemView.getContext().getString(formatRes), displayValue));
                tideLevel.setVisibility(View.VISIBLE);
            } else {
                tideLevel.setText(R.string.tide_level_unknown);
                tideLevel.setVisibility(View.VISIBLE);
            }

            // Upcoming tide (next high or low) with visual indicator and time
            if (progress != null) {
                double displayValue = useMetric ? progress.upcomingTideMeters
                        : progress.upcomingTideMeters * METERS_TO_FEET;
                int formatRes = useMetric ? R.string.tide_upcoming_metric : R.string.tide_upcoming_imperial;
                upcomingTideLevel.setText(String.format(Locale.US,
                        itemView.getContext().getString(formatRes), displayValue));
                upcomingTideIndicator.setImageResource(progress.upcomingTideIsHigh
                        ? R.drawable.ic_tide_high : R.drawable.ic_tide_low);
                upcomingTideIndicator.setContentDescription(progress.upcomingTideIsHigh
                        ? itemView.getContext().getString(R.string.tide_upcoming_high)
                        : itemView.getContext().getString(R.string.tide_upcoming_low));
                upcomingTideIndicator.setColorFilter(MaterialColors.getColor(itemView,
                        progress.upcomingTideIsHigh
                                ? com.google.android.material.R.attr.colorPrimary
                                : com.google.android.material.R.attr.colorTertiary));

                // Compact upcoming tide time
                upcomingTideTime.setText(formatUpcomingTime(
                        progress.upcomingTideEpochSeconds));
                upcomingTideTime.setVisibility(View.VISIBLE);

                upcomingTideRow.setVisibility(View.VISIBLE);
            } else {
                upcomingTideRow.setVisibility(View.GONE);
            }

            // Tide progress bar (unchanged)
            if (progress != null) {
                tideBarTrack.setVisibility(View.VISIBLE);
                tideBarFill.clearAnimation();

                float fraction = Math.max(0f, Math.min(progress.progressFraction, 1f));
                tideBarFill.post(() -> {
                    ViewGroup parent = (ViewGroup) tideBarTrack.getParent();
                    int availableWidth = parent.getWidth()
                            - parent.getPaddingStart() - parent.getPaddingEnd();
                    if (availableWidth <= 0) return;

                    int targetWidth = Math.max(1, (int) (availableWidth * fraction));

                    ViewGroup.LayoutParams fillParams = tideBarFill.getLayoutParams();
                    fillParams.width = targetWidth;
                    tideBarFill.setLayoutParams(fillParams);

                    ScaleAnimation scaleAnim = new ScaleAnimation(
                            0f, 1f, 1f, 1f,
                            Animation.RELATIVE_TO_SELF, 0f,
                            Animation.RELATIVE_TO_SELF, 0f);
                    scaleAnim.setDuration(BAR_ANIM_DURATION_MS);
                    scaleAnim.setInterpolator(new DecelerateInterpolator());
                    tideBarFill.startAnimation(scaleAnim);
                });
            } else {
                tideBarTrack.setVisibility(View.GONE);
                tideBarFill.clearAnimation();
                ViewGroup.LayoutParams fillParams = tideBarFill.getLayoutParams();
                fillParams.width = 0;
                tideBarFill.setLayoutParams(fillParams);
            }
        }

        /**
         * Formats the upcoming tide time compactly:
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
            return itemView.getContext().getString(R.string.tide_upcoming_time_today,
                    fmt.format(date));
        }
    }
}

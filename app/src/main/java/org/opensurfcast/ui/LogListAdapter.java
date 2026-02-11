package org.opensurfcast.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;

import org.opensurfcast.R;
import org.opensurfcast.log.LogEntry;
import org.opensurfcast.log.LogLevel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for displaying log entries.
 */
public class LogListAdapter extends RecyclerView.Adapter<LogListAdapter.ViewHolder> {

    private static final SimpleDateFormat TIME_FORMAT =
            new SimpleDateFormat("MMM dd, HH:mm:ss.SSS", Locale.US);

    private final List<LogEntry> entries = new ArrayList<>();

    /**
     * Replaces the adapter data with the given list and refreshes the view.
     */
    public void submitList(List<LogEntry> newEntries) {
        entries.clear();
        if (newEntries != null) {
            entries.addAll(newEntries);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_log_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(entries.get(position));
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final View levelIndicator;
        private final TextView logLevel;
        private final TextView logTimestamp;
        private final TextView logMessage;
        private final TextView logStackTrace;

        private boolean stackTraceExpanded = false;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            levelIndicator = itemView.findViewById(R.id.level_indicator);
            logLevel = itemView.findViewById(R.id.log_level);
            logTimestamp = itemView.findViewById(R.id.log_timestamp);
            logMessage = itemView.findViewById(R.id.log_message);
            logStackTrace = itemView.findViewById(R.id.log_stack_trace);
        }

        void bind(LogEntry entry) {
            Context context = itemView.getContext();

            // Level text
            logLevel.setText(entry.getLevel().name());

            // Timestamp
            logTimestamp.setText(TIME_FORMAT.format(new Date(entry.getTimestamp())));

            // Message
            logMessage.setText(entry.getMessage());

            // Level indicator color
            int color = getLevelColor(context, entry.getLevel());
            GradientDrawable dot = new GradientDrawable();
            dot.setShape(GradientDrawable.OVAL);
            dot.setColor(color);
            levelIndicator.setBackground(dot);

            // Level text color matches indicator
            logLevel.setTextColor(color);

            // Stack trace
            if (entry.getStackTrace() != null && !entry.getStackTrace().isEmpty()) {
                logStackTrace.setText(entry.getStackTrace());
                stackTraceExpanded = false;
                logStackTrace.setVisibility(View.GONE);

                itemView.setOnClickListener(v -> {
                    stackTraceExpanded = !stackTraceExpanded;
                    logStackTrace.setVisibility(stackTraceExpanded ? View.VISIBLE : View.GONE);
                });
            } else {
                logStackTrace.setVisibility(View.GONE);
                logStackTrace.setText(null);
                itemView.setOnClickListener(null);
                itemView.setClickable(false);
            }
        }

        private int getLevelColor(Context context, LogLevel level) {
            switch (level) {
                case DEBUG:
                    return MaterialColors.getColor(itemView,
                            com.google.android.material.R.attr.colorOutline);
                case INFO:
                    return MaterialColors.getColor(itemView,
                            com.google.android.material.R.attr.colorPrimary);
                case WARN:
                    boolean nightMode = (context.getResources().getConfiguration().uiMode
                            & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
                    return ContextCompat.getColor(context,
                            nightMode ? R.color.log_warn_dark : R.color.log_warn_light);
                case ERROR:
                    return MaterialColors.getColor(itemView,
                            com.google.android.material.R.attr.colorError);
                default:
                    return MaterialColors.getColor(itemView,
                            com.google.android.material.R.attr.colorOutline);
            }
        }
    }
}

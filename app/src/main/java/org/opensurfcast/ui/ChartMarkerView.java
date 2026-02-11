package org.opensurfcast.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import com.google.android.material.color.MaterialColors;

import org.opensurfcast.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * A popup annotation that appears when a data point is selected on a chart.
 * Shows the observation date/time and the formatted value.
 */
public class ChartMarkerView extends MarkerView {

    /**
     * Callback to format the Y-value into a display string (e.g. "5.2 ft").
     */
    public interface ValueFormatter {
        String format(float yValue);
    }

    private final TextView dateText;
    private final TextView valueText;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MMM d, yyyy  HH:mm", Locale.US);
    private final ValueFormatter valueFormatter;

    /**
     * Creates a marker view.
     *
     * @param context        the context
     * @param valueFormatter formats the Y-value for display
     */
    public ChartMarkerView(Context context, ValueFormatter valueFormatter) {
        super(context, R.layout.chart_marker_view);
        this.valueFormatter = valueFormatter;

        dateText = findViewById(R.id.marker_date);
        valueText = findViewById(R.id.marker_value);

        applyThemedBackground(context);
        applyThemedTextColors(context);
    }

    private void applyThemedBackground(Context context) {
        int bgColor = resolveThemeColor(context,
                com.google.android.material.R.attr.colorSurfaceContainerHigh);
        int strokeColor = resolveThemeColor(context,
                com.google.android.material.R.attr.colorOutlineVariant);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(context, 8));
        bg.setColor(bgColor);
        bg.setStroke(dpToPx(context, 1), strokeColor);

        LinearLayout root = findViewById(R.id.marker_root);
        root.setBackground(bg);
    }

    private void applyThemedTextColors(Context context) {
        int dateColor = resolveThemeColor(context,
                com.google.android.material.R.attr.colorOnSurfaceVariant);
        int valColor = resolveThemeColor(context,
                com.google.android.material.R.attr.colorOnSurface);
        dateText.setTextColor(dateColor);
        valueText.setTextColor(valColor);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        // X-axis is epoch seconds
        long epochMillis = (long) e.getX() * 1000L;
        dateText.setText(dateFormat.format(new Date(epochMillis)));
        valueText.setText(valueFormatter.format(e.getY()));

        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        // Centre horizontally above the highlight point
        return new MPPointF(-(getWidth() / 2f), -getHeight() - dpToPx(getContext(), 8));
    }

    @Override
    public void draw(Canvas canvas, float posX, float posY) {
        // Clamp so the marker stays within the chart bounds
        float width = getWidth();
        float height = getHeight();
        float offsetX = getOffset().x;
        float offsetY = getOffset().y;

        float drawX = posX + offsetX;
        float drawY = posY + offsetY;

        // Keep within left/right edges
        if (drawX < 0) {
            drawX = 0;
        } else if (drawX + width > canvas.getWidth()) {
            drawX = canvas.getWidth() - width;
        }

        // If clipped above the top, flip below the point
        if (drawY < 0) {
            drawY = posY + dpToPx(getContext(), 8);
        }

        canvas.save();
        canvas.translate(drawX, drawY);
        draw(canvas);
        canvas.restore();
    }

    // ========================================================================
    // Utility
    // ========================================================================

    private static int resolveThemeColor(Context context, int attr) {
        return MaterialColors.getColor(context, attr, 0);
    }

    private static int dpToPx(Context context, int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
    }
}

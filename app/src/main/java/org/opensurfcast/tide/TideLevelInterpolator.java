package org.opensurfcast.tide;

import androidx.annotation.Nullable;

import java.util.List;

/**
 * Interpolates the current tide level between two high/low tide predictions
 * using a sine wave. The sine function provides a smooth S-curve approximation
 * of tidal motion between consecutive shift points.
 */
public final class TideLevelInterpolator {

    private TideLevelInterpolator() {
    }

    /**
     * Interpolates the water level at the given epoch time from a sorted list
     * of tide predictions.
     *
     * @param predictions     list of predictions sorted by epoch_seconds ASC
     * @param nowEpochSeconds  current time as Unix epoch seconds
     * @return interpolated level in meters, or null if outside prediction range
     *         or if there are fewer than two predictions
     */
    @Nullable
    public static Double interpolate(List<TidePrediction> predictions, long nowEpochSeconds) {
        if (predictions == null || predictions.size() < 2) {
            return null;
        }

        // Find the segment where t1 <= now < t2
        TidePrediction prev = null;
        for (TidePrediction p : predictions) {
            if (p.epochSeconds > nowEpochSeconds) {
                if (prev == null) {
                    // now is before first prediction
                    return null;
                }
                // Interpolate between prev and p
                long t1 = prev.epochSeconds;
                long t2 = p.epochSeconds;
                double h1 = prev.value;
                double h2 = p.value;

                double phase = (double) (nowEpochSeconds - t1) / (t2 - t1);
                double level = h1 + (h2 - h1) * (1.0 - Math.cos(Math.PI * phase)) / 2.0;
                return level;
            }
            prev = p;
        }

        // now is after last prediction
        return null;
    }

    /**
     * Result of tide interpolation with progress within the current segment.
     */
    public static final class Result {
        /** Interpolated water level in meters. */
        public final double levelMeters;
        /** Progress in the current segment: 0 = low tide, 1 = high tide. */
        public final float progressFraction;
        /** Upcoming tide level in meters (the next high or low). */
        public final double upcomingTideMeters;
        /** True if the upcoming tide is high, false if low. */
        public final boolean upcomingTideIsHigh;
        /** Epoch seconds of the upcoming tide shift. */
        public final long upcomingTideEpochSeconds;

        public Result(double levelMeters, float progressFraction,
                      double upcomingTideMeters, boolean upcomingTideIsHigh,
                      long upcomingTideEpochSeconds) {
            this.levelMeters = levelMeters;
            this.progressFraction = progressFraction;
            this.upcomingTideMeters = upcomingTideMeters;
            this.upcomingTideIsHigh = upcomingTideIsHigh;
            this.upcomingTideEpochSeconds = upcomingTideEpochSeconds;
        }
    }

    /**
     * Interpolates the water level at the given epoch time and returns both
     * the level and progress fraction within the current high/low segment,
     * plus the upcoming tide (next high or low).
     *
     * @param predictions     list of predictions sorted by epoch_seconds ASC
     * @param nowEpochSeconds current time as Unix epoch seconds
     * @return result with level, progress, and upcoming tide; or null if
     *         outside prediction range or fewer than two predictions
     */
    @Nullable
    public static Result interpolateWithProgress(List<TidePrediction> predictions,
                                                long nowEpochSeconds) {
        if (predictions == null || predictions.size() < 2) {
            return null;
        }

        TidePrediction prev = null;
        for (TidePrediction p : predictions) {
            if (p.epochSeconds > nowEpochSeconds) {
                if (prev == null) {
                    return null;
                }
                long t1 = prev.epochSeconds;
                long t2 = p.epochSeconds;
                double h1 = prev.value;
                double h2 = p.value;

                double phase = (double) (nowEpochSeconds - t1) / (t2 - t1);
                double level = h1 + (h2 - h1) * (1.0 - Math.cos(Math.PI * phase)) / 2.0;

                double minH = Math.min(h1, h2);
                double maxH = Math.max(h1, h2);
                float progressFraction = (maxH > minH)
                        ? (float) ((level - minH) / (maxH - minH))
                        : 0.5f;

                // p is the upcoming tide (first prediction after now)
                return new Result(level, progressFraction, p.value, p.isHighTide(),
                        p.epochSeconds);
            }
            prev = p;
        }

        return null;
    }
}

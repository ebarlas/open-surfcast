package org.opensurfcast.tide;

import androidx.annotation.Nullable;

import java.util.List;

/**
 * Interpolates the current velocity between consecutive predictions using
 * cosine/sine quarter-wave segments anchored on slack points.
 * <p>
 * All prediction types (flood, ebb, slack) are used as interpolation anchors.
 * The curve passes through zero exactly at each slack time:
 * <ul>
 *   <li><b>Peak &rarr; Slack:</b> {@code v = v_peak * cos(&pi;/2 * phase)}</li>
 *   <li><b>Slack &rarr; Peak:</b> {@code v = v_peak * sin(&pi;/2 * phase)}</li>
 *   <li><b>Peak &rarr; Peak (fallback):</b> cosine half-wave</li>
 * </ul>
 * This produces a single smooth curve that is flat at each peak and crosses
 * through zero with non-zero slope at each slack, similar to a sine wave.
 * <p>
 * Predictions are expected to be sorted by epoch_seconds ASC and typically
 * follow a pattern: slack &rarr; max-flood &rarr; slack &rarr; max-ebb &rarr; slack &rarr; &hellip;
 */
public final class CurrentVelocityInterpolator {

    private CurrentVelocityInterpolator() {
    }

    /**
     * Interpolates the current velocity at the given epoch time from a sorted
     * list of current predictions. All prediction types (flood, ebb, slack)
     * are used as interpolation anchors. The interpolation formula varies by
     * segment type:
     * <ul>
     *   <li>Peak &rarr; Slack: cosine quarter-wave ({@code v_peak * cos(&pi;/2 * phase)})</li>
     *   <li>Slack &rarr; Peak: sine quarter-wave ({@code v_peak * sin(&pi;/2 * phase)})</li>
     *   <li>Peak &rarr; Peak: cosine half-wave (fallback for missing slack)</li>
     * </ul>
     *
     * @param predictions    list of predictions sorted by epoch_seconds ASC
     * @param nowEpochSeconds current time as Unix epoch seconds
     * @return interpolated velocity in cm/s, or null if outside prediction range
     *         or if there are fewer than two predictions
     */
    @Nullable
    public static Double interpolate(List<CurrentPrediction> predictions, long nowEpochSeconds) {
        if (predictions == null || predictions.size() < 2) {
            return null;
        }

        // Find the segment where t1 <= now < t2
        CurrentPrediction prev = null;
        for (CurrentPrediction p : predictions) {
            if (p.epochSeconds > nowEpochSeconds) {
                if (prev == null) {
                    return null;
                }
                long t1 = prev.epochSeconds;
                long t2 = p.epochSeconds;
                double v1 = prev.velocityMajor;
                double v2 = p.velocityMajor;

                double phase = (double) (nowEpochSeconds - t1) / (t2 - t1);

                if (prev.isSlack()) {
                    // Slack -> Peak: sine quarter-wave
                    return v2 * Math.sin(Math.PI / 2.0 * phase);
                } else if (p.isSlack()) {
                    // Peak -> Slack: cosine quarter-wave
                    return v1 * Math.cos(Math.PI / 2.0 * phase);
                } else {
                    // Peak -> Peak (no slack between): cosine half-wave
                    return v1 + (v2 - v1) * (1.0 - Math.cos(Math.PI * phase)) / 2.0;
                }
            }
            prev = p;
        }

        return null;
    }

    /**
     * Result of current velocity interpolation with progress and upcoming event.
     */
    public static final class Result {
        /** Interpolated velocity in cm/s. Positive = flood, negative = ebb. */
        public final double velocityCmPerSec;
        /** Flood-ebb progress: 0 = max-ebb (empty bar), 1 = max-flood (full bar). */
        public final float progressFraction;
        /** Upcoming prediction velocity in cm/s. */
        public final double upcomingVelocityCmPerSec;
        /** Type of the upcoming prediction ("flood" or "ebb"). */
        public final String upcomingType;
        /** Epoch seconds of the upcoming prediction. */
        public final long upcomingEpochSeconds;

        public Result(double velocityCmPerSec, float progressFraction,
                      double upcomingVelocityCmPerSec, String upcomingType,
                      long upcomingEpochSeconds) {
            this.velocityCmPerSec = velocityCmPerSec;
            this.progressFraction = progressFraction;
            this.upcomingVelocityCmPerSec = upcomingVelocityCmPerSec;
            this.upcomingType = upcomingType;
            this.upcomingEpochSeconds = upcomingEpochSeconds;
        }
    }

    /**
     * Interpolates the current velocity at the given epoch time and returns
     * both the velocity and progress fraction between consecutive peaks,
     * plus the upcoming peak event (next flood or ebb).
     * <p>
     * Progress represents a flood-ebb level analogous to the tide high-low
     * level: 0 at max-ebb (empty bar), approximately 0.5 at slack, and 1 at
     * max-flood (full bar). The bar fills when approaching flood and drains
     * when approaching ebb. Slack predictions are skipped when determining
     * the upcoming event.
     *
     * @param predictions    list of predictions sorted by epoch_seconds ASC
     * @param nowEpochSeconds current time as epoch seconds
     * @return result with velocity, progress, and upcoming event; or null if
     *         outside prediction range or fewer than two predictions
     */
    @Nullable
    public static Result interpolateWithProgress(List<CurrentPrediction> predictions,
                                                 long nowEpochSeconds) {
        if (predictions == null || predictions.size() < 2) {
            return null;
        }

        Double velocity = interpolate(predictions, nowEpochSeconds);
        if (velocity == null) {
            return null;
        }

        // Find the previous peak (last non-slack prediction at or before now)
        CurrentPrediction prevPeak = null;
        for (CurrentPrediction p : predictions) {
            if (p.epochSeconds > nowEpochSeconds) break;
            if (!p.isSlack()) {
                prevPeak = p;
            }
        }

        // Find the next peak (first non-slack prediction after now)
        CurrentPrediction nextPeak = null;
        for (CurrentPrediction p : predictions) {
            if (p.epochSeconds > nowEpochSeconds && !p.isSlack()) {
                nextPeak = p;
                break;
            }
        }

        if (nextPeak == null) {
            return null;
        }

        // Compute flood-ebb progress: 0 = max-ebb, 1 = max-flood.
        // Time-based fraction between peaks, inverted when approaching ebb
        // so that the bar drains toward ebb (like tide bar drains toward low).
        float progressFraction;
        if (prevPeak != null && nextPeak.epochSeconds > prevPeak.epochSeconds) {
            float timeFraction = (float) (nowEpochSeconds - prevPeak.epochSeconds)
                    / (float) (nextPeak.epochSeconds - prevPeak.epochSeconds);
            timeFraction = Math.max(0f, Math.min(timeFraction, 1f));
            progressFraction = nextPeak.isEbb() ? 1f - timeFraction : timeFraction;
        } else {
            progressFraction = nextPeak.isFlood() ? 0f : 1f;
        }

        return new Result(velocity, progressFraction, nextPeak.velocityMajor,
                nextPeak.type, nextPeak.epochSeconds);
    }
}

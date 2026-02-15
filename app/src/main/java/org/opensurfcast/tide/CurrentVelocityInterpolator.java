package org.opensurfcast.tide;

import androidx.annotation.Nullable;

import java.util.List;

/**
 * Interpolates the current velocity between consecutive predictions using
 * cubic Hermite splines with Catmull-Rom tangent estimation.
 * <p>
 * All prediction types (flood, ebb, slack) are used as interpolation anchors.
 * The curve passes through zero exactly at each slack time, is flat at each
 * peak, and has C1 continuity (smooth slope) at all boundaries.
 * <p>
 * Slope selection:
 * <ul>
 *   <li><b>Peak (flood/ebb):</b> slope = 0</li>
 *   <li><b>Slack:</b> slope = chord from prev peak to next peak</li>
 *   <li><b>Boundaries:</b> one-sided finite difference</li>
 * </ul>
 * <p>
 * Predictions are expected to be sorted by epoch_seconds ASC and typically
 * follow a pattern: slack &rarr; max-flood &rarr; slack &rarr; max-ebb &rarr; slack &rarr; &hellip;
 */
public final class CurrentVelocityInterpolator {

    private CurrentVelocityInterpolator() {
    }

    /**
     * Computes slopes (dv/dt) for each prediction using Catmull-Rom style
     * tangent estimation. Peaks get slope 0; slacks get chord slope between
     * adjacent peaks; boundaries use one-sided finite difference.
     */
    private static double[] computeSlopes(List<CurrentPrediction> predictions) {
        int n = predictions.size();
        double[] slopes = new double[n];

        for (int i = 0; i < n; i++) {
            CurrentPrediction curr = predictions.get(i);
            if (curr.isFlood() || curr.isEbb()) {
                slopes[i] = 0.0;
            } else {
                // Slack: chord slope from prev peak to next peak
                CurrentPrediction prevPeak = null;
                CurrentPrediction nextPeak = null;
                for (int j = i - 1; j >= 0; j--) {
                    if (!predictions.get(j).isSlack()) {
                        prevPeak = predictions.get(j);
                        break;
                    }
                }
                for (int j = i + 1; j < n; j++) {
                    if (!predictions.get(j).isSlack()) {
                        nextPeak = predictions.get(j);
                        break;
                    }
                }
                if (prevPeak != null && nextPeak != null) {
                    long dt = nextPeak.epochSeconds - prevPeak.epochSeconds;
                    slopes[i] = dt != 0
                            ? (nextPeak.velocityMajor - prevPeak.velocityMajor) / (double) dt
                            : 0.0;
                } else if (prevPeak != null && i > 0) {
                    // One-sided: slope from prev to curr
                    long dt = curr.epochSeconds - prevPeak.epochSeconds;
                    slopes[i] = dt != 0
                            ? (curr.velocityMajor - prevPeak.velocityMajor) / (double) dt
                            : 0.0;
                } else if (nextPeak != null && i < n - 1) {
                    // One-sided: slope from curr to next
                    long dt = nextPeak.epochSeconds - curr.epochSeconds;
                    slopes[i] = dt != 0
                            ? (nextPeak.velocityMajor - curr.velocityMajor) / (double) dt
                            : 0.0;
                } else {
                    slopes[i] = 0.0;
                }
            }
        }

        // Boundary points: overwrite with one-sided finite difference if needed
        if (n >= 2) {
            CurrentPrediction p0 = predictions.get(0);
            CurrentPrediction p1 = predictions.get(1);
            if (p0.isFlood() || p0.isEbb()) {
                slopes[0] = 0.0; // peak stays 0
            } else {
                long dt = p1.epochSeconds - p0.epochSeconds;
                slopes[0] = dt != 0
                        ? (p1.velocityMajor - p0.velocityMajor) / (double) dt
                        : 0.0;
            }
            CurrentPrediction pn1 = predictions.get(n - 2);
            CurrentPrediction pn = predictions.get(n - 1);
            if (pn.isFlood() || pn.isEbb()) {
                slopes[n - 1] = 0.0; // peak stays 0
            } else {
                long dt = pn.epochSeconds - pn1.epochSeconds;
                slopes[n - 1] = dt != 0
                        ? (pn.velocityMajor - pn1.velocityMajor) / (double) dt
                        : 0.0;
            }
        }

        return slopes;
    }

    /**
     * Evaluates cubic Hermite interpolation at phase t in [0,1].
     * v = h00*v1 + h10*dt*m1 + h01*v2 + h11*dt*m2
     */
    private static double hermite(double t, double v1, double v2, double m1, double m2, long dt) {
        double t2 = t * t;
        double t3 = t2 * t;
        double h00 = 2 * t3 - 3 * t2 + 1;
        double h10 = t3 - 2 * t2 + t;
        double h01 = -2 * t3 + 3 * t2;
        double h11 = t3 - t2;
        double dtD = (double) dt;
        return h00 * v1 + h10 * dtD * m1 + h01 * v2 + h11 * dtD * m2;
    }

    /**
     * Interpolates the current velocity at the given epoch time from a sorted
     * list of current predictions using cubic Hermite splines. All prediction
     * types (flood, ebb, slack) are used as interpolation anchors.
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

        double[] slopes = computeSlopes(predictions);

        // Find the segment where t1 <= now < t2
        for (int i = 0; i < predictions.size() - 1; i++) {
            CurrentPrediction prev = predictions.get(i);
            CurrentPrediction p = predictions.get(i + 1);
            if (p.epochSeconds > nowEpochSeconds) {
                long t1 = prev.epochSeconds;
                long t2 = p.epochSeconds;
                long dt = t2 - t1;
                if (dt <= 0) continue;

                double v1 = prev.velocityMajor;
                double v2 = p.velocityMajor;
                double m1 = slopes[i];
                double m2 = slopes[i + 1];

                double phase = (double) (nowEpochSeconds - t1) / (double) dt;
                return hermite(phase, v1, v2, m1, m2, dt);
            }
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

package org.opensurfcast.tide;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Interpolates the current velocity between consecutive max-flood and max-ebb
 * predictions using a sine wave. Slack predictions are excluded from the
 * interpolation anchors -- the sine curve between alternating flood (+V) and
 * ebb (-V) peaks naturally passes through zero between them.
 * <p>
 * Predictions are expected to be sorted by epoch_seconds ASC and typically
 * follow a pattern: slack -> max-flood -> slack -> max-ebb -> slack -> ...
 * Only the flood and ebb entries are used as interpolation control points.
 */
public final class CurrentVelocityInterpolator {

    private CurrentVelocityInterpolator() {
    }

    /**
     * Returns a filtered list containing only non-slack predictions (flood and ebb).
     */
    private static List<CurrentPrediction> filterAnchors(List<CurrentPrediction> predictions) {
        List<CurrentPrediction> anchors = new ArrayList<>();
        for (CurrentPrediction p : predictions) {
            if (!p.isSlack()) {
                anchors.add(p);
            }
        }
        return anchors;
    }

    /**
     * Interpolates the current velocity at the given epoch time from a sorted
     * list of current predictions. Only flood and ebb predictions are used as
     * interpolation anchors; slack predictions are skipped.
     *
     * @param predictions    list of predictions sorted by epoch_seconds ASC
     * @param nowEpochSeconds current time as Unix epoch seconds
     * @return interpolated velocity in cm/s, or null if outside prediction range
     *         or if there are fewer than two non-slack predictions
     */
    @Nullable
    public static Double interpolate(List<CurrentPrediction> predictions, long nowEpochSeconds) {
        if (predictions == null || predictions.size() < 2) {
            return null;
        }

        List<CurrentPrediction> anchors = filterAnchors(predictions);
        if (anchors.size() < 2) {
            return null;
        }

        // Find the segment where t1 <= now < t2 among anchors
        CurrentPrediction prev = null;
        for (CurrentPrediction p : anchors) {
            if (p.epochSeconds > nowEpochSeconds) {
                if (prev == null) {
                    return null;
                }
                long t1 = prev.epochSeconds;
                long t2 = p.epochSeconds;
                double v1 = prev.velocityMajor;
                double v2 = p.velocityMajor;

                double phase = (double) (nowEpochSeconds - t1) / (t2 - t1);
                double velocity = v1 + (v2 - v1) * (1.0 - Math.cos(Math.PI * phase)) / 2.0;
                return velocity;
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
        /** Progress in the current cycle: 0 = slack, 1 = peak (flood or ebb). */
        public final float progressFraction;
        /** Upcoming prediction velocity in cm/s. */
        public final double upcomingVelocityCmPerSec;
        /** Type of the upcoming prediction ("flood", "ebb", or "slack"). */
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
     * both the velocity and progress fraction within the current segment,
     * plus the upcoming event (next slack, flood, or ebb from the full list).
     * <p>
     * Interpolation uses only flood/ebb anchors (slack excluded). The upcoming
     * event is taken from the full (unfiltered) prediction list so that slack
     * events are still reported.
     * <p>
     * Progress is computed as the absolute velocity relative to the peak
     * velocity in the segment: 0 at slack, 1 at max flood/ebb.
     *
     * @param predictions    list of predictions sorted by epoch_seconds ASC
     * @param nowEpochSeconds current time as epoch seconds
     * @return result with velocity, progress, and upcoming event; or null if
     *         outside prediction range or fewer than two non-slack predictions
     */
    @Nullable
    public static Result interpolateWithProgress(List<CurrentPrediction> predictions,
                                                 long nowEpochSeconds) {
        if (predictions == null || predictions.size() < 2) {
            return null;
        }

        // Interpolate using flood/ebb anchors only
        Double velocity = interpolate(predictions, nowEpochSeconds);
        if (velocity == null) {
            return null;
        }

        // Find the enclosing anchor segment for progress calculation
        List<CurrentPrediction> anchors = filterAnchors(predictions);
        double peakAbsVelocity = 0;
        for (int i = 1; i < anchors.size(); i++) {
            if (anchors.get(i).epochSeconds > nowEpochSeconds) {
                peakAbsVelocity = Math.max(
                        Math.abs(anchors.get(i - 1).velocityMajor),
                        Math.abs(anchors.get(i).velocityMajor));
                break;
            }
        }

        float progressFraction = (peakAbsVelocity > 0)
                ? (float) (Math.abs(velocity) / peakAbsVelocity)
                : 0f;

        // Find the upcoming event from the full (unfiltered) list
        // so that slack events are still reported
        CurrentPrediction upcoming = null;
        for (CurrentPrediction p : predictions) {
            if (p.epochSeconds > nowEpochSeconds) {
                upcoming = p;
                break;
            }
        }

        if (upcoming == null) {
            return null;
        }

        return new Result(velocity, progressFraction, upcoming.velocityMajor,
                upcoming.type, upcoming.epochSeconds);
    }
}

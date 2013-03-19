/**
 * 
 */
package edu.berkeley.nlp.util;

/**
 * Provides a scaling system, to avoid numeric overflow and underflow with IEEE 64-bit double precision floating-point
 * numbers (limited to ~e^709).
 * 
 * @author petrov
 */
public class ScalingTools {

    private static final int LOGSCALE = 100;
    public static final double SCALE = Math.exp(LOGSCALE);

    public static double calcScaleFactor(final double logScale) {
        return calcScaleFactor(logScale, SCALE);
    }

    public static double calcScaleFactor(final double logScale, final double scale) {

        if (logScale == Integer.MIN_VALUE) {
            return 0.0;
        }

        if (logScale == 0.0) {
            return 1.0;
        }

        if (logScale == 1.0) {
            return scale;
        }

        if (logScale == 2.0) {
            return scale * scale;
        }

        if (logScale == 3.0) {
            return scale * scale * scale;
        }

        if (logScale == -1.0) {
            return 1.0 / scale;
        }

        if (logScale == -2.0) {
            return 1.0 / scale / scale;
        }

        if (logScale == -3.0) {
            return 1.0 / scale / scale / scale;
        }

        return Math.pow(scale, logScale);
    }

    public static int scaleArray(final double[] scores, final int previousScale) {

        if (previousScale == Integer.MIN_VALUE) {
            return previousScale;
        }

        int logScale = 0;
        double scale = 1.0;
        double max = ArrayUtil.max(scores);
        if (max == Double.POSITIVE_INFINITY) {
            return 0;
        }

        if (max == 0) {
            return previousScale;
        }

        while (max > SCALE) {
            max /= SCALE;
            scale *= SCALE;
            logScale += 1;
        }

        while (max > 0.0 && max < 1.0 / SCALE) {
            max *= SCALE;
            scale /= SCALE;
            logScale -= 1;
        }

        if (logScale != 0) {
            for (int i = 0; i < scores.length; i++) {
                scores[i] /= scale;
            }
        }

        return previousScale + logScale;
    }
}

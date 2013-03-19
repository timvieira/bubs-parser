package edu.berkeley.nlp.util;

/**
 * Provides a scaling system, to avoid numeric overflow and underflow with IEEE 64-bit double precision floating-point
 * numbers. Scales the actual value by a series of (large) constants, effectively expanding the exponent beyond its
 * normal minimum of approximately -307 (equivalent to e^-709).
 * 
 * Scaling is performed in steps of fixed size <i>s</i> (Defined by {@link #SCALE1}), and ensures that the final
 * (scaled) value remains between <i>1/s</i> and <i<s</i>.
 * 
 * The value of <i>s</i> is relatively unimportant; a larger <i>s</i> will result in fewer scaling steps, but
 * (potentially) lose some precision if the range of a scaled array is very large. However, given the limited precision
 * of IEEE double-precision arithmetic, an array of very wide range will lose some precision regardless of the choice of
 * <i>s</i>, and that loss is rarely important during grammar learning. In practice, any value between about 1e20
 * (~e^22) and 1e100 (~e^230) ought to work.
 * 
 * @author petrov
 */
public class IEEEDoubleScaling {

    static final int LN_S = 100;

    // Precompute constants for the most common scaling factors
    static final double SCALE1 = Math.exp(LN_S);
    private static final double SCALE2 = SCALE1 * SCALE1;
    private static final double SCALE3 = SCALE1 * SCALE1;
    private static final double SCALE_1 = 1 / SCALE1;
    private static final double SCALE_2 = 1 / SCALE1 / SCALE1;
    private static final double SCALE_3 = 1 / SCALE1 / SCALE1 / SCALE1;

    private static final double LOG_SCALE1 = Math.log(SCALE1);

    /**
     * Returns the multiplier required to scale an IEEE floating-point number by the specified number of steps
     * 
     * @param scaleStep
     * @return The multiplier required to scale an IEEE floating-point number by the specified number of steps
     */
    public static double scalingMultiplier(final int scaleStep) {

        switch (scaleStep) {
        case 0:
            return 1.0;
        case 1:
            return SCALE1;
        case 2:
            return SCALE2;
        case 3:
            return SCALE3;
        case -1:
            return SCALE_1;
        case -2:
            return SCALE_2;
        case -3:
            return SCALE_3;
        default:
            return Math.pow(SCALE1, scaleStep);
        }
    }

    /**
     * Returns the log probability represented by a scaled score
     * 
     * @param scaledScore
     * @param scaleStep
     * @return the log probability represented by a scaled score
     */
    public static double logLikelihood(final double scaledScore, final int scaleStep) {
        return Math.log(scaledScore) + LOG_SCALE1 * scaleStep;
    }

    /**
     * Rescales the supplied score array in place, constraining the maximum entry to between 1/e^SCALE1 and e^SCALE1.
     * 
     * @param scores
     * @param previousScaleStep
     * @return The new scaling step required to properly constrain the array
     */
    public static int scaleArray(final double[] scores, final int previousScaleStep) {

        double max = ArrayUtil.max(scores);

        // If max is between 1/SCALE1 and SCALE1, we're fine
        if (max > SCALE_1 && max < SCALE1) {
            return previousScaleStep;
        }

        if (max == Double.POSITIVE_INFINITY) {
            return 0;
        }

        // Compute a multiplier (an exponentiation of SCALE1) that will scale the maximum observed value between
        // 1/SCALE1 and SCALE1
        int scaleFactor = 0;
        double multiplier = 1.0;

        while (max > SCALE1) {
            max *= SCALE_1;
            multiplier *= SCALE_1;
            scaleFactor++;
        }

        while (max < SCALE_1) {
            max *= SCALE1;
            multiplier *= SCALE1;
            scaleFactor--;
        }

        for (int i = 0; i < scores.length; i++) {
            scores[i] *= multiplier;
        }

        return previousScaleStep + scaleFactor;
    }
}

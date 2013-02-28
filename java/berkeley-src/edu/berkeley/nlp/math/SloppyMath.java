package edu.berkeley.nlp.math;

/**
 * The class <code>SloppyMath</code> contains methods for performing basic numeric operations. In some cases, such as
 * max and min, they cut a few corners in the implementation for the sake of efficiency. In particular, they may not
 * handle special notions like NaN and -0.0 correctly. This was the origin of the class name, but some other operations
 * are just useful math additions, such as logSum.
 * 
 * @author Christopher Manning
 * @version 2003/01/02
 */
public final class SloppyMath {

    /**
     * Returns true if the argument is a "dangerous" double to have around, namely one that is infinite, NaN or zero.
     */
    public static boolean isDangerous(final double d) {
        return Double.isInfinite(d) || Double.isNaN(d) || d == 0.0;
    }

    public static boolean isDangerous(final float d) {
        return Float.isInfinite(d) || Float.isNaN(d) || d == 0.0;
    }

    /**
     * Returns true if the argument is a "very dangerous" double to have around, namely one that is infinite or NaN.
     */
    public static boolean isVeryDangerous(final double d) {
        return Double.isInfinite(d) || Double.isNaN(d);
    }

    /**
     * If a difference is bigger than this in log terms, then the sum or difference of them will just be the larger (to
     * 12 or so decimal places for double, and 7 or 8 for float).
     */
    public static final double LOGTOLERANCE = 30.0;
    static final float LOGTOLERANCE_F = 10.0f;

    /**
     * Returns the log of the sum of two numbers, which are themselves input in log form. This uses natural logarithms.
     * Reasonable care is taken to do this as efficiently as possible (under the assumption that the numbers might
     * differ greatly in magnitude), with high accuracy, and without numerical overflow. Also, handle correctly the case
     * of arguments being -Inf (e.g., probability 0).
     * 
     * @param lx First number, in log form
     * @param ly Second number, in log form
     * @return log(exp(lx) + exp(ly))
     */
    public static float logAdd(final float lx, final float ly) {
        float max, negDiff;
        if (lx > ly) {
            max = lx;
            negDiff = ly - lx;
        } else {
            max = ly;
            negDiff = lx - ly;
        }
        if (max == Double.NEGATIVE_INFINITY) {
            return max;
        } else if (negDiff < -LOGTOLERANCE_F) {
            return max;
        } else {
            return max + (float) Math.log(1.0f + Math.exp(negDiff));
        }
    }

    /**
     * Returns the log of the sum of two numbers, which are themselves input in log form. This uses natural logarithms.
     * Reasonable care is taken to do this as efficiently as possible (under the assumption that the numbers might
     * differ greatly in magnitude), with high accuracy, and without numerical overflow. Also, handle correctly the case
     * of arguments being -Inf (e.g., probability 0).
     * 
     * @param lx First number, in log form
     * @param ly Second number, in log form
     * @return log(exp(lx) + exp(ly))
     */
    public static double logAdd(final double lx, final double ly) {
        double max, negDiff;
        if (lx > ly) {
            max = lx;
            negDiff = ly - lx;
        } else {
            max = ly;
            negDiff = lx - ly;
        }
        if (max == Double.NEGATIVE_INFINITY) {
            return max;
        } else if (negDiff < -LOGTOLERANCE) {
            return max;
        } else {
            return max + Math.log(1.0 + Math.exp(negDiff));
        }
    }

    public static double logAdd(final float[] logV) {
        double maxIndex = 0;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < logV.length; i++) {
            if (logV[i] > max) {
                max = logV[i];
                maxIndex = i;
            }
        }
        if (max == Double.NEGATIVE_INFINITY)
            return Double.NEGATIVE_INFINITY;
        // compute the negative difference
        final double threshold = max - LOGTOLERANCE;
        double sumNegativeDifferences = 0.0;
        for (int i = 0; i < logV.length; i++) {
            if (i != maxIndex && logV[i] > threshold) {
                sumNegativeDifferences += Math.exp(logV[i] - max);
            }
        }
        if (sumNegativeDifferences > 0.0) {
            return max + Math.log(1.0 + sumNegativeDifferences);
        }
        return max;
    }

    public static double logAdd(final double[] logV) {
        double maxIndex = 0;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < logV.length; i++) {
            if (logV[i] > max) {
                max = logV[i];
                maxIndex = i;
            }
        }
        if (max == Double.NEGATIVE_INFINITY)
            return Double.NEGATIVE_INFINITY;
        // compute the negative difference
        final double threshold = max - LOGTOLERANCE;
        double sumNegativeDifferences = 0.0;
        for (int i = 0; i < logV.length; i++) {
            if (i != maxIndex && logV[i] > threshold) {
                sumNegativeDifferences += Math.exp(logV[i] - max);
            }
        }
        if (sumNegativeDifferences > 0.0) {
            return max + Math.log(1.0 + sumNegativeDifferences);
        }
        return max;
    }

    public static double exp(final double logX) {
        // if x is very near one, use the linear approximation
        if (Math.abs(logX) < 0.001)
            return 1 + logX;
        return Math.exp(logX);
    }

    public static double approxLog(final double val) {
        if (val < 0.0)
            return Double.NaN;
        if (val == 0.0)
            return Double.NEGATIVE_INFINITY;
        final double r = val - 1;
        if (Math.abs(r) < 0.3) {
            // use first few terms of taylor series

            final double rSquared = r * r;
            return r - rSquared / 2 + rSquared * r / 3;
        }
        final double x = (Double.doubleToLongBits(val) >> 32);
        return (x - 1072632447) / 1512775;

    }

    public static double approxExp(final double val) {

        if (Math.abs(val) < 0.1)
            return 1 + val;
        final long tmp = (long) (1512775 * val + (1072693248 - 60801));
        return Double.longBitsToDouble(tmp << 32);

    }

    // public static double approxLogAdd(double a, double b)
    // {
    //
    // final long tmp1 = (long) (1512775 * a + (1072693248 - 60801));
    // double ea = Double.longBitsToDouble(tmp1 << 32);
    // final long tmp2 = (long) (1512775 * b + (1072693248 - 60801));
    // double eb = Double.longBitsToDouble(tmp2 << 32);
    //
    // final double x = (Double.doubleToLongBits(ea + eb) >> 32);
    // return (x - 1072632447) / 1512775;
    //
    // }

}

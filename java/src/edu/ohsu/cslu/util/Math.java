/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
 * 
 * This file is part of the cslu-common project.
 * 
 * cslu-common is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * cslu-common is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with cslu-common. If not, see <http://www.gnu.org/licenses/>
 */
/**
 * Math.java
 */
package edu.ohsu.cslu.util;


/**
 * Implements common mathematical functions. Suitable for static import.
 * 
 * Includes a lot of duplicated method code to avoid needing automatic up-conversion from int->float and float->double
 * (and re-casting the results downward).
 * 
 * @author Aaron Dunlop
 * @since Jun 12, 2008
 * 
 *        $Id$
 */
public class Math {

    /**
     * Maximum |a - b| at which to compute log-sum (logSum and logSumExp methods). This is approximately the minimum
     * precision available in a 32-bit float.
     */
    private final static float LOG_SUM_DEFAULT_DELTA = 16f;

    /**
     * Returns the maximum of the arguments supplied
     * 
     * @param arguments
     * @return maximum of the arguments supplied, or 0 if no arguments are supplied
     */
    public static int max(final int... arguments) {
        return rangeMax(arguments, 0, arguments.length);
    }

    /**
     * Returns the maximum of the arguments supplied
     * 
     * @param array
     * @param start
     * @param end
     * @return maximum of the arguments supplied, or 0 if no arguments are supplied
     */
    public static int rangeMax(final int[] array, final int start, final int end) {
        if (array.length == 0) {
            return 0;
        }

        int max = array[start];
        for (int i = start; i < end && i < array.length; i++) {
            final int current = array[i];
            if (current > max) {
                max = current;
            }
        }
        return max;
    }

    /**
     * Returns the maximum of the arguments supplied
     * 
     * @param arguments
     * @return maximum of the arguments supplied, or 0 if no arguments are supplied
     */
    public static int max(final short... arguments) {
        return rangeMax(arguments, 0, arguments.length);
    }

    /**
     * Returns the maximum of the arguments supplied
     * 
     * @param array
     * @param start
     * @param end
     * @return maximum of the arguments supplied, or 0 if no arguments are supplied
     */
    public static short rangeMax(final short[] array, final int start, final int end) {
        if (array.length == 0) {
            return 0;
        }

        short max = array[start];
        for (int i = start; i < end && i < array.length; i++) {
            final short current = array[i];
            if (current > max) {
                max = current;
            }
        }
        return max;
    }

    /**
     * Returns the maximum of the arguments supplied
     * 
     * @param arguments
     * @return maximum of the arguments supplied, or 0 if no arguments are supplied
     */
    public static char max(final char... arguments) {
        if (arguments.length == 0) {
            return 0;
        }

        char max = arguments[0];
        for (int i = 1; i < arguments.length; i++) {
            final char current = arguments[i];
            if (current > max) {
                max = current;
            }
        }
        return max;
    }

    /**
     * Returns the maximum of the arguments supplied. Functionally identical to the various <code>max</code> methods,
     * but named differently to avoid compiler ambiguity due to problems in JLS 15.12.2.5.
     * 
     * @param arguments
     * @return maximum of the arguments supplied, or 0 if no arguments are supplied
     */
    public static float floatMax(final float... arguments) {
        if (arguments.length == 0) {
            return 0;
        }

        float max = arguments[0];
        for (int i = 1; i < arguments.length; i++) {
            final float current = arguments[i];
            if (current > max) {
                max = current;
            }
        }
        return max;
    }

    /**
     * Returns the maximum of the arguments supplied. Functionally identical to the various <code>max</code> methods,
     * but named differently to avoid compiler ambiguity due to problems in JLS 15.12.2.5.
     * 
     * @param arguments
     * @return maximum of the arguments supplied, or 0 if no arguments are supplied
     */
    public static double doubleMax(final double[] arguments) {
        if (arguments.length == 0) {
            return 0;
        }

        double max = arguments[0];
        for (int i = 1; i < arguments.length; i++) {
            final double current = arguments[i];
            if (current > max) {
                max = current;
            }
        }
        return max;
    }

    /**
     * Returns the minimum of the arguments supplied
     * 
     * @param arguments
     * @return minimum of the arguments supplied, or 0 if no arguments are supplied
     */
    public static int min(final int... arguments) {
        if (arguments.length == 0) {
            return 0;
        }

        int min = arguments[0];
        for (int i = 1; i < arguments.length; i++) {
            final int current = arguments[i];
            if (current < min) {
                min = current;
            }
        }
        return min;
    }

    /**
     * Returns the minimum of the arguments supplied
     * 
     * @param arguments
     * @return minimum of the arguments supplied, or 0 if no arguments are supplied
     */
    public static char min(final char... arguments) {
        if (arguments.length == 0) {
            return 0;
        }

        char min = arguments[0];
        for (int i = 1; i < arguments.length; i++) {
            final char current = arguments[i];
            if (current < min) {
                min = current;
            }
        }
        return min;
    }

    /**
     * Returns the minimum of the arguments supplied. Functionally identical to the various <code>max</code> methods,
     * but named differently to avoid compiler ambiguity due to problems in JLS 15.12.2.5.
     * 
     * @param arguments
     * @return minimum of the arguments supplied, or 0 if no arguments are supplied
     */
    public static float floatMin(final float... arguments) {
        if (arguments.length == 0) {
            return 0;
        }

        float min = arguments[0];
        for (int i = 1; i < arguments.length; i++) {
            final float current = arguments[i];
            if (current < min) {
                min = current;
            }
        }
        return min;
    }

    /**
     * Returns the minimum of the arguments supplied. Functionally identical to the various <code>min</code> methods,
     * but named differently to avoid compiler ambiguity due to problems in JLS 15.12.2.5.
     * 
     * @param arguments
     * @return minimum of the arguments supplied, or 0 if no arguments are supplied
     */
    public static double doubleMin(final double... arguments) {
        if (arguments.length == 0) {
            return 0;
        }

        double min = arguments[0];
        for (int i = 1; i < arguments.length; i++) {
            final double current = arguments[i];
            if (current < min) {
                min = current;
            }
        }
        return min;
    }

    /**
     * Returns the index of the maximum argument supplied
     * 
     * @param arguments
     * @return index of the maximum argument supplied
     */
    public static int argmax(final int... arguments) {
        int max = arguments[0];
        int argmax = 0;
        for (int i = 1; i < arguments.length; i++) {
            final int current = arguments[i];
            if (current > max) {
                max = current;
                argmax = i;
            }
        }
        return argmax;
    }

    /**
     * Returns the index of the maximum argument supplied
     * 
     * @param arguments
     * @return index of the maximum argument supplied
     */
    public static int argmax(final char... arguments) {
        char max = arguments[0];
        int argmax = 0;
        for (int i = 1; i < arguments.length; i++) {
            final char current = arguments[i];
            if (current > max) {
                max = current;
                argmax = i;
            }
        }
        return argmax;
    }

    /**
     * Returns the index of the maximum argument supplied
     * 
     * @param arguments
     * @return index of the maximum argument supplied
     */
    public static int argmax(final float... arguments) {
        float max = arguments[0];
        int argmax = 0;
        for (int i = 1; i < arguments.length; i++) {
            final float current = arguments[i];
            if (current > max) {
                max = current;
                argmax = i;
            }
        }
        return argmax;
    }

    /**
     * Returns the index of the maximum argument supplied
     * 
     * @param arguments
     * @return index of the maximum argument supplied
     */
    public static int argmax(final double... arguments) {
        double max = arguments[0];
        int argmax = 0;
        for (int i = 1; i < arguments.length; i++) {
            final double current = arguments[i];
            if (current > max) {
                max = current;
                argmax = i;
            }
        }
        return argmax;
    }

    /**
     * Returns the index of the minimum argument supplied
     * 
     * @param arguments
     * @return index of the minimum argument supplied
     */
    public static int argmin(final int... arguments) {
        int min = arguments[0];
        int argmin = 0;
        for (int i = 1; i < arguments.length; i++) {
            final int current = arguments[i];
            if (current < min) {
                min = current;
                argmin = i;
            }
        }
        return argmin;
    }

    /**
     * Returns the index of the minimum argument supplied
     * 
     * @param arguments
     * @return index of the minimum argument supplied
     */
    public static int argmin(final char... arguments) {
        char min = arguments[0];
        int argmin = 0;
        for (int i = 1; i < arguments.length; i++) {
            final char current = arguments[i];
            if (current < min) {
                min = current;
                argmin = i;
            }
        }
        return argmin;
    }

    /**
     * Returns the index of the minimum argument supplied
     * 
     * @param arguments
     * @return index of the minimum argument supplied
     */
    public static int argmin(final float... arguments) {
        float min = arguments[0];
        int argmin = 0;
        for (int i = 1; i < arguments.length; i++) {
            final float current = arguments[i];
            if (current < min) {
                min = current;
                argmin = i;
            }
        }
        return argmin;
    }

    /**
     * Returns the index of the minimum argument supplied
     * 
     * @param arguments
     * @return index of the minimum argument supplied
     */
    public static int argmin(final double... arguments) {
        double min = arguments[0];
        int argmin = 0;
        for (int i = 1; i < arguments.length; i++) {
            final double current = arguments[i];
            if (current < min) {
                min = current;
                argmin = i;
            }
        }
        return argmin;
    }

    /**
     * Raises an integer base to the specified integer power
     * 
     * @param base
     * @param power
     * @return base raised to the power 'power'
     */
    public static int pow(final int base, final int power) {
        if (power == 0) {
            if (base == 0) {
                throw new IllegalArgumentException("Cannot raise 0 to the 0'th power");
            }
            return 1;
        }

        int pow = base;
        // TODO: This could be implemented more efficiently
        for (int i = 1; i < power; i++) {
            pow *= base;
        }
        return pow;
    }

    /**
     * Returns true if n is a power-of-2
     * 
     * @param n
     * @return true if n is a power-of-2
     */
    public static boolean isPowerOf2(final int n) {
        return ((n - 1) & n) == 0;
    }

    /**
     * Rounds up to the nearest power of 2
     * 
     * @param n Integer to round up to the nearest power of 2
     * @return The lowest power of 2 greater than or equal to n
     */
    public static int nextPowerOf2(int n) {
        // if (n < 0) {
        // throw new IllegalArgumentException("Cannot find the next ")
        // }
        n = n - 1;
        n = n | (n >> 1);
        n = n | (n >> 2);
        n = n | (n >> 4);
        n = n | (n >> 8);
        n = n | (n >> 16);
        n = n + 1;
        return n;
    }

    /**
     * Rounds down to the nearest power of 2
     * 
     * @param n Integer to round down to the nearest power of 2
     * @return The highest power of 2 less than or equal to n
     */
    public static int previousPowerOf2(final int n) {
        if (isPowerOf2(n)) {
            return n;
        }
        return (nextPowerOf2(n) >> 1);
    }

    /**
     * Returns the integer log (base 2) of an integer (equivalent to the highest bit set) Simple, but not incredibly
     * efficient.
     * 
     * @param n Integer to take the logarithm of
     * @return The the integer log (base 2) of an integer
     */
    public static int logBase2(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Cannot take the log of a negative number");
        }

        int i = 0;
        while (n > 1) {
            n = n >> 1;
            i++;
        }
        return i;
    }

    /**
     * Rounds n up to the next higher multiple of increment
     * 
     * @param n number to round
     * @param increment Step size to round to
     * @return n rounded up to the next higher multiple of increment
     */
    public static int roundUp(final int n, final int increment) {
        if (n % increment == 0) {
            return n;
        }
        return (n / increment + 1) * increment;
    }

    /**
     * Returns log_e(e^a + e^b), using the derivation:
     * 
     * log(exp(a) + exp(b)) =
     * 
     * log(exp(a) * (exp(0) + exp(b-a))) =
     * 
     * log(exp(a) * (1 + exp(b-a))) =
     * 
     * a + log(1 + exp(b-a))
     * 
     * @param a
     * @param b
     * @return log_e(e^a + e^b)
     */
    public static float logSum(final float a, final float b) {
        return logSum(a, b, LOG_SUM_DEFAULT_DELTA);
    }

    /**
     * Returns log_e(e^a + e^b), using the derivation:
     * 
     * log(exp(a) + exp(b)) =
     * 
     * log(exp(a) * (exp(0) + exp(b-a))) =
     * 
     * log(exp(a) * (1 + exp(b-a))) =
     * 
     * a + log(1 + exp(b-a))
     * 
     * @param a
     * @param b
     * @param maxDelta log delta between a and b. If the two values differ by more than this delta, the greater will be
     *            returned.
     * @return log_e(e^a + e^b)
     */
    public static float logSum(final float a, final float b, final float maxDelta) {

        // Two similar cases - one for b > a and one for a >= b

        if (b > a) {
            if (a == Float.NEGATIVE_INFINITY || b - a > maxDelta) {
                return b;
            }
            final float logSum = (float) (b + java.lang.Math.log(1 + java.lang.Math.exp(a - b)));
            return logSum;
        }

        // a >= b

        if (b == Float.NEGATIVE_INFINITY || a - b > maxDelta) {
            return a;
        }
        final float logSum = (float) (a + java.lang.Math.log(1 + java.lang.Math.exp(b - a)));
        return logSum;
    }

    /**
     * Computes log_e(e^x_1 + e^x_2 + ...)
     * 
     * @param x
     * @return log_e(e^x_1 + e^x_2 + ...)
     */
    public static float logSumExp(final float... x) {
        return logSumExp(LOG_SUM_DEFAULT_DELTA, x);
    }

    /**
     * Computes log_e(e^x_1 + e^x_2 + ...)
     * 
     * @param maxDelta log delta between maximum element of x and each other element. Elements below this cutoff will
     *            not be included in sum.
     * @param x
     * @return log_e(e^x_1 + e^x_2 + ...)
     */
    public static float logSumExp(final float maxDelta, final float... x) {

        final int max = argmax(x);
        double sum = 0;
        for (int i = 0; i < x.length; i++) {
            if (i != max && (x[max] - x[i] < maxDelta)) {
                sum += java.lang.Math.exp(x[i] - x[max]);
            }
        }
        return (float) java.lang.Math.log1p(sum) + x[max];
    }

    /**
     * Returns sum(e^x)
     * 
     * @return e^x_1 + e^x_2 + ...
     */
    public static double sumExp(final float... x) {

        double sum = 0;
        for (int i = 0; i < x.length; i++) {
            sum += java.lang.Math.exp(x[i]);
        }
        return sum;
    }

    /**
     * Returns sum(e^x)
     * 
     * @return e^x_1 + e^x_2 + ...
     */
    public static double sumExp(final double... x) {

        double sum = 0;
        for (int i = 0; i < x.length; i++) {
            sum += java.lang.Math.exp(x[i]);
        }
        return sum;
    }

    /**
     * Returns an approximation of log_e(x). In microbenchmarks as of 08/2012, this method is approximately 3x faster
     * than {@link java.lang.Math#log(double)}.
     * 
     * @param x
     * @return log_e(x)
     */
    public static float approximateLog(final float x) {
        if (x == 0) {
            return Float.NEGATIVE_INFINITY;
        }
        final int i = Float.floatToRawIntBits(x);
        final float f = Float.intBitsToFloat((i & 0x007FFFFF) | 0x3f000000);
        return ((i * 1.1920928955078125e-7f) - 124.22551499f - (1.498030302f * f) - (1.72587999f / (0.3520887068f + f))) * 0.69314718f;
    }

    /**
     * Returns an approximation of log_e(x + 1).
     * 
     * @param x
     * @return log_e(x + 1)
     */
    public static float approximateLog1p(final float x) {
        return approximateLog(x + 1);
    }

    /**
     * Returns an approximation of log_e(x), using a simpler and potentially faster, although less accurate
     * approximation than {@link #approximateLog(float)}.
     * 
     * Note: In the microbenchmarks we've run as of 08/2012, this method is not appreciably faster than
     * {@link #approximateLog(float)}, so we'd generally recommend that approach instead.
     * 
     * @param x
     * @return log_e(x)
     */
    public static float fastApproximateLog(final float x) {
        if (x == 0) {
            return Float.NEGATIVE_INFINITY;
        } else if (x == 1) {
            return 0f;
        }
        float y = Float.floatToRawIntBits(x);
        y *= 8.2629582881927490e-8f;
        return y - 87.989971088f;
    }

    /**
     * Returns an approximation of log_e(e^a + e^b), using Schraudolph's algorthm and the derivation:
     * 
     * log(exp(a) + exp(b)) = a + log(1 + exp(b-a))
     * 
     * @param a
     * @param b
     * @return log_e(e^a + e^b)
     */
    public static float approximateLogSum(final float a, final float b) {
        return approximateLogSum(a, b, LOG_SUM_DEFAULT_DELTA);
    }

    /**
     * Returns an approximation of log_e(e^a + e^b), using Schraudolph's algorthm and the derivation:
     * 
     * log(exp(a) + exp(b)) = a + log(1 + exp(b-a))
     * 
     * @param a
     * @param b
     * @param maxDelta log delta between a and b. If the two values differ by more than this delta, the greater will be
     *            returned.
     * @return log_e(e^a + e^b)
     */
    public static float approximateLogSum(final float a, final float b, final float maxDelta) {

        // Two similar cases - one for b > a and one for a >= b

        if (b > a) {
            if (a == Float.NEGATIVE_INFINITY || b - a > maxDelta) {
                return b;
            }
            final float logSum = (b + approximateLog1p((float) approximateExp(a - b)));
            return logSum;
        }

        // a >= b

        if (b == Float.NEGATIVE_INFINITY || a - b > maxDelta) {
            return a;
        }
        final float logSum = (a + approximateLog1p((float) approximateExp(b - a)));
        return logSum;
    }

    /**
     * Returns an approximation of log(sum(e^x)) using Schraudolph's algorithm
     * 
     * @param x
     * @return log_e(e^x_1 + e^x_2 + ...)
     */
    public static float approximateLogSumExp(final float... x) {
        return approximateLogSumExp(LOG_SUM_DEFAULT_DELTA, x);
    }

    /**
     * Returns an approximation of log(sum(e^x)) using Schraudolph's algorithm
     * 
     * @param maxDelta log delta between maximum element of x and each other element. Elements below this cutoff will
     *            not be included in sum.
     * @param x
     * @return log_e(e^x_1 + e^x_2 + ...)
     */
    public static float approximateLogSumExp(final float maxDelta, final float... x) {

        final int max = argmax(x);
        float sum = 0;
        for (int i = 0; i < x.length; i++) {
            if (i != max && (x[max] - x[i] < maxDelta)) {
                sum += approximateExp(x[i] - x[max]);
            }
        }
        return approximateLog1p(sum) + x[max];
    }

    /**
     * Returns an approximation of sum(e^x) using Schraudolph's algorithm
     * 
     * @return e^x_1 + e^x_2 + ...
     */
    public static double approximateSumExp(final float... x) {

        double sum = 0;
        for (int i = 0; i < x.length; i++) {
            sum += approximateExp(x[i]);
        }
        return sum;
    }

    /**
     * Returns an approximation of sum(e^x) using Schraudolph's algorithm
     * 
     * @return e^x_1 + e^x_2 + ...
     */
    public static double approximateSumExp(final double... x) {

        double sum = 0;
        for (int i = 0; i < x.length; i++) {
            sum += approximateExp(x[i]);
        }
        return sum;
    }

    /**
     * Returns an approximation to {@link java.lang.Math#exp(double)}, using Schraudolph's algorithm from
     * "A Fast, Compact Approximation of the Exponential Function". This algorithm depends on 64-bit IEEE-754
     * floating-point representation. It is not very accurate, but is several times faster than
     * {@link java.lang.Math#exp(double)}, and may be applicable for some NLP applications (e.g., log sum in
     * inside-outside grammar re-estimation}.
     * 
     * @param val
     * @return An approximation to {@link java.lang.Math#exp(double)}
     */
    public static double approximateExp(final double val) {
        final long tmp = (long) (1512775 * val + (1072693248 - 60801));
        return Double.longBitsToDouble(tmp << 32);
    }

    /**
     * @param val
     * @return The logistic of the supplied value
     */
    public static float logistic(final float val) {
        return (float) (1.0 / (1 + java.lang.Math.exp(-val)));
    }

    /**
     * A generalized logistic function, allowing varying growth rates
     * 
     * @param B Growth rate
     * @param val
     * @return The generalized logistic of the supplied growth rate and value
     */
    public static float logistic(final float B, final float val) {
        return (float) (1.0 / (1 + java.lang.Math.exp(-B * val)));
    }

    // TODO Stolen from Berkeley-parser. Compare with other implementations here
    /**
     * If a difference is bigger than this in log terms, then the sum or difference of them will just be the larger (to
     * 12 or so decimal places for double, and 7 or 8 for float).
     */
    public static final double LOGTOLERANCE = 30.0;

    private static double approxLogSum(final double[] logInputs, final int leng) {

        if (leng == 0) {
            throw new IllegalArgumentException();
        }
        int maxIdx = 0;
        double max = logInputs[0];
        for (int i = 1; i < leng; i++) {
            if (logInputs[i] > max) {
                maxIdx = i;
                max = logInputs[i];
            }
        }
        boolean haveTerms = false;
        double intermediate = 0.0;
        final double cutoff = max - LOGTOLERANCE;
        // we avoid rearranging the array and so test indices each time!
        for (int i = 0; i < leng; i++) {
            if (i != maxIdx && logInputs[i] > cutoff) {
                haveTerms = true;
                intermediate += approxExp(logInputs[i] - max);
            }
        }
        if (haveTerms) {
            return max + approxLog(1.0 + intermediate);
        }
        return max;
    }

    private static double approxLog(final double val) {
        if (val < 0.0)
            return Double.NaN;
        if (val == 0.0)
            return Double.NEGATIVE_INFINITY;
        final double r = val - 1;
        if (java.lang.Math.abs(r) < 0.3) {
            // use first few terms of taylor series

            final double rSquared = r * r;
            return r - rSquared / 2 + rSquared * r / 3;
        }
        final double x = (Double.doubleToLongBits(val) >> 32);
        return (x - 1072632447) / 1512775;

    }

    private static double approxExp(final double val) {

        if (java.lang.Math.abs(val) < 0.1)
            return 1 + val;
        final long tmp = (long) (1512775 * val + (1072693248 - 60801));
        return Double.longBitsToDouble(tmp << 32);

    }
}

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
     * Returns the maximum of the arguments supplied
     * 
     * @param arguments
     * @return maximum of the arguments supplied, or 0 if no arguments are supplied
     */
    public static int max(final int... arguments) {
        return max(arguments, 0, arguments.length);
    }

    /**
     * Returns the maximum of the arguments supplied
     * 
     * @param array
     * @param start
     * @param end
     * @return maximum of the arguments supplied, or 0 if no arguments are supplied
     */
    public static int max(final int[] array, final int start, final int end) {
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
        return max(arguments, 0, arguments.length);
    }

    /**
     * Returns the maximum of the arguments supplied
     * 
     * @param array
     * @param start
     * @param end
     * @return maximum of the arguments supplied, or 0 if no arguments are supplied
     */
    public static short max(final short[] array, final int start, final int end) {
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
     * Returns the maximum of the arguments supplied
     * 
     * @param arguments
     * @return maximum of the arguments supplied, or 0 if no arguments are supplied
     */
    public static float max(final float... arguments) {
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
     * Returns the maximum of the arguments supplied
     * 
     * @param arguments
     * @return maximum of the arguments supplied, or 0 if no arguments are supplied
     */
    public static double max(final double... arguments) {
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

    // TODO: Figure out why references to this method are considered ambiguous
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
     * Returns the minimum of the arguments supplied
     * 
     * @param arguments
     * @return minimum of the arguments supplied, or 0 if no arguments are supplied
     */
    public static float min(final float... arguments) {
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
     * Returns the minimum of the arguments supplied
     * 
     * @param arguments
     * @return minimum of the arguments supplied, or 0 if no arguments are supplied
     */
    public static double min(final double... arguments) {
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
     * log(exp(a) + exp(b)) = a + log(1 + exp(b-a))
     * 
     * @param a
     * @param b
     * @return log_e(e^a + e^b)
     */
    public static float logSum(final float a, final float b) {
        return (float) (a + java.lang.Math.log1p(java.lang.Math.exp(b - a)));
    }
}

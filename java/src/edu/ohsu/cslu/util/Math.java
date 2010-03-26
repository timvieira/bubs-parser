/**
 * Math.java
 */
package edu.ohsu.cslu.util;

/**
 * Implements common mathematical functions. Suitable for static import.
 * 
 * Includes a lot of duplicated method code to avoid needing automatic up-conversion from int->float and float->double (and re-casting the results downward).
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
        if (arguments.length == 0) {
            return 0;
        }

        int max = arguments[0];
        for (int i = 1; i < arguments.length; i++) {
            final int current = arguments[i];
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
     * Raises a base to the specified power
     * 
     * @param base
     * @param power
     * @return base raised to the power 'power'
     */
    public static int pow(final int base, final int power) {
        int pow = base;
        // TODO: This could be implemented more efficiently
        for (int i = 1; i < power; i++) {
            pow *= base;
        }
        return pow;
    }

    /**
     * Rounds up to the nearest power of 2
     * 
     * @param n Integer to round up to the nearest power of 2
     * @return The lowest power of 2 greater than or equal to n
     */
    public static int nextPowerOf2(int n) {
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
     * Returns the integer log (base 2) of an integer (equivalent to the highest bit set) Simple, but not incredibly efficient.
     * 
     * @param n Integer to take the logarithm of
     * @return The the integer log (base 2) of an integer
     */
    public static int logBase2(int n) {
        int i = 0;
        while (n > 0) {
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
     * Sort CSR int[] and float[] arrays (stolen from java.util.Arrays and modified to sort parallel arrays)
     * 
     * TODO Untested (caveat emptor)
     */
    public static void sort(final int x[], final float f[]) {
        sort1(x, f, 0, x.length);
    }

    private static void sort1(final int x[], final float f[], final int off, final int len) {
        // Insertion sort on smallest arrays
        if (len < 7) {
            for (int i = off; i < len + off; i++)
                for (int j = i; j > off && x[j - 1] > x[j]; j--)
                    swap(x, f, j, j - 1);
            return;
        }

        // Choose a partition element, v
        int m = off + (len >> 1); // Small arrays, middle element
        if (len > 7) {
            int l = off;
            int n = off + len - 1;
            if (len > 40) { // Big arrays, pseudomedian of 9
                final int s = len / 8;
                l = med3(x, l, l + s, l + 2 * s);
                m = med3(x, m - s, m, m + s);
                n = med3(x, n - 2 * s, n - s, n);
            }
            m = med3(x, l, m, n); // Mid-size, med of 3
        }
        final int v = x[m];

        // Establish Invariant: v* (<v)* (>v)* v*
        int a = off, b = a, c = off + len - 1, d = c;
        while (true) {
            while (b <= c && x[b] <= v) {
                if (x[b] == v)
                    swap(x, f, a++, b);
                b++;
            }
            while (c >= b && x[c] >= v) {
                if (x[c] == v)
                    swap(x, f, c, d--);
                c--;
            }
            if (b > c)
                break;
            swap(x, f, b++, c--);
        }

        // Swap partition elements back to middle
        int s;
        final int n = off + len;
        s = java.lang.Math.min(a - off, b - a);
        vecswap(x, f, off, b - s, s);
        s = java.lang.Math.min(d - c, n - d - 1);
        vecswap(x, f, b, n - s, s);

        // Recursively sort non-partition-elements
        if ((s = b - a) > 1)
            sort1(x, f, off, s);
        if ((s = d - c) > 1)
            sort1(x, f, n - s, s);
    }

    /**
     * Swaps x[a] with x[b] and f[a] with f[b].
     */
    private static void swap(final int x[], final float f[], final int a, final int b) {
        final int t = x[a];
        x[a] = x[b];
        x[b] = t;

        final float t2 = f[a];
        f[a] = f[b];
        f[b] = t2;
    }

    /**
     * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)] and f[a .. (a+n-1)] with f[b .. (b+n-1)].
     */
    private static void vecswap(final int x[], final float f[], int a, int b, final int n) {
        for (int i = 0; i < n; i++, a++, b++) {
            swap(x, f, a, b);
        }
    }

    /**
     * Returns the index of the median of the three indexed integers.
     */
    private static int med3(final int x[], final int a, final int b, final int c) {
        return (x[a] < x[b] ? (x[b] < x[c] ? b : x[a] < x[c] ? c : a) : (x[b] > x[c] ? b : x[a] > x[c] ? c : a));
    }
}

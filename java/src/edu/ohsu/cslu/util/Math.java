/**
 * Math.java
 */
package edu.ohsu.cslu.util;

/**
 * Implements common mathematical functions. Suitable for static import.
 * 
 * Includes a lot of duplicated method code to avoid needing automatic up-conversion from int->float and
 * float->double (and re-casting the results downward).
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
     * @return maximum of the arguments supplied
     */
    public static int max(int... arguments) {
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
     * @return maximum of the arguments supplied
     */
    public static char max(char... arguments) {
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
     * @return maximum of the arguments supplied
     */
    public static float max(float... arguments) {
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
     * @return maximum of the arguments supplied
     */
    public static double max(double... arguments) {
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
     * @return minimum of the arguments supplied
     */
    public static int min(int... arguments) {
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
     * @return minimum of the arguments supplied
     */
    public static char min(char... arguments) {
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
     * @return minimum of the arguments supplied
     */
    public static float min(float... arguments) {
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
     * @return minimum of the arguments supplied
     */
    public static double min(double... arguments) {
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
    public static int argmax(int... arguments) {
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
    public static int argmax(char... arguments) {
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
    public static int argmax(float... arguments) {
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
    public static int argmax(double... arguments) {
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
    public static int argmin(int... arguments) {
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
    public static int argmin(char... arguments) {
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
    public static int argmin(float... arguments) {
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
    public static int argmin(double... arguments) {
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
     * @param n
     *            Integer to round up to the nearest power of 2
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

}

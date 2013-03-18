package edu.berkeley.nlp.util;

import java.util.Arrays;

import edu.berkeley.nlp.math.SloppyMath;

public class ArrayUtil {

    // ARITHMETIC FUNCTIONS

    // TODO Compare with edu.ohsu.cslu.util.Math versions and combine the best of both implementations. Remove
    // referenced methods in SloppyMath too
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
        final double cutoff = max - SloppyMath.LOGTOLERANCE;
        // we avoid rearranging the array and so test indices each time!
        for (int i = 0; i < leng; i++) {
            if (i != maxIdx && logInputs[i] > cutoff) {
                haveTerms = true;
                intermediate += SloppyMath.approxExp(logInputs[i] - max);
            }
        }
        if (haveTerms) {
            return max + SloppyMath.approxLog(1.0 + intermediate);
        }
        return max;
    }

    /**
     * @return the index of the max value; if max is a tie, returns the first one.
     */
    public static int argmax(final short[] a) {
        float max = Short.MIN_VALUE;
        int argmax = 0;
        for (int i = 0; i < a.length; i++) {
            if (a[i] > max) {
                max = a[i];
                argmax = i;
            }
        }
        return argmax;
    }

    public static double[] copy(final double[] mat) {
        if (mat == null) {
            return null;
        }
        final int m = mat.length;
        final double[] newMat = new double[m];
        System.arraycopy(mat, 0, newMat, 0, mat.length);
        return newMat;
    }

    public static double[][] copy(final double[][] mat) {
        final int m = mat.length;
        final double[][] newMat = new double[m][];
        for (int r = 0; r < m; r++)
            newMat[r] = copy(mat[r]);
        return newMat;
    }

    public static double[][][] copy(final double[][][] mat) {
        final int m = mat.length;
        final double[][][] newMat = new double[m][][];
        for (int r = 0; r < m; r++)
            newMat[r] = copy(mat[r]);
        return newMat;
    }

    public static void fill(final double[][] a, final double val) {
        for (int i = 0; i < a.length; i++) {
            Arrays.fill(a[i], val);
        }
    }

    public static void fill(final double[][][] a, final double val) {
        for (int i = 0; i < a.length; i++) {
            fill(a[i], val);
        }
    }

    public static void fill(final int[][] a, final int val) {
        for (int i = 0; i < a.length; ++i) {
            Arrays.fill(a[i], 0, a[0].length, val);
        }
    }

    public static float max(final short[] a) {
        return a[argmax(a)];
    }

    public static double sum(final double[] a) {
        if (a == null) {
            return 0.0;
        }
        double result = 0.0;
        for (int i = 0; i < a.length; i++) {
            result += a[i];
        }
        return result;
    }

    public static double max(final double[] v) {
        double maxV = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < v.length; i++) {
            if (v[i] > maxV) {
                maxV = v[i];
            }
        }
        return maxV;
    }
}

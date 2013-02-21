package edu.berkeley.nlp.util;

public class NumUtils {

    public static boolean isFinite(final double x) {
        return !Double.isNaN(x) && !Double.isInfinite(x);
    }

    public static boolean isProb(final double x) {
        return x >= 0 && x <= 1 && !Double.isNaN(x);
    }

    public static void assertIsProb(final double x) {
        assert isProb(x) : "Not a probability [0, 1]: " + x;
    }

    // Vector, matrix operations {
    public static boolean normalize(final float[] data) {
        float sum = 0;
        for (final float x : data)
            sum += x;
        if (sum == 0)
            return false;
        for (int i = 0; i < data.length; i++)
            data[i] /= sum;
        return true;
    }

    public static boolean normalize(final double[] data) {
        double sum = 0;
        for (final double x : data)
            sum += x;
        if (sum == 0)
            return false;
        for (int i = 0; i < data.length; i++)
            data[i] /= sum;
        return true;
    }

    public static boolean normalize(final double[][] data) {
        double sum = 0;
        for (final double[] v : data)
            for (final double x : v)
                sum += x;
        if (sum == 0)
            return false;
        for (final double[] v : data)
            for (int i = 0; i < v.length; i++)
                v[i] /= sum;
        return true;
    }

    public static boolean normalizeEachRow(final double[][] data) {
        boolean allRowsOkay = true;
        for (final double[] row : data) {
            if (!NumUtils.normalize(row))
                allRowsOkay = false;
        }
        return allRowsOkay;
    }

    public static boolean normalize(final double[][][] data) {
        double sum = 0;
        for (final double[][] m : data)
            for (final double[] v : m)
                for (final double x : v)
                    sum += x;
        if (sum == 0)
            return false;
        for (final double[][] m : data)
            for (final double[] v : m)
                for (int i = 0; i < v.length; i++)
                    v[i] /= sum;
        return true;
    }

    public static boolean expNormalize(final double[] probs) {
        // Input: log probabilities (unnormalized too)
        // Output: normalized probabilities
        // probs actually contains log probabilities; so we can add an arbitrary
        // constant to make
        // the largest log prob 0 to prevent overflow problems
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < probs.length; i++)
            max = Math.max(max, probs[i]);
        for (int i = 0; i < probs.length; i++)
            probs[i] = Math.exp(probs[i] - max);
        return normalize(probs);
    }

    public static boolean expNormalize(final double[][] probs) {
        double max = Double.NEGATIVE_INFINITY;
        for (final double[] v : probs)
            for (int i = 0; i < v.length; i++)
                max = Math.max(max, v[i]);
        for (final double[] v : probs)
            for (int i = 0; i < v.length; i++)
                v[i] = Math.exp(v[i] - max);
        return normalize(probs);
    }

    public static boolean expNormalize(final double[][][] probs) {
        double max = Double.NEGATIVE_INFINITY;
        for (final double[][] m : probs)
            for (final double[] v : m)
                for (int i = 0; i < v.length; i++)
                    max = Math.max(max, v[i]);
        for (final double[][] m : probs)
            for (final double[] v : m)
                for (int i = 0; i < v.length; i++)
                    v[i] = Math.exp(v[i] - max);
        return normalize(probs);
    }

    public static int[][] toInt(final double[][] data) {
        final int[][] newdata = new int[data.length][];
        for (int r = 0; r < data.length; r++) {
            newdata[r] = new int[data[r].length];
            for (int c = 0; c < data[r].length; c++)
                newdata[r][c] = (int) data[r][c];
        }
        return newdata;
    }

    public static double l1Dist(final double[] x, final double[] y) {
        double sum = 0;
        for (int i = 0; i < x.length; i++)
            sum += Math.abs(x[i] - y[i]);
        return sum;
    }

    public static double lInfDist(final double[] x, final double[] y) {
        double max = 0;
        for (int i = 0; i < x.length; i++)
            max = Math.max(max, Math.abs(x[i] - y[i]));
        return max;
    }

    public static double l2Dist(final double[] x, final double[] y) {
        return Math.sqrt(l2DistSquared(x, y));
    }

    public static double l2DistSquared(final double[] x, final double[] y) {
        double sum = 0;
        for (int i = 0; i < x.length; i++)
            sum += (x[i] - y[i]) * (x[i] - y[i]);
        return sum;
    }

    public static double l2Norm(final double[] x) {
        return Math.sqrt(l2NormSquared(x));
    }

    public static double l2NormSquared(final double[] x) {
        double sum = 0;
        for (int i = 0; i < x.length; i++)
            sum += x[i] * x[i];
        return sum;
    }

    public static double[] l2NormalizedMut(final double[] x) {
        final double norm = l2Norm(x);
        if (norm > 0)
            ListUtils.multMut(x, 1.0 / norm);
        return x;
    }

    // If sum is 0, set to uniform
    // Return false if we had to set to uniform
    public static boolean normalizeForce(final double[] data) {
        double sum = 0;
        for (final double x : data)
            sum += x;
        if (sum == 0) {
            for (int i = 0; i < data.length; i++)
                data[i] = 1.0 / data.length;
            return false;
        } else {
            for (int i = 0; i < data.length; i++)
                data[i] /= sum;
            return true;
        }
    }

    public static double[][] transpose(final double[][] mat) {
        final int m = mat.length, n = mat[0].length;
        final double[][] newMat = new double[n][m];
        for (int r = 0; r < m; r++)
            for (int c = 0; c < n; c++)
                newMat[c][r] = mat[r][c];
        return newMat;
    }

    public static double[][] elementWiseMult(final double[][] mat1, final double[][] mat2) {
        final int m = mat1.length, n = mat1[0].length;
        final double[][] newMat = new double[m][n];
        for (int r = 0; r < m; r++)
            for (int c = 0; c < n; c++)
                newMat[r][c] = mat1[r][c] * mat2[r][c];
        return newMat;
    }

    public static void scalarMult(final double[][] mat, final double x) {
        final int m = mat.length, n = mat[0].length;
        for (int r = 0; r < m; r++)
            for (int c = 0; c < n; c++)
                mat[r][c] *= x;
    }

    public static double[][] copy(final double[][] mat) {
        final int m = mat.length;
        final double[][] newMat = new double[m][];
        for (int r = 0; r < m; r++) {
            final int n = mat[r].length;
            newMat[r] = new double[n];
            for (int c = 0; c < n; c++)
                newMat[r][c] = mat[r][c];
        }
        return newMat;
    }

    public static boolean equals(final double x, final double y) {
        return Math.abs(x - y) < 1e-10;
    }

    public static boolean equals(final double x, final double y, final double tol) {
        return Math.abs(x - y) < tol;
    }

    public static double[] round(final double[] vec, final int numPlaces) {
        final double[] newVec = new double[vec.length];
        final double scale = Math.pow(10, numPlaces);
        for (int i = 0; i < vec.length; i++)
            newVec[i] = Math.round(vec[i] * scale) / scale;
        return newVec;
    }

    public static double bound(final double x, final double lower, final double upper) {
        if (x < lower)
            return lower;
        if (x > upper)
            return upper;
        return x;
    }

    // }

    public static double entropy(final double[] probs) {
        double e = 0;
        for (final double p : probs) {
            if (p > 0)
                e += -p * Math.log(p);
        }
        return e;
    }

    // Return log(exp(a)+exp(b))
    private static double logMaxValue = Math.log(Double.MAX_VALUE);

    public static double logAdd(final double a, final double b) {
        if (a > b) {
            if (Double.isInfinite(b) || a - b > logMaxValue)
                return a;
            return b + Math.log(1 + Math.exp(a - b));
        } else {
            if (Double.isInfinite(a) || b - a > logMaxValue)
                return b;
            return a + Math.log(1 + Math.exp(b - a));
        }
    }

    // Fast exponential
    // http://martin.ankerl.com/2007/10/04/optimized-pow-approximation-for-java-and-c-c/
    public static double fastExp(final double val) {
        final long tmp = (long) (1512775 * val + (1072693248 - 60801));
        return Double.longBitsToDouble(tmp << 32);
    }

    public static double fastLog(final double val) {
        final double x = (Double.doubleToLongBits(val) >> 32);
        return (x - 1072632447) / 1512775;
    }
}

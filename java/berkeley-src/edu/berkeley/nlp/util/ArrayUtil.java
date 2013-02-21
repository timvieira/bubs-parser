package edu.berkeley.nlp.util;

import java.text.NumberFormat;
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
        } else {
            return max;
        }
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

    public static double[][] clone(final double[][] a) {
        final double[][] res = new double[a.length][];
        for (int i = 0; i < a.length; i++) {
            if (a[i] != null)
                res[i] = a[i].clone();
        }
        return res;
    }

    public static double[][][] clone(final double[][][] a) {
        final double[][][] res = new double[a.length][][];
        for (int i = 0; i < a.length; i++) {
            if (a[i] != null)
                res[i] = clone(a[i]);
        }
        return res;
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

    public static double[] exp(final double[] a) {
        final double[] result = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = Math.exp(a[i]);
        }
        return result;
    }

    public static void fill(final double[][] a, final double val) {
        for (int i = 0; i < a.length; i++) {
            Arrays.fill(a[i], val);
        }
    }

    public static void fill(final double[][] a, final int until1, final int until2, final double val) {
        for (int i = 0; i < until1; ++i) {
            Arrays.fill(a[i], 0, until2 == Integer.MAX_VALUE ? a[i].length : until2, val);
        }
    }

    public static void fill(final double[][][] a, final double val) {
        for (int i = 0; i < a.length; i++) {
            fill(a[i], val);
        }
    }

    public static void fill(final double[][][] a, final int until1, final int until2, final double val) {
        for (int i = 0; i < until1; i++) {
            fill(a[i], until2, Integer.MAX_VALUE, val);
        }
    }

    public static void fill(final double[][][] a, final int until1, final int until2, final int until3, final double val) {
        for (int i = 0; i < until1; i++) {
            fill(a[i], until2, until3, val);
        }
    }

    public static void fill(final double[][][][] a, final double val) {
        for (int i = 0; i < a.length; i++) {
            fill(a[i], val);
        }
    }

    public static void fill(final double[][][][] a, final int until1, final int until2, final int until3,
            final int until4, final double val) {
        for (int i = 0; i < until1; i++) {
            fill(a[i], until2, until3, until4, val);
        }
    }

    public static void fill(final double[][][][][] a, final double val) {
        for (int i = 0; i < a.length; i++) {
            fill(a[i], val);
        }
    }

    public static void fill(final float[][] a, final float val) {
        for (int i = 0; i < a.length; i++) {
            Arrays.fill(a[i], val);
        }
    }

    public static void fill(final float[][][] a, final float val) {
        for (int i = 0; i < a.length; i++) {
            fill(a[i], val);
        }
    }

    public static void fill(final float[][][][] a, final float val) {
        for (int i = 0; i < a.length; i++) {
            fill(a[i], val);
        }
    }

    public static void fill(final float[][][][][] a, final float val) {
        for (int i = 0; i < a.length; i++) {
            fill(a[i], val);
        }
    }

    public static void fill(final int[][] a, final int val) {
        fill(a, a.length, a[0].length, val);
    }

    public static void fill(final int[][] a, final int until1, final int until2, final int val) {
        for (int i = 0; i < until1; ++i) {
            Arrays.fill(a[i], 0, until2, val);
        }
    }

    public static void fill(final int[][][] a, final int until1, final int until2, final int until3, final int val) {
        for (int i = 0; i < until1; i++) {
            fill(a[i], until2, until3, val);
        }
    }

    public static void fill(final double[][][] a, final int until, final double val) {
        for (int i = 0; i < until; i++) {
            fill(a[i], val);
        }
    }

    public static void fill(final int[][][] a, final int until, final int val) {
        for (int i = 0; i < until; i++) {
            fill(a[i], val);
        }
    }

    public static void fill(final int[][][] a, final int val) {
        for (int i = 0; i < a.length; i++) {
            fill(a[i], val);
        }
    }

    public static void fill(final int[][][][] a, final int until1, final int until2, final int until3,
            final int until4, final int val) {
        for (int i = 0; i < until1; i++) {
            fill(a[i], until2, until3, until4, val);
        }
    }

    public static void fill(final Object[][] a, final int until1, final int until2, final Object val) {
        for (int i = 0; i < until1; ++i) {
            Arrays.fill(a[i], 0, until2 == Integer.MAX_VALUE ? a[i].length : until2, val);
        }
    }

    public static void fill(final Object[][][] a, final int until1, final int until2, final int until3, final Object val) {
        for (int i = 0; i < until1; ++i) {
            fill(a[i], until2 == Integer.MAX_VALUE ? a[i].length : until2, until3, val);
        }
    }

    public static void fill(final Object[][][][] a, final int until1, final int until2, final int until3,
            final int until4, final Object val) {
        for (int i = 0; i < until1; ++i) {
            fill(a[i], until2 == Integer.MAX_VALUE ? a[i].length : until2, until3, until4, val);
        }
    }

    // UTILITIES

    public static float max(final short[] a) {
        return a[argmax(a)];
    }

    /**
     * Computes 2-norm of vector
     * 
     * @param a
     * @return Euclidean norm of a
     */
    public static double norm(final double[] a) {
        double squaredSum = 0;
        for (int i = 0; i < a.length; i++) {
            squaredSum += a[i] * a[i];
        }
        return Math.sqrt(squaredSum);
    }

    // PRINTING FUNCTIONS

    /**
     * Computes 2-norm of vector
     * 
     * @param a
     * @return Euclidean norm of a
     */
    public static double norm(final float[] a) {
        double squaredSum = 0;
        for (int i = 0; i < a.length; i++) {
            squaredSum += a[i] * a[i];
        }
        return Math.sqrt(squaredSum);
    }

    /**
     * Computes 1-norm of vector
     * 
     * @param a
     * @return 1-norm of a
     */
    public static double norm_1(final double[] a) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += (a[i] < 0 ? -a[i] : a[i]);
        }
        return sum;
    }

    /**
     * Computes 1-norm of vector
     * 
     * @param a
     * @return 1-norm of a
     */
    public static double norm_1(final float[] a) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += (a[i] < 0 ? -a[i] : a[i]);
        }
        return sum;
    }

    /**
     * Computes inf-norm of vector
     * 
     * @param a
     * @return inf-norm of a
     */
    public static double norm_inf(final double[] a) {
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < a.length; i++) {
            if (Math.abs(a[i]) > max) {
                max = Math.abs(a[i]);
            }
        }
        return max;
    }

    /**
     * Computes inf-norm of vector
     * 
     * @param a
     * @return inf-norm of a
     */
    public static double norm_inf(final float[] a) {
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < a.length; i++) {
            if (Math.abs(a[i]) > max) {
                max = Math.abs(a[i]);
            }
        }
        return max;
    }

    /**
     * Scales the values in this array by b. Does it in place.
     */
    public static void scale(final double[] a, final double b) {
        for (int i = 0; i < a.length; i++) {
            a[i] = a[i] * b;
        }
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

    public static String toString(final boolean[][] a) {
        String s = "[";
        for (int i = 0; i < a.length; i++) {
            s = s.concat(Arrays.toString(a[i]) + ", ");
        }
        return s + "]";
    }

    public static String toString(final byte[] a) {
        return toString(a, null);
    }

    public static String toString(final byte[] a, final NumberFormat nf) {
        if (a == null)
            return null;
        if (a.length == 0)
            return "[]";
        final StringBuffer b = new StringBuffer();
        b.append("[");
        for (int i = 0; i < a.length - 1; i++) {
            String s;
            if (nf == null) {
                s = String.valueOf(a[i]);
            } else {
                s = nf.format(a[i]);
            }
            b.append(s);
            b.append(", ");
        }
        String s;
        if (nf == null) {
            s = String.valueOf(a[a.length - 1]);
        } else {
            s = nf.format(a[a.length - 1]);
        }
        b.append(s);
        b.append(']');
        return b.toString();
    }

    public static String toString(final double[] a) {
        return toString(a, null);
    }

    public static String toString(final double[] a, final NumberFormat nf) {
        if (a == null)
            return null;
        if (a.length == 0)
            return "[]";
        final StringBuffer b = new StringBuffer();
        b.append("[");
        for (int i = 0; i < a.length - 1; i++) {
            String s;
            if (nf == null) {
                s = String.valueOf(a[i]);
            } else {
                s = nf.format(a[i]);
            }
            b.append(s);
            b.append(", ");
        }
        String s;
        if (nf == null) {
            s = String.valueOf(a[a.length - 1]);
        } else {
            s = nf.format(a[a.length - 1]);
        }
        b.append(s);
        b.append(']');
        return b.toString();
    }

    public static String toString(final double[][] counts) {
        return toString(counts, 10, null, null, NumberFormat.getInstance(), false);
    }

    public static String toString(final double[][] counts, final int cellSize, final Object[] rowLabels,
            final Object[] colLabels, final NumberFormat nf, final boolean printTotals) {
        if (counts == null)
            return null;
        // first compute row totals and column totals
        final double[] rowTotals = new double[counts.length];
        final double[] colTotals = new double[counts[0].length]; // assume it's square
        double total = 0.0;
        for (int i = 0; i < counts.length; i++) {
            for (int j = 0; j < counts[i].length; j++) {
                rowTotals[i] += counts[i][j];
                colTotals[j] += counts[i][j];
                total += counts[i][j];
            }
        }
        final StringBuffer result = new StringBuffer();
        // column labels
        if (colLabels != null) {
            result.append(StringUtils.padLeft("", cellSize));
            for (int j = 0; j < counts[0].length; j++) {
                String s = colLabels[j].toString();
                if (s.length() > cellSize - 1) {
                    s = s.substring(0, cellSize - 1);
                }
                s = StringUtils.padLeft(s, cellSize);
                result.append(s);
            }
            if (printTotals) {
                result.append(StringUtils.padLeft("Total", cellSize));
            }
            result.append("\n\n");
        }
        for (int i = 0; i < counts.length; i++) {
            // row label
            if (rowLabels != null) {
                String s = rowLabels[i].toString();
                s = StringUtils.pad(s, cellSize); // left align this guy only
                result.append(s);
            }
            // value
            for (int j = 0; j < counts[i].length; j++) {
                result.append(StringUtils.padLeft(nf.format(counts[i][j]), cellSize));
            }
            // the row total
            if (printTotals) {
                result.append(StringUtils.padLeft(nf.format(rowTotals[i]), cellSize));
            }
            result.append("\n");
        }
        result.append("\n");
        // the col totals
        if (printTotals) {
            result.append(StringUtils.pad("Total", cellSize));
            for (int j = 0; j < colTotals.length; j++) {
                result.append(StringUtils.padLeft(nf.format(colTotals[j]), cellSize));
            }
            result.append(StringUtils.padLeft(nf.format(total), cellSize));
        }
        result.append("\n");
        return result.toString();
    }

    public static String toString(final double[][][] a) {
        String s = "[";
        for (int i = 0; i < a.length; i++) {
            s = s.concat(toString(a[i]) + ", ");
        }
        return s + "]";
    }

    public static String toString(final float[] a) {
        return toString(a, null);
    }

    public static String toString(final float[] a, final NumberFormat nf) {
        if (a == null)
            return null;
        if (a.length == 0)
            return "[]";
        final StringBuffer b = new StringBuffer();
        b.append("[");
        for (int i = 0; i < a.length - 1; i++) {
            String s;
            if (nf == null) {
                s = String.valueOf(a[i]);
            } else {
                s = nf.format(a[i]);
            }
            b.append(s);
            b.append(", ");
        }
        String s;
        if (nf == null) {
            s = String.valueOf(a[a.length - 1]);
        } else {
            s = nf.format(a[a.length - 1]);
        }
        b.append(s);
        b.append(']');
        return b.toString();
    }

    // CASTS

    public static String toString(final float[][] counts, final int cellSize, final Object[] rowLabels,
            final Object[] colLabels, final NumberFormat nf, final boolean printTotals) {
        // first compute row totals and column totals
        final double[] rowTotals = new double[counts.length];
        final double[] colTotals = new double[counts[0].length]; // assume it's square
        double total = 0.0;
        for (int i = 0; i < counts.length; i++) {
            for (int j = 0; j < counts[i].length; j++) {
                rowTotals[i] += counts[i][j];
                colTotals[j] += counts[i][j];
                total += counts[i][j];
            }
        }
        final StringBuffer result = new StringBuffer();
        // column labels
        if (colLabels != null) {
            result.append(StringUtils.padLeft("", cellSize));
            for (int j = 0; j < counts[0].length; j++) {
                String s = colLabels[j].toString();
                if (s.length() > cellSize - 1) {
                    s = s.substring(0, cellSize - 1);
                }
                s = StringUtils.padLeft(s, cellSize);
                result.append(s);
            }
            if (printTotals) {
                result.append(StringUtils.padLeft("Total", cellSize));
            }
            result.append("\n\n");
        }
        for (int i = 0; i < counts.length; i++) {
            // row label
            if (rowLabels != null) {
                String s = rowLabels[i].toString();
                s = StringUtils.pad(s, cellSize); // left align this guy only
                result.append(s);
            }
            // value
            for (int j = 0; j < counts[i].length; j++) {
                result.append(StringUtils.padLeft(nf.format(counts[i][j]), cellSize));
            }
            // the row total
            if (printTotals) {
                result.append(StringUtils.padLeft(nf.format(rowTotals[i]), cellSize));
            }
            result.append("\n");
        }
        result.append("\n");
        // the col totals
        if (printTotals) {
            result.append(StringUtils.pad("Total", cellSize));
            for (int j = 0; j < colTotals.length; j++) {
                result.append(StringUtils.padLeft(nf.format(colTotals[j]), cellSize));
            }
            result.append(StringUtils.padLeft(nf.format(total), cellSize));
        }
        result.append("\n");
        return result.toString();
    }

    public static String toString(final int[] a) {
        return toString(a, null);
    }

    public static String toString(final int[] a, final NumberFormat nf) {
        if (a == null)
            return null;
        if (a.length == 0)
            return "[]";
        final StringBuffer b = new StringBuffer();
        b.append("[");
        for (int i = 0; i < a.length - 1; i++) {
            String s;
            if (nf == null) {
                s = String.valueOf(a[i]);
            } else {
                s = nf.format(a[i]);
            }
            b.append(s);
            b.append(", ");
        }
        String s;
        if (nf == null) {
            s = String.valueOf(a[a.length - 1]);
        } else {
            s = nf.format(a[a.length - 1]);
        }
        b.append(s);
        b.append(']');
        return b.toString();
    }

    // ARITHMETIC FUNCTIONS

    public static String toString(final int[][] counts) {
        return toString(counts, 10, null, null, NumberFormat.getInstance(), false);
    }

    public static String toString(final int[][] counts, final int cellSize, final Object[] rowLabels,
            final Object[] colLabels, final NumberFormat nf, final boolean printTotals) {
        // first compute row totals and column totals
        final int[] rowTotals = new int[counts.length];
        final int[] colTotals = new int[counts[0].length]; // assume it's square
        int total = 0;
        for (int i = 0; i < counts.length; i++) {
            for (int j = 0; j < counts[i].length; j++) {
                rowTotals[i] += counts[i][j];
                colTotals[j] += counts[i][j];
                total += counts[i][j];
            }
        }
        final StringBuffer result = new StringBuffer();
        // column labels
        if (colLabels != null) {
            result.append(StringUtils.padLeft("", cellSize));
            for (int j = 0; j < counts[0].length; j++) {
                String s = colLabels[j].toString();
                if (s.length() > cellSize - 1) {
                    s = s.substring(0, cellSize - 1);
                }
                s = StringUtils.padLeft(s, cellSize);
                result.append(s);
            }
            if (printTotals) {
                result.append(StringUtils.padLeft("Total", cellSize));
            }
            result.append("\n\n");
        }
        for (int i = 0; i < counts.length; i++) {
            // row label
            if (rowLabels != null) {
                String s = rowLabels[i].toString();
                s = StringUtils.padOrTrim(s, cellSize); // left align this guy
                                                        // only
                result.append(s);
            }
            // value
            for (int j = 0; j < counts[i].length; j++) {
                result.append(StringUtils.padLeft(nf.format(counts[i][j]), cellSize));
            }
            // the row total
            if (printTotals) {
                result.append(StringUtils.padLeft(nf.format(rowTotals[i]), cellSize));
            }
            result.append("\n");
        }
        result.append("\n");
        // the col totals
        if (printTotals) {
            result.append(StringUtils.pad("Total", cellSize));
            for (int j = 0; j < colTotals.length; j++) {
                result.append(StringUtils.padLeft(nf.format(colTotals[j]), cellSize));
            }
            result.append(StringUtils.padLeft(nf.format(total), cellSize));
        }
        result.append("\n");
        return result.toString();
    }
}

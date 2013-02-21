package edu.berkeley.nlp.util;

import java.text.SimpleDateFormat;

/**
 * Formatting class. I'm really lazy. D() is a family of default functions for formatting various types of objects.
 */
public class Fmt {
    public static String D(final double x) {
        if (Math.abs(x - (int) x) < 1e-40) // An integer (probably)
            return "" + (int) x;
        if (Math.abs(x) < 1e-3) // Scientific notation (close to 0)
            return String.format("%.2e", x);
        return String.format("%.3f", x);
    }

    public static String D(final boolean[] x) {
        return StrUtils.join(x);
    }

    public static String D(final int[] x) {
        return StrUtils.join(x);
    }

    public static String D(final double[] x) {
        return D(x, " ");
    }

    public static String D(final double[] xs, final String delim) {
        final StringBuilder sb = new StringBuilder();
        for (final double x : xs) {
            if (sb.length() > 0)
                sb.append(delim);
            sb.append(Fmt.D(x));
        }
        return sb.toString();
    }

    // Print out only first N
    public static String D(final double[] x, final int firstN) {
        if (firstN >= x.length)
            return D(x);
        return D(ListUtils.subArray(x, 0, firstN)) + " ...(" + (x.length - firstN) + " more)";
    }

    public static String D(final double[][] x) {
        return D(x, " ");
    }

    public static String D(final double[][] xs, final String delim) {
        final StringBuilder sb = new StringBuilder();
        for (final double[] x : xs) {
            if (sb.length() > 0)
                sb.append(delim);
            sb.append(Fmt.D(x));
        }
        return sb.toString();
    }

    public static String D(final TDoubleMap map) {
        return D(map, 20);
    }

    public static String D(final TDoubleMap map, final int numTop) {
        return MapUtils.topNToString(map, numTop);
    }

    public static String D(final Object o) {
        if (o instanceof double[])
            return Fmt.D((double[]) o);
        if (o instanceof double[][])
            return Fmt.D((double[][]) o);
        if (o instanceof double[][][])
            return Fmt.D(o);
        throw Exceptions.unknownCase;
    }

    public static String bytesToString(final long b) {
        final double gb = (double) b / (1024 * 1024 * 1024);
        if (gb >= 1)
            return gb >= 10 ? (int) gb + "G" : round(gb, 1) + "G";
        final double mb = (double) b / (1024 * 1024);
        if (mb >= 1)
            return mb >= 10 ? (int) mb + "M" : round(mb, 1) + "M";
        final double kb = (double) b / (1024);
        if (kb >= 1)
            return kb >= 10 ? (int) kb + "K" : round(kb, 1) + "K";
        return b + "";
    }

    private static double round(final double x, final int numPlaces) {
        final double scale = Math.pow(10, numPlaces);
        return Math.round(x * scale) / scale;
    }

    public static String formatEasyDateTime(final long t) {
        return new SimpleDateFormat("MM/dd HH:mm").format(t);
    }
}

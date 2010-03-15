package edu.ohsu.cslu.parser.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

public class ParserUtil {

    // parse 'line' into tokens and return them in an array
    public static String[] tokenize(final String line) {
        final StringTokenizer st = new StringTokenizer(line);
        final int numTokens = st.countTokens();
        final String[] tokens = new String[numTokens];

        int i = 0;
        while (st.hasMoreTokens()) {
            tokens[i++] = st.nextToken();
        }

        return tokens;
    }

    public static String join(final Collection<String> s, final String delimiter) {
        final StringBuffer buffer = new StringBuffer();
        final Iterator<String> iter = s.iterator();
        while (iter.hasNext()) {
            buffer.append(iter.next());
            if (iter.hasNext()) {
                buffer.append(delimiter);
            }
        }
        return buffer.toString();
    }

    public static boolean isUpperCase(final String s) {
        return s == s.toUpperCase();
    }

    public static boolean isLowerCase(final String s) {
        return s == s.toLowerCase();
    }

    public static boolean containsDigit(final String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isDigit(s.charAt(i)))
                return true;
        }
        return false;
    }

    public class Pair<Type1, Type2> {
        public final Type1 one;
        public final Type2 two;

        public Pair(final Type1 a, final Type2 b) {
            one = a;
            two = b;
        }
    }

    public double safeLog(final double value) {
        if (value == 0) {
            return Double.NEGATIVE_INFINITY;
        }
        return Math.log(value);
    }

    public static double logSum(final double a, final double b) {
        // NOTE: these conditions were necessary when multiplying multiple values
        // of Float.NEGATIVE_INFINITY for the result of a or b because logSum was
        // returning NaN
        if (a <= Double.NEGATIVE_INFINITY) {
            return b;
        }
        if (b <= Double.NEGATIVE_INFINITY) {
            return a;
        }

        if (a > b) {
            return a + Math.log(Math.exp(b - a) + 1);
        }
        return b + Math.log(Math.exp(a - b) + 1);
    }

    public static List<Boolean> binValue(final float value, final int min, final int max, final int numBins) {
        final List<Boolean> bins = new LinkedList<Boolean>();
        final float step = (float) (max - min) / (float) (numBins - 1);

        for (float thresh = min; thresh <= max; thresh += step) {
            if (value >= thresh) {
                bins.add(true);
            } else {
                bins.add(false);
            }
        }

        // System.out.println("binValue() value=" + value + " min=" + min + " max=" + max + " numBins=" + numBins + " step=" + step + " bin=" + boolListToString(bins));

        return bins;
    }

    public static int boolToInt(final boolean value) {
        if (value == true) {
            return 1;
        }
        return 0;
    }
}

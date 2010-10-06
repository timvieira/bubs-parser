package edu.ohsu.cslu.parser;

import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;

public class ParserUtil {

    // TODO Replace with String.split() ?
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
}

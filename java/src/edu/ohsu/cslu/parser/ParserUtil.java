package edu.ohsu.cslu.parser;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;

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

    // TODO Merge with Math.logSum()
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

    public static InputStream file2inputStream(final String fileName) throws IOException {
        InputStream is = new FileInputStream(fileName);
        if (fileName.endsWith(".gz")) {
            is = new GZIPInputStream(is);
        }
        return is;
    }

    public static String intArray2Str(final int[] data) {
        String result = "";
        for (final int val : data) {
            result += val + " ";
        }
        return result.trim();
    }

    public static int bool2int(final boolean value) {
        if (value) {
            return 1;
        }
        return 0;
    }

    public static int[] strToIntArray(final String str) {
        return strToIntArray(str, ",", 0, -1);
    }

    public static int[] strToIntArray(final String str, final String delim, final float defaultVal, final int numBins) {
        final float[] floatArray = strToFloatArray(str, delim, defaultVal, numBins);
        final int[] intArray = new int[floatArray.length];
        for (int i = 0; i < floatArray.length; i++) {
            intArray[i] = (int) floatArray[i];
        }
        return intArray;
    }

    public static float[] strToFloatArray(final String str) {
        return strToFloatArray(str, ",", 0f, -1);
    }

    // assuming str has the form: x,y,z
    public static float[] strToFloatArray(final String str, final String delim, final float defaultVal, int numBins) {
        final String[] tokens = str.split(delim);
        if (numBins == -1) {
            numBins = tokens.length;
        }

        final float[] array = new float[tokens.length];
        Arrays.fill(array, defaultVal);

        for (int i = 0; i < tokens.length; i++) {
            array[i] = Float.parseFloat(tokens[i]);
        }
        return array;
    }
}

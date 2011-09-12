/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
 * 
 * This file is part of the BUBS Parser.
 * 
 * The BUBS Parser is free software: you can redistribute it and/or 
 * modify  it under the terms of the GNU Affero General Public License 
 * as published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * The BUBS Parser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>
 */
package edu.ohsu.cslu.parser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

public class Util {

    public static String join(final Collection<String> s, final String delimiter) {
        final StringBuilder buffer = new StringBuilder();
        final Iterator<String> iter = s.iterator();
        while (iter.hasNext()) {
            buffer.append(iter.next());
            if (iter.hasNext()) {
                buffer.append(delimiter);
            }
        }
        return buffer.toString();
    }

    public static String join(final Object[] objs, final String delimiter) {
        final StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < objs.length - 1; i++) {
            buffer.append(objs[i].toString());
            buffer.append(delimiter);
        }
        buffer.append(objs[objs.length - 1].toString());
        return buffer.toString();
    }

    public static String join(final long[] objs, final String delimiter) {
        final StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < objs.length - 1; i++) {
            buffer.append(Long.toString(objs[i]));
            buffer.append(delimiter);
        }
        buffer.append(Long.toString(objs[objs.length - 1]));
        return buffer.toString();
    }

    public static String join(final int[] objs, final String delimiter) {
        final StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < objs.length - 1; i++) {
            buffer.append(Integer.toString(objs[i]));
            buffer.append(delimiter);
        }
        buffer.append(Integer.toString(objs[objs.length - 1]));
        return buffer.toString();
    }

    public static String join(final short[] objs, final String delimiter) {
        final StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < objs.length - 1; i++) {
            buffer.append(Short.toString(objs[i]));
            buffer.append(delimiter);
        }
        buffer.append(Short.toString(objs[objs.length - 1]));
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

    public static InputStream file2inputStream(final String fileName) throws FileNotFoundException, IOException {
        if (fileName.endsWith(".gz")) {
            return new GZIPInputStream(new FileInputStream(fileName));
        }
        return new FileInputStream(fileName);
    }

    public static String intArray2Str(final int[] data) {
        String result = "";
        for (final int val : data) {
            result += val + " ";
        }
        return result.trim();
    }

    public static String floatArray2Str(final float[] data) {
        String result = "";
        for (final float val : data) {
            result += String.format("%.2f ", val);
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

    public static float str2float(final String s) {
        if (s.toLowerCase().equals("inf")) {
            return Float.POSITIVE_INFINITY;
        }
        return Float.parseFloat(s);
    }

    public static HashMap<String, String> readKeyValuePairs(final String line, final String delim) {
        final HashMap<String, String> keyVals = new HashMap<String, String>();
        final String[] toks = line.trim().split(" +");
        for (final String item : toks) {
            if (item.contains(delim)) {
                final String[] keyValStr = item.split(delim);
                keyVals.put(keyValStr[0], keyValStr[1]);
            }
        }
        return keyVals;
    }

    public static HashMap<String, String> readKeyValuePairs(final String line) {
        return readKeyValuePairs(line, "=");
    }
}

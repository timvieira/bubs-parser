/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
 * 
 * This file is part of the cslu-common project.
 * 
 * cslu-common is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * cslu-common is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with cslu-common. If not, see <http://www.gnu.org/licenses/>
 */
package edu.ohsu.cslu.tests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import edu.ohsu.cslu.util.Strings;

/**
 * JUnit assertion utility methods.
 * 
 * @author Aaron Dunlop
 * @since Feb 7, 2011
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class JUnit {

    public final static String UNIT_TEST_DIR = "unit-test-data/";
    public final static String SHARED_UNIT_TEST_DIR = "../shared-nlp-code/" + UNIT_TEST_DIR;

    /**
     * Returns a {@link Reader} reading the specified unit test file (from the shared unit test data directory).
     * Uncompresses gzip-compressed files transparently.
     * 
     * @param filename Unit test file
     * @return a Reader reading the specified unit test file
     * @throws IOException If unable to find or open the file
     */
    public static Reader unitTestDataAsReader(final String filename) throws IOException {
        return new InputStreamReader(unitTestDataAsStream(filename));
    }

    /**
     * Returns an {@link InputStream} reading the specified unit test file (from the shared unit test data directory).
     * Uncompresses gzip-compressed files transparently.
     * 
     * @param filename Unit test file
     * @return a InputStream reading the specified unit test file
     * @throws IOException If unable to find or open the file
     */
    public static InputStream unitTestDataAsStream(final String filename) throws IOException {
        try {
            InputStream is = new FileInputStream(UNIT_TEST_DIR + filename);
            if (filename.endsWith(".gz")) {
                is = new GZIPInputStream(is);
            }
            return is;
        } catch (final FileNotFoundException e) {
            // A hack to read files in the shared unit test data directory from tests run within
            // other projects
            InputStream is = new FileInputStream(SHARED_UNIT_TEST_DIR + filename);
            if (filename.endsWith(".gz")) {
                is = new GZIPInputStream(is);
            }
            return is;
        }
    }

    /**
     * Returns a {@link String} containing the contents of the specified unit test file (from the shared unit test data
     * directory). Uncompresses gzip-compressed files transparently.
     * 
     * @param filename Unit test file
     * @return a String containing the contents of the specified unit test file
     * @throws IOException If unable to find or open the file
     */
    public static String unitTestDataAsString(final String filename) throws IOException {
        return new String(readUnitTestData(filename));
    }

    // TODO Document, rename
    public static byte[] readUnitTestData(final String filename) throws IOException {
        return readUnitTestData(unitTestDataAsStream(filename));
    }

    private static byte[] readUnitTestData(final InputStream is) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
        final byte[] buf = new byte[1024];
        for (int i = is.read(buf); i >= 0; i = is.read(buf)) {
            bos.write(buf, 0, i);
        }
        is.close();
        return bos.toByteArray();
    }

    public static void assertArrayEquals(final String message, final float[][] expected, final float[][] actual,
            final float delta) {
        for (int i = 0; i < actual.length; i++) {
            for (int j = 0; j < actual[0].length; j++) {
                org.junit.Assert.assertEquals(message, expected[i][j], actual[i][j], delta);
            }
        }
    }

    public static void assertArrayEquals(final float[][] expected, final float[][] actual, final float delta) {
        assertArrayEquals(null, expected, actual, delta);
    }

    public static void assertArrayEquals(final String message, final int[][] expected, final int[][] actual) {
        for (int i = 0; i < actual.length; i++) {
            for (int j = 0; j < actual[0].length; j++) {
                org.junit.Assert.assertEquals(message, expected[i][j], actual[i][j]);
            }
        }
    }

    public static void assertArrayEquals(final int[][] expected, final int[][] actual) {
        assertArrayEquals(null, expected, actual);
    }

    public static void assertArrayEquals(final String message, final short[][] expected, final short[][] actual) {
        for (int i = 0; i < actual.length; i++) {
            for (int j = 0; j < actual[0].length; j++) {
                org.junit.Assert.assertEquals(message, expected[i][j], actual[i][j]);
            }
        }
    }

    public static void assertArrayEquals(final short[][] expected, final short[][] actual) {
        assertArrayEquals(null, expected, actual);
    }

    public static void assertContains(final Collection<?> collection, final Object expected) {
        assertTrue("Expected " + expected.toString(), collection.contains(expected));
    }

    public static <K, V> void assertMapContains(final Map<K, V> map, final K key, final V value) {
        assertTrue("Expected " + key.toString() + " -> " + value.toString(), map.containsKey(key)
                && map.get(key).equals(value));
    }

    /**
     * Asserts that two doubles or floats are equal to within a positive delta. If they are not, an
     * {@link AssertionError} is thrown. The values are expected to be natural logs of probabilities, and the failure
     * message is formulated as a fraction. e.g. assertLogFractionEquals(Math.log(.5), Math.log(.25), .01) should return
     * "expected 1/2 but was 1/4".
     * 
     * If the expected value is infinity then the delta value is ignored. NaNs are considered equal:
     * <code>assertEquals(Double.NaN, Double.NaN, *)</code> passes
     * 
     * @param expected expected value
     * @param actual the value to check against <code>expected</code>
     * @param delta the maximum delta between <code>expected</code> and <code>actual</code> for which both numbers are
     *            still considered equal.
     */
    public static void assertLogFractionEquals(final double expected, final double actual, final double delta) {
        if (Double.compare(expected, actual) == 0) {
            return;
        }

        if (!(Math.abs(expected - actual) <= delta)) {
            fail("expected log(" + Strings.fraction(expected) + ") but was log(" + Strings.fraction(actual) + ")");
        }
    }
}

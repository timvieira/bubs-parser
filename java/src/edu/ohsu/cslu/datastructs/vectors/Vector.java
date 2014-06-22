/*
 * Copyright 2010-2014, Oregon Health & Science University
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
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Further documentation and contact information is available at
 *   https://code.google.com/p/bubs-parser/ 
 */
package edu.ohsu.cslu.datastructs.vectors;

import it.unimi.dsi.fastutil.longs.Long2FloatMap;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import edu.ohsu.cslu.util.Strings;

/**
 * Base interface for all vector classes.
 * 
 * @author Aaron Dunlop
 * @since Sep 17, 2008
 * 
 *        $Id$
 */
public interface Vector {

    /**
     * Returns the length of the vector.
     * 
     * @return length.
     */
    public long length();

    /**
     * Retrieves a vector element
     * 
     * @param i index
     * @return the vector element as an integer
     */
    public int getInt(final int i);

    /**
     * Retrieves a vector element
     * 
     * @param i index
     * @return the vector element as a float
     */
    public float getFloat(final int i);

    /**
     * Retrieves a vector element
     * 
     * @param i index
     * @return the vector element as a boolean
     */
    public boolean getBoolean(final int i);

    /**
     * Sets a vector element
     * 
     * @param i index
     * @param value new value
     */
    public void set(final int i, final int value);

    /**
     * Sets a vector element
     * 
     * @param i index
     * @param value new value
     */
    public void set(final int i, final float value);

    /**
     * Sets a vector element
     * 
     * @param i index
     * @param value new value
     */
    public void set(final int i, final boolean value);

    /**
     * Parses and sets a vector element
     * 
     * @param i index
     * @param newValue new value
     */
    public void set(final int i, final String newValue);

    /**
     * Adds two {@link Vector}s, returning a new {@link Vector}
     * 
     * @param v addend
     * @return sum of the two {@link Vector}s, as a new {@link Vector} instance
     */
    public NumericVector add(Vector v);

    /**
     * Multiplies each element of two {@link Vector}s, returning a new {@link Vector}
     * 
     * @param v multiplicand, a vector of the same length.
     * @return a new vector in which each element is the multiple of the corresponding elements of the two supplied
     *         {@link Vector}s.
     */
    public Vector elementwiseMultiply(Vector v);

    /**
     * @return the maximum value which can be stored by this vector
     */
    public float infinity();

    /**
     * @return the minimum value which can be stored by this vector
     */
    public float negativeInfinity();

    public float dotProduct(Vector v);

    /**
     * Creates a subvector spanning the specified indices.
     * 
     * @param i0 Initial index (inclusive)
     * @param i1 Final index (inclusive)
     * @return A(i0:i1)
     * @throws ArrayIndexOutOfBoundsException if subVector indices are outside the range of the Vector
     */
    public Vector subVector(final int i0, final int i1);

    /**
     * Not terribly useful mathematically, but useful as a simple single statistic about a vector for unit testing.
     * 
     * Note that we don't test this method, since it's not really meaningful anyway.
     * 
     * @return the sum of all entries in this vector
     */
    public float sum();

    /**
     * Write this vector to the specified writer
     * 
     * @param writer the output location
     * @throws IOException if the write fails
     */
    public void write(Writer writer) throws IOException;

    /**
     * Type-strengthen return-type
     * 
     * @return a copy of this vector
     */
    public Vector clone();

    /**
     * Factory class for all vector types
     */
    public static class Factory {

        private final static String ATTRIBUTE_TYPE = "type";
        private final static String ATTRIBUTE_LENGTH = "length";

        // TODO Move these into the actual implementation classes?
        public final static String ATTRIBUTE_TYPE_INT = "int";
        public final static String ATTRIBUTE_TYPE_PACKED_INTEGER = "packed-int";
        public final static String ATTRIBUTE_TYPE_MUTABLE_SPARSE_INT = "mutable-sparse-int";
        public final static String ATTRIBUTE_TYPE_LARGE_SPARSE_INT = "large-sparse-int";
        public final static String ATTRIBUTE_TYPE_DENSE_FLOAT = "float";
        public final static String ATTRIBUTE_TYPE_MUTABLE_SPARSE_FLOAT = "mutable-sparse-float";
        public final static String ATTRIBUTE_TYPE_LARGE_SPARSE_FLOAT = "large-sparse-float";
        public final static String ATTRIBUTE_TYPE_FIXED_POINT_SHORT = "fixed-point-short";
        public final static String ATTRIBUTE_TYPE_PACKED_BIT = "packed-bit";
        public final static String ATTRIBUTE_TYPE_SPARSE_BIT = "sparse-bit";
        public final static String ATTRIBUTE_TYPE_LARGE_SPARSE_BIT = "large-sparse-bit";
        public final static String ATTRIBUTE_TYPE_MUTABLE_SPARSE_BIT = "mutable-sparse-bit";
        public final static String ATTRIBUTE_TYPE_MUTABLE_LARGE_SPARSE_BIT = "mutable-large-sparse-bit";

        public static Vector read(final String s) throws IOException {
            return read(new StringReader(s));
        }

        public static Vector read(final File f) throws IOException {
            InputStream is = new FileInputStream(f);
            if (f.getName().endsWith(".gz")) {
                is = new GZIPInputStream(is);
            }
            return read(is);
        }

        public static Vector read(final InputStream inputStream) throws IOException {
            return read(new InputStreamReader(inputStream));
        }

        public static Vector read(final Reader reader) throws IOException {
            final BufferedReader br = (reader instanceof BufferedReader) ? (BufferedReader) reader
                    : new BufferedReader(reader);
            final String headerLine = br.readLine();
            final Map<String, String> attributes = Strings.headerAttributes(headerLine);

            final String type = attributes.get(ATTRIBUTE_TYPE);

            final long length = Long.parseLong(attributes.get(ATTRIBUTE_LENGTH));

            if (type.equals(ATTRIBUTE_TYPE_INT)) {
                return new DenseIntVector(readFloatArray(br, length));

            } else if (type.equals(ATTRIBUTE_TYPE_PACKED_INTEGER)) {
                return new PackedIntVector(readIntArray(br));

            } else if (type.equals(ATTRIBUTE_TYPE_MUTABLE_SPARSE_INT)) {
                final Long2IntMap map = readSparseIntMap(br);
                return new MutableSparseIntVector(length, map);

            } else if (type.equals(ATTRIBUTE_TYPE_LARGE_SPARSE_INT)) {
                final Long2IntMap map = readSparseIntMap(br);
                return new LargeSparseIntVector(length, map);

            } else if (type.equals(ATTRIBUTE_TYPE_DENSE_FLOAT)) {
                return new DenseFloatVector(readFloatArray(br, length));

            } else if (type.equals(ATTRIBUTE_TYPE_MUTABLE_SPARSE_FLOAT)) {
                final Long2FloatMap map = readSparseFloatMap(br);
                return new MutableSparseFloatVector(length, map);

            } else if (type.equals(ATTRIBUTE_TYPE_LARGE_SPARSE_FLOAT)) {
                final Long2FloatMap map = readSparseFloatMap(br);
                return new LargeSparseFloatVector(length, map);

            } else if (type.equals(ATTRIBUTE_TYPE_PACKED_BIT)) {
                return new PackedBitVector(length, readLongArray(br));

            } else if (type.equals(ATTRIBUTE_TYPE_SPARSE_BIT)) {
                // Read in the contents as an array of integers
                return new SparseBitVector(readIntArray(br));

            } else if (type.equals(ATTRIBUTE_TYPE_LARGE_SPARSE_BIT)) {
                // Read in the contents as an array of integers
                return new LargeSparseBitVector(length, readLongArray(br));

            } else if (type.equals(ATTRIBUTE_TYPE_MUTABLE_SPARSE_BIT)) {
                // Read in the contents as an array of integers
                return new MutableSparseBitVector(readIntArray(br));

            } else if (type.equals(ATTRIBUTE_TYPE_MUTABLE_LARGE_SPARSE_BIT)) {
                // Read in the contents as an array of integers
                return new MutableLargeSparseBitVector(length, readLongArray(br));

            } else {
                throw new RuntimeException(type + " type is not supported");
            }
        }

        private static int[] readIntArray(final BufferedReader br) throws IOException {
            final String[] split = Strings.splitOnSpace(br.readLine());
            final int[] array = new int[split.length];
            for (int i = 0; i < array.length; i++) {
                array[i] = Integer.parseInt(split[i]);
            }
            return array;
        }

        private static long[] readLongArray(final BufferedReader br) throws IOException {
            final String[] split = Strings.splitOnSpace(br.readLine());
            final long[] array = new long[split.length];
            for (int i = 0; i < array.length; i++) {
                array[i] = Long.parseLong(split[i]);
            }
            return array;
        }

        private static Long2IntMap readSparseIntMap(final BufferedReader br) throws IOException {
            final String[] split = Strings.splitOnSpace(br.readLine());
            final Long2IntMap map = new Long2IntOpenHashMap(split.length / 2);
            for (int i = 0; i < split.length; i += 2) {
                map.put(Long.parseLong(split[i]), Integer.parseInt(split[i + 1]));
            }
            return map;
        }

        private static float[] readFloatArray(final BufferedReader br, final long length) throws IOException {
            final String[] split = Strings.splitOnSpace(br.readLine());
            final float[] array = new float[(int) length];
            for (int i = 0; i < split.length; i++) {
                array[i] = Float.parseFloat(split[i]);
            }
            return array;
        }

        private static Long2FloatMap readSparseFloatMap(final BufferedReader br) throws IOException {
            final String[] split = Strings.splitOnSpace(br.readLine());
            final Long2FloatMap map = new Long2FloatOpenHashMap(split.length / 2);
            for (int i = 0; i < split.length; i += 2) {
                map.put(Long.parseLong(split[i]), Float.parseFloat(split[i + 1]));
            }
            return map;
        }

        static int[] sparseIntElementArray(final int[] sparseIntArray) {
            final int[] tmpElements = new int[sparseIntArray.length / 2];
            int j = 0;
            for (int i = 0; i < sparseIntArray.length; i = i + 2) {
                if (sparseIntArray[i + 1] != 0) {
                    tmpElements[j++] = sparseIntArray[i];
                }
            }
            Arrays.sort(tmpElements);

            if (j == sparseIntArray.length / 2) {
                return tmpElements;
            }

            final int[] elements = new int[j];
            System.arraycopy(tmpElements, 0, elements, 0, j);
            return elements;
        }
    }
}

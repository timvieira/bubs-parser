package edu.ohsu.cslu.datastructs.vectors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
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
public interface Vector
{
    /**
     * Returns the length of the vector.
     * 
     * @return length.
     */
    public int length();

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
     * @return a new vector in which each element is the multiple of the corresponding elements of
     *         the two supplied {@link Vector}s.
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

    /**
     * @return the maximum value present in the vector
     */
    public float max();

    /**
     * @return the maximum value present in the vector as an int
     */
    public int intMax();

    /**
     * @return the index of the maximum value present in the vector; i such that
     *         {@link #getInt(int)} will return the same value as {@link #max()}.
     */
    public int argMax();

    /**
     * @return the minimum value present in the vector
     */
    public float min();

    /**
     * @return the minimum value present in the vector as an int
     */
    public int intMin();

    /**
     * @return the index of the minimum value present in the vector; i such that
     *         {@link #getInt(int)} will return the same value as {@link #min()}.
     */
    public int argMin();

    /**
     * @param addend The value to add
     * @return a new vector scaled by the provided addend
     */
    public NumericVector scalarAdd(float addend);

    /**
     * @param addend The value to add
     * @return a new vector scaled by the provided addend
     */
    public NumericVector scalarAdd(int addend);

    /**
     * @param multiplier
     * @return a new vector scaled by the provided multiplier
     */
    public NumericVector scalarMultiply(float multiplier);

    /**
     * @param multiplier
     * @return a new vector scaled by the provided multiplier
     */
    public NumericVector scalarMultiply(int multiplier);

    public float dotProduct(Vector v);

    /**
     * Creates a subvector spanning the specified indices.
     * 
     * @param i0 Initial index (inclusive)
     * @param i1 Final index (inclusive)
     * @return A(i0:i1)
     * @throws ArrayIndexOutOfBoundsException if subVector indices are outside the range of the
     *             Vector
     */
    public Vector subVector(final int i0, final int i1);

    /**
     * Not terribly useful mathematically, but useful as a simple single statistic about a vector
     * for unit testing.
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
    public static class Factory
    {
        private final static String ATTRIBUTE_TYPE = "type";
        private final static String ATTRIBUTE_LENGTH = "length";
        // /** Used for fixed-point implementations */
        // private final static String ATTRIBUTE_PRECISION = "precision";
        /** Used for packed integer implementations */
        private final static String ATTRIBUTE_BITS = "bits";

        public final static String ATTRIBUTE_TYPE_INT = "int";
        public final static String ATTRIBUTE_TYPE_PACKED_INTEGER = "packed-int";
        public final static String ATTRIBUTE_TYPE_FLOAT = "float";
        public final static String ATTRIBUTE_TYPE_FIXED_POINT_SHORT = "fixed-point-short";
        public final static String ATTRIBUTE_TYPE_PACKED_BIT = "packed-bit";
        public final static String ATTRIBUTE_TYPE_SPARSE_BIT = "sparse-bit";
        public final static String ATTRIBUTE_TYPE_MUTABLE_SPARSE_BIT = "mutable-sparse-bit";

        public static Vector read(String s) throws IOException
        {
            return read(new StringReader(s));
        }

        public static Vector read(File f) throws IOException
        {
            InputStream is = new FileInputStream(f);
            if (f.getName().endsWith(".gz"))
            {
                is = new GZIPInputStream(is);
            }
            return read(is);
        }

        public static Vector read(InputStream inputStream) throws IOException
        {
            return read(new InputStreamReader(inputStream));
        }

        public static Vector read(Reader reader) throws IOException
        {
            BufferedReader br = new BufferedReader(reader);
            Map<String, String> attributes = Strings.headerAttributes(br.readLine());

            String type = attributes.get(ATTRIBUTE_TYPE);

            int length = Integer.parseInt(attributes.get(ATTRIBUTE_LENGTH));

            Vector vector = null;
            if (type.equals(ATTRIBUTE_TYPE_INT))
            {
                vector = new IntVector(length);
            }
            else if (type.equals(ATTRIBUTE_TYPE_FLOAT))
            {
                vector = new FloatVector(length);
            }
            else if (type.equals(ATTRIBUTE_TYPE_FIXED_POINT_SHORT))
            {
                throw new RuntimeException(type + " type is not supported");
            }
            else if (type.equals(ATTRIBUTE_TYPE_PACKED_BIT))
            {
                vector = new PackedBitVector(length);
            }
            else if (type.equals(ATTRIBUTE_TYPE_SPARSE_BIT))
            {
                // Special-case, to read in the contents as an array of integers
                return new SparseBitVector(readIntArray(br));
            }
            else if (type.equals(ATTRIBUTE_TYPE_MUTABLE_SPARSE_BIT))
            {
                // Special-case, to read in the contents as an array of integers
                return new MutableSparseBitVector(readIntArray(br));
            }
            else if (type.equals(ATTRIBUTE_TYPE_PACKED_INTEGER))
            {
                int bits = Integer.parseInt(attributes.get(ATTRIBUTE_BITS));
                vector = new PackedIntVector(length, bits);
            }
            else if (type.equals("double"))
            {
                throw new RuntimeException(type + " type is not supported");
            }
            else
            {
                throw new RuntimeException(type + " type is not supported");
            }

            // Read and initialize vector
            String[] split = br.readLine().split(" +");
            if (split.length != length)
            {
                throw new IllegalArgumentException("Serialized vector length mismatch");
            }
            for (int i = 0; i < split.length; i++)
            {
                vector.set(i, split[i]);
            }

            return vector;
        }

        private static int[] readIntArray(BufferedReader br) throws IOException
        {
            String[] split = br.readLine().split(" +");
            int[] elements = new int[split.length];
            for (int i = 0; i < elements.length; i++)
            {
                elements[i] = Integer.parseInt(split[i]);
            }
            return elements;
        }
    }
}

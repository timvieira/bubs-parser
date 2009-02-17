package edu.ohsu.cslu.math.linear;

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
 * TODO: Move infinity, min/max methods to a superinterface of both Vector and Matrix? If so, what
 * would we name it?
 * 
 * @author Aaron Dunlop
 * @since Sep 17, 2008
 * 
 *        $Id$
 */
public interface Vector
{
    /**
     * Get length of the vector.
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
    public Vector scalarAdd(float addend);

    /**
     * @param addend The value to add
     * @return a new vector scaled by the provided addend
     */
    public Vector scalarAdd(int addend);

    /**
     * @param multiplier
     * @return a new vector scaled by the provided multiplier
     */
    public Vector scalarMultiply(float multiplier);

    /**
     * @param multiplier
     * @return a new vector scaled by the provided multiplier
     */
    public Vector scalarMultiply(int multiplier);

    public float dotProduct(Vector v);

    /**
     * Create a subvector spanning the specified indices.
     * 
     * @param i0 Initial index
     * @param i1 Final index
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
        public final static String ATTRIBUTE_TYPE_FLOAT = "float";
        public final static String ATTRIBUTE_TYPE_FIXED_POINT_SHORT = "fixed-point-short";
        public final static String ATTRIBUTE_TYPE_BIT = "bit";
        public final static String ATTRIBUTE_TYPE_PACKED_INTEGER = "packed-int";

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
            else if (type.equals(ATTRIBUTE_TYPE_BIT))
            {
                vector = new BitVector(length);
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
            String line = br.readLine();
            String[] split = line.split(" +");
            if (split.length != vector.length())
            {
                throw new IllegalArgumentException("Serialized vector length mismatch");
            }
            for (int i = 0; i < split.length; i++)
            {
                vector.set(i, split[i]);
            }

            return vector;

        }
    }

}

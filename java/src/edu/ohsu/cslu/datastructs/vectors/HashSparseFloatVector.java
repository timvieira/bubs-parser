package edu.ohsu.cslu.datastructs.vectors;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;

import java.io.IOException;
import java.io.Writer;

/**
 * Sparse float vector which stores entries in a hash.
 * 
 * TODO Add getter and setter methods which index columns by a long instead of an int.
 * 
 * @author Aaron Dunlop
 * @since Dec 16, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class HashSparseFloatVector extends BaseNumericVector
{
    private Long2FloatOpenHashMap map = new Long2FloatOpenHashMap();

    /**
     * Initializes all elements to a default value.
     * 
     * @param length The size of the vector
     * @param defaultValue The value assigned to all elements
     */
    public HashSparseFloatVector(final int length, final float defaultValue)
    {
        super(length);
        map.defaultReturnValue(defaultValue);
    }

    /**
     * Initializes all elements to 0.
     * 
     * @param length The size of the vector
     */
    public HashSparseFloatVector(final int length)
    {
        this(length, 0);
    }

    /**
     * Initializes a {@link HashSparseFloatVector} from a floating-point array. Vector indices and
     * values are assumed to be in sorted order in adjacent array slots. (e.g, {2, 4, 5, 8}
     * represents a Vector of length 6 in which 2 elements are present). The length of the vector is
     * the final populated index.
     * 
     * @param vector The populated vector elements in index, value tuples.
     */
    public HashSparseFloatVector(final float[] vector)
    {
        this(Math.round(vector[vector.length - 2]) + 1, 0);
        for (int i = 0; i < vector.length; i = i + 2)
        {
            set(Math.round(vector[i]), vector[i + 1]);
        }
    }

    /**
     * Copies an {@link Int2FloatOpenHashMap} into a new {@link HashSparseFloatVector}.
     * 
     * @param length
     * @param map
     */
    private HashSparseFloatVector(final int length, final Long2FloatOpenHashMap map)
    {
        this(length, map.defaultReturnValue());
        this.map.putAll(map);
    }

    @Override
    public float getFloat(final int i)
    {
        return map.get(i);
    }

    @Override
    public int getInt(final int i)
    {
        return Math.round(map.get(i));
    }

    @Override
    public float infinity()
    {
        return Float.POSITIVE_INFINITY;
    }

    @Override
    public float negativeInfinity()
    {
        return Float.NEGATIVE_INFINITY;
    }

    @Override
    public void set(final int i, final int value)
    {
        map.put(i, value);
    }

    @Override
    public void set(final int i, final float value)
    {
        map.put(i, value);
    }

    @Override
    public void set(final int i, final boolean value)
    {
        map.put(i, value ? 1 : 0);
    }

    @Override
    public void set(final int i, final String newValue)
    {
        map.put(i, Float.parseFloat(newValue));
    }

    @Override
    public Vector subVector(final int i0, final int i1)
    {
        final Long2FloatOpenHashMap newMap = new Long2FloatOpenHashMap();

        for (int i = i0; i <= i1; i++)
        {
            if (map.containsKey(i))
            {
                newMap.put(i - i0, map.get(i));
            }
        }

        return new HashSparseFloatVector(i1 - i0 + 1, newMap);
    }

    @Override
    protected HashSparseFloatVector createIntVector(final int newVectorLength)
    {
        return new HashSparseFloatVector(newVectorLength);
    }

    @Override
    protected HashSparseFloatVector createFloatVector(final int newVectorLength)
    {
        return new HashSparseFloatVector(newVectorLength);
    }

    @Override
    public void write(final Writer writer) throws IOException
    {
        writer.write(String.format("vector type=hash-sparse-float length=%d sparse=true\n", length));

        // Write Vector contents
        for (int i = 0; i < length - 1; i++)
        {
            if (map.containsKey(i))
            {
                writer.write(String.format("%d %f ", i, getFloat(i)));
            }
        }
        writer.write(String.format("%d %f\n", length - 1, getFloat(length - 1)));
        writer.flush();
    }

    @Override
    public HashSparseFloatVector clone()
    {
        return new HashSparseFloatVector(length, map);
    }

}
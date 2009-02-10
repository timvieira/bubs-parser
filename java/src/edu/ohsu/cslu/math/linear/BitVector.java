package edu.ohsu.cslu.math.linear;

import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;

/**
 * Implementation of the {@link Vector} interface which stores single bits (logically booleans)
 * packed into an array of 32-bit ints. This class is generally useful to store binary feature
 * vectors and boolean sets. In addition to the normal {@link Vector} methods, it also implements
 * convenience methods named in standard set convention.
 * 
 * @author Aaron Dunlop
 * @since Sep 11, 2008
 * 
 *        $Id$
 */
public final class BitVector extends BaseVector implements Vector, Serializable
{
    private final static long serialVersionUID = 379752896212698724L;

    private final int[] packedVector;

    public BitVector(final int length)
    {
        super(length);
        packedVector = new int[(length >> 5) + 1];
    }

    public BitVector(final int[] array)
    {
        super(array.length);
        this.packedVector = new int[(array.length >> 5) + 1];
        for (int i = 0; i < length; i++)
        {
            set(i, array[i] != 0);
        }
    }

    @Override
    public final boolean getBoolean(final int i)
    {
        final int index = i >> 5;
        final int shift = i & 0x1f;
        return ((packedVector[index] >> shift) & 0x01) == 1;
    }

    @Override
    public final float getFloat(final int i)
    {
        return getBoolean(i) ? 1 : 0;
    }

    @Override
    public final int getInt(final int i)
    {
        return getBoolean(i) ? 1 : 0;
    }

    @Override
    public void set(final int i, final int value)
    {
        set(i, value != 0);
    }

    @Override
    public void set(final int i, final float value)
    {
        set(i, value != 0);
    }

    @Override
    public void set(final int i, boolean value)
    {
        final int index = i >> 5;
        final int shift = (i & 0x1f);

        // Set
        if (value)
        {
            packedVector[index] = packedVector[index] | (1 << shift);
        }
        else
        {
            // Unset
            packedVector[index] = packedVector[index] & (~(1 << shift));
        }
    }

    @Override
    public void set(final int i, String newValue)
    {
        try
        {
            set(i, Integer.parseInt(newValue));
        }
        catch (NumberFormatException e)
        {
            set(i, Boolean.parseBoolean(newValue));
        }
    }

    @Override
    public final float infinity()
    {
        return 1;
    }

    @Override
    public final float negativeInfinity()
    {
        return 0;
    }

    @Override
    public final float dotProduct(Vector v)
    {
        // TODO: This can be made more efficient
        return super.dotProduct(v);
    }

    @Override
    public Vector subVector(final int i0, final int i1)
    {
        final int subVectorLength = i1 - i0 + 1;
        BitVector subVector = new BitVector(subVectorLength);
        for (int i = 0; i < subVectorLength; i++)
        {
            subVector.set(i, getBoolean(i0 + i));
        }
        return subVector;
    }

    @Override
    public void write(Writer writer) throws IOException
    {
        write(writer, String.format("vector type=bit length=%d\n", length));
    }

    /**
     * Set-convention convenience method
     * 
     * @param toAdd element to add to the set
     */
    public final void add(final int toAdd)
    {
        set(toAdd, true);
    }

    /**
     * Set-convention convenience method
     * 
     * @param toAdd elements to add to the set
     */
    public final void addAll(final int[] toAdd)
    {
        for (final int i : toAdd)
        {
            set(i, true);
        }
    }

    /**
     * Set-convention convenience method
     * 
     * @param toAdd elements to add to the set
     */
    public final void addAll(IntSet toAdd)
    {
        for (final int i : toAdd)
        {
            set(i, true);
        }
    }

    /**
     * Set-convention convenience method
     * 
     * @param i element whose presence in this set is to be tested
     * @return True if the specified element is contained in this set
     */
    public final boolean contains(final int i)
    {
        return getBoolean(i);
    }

    /**
     * Set-convention convenience method
     * 
     * @param toRemove element to remove from the set
     * @return True if the specified element was contained in this set
     */
    public final boolean remove(final int toRemove)
    {
        final int index = toRemove >> 5;
        final int shift = (toRemove & 0x1f);
        final int oldValue = packedVector[index];
        final int newValue = oldValue & (~(1 << shift));
        packedVector[index] = newValue;

        return (newValue != oldValue);
    }

    /**
     * Set-convention convenience method
     * 
     * @param toRemove elements to remove from the set
     */
    public final void removeAll(final int[] toRemove)
    {
        for (final int i : toRemove)
        {
            set(i, false);
        }
    }

    /**
     * Set-convention convenience method
     * 
     * @param toRemove elements to remove from the set
     */
    public final void removeAll(final IntSet toRemove)
    {
        for (final int i : toRemove)
        {
            set(i, false);
        }
    }

    @Override
    public Vector clone()
    {
        BitVector v = new BitVector(length);
        System.arraycopy(packedVector, 0, v.packedVector, 0, packedVector.length);
        return v;
    }

    // TODO: intersect, union?
    // TODO: Implement size() ? This might turn out to be useful, but could also be inefficient
}

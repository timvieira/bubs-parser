package edu.ohsu.cslu.datastructs.vectors;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

/**
 * Implementation of the {@link BitVector} interface which stores the indices of populated bits in
 * an {@link IntSet}.
 * 
 * This class is generally useful to store binary feature vectors in which a few of the bits will be
 * populated - if a large number of bits are likely to be populated, {@link PackedBitVector} will
 * likely be more efficient.
 * 
 * @author Aaron Dunlop
 * @since Sep 11, 2008
 * 
 *        $Id$
 */
public class SparseBitVector extends BaseVector implements BitVector
{
    // Allow direct access to other classes in the same package
    final int[] elements;

    public SparseBitVector()
    {
        super(0);
        elements = new int[0];
    }

    /**
     * Constructs a {@link SparseBitVector} from an integer array. Note that the semantics of this
     * constructor are different from those of most other {@link Vector} constructors with the same
     * signature - the int values contained in the parameter are themselves populated, whereas most
     * other constructors populate the _indices_ of the array which contain non-zero values.
     * 
     * @param array The vector indices to populate
     */
    public SparseBitVector(final int[] array)
    {
        super(array.length == 0 ? 0 : array[array.length - 1] + 1);
        this.elements = new int[array.length];
        System.arraycopy(array, 0, this.elements, 0, array.length);
        Arrays.sort(this.elements);
    }

    /**
     * Constructs a {@link SparseBitVector} from a boolean array.
     * 
     * @param array array of populated bits
     */
    public SparseBitVector(final boolean[] array)
    {
        super(array.length);

        IntSet newElements = new IntRBTreeSet();
        for (int i = 0; i < array.length; i++)
        {
            if (array[i])
            {
                newElements.add(i);
            }
        }

        this.elements = newElements.toIntArray();
    }

    @Override
    public Vector elementwiseMultiply(Vector v)
    {
        if (!(v instanceof BitVector))
        {
            return super.elementwiseMultiply(v);
        }

        // If we're multiplying two SparseBitVector instances, iterate through the smaller one.
        if (v instanceof SparseBitVector && ((SparseBitVector) v).elements.length < elements.length)
        {
            return ((SparseBitVector) v).elementwiseMultiply(this);
        }

        IntSet newContents = new IntRBTreeSet();
        for (int i : elements)
        {
            if (v.getBoolean(i))
            {
                newContents.add(i);
            }
        }

        return new SparseBitVector(newContents.toIntArray());
    }

    @Override
    public void add(int toAdd)
    {
        throw new UnsupportedOperationException("SparseBitVector is immutable");
    }

    @Override
    public void addAll(int[] toAdd)
    {
        throw new UnsupportedOperationException("SparseBitVector is immutable");
    }

    @Override
    public void addAll(IntSet toAdd)
    {
        throw new UnsupportedOperationException("SparseBitVector is immutable");
    }

    @Override
    public boolean contains(int i)
    {
        // TODO Since we maintain the elements in sorted order, we could use a binary search here
        for (int element : elements)
        {
            if (element == i)
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean remove(int toRemove)
    {
        throw new UnsupportedOperationException("SparseBitVector is immutable");
    }

    @Override
    public void removeAll(int[] toRemove)
    {
        throw new UnsupportedOperationException("SparseBitVector is immutable");
    }

    @Override
    public void removeAll(IntSet toRemove)
    {
        throw new UnsupportedOperationException("SparseBitVector is immutable");
    }

    @Override
    public int argMax()
    {
        // Return the lowest populated index (or 0 if the set is empty)
        return elements.length > 0 ? elements[0] : 0;
    }

    @Override
    public int argMin()
    {
        // If no index is populated, return 0
        if (elements.length == 0)
        {
            return 0;
        }

        // TODO This could probably be a lot more efficient
        for (int i = 0; i < length; i++)
        {
            if (!contains(i))
            {
                return i;
            }
        }
        return 0;
    }

    @Override
    public float dotProduct(Vector v)
    {
        try
        {
            float dotProduct = 0f;
            for (final int i : elements)
            {
                dotProduct += v.getFloat(i);
            }
            return dotProduct;
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            throw new IllegalArgumentException("Vector length mismatch");
        }
    }

    @Override
    public boolean getBoolean(int i)
    {
        return contains(i);
    }

    @Override
    public float getFloat(int i)
    {
        return contains(i) ? 1f : 0f;
    }

    @Override
    public int getInt(int i)
    {
        return contains(i) ? 1 : 0;
    }

    @Override
    public float infinity()
    {
        return 1;
    }

    @Override
    public int intMax()
    {
        // If any indices are populated, the maximum value is 1; otherwise, 0
        return elements.length > 0 ? 1 : 0;
    }

    @Override
    public int intMin()
    {
        // If all indices are populated, the minimum value is 1; otherwise, 0.
        return length == elements.length ? 1 : 0;
    }

    @Override
    public int length()
    {
        // Return the highest populated index + 1 (or 0 if no elements are populated)
        return elements.length > 0 ? elements[elements.length - 1] + 1 : 0;
    }

    @Override
    public float max()
    {
        return intMax();
    }

    @Override
    public float min()
    {
        return intMin();
    }

    @Override
    public float negativeInfinity()
    {
        return 0;
    }

    @Override
    public NumericVector scalarMultiply(float multiplier)
    {
        NumericVector v = new FloatVector(length);
        for (int i : elements)
        {
            v.set(i, multiplier);
        }
        return v;
    }

    @Override
    public NumericVector scalarMultiply(int multiplier)
    {
        NumericVector v = createIntVector();
        for (int i : elements)
        {
            v.set(i, multiplier);
        }
        return v;
    }

    @Override
    public void set(int i, int value)
    {
        set(i, value != 0);
    }

    @Override
    public void set(int i, float value)
    {
        set(i, value != 0);
    }

    @Override
    public void set(int i, boolean value)
    {
        if (value)
        {
            add(i);
        }
        else
        {
            remove(i);
        }
    }

    @Override
    public void set(int i, String newValue)
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
    public Vector subVector(int i0, int i1)
    {
        IntArrayList newElements = new IntArrayList();
        for (int i = 0; i < elements.length; i++)
        {
            if (elements[i] >= i0 && elements[i] <= i1)
            {
                newElements.add(elements[i] - i0);
            }
        }
        return new SparseBitVector(newElements.toIntArray());
    }

    @Override
    public BitVector intersection(BitVector v)
    {
        return (BitVector) elementwiseMultiply(v);
    }

    @Override
    public float sum()
    {
        return elements.length;
    }

    @Override
    public void write(Writer writer) throws IOException
    {
        // Unlike PackedBitVector, this outputs only the populated indices (e.g. "2 4...")
        writer.write(String.format("vector type=sparse-bit length=%d\n", length()));

        // Write Vector contents
        for (int i = 0; i < elements.length - 1; i++)
        {
            writer.write(String.format("%d ", elements[i]));
        }
        writer.write(String.format("%d\n", elements[elements.length - 1]));
        writer.flush();
    }

    @Override
    public int[] values()
    {
        // Return the values in-order
        return elements;
    }

    @Override
    public SparseBitVector clone()
    {
        return new SparseBitVector(elements);
    }
}

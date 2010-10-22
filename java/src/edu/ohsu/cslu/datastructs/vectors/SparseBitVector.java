package edu.ohsu.cslu.datastructs.vectors;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

/**
 * Immutable implementation of the {@link BitVector} interface which stores the indices of populated bits in an int
 * array.
 * 
 * This class is generally useful to store binary feature vectors in which a few of the bits will be populated - if a
 * large number of bits are likely to be populated, {@link PackedBitVector} will likely be more efficient.
 * 
 * @author Aaron Dunlop
 * @since Sep 11, 2008
 * 
 *        $Id$
 */
public class SparseBitVector extends BaseVector implements BitVector {

    // Allow direct access to other classes in the same package
    final int[] elements;

    public SparseBitVector() {
        super(0);
        elements = new int[0];
    }

    /**
     * Constructs a {@link SparseBitVector} from an integer array. Note that the semantics of this constructor are
     * different from those of most other {@link Vector} constructors with the same signature. The array should consist
     * of either indices or index, boolean tuples (as specified by the 'tuples' parameter)
     * 
     * @param array Index, boolean tuples
     * @param tuples Treat the supplied array as a set of index, value tuples
     */
    public SparseBitVector(final int[] array, final boolean tuples) {
        super(array.length == 0 ? 0 : (tuples ? array[array.length - 2] + 1 : array[array.length - 1] + 1));
        if (tuples) {
            this.elements = Vector.Factory.sparseIntElementArray(array);
        } else {
            this.elements = Arrays.copyOf(array, array.length);
            Arrays.sort(elements);
        }
    }

    /**
     * Constructs a {@link SparseBitVector} from a boolean array.
     * 
     * @param array array of populated bits
     */
    public SparseBitVector(final boolean[] array) {
        super(array.length);

        final IntSet newElements = new IntRBTreeSet();
        for (int i = 0; i < array.length; i++) {
            if (array[i]) {
                newElements.add(i);
            }
        }

        this.elements = newElements.toIntArray();
    }

    private SparseBitVector(final int length, final int[] elements) {
        super(length);
        this.elements = elements;
    }

    /**
     * Returns a clone of the backing store
     * 
     * TODO Expose the true backing store? It would be efficient, but unsafe.
     * 
     * @return a clone of the backing store
     */
    public int[] elements() {
        return elements.clone();
    }

    @Override
    public Vector elementwiseMultiply(final Vector v) {
        if (!(v instanceof BitVector)) {
            return super.elementwiseMultiply(v);
        }

        // If we're multiplying two SparseBitVector instances, iterate through the smaller one.
        if (v instanceof SparseBitVector && ((SparseBitVector) v).elements.length < elements.length) {
            return ((SparseBitVector) v).elementwiseMultiply(this);
        }

        final IntSet newContents = new IntRBTreeSet();
        for (final int i : elements) {
            if (v.getBoolean(i)) {
                newContents.add(i);
            }
        }

        return new SparseBitVector(Math.max(length, v.length()), newContents.toIntArray());
    }

    @Override
    public boolean add(final int toAdd) {
        throw new UnsupportedOperationException("SparseBitVector is immutable");
    }

    @Override
    public void addAll(final int[] toAdd) {
        throw new UnsupportedOperationException("SparseBitVector is immutable");
    }

    @Override
    public void addAll(final IntSet toAdd) {
        throw new UnsupportedOperationException("SparseBitVector is immutable");
    }

    @Override
    public boolean contains(final int i) {
        // TODO Since we maintain the elements in sorted order, we could use a binary search here
        // TODO: (nate) or you could hash the values
        for (final int element : elements) {
            if (element == i) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean remove(final int toRemove) {
        throw new UnsupportedOperationException("SparseBitVector is immutable");
    }

    @Override
    public void removeAll(final int[] toRemove) {
        throw new UnsupportedOperationException("SparseBitVector is immutable");
    }

    @Override
    public void removeAll(final IntSet toRemove) {
        throw new UnsupportedOperationException("SparseBitVector is immutable");
    }

    @Override
    public int argMax() {
        // Return the lowest populated index (or 0 if the set is empty)
        return elements.length > 0 ? elements[0] : 0;
    }

    @Override
    public int argMin() {
        // If no index is populated, return 0
        if (elements.length == 0) {
            return 0;
        }

        // TODO This could probably be a lot more efficient
        for (int i = 0; i < length; i++) {
            if (!contains(i)) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public float dotProduct(final Vector v) {
        try {
            float dotProduct = 0f;
            for (final int i : elements) {
                dotProduct += v.getFloat(i);
            }
            return dotProduct;
        } catch (final ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Vector length mismatch");
        }
    }

    @Override
    public boolean getBoolean(final int i) {
        return contains(i);
    }

    @Override
    public float getFloat(final int i) {
        return contains(i) ? 1f : 0f;
    }

    @Override
    public int getInt(final int i) {
        return contains(i) ? 1 : 0;
    }

    @Override
    public float infinity() {
        return 1;
    }

    @Override
    public int intMax() {
        // If any indices are populated, the maximum value is 1; otherwise, 0
        return elements.length > 0 ? 1 : 0;
    }

    @Override
    public int intMin() {
        // If all indices are populated, the minimum value is 1; otherwise, 0.
        return length == elements.length ? 1 : 0;
    }

    @Override
    public int length() {
        // Return the highest populated index + 1 (or 0 if no elements are populated)
        return elements.length > 0 ? elements[elements.length - 1] + 1 : 0;
    }

    @Override
    public float max() {
        return intMax();
    }

    @Override
    public float min() {
        return intMin();
    }

    @Override
    public float negativeInfinity() {
        return 0;
    }

    @Override
    public FloatVector scalarMultiply(final float multiplier) {
        final FloatVector v = new FloatVector(length);
        for (final int i : elements) {
            v.set(i, multiplier);
        }
        return v;
    }

    @Override
    public NumericVector scalarMultiply(final int multiplier) {
        final NumericVector v = createIntVector();
        for (final int i : elements) {
            v.set(i, multiplier);
        }
        return v;
    }

    @Override
    public void set(final int i, final int value) {
        set(i, value != 0);
    }

    @Override
    public void set(final int i, final float value) {
        set(i, value != 0);
    }

    @Override
    public void set(final int i, final boolean value) {
        if (value) {
            add(i);
        } else {
            remove(i);
        }
    }

    @Override
    public void set(final int i, final String newValue) {
        try {
            set(i, Integer.parseInt(newValue));
        } catch (final NumberFormatException e) {
            set(i, Boolean.parseBoolean(newValue));
        }
    }

    @Override
    public Vector subVector(final int i0, final int i1) {
        final IntArrayList newElements = new IntArrayList();
        for (int i = 0; i < elements.length; i++) {
            if (elements[i] >= i0 && elements[i] <= i1) {
                newElements.add(elements[i] - i0);
            }
        }
        return new SparseBitVector(i1 - i0 + 1, newElements.toIntArray());
    }

    @Override
    public BitVector intersection(final BitVector v) {
        return (BitVector) elementwiseMultiply(v);
    }

    @Override
    public float sum() {
        return elements.length;
    }

    @Override
    public void write(final Writer writer) throws IOException {
        // Write Vector contents in sparse-vector notation
        writer.write(String.format("vector type=sparse-bit length=%d sparse=true\n", length()));

        for (int i = 0; i < elements.length - 1; i++) {
            writer.write(String.format("%d 1 ", elements[i]));
        }
        writer.write(String.format("%d 1\n", elements[elements.length - 1]));
        writer.flush();
    }

    @Override
    public int[] values() {
        // Return the values in-order
        return elements;
    }

    @Override
    public SparseBitVector clone() {
        final int[] newElements = new int[elements.length];
        System.arraycopy(elements, 0, newElements, 0, elements.length);
        return new SparseBitVector(length, newElements);
    }
}

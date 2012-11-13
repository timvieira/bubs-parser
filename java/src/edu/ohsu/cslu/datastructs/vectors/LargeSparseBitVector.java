package edu.ohsu.cslu.datastructs.vectors;

import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongRBTreeSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

public class LargeSparseBitVector extends BaseVector implements LargeBitVector, SparseVector {

    private final long[] elements;

    /**
     * Constructs a {@link SparseBitVector} from a long array.
     * 
     * @param array populated indices
     */
    public LargeSparseBitVector(final long length, final long[] array) {
        super(length);
        elements = array.clone();
        Arrays.sort(elements);
    }

    /**
     * Constructs a {@link MutableLargeSparseBitVector} from a {@link LongSet}.
     * 
     * @param length Vector length
     * @param set Initial values
     */
    public LargeSparseBitVector(final long length, final LongSet set) {
        super(length);
        elements = set.toLongArray();
    }

    @Override
    public Vector elementwiseMultiply(final Vector v) {
        if (!(v instanceof LargeVector)) {
            throw new IllegalArgumentException("Argument must implement " + LargeVector.class.getName());
        }
        final LargeVector lv = (LargeVector) v;

        if (!(lv instanceof BitVector)) {
            return super.elementwiseMultiply(v);
        }

        // If we're multiplying two LargeSparseBitVector instances, iterate through the smaller one.
        if (v instanceof LargeSparseBitVector && ((LargeSparseBitVector) v).elements.length < elements.length) {
            return ((LargeSparseBitVector) v).elementwiseMultiply(this);
        }

        final LongSet newContents = new LongRBTreeSet();
        for (final long i : elements) {
            if (lv.getBoolean(i)) {
                newContents.add(i);
            }
        }

        return new LargeSparseBitVector(Math.max(length, v.length()), newContents.toLongArray());
    }

    @Override
    public boolean add(final int toAdd) {
        throw new UnsupportedOperationException("LargeSparseBitVector is immutable");
    }

    @Override
    public void addAll(final int[] toAdd) {
        throw new UnsupportedOperationException("LargeSparseBitVector is immutable");
    }

    @Override
    public void addAll(final IntSet toAdd) {
        throw new UnsupportedOperationException("LargeSparseBitVector is immutable");
    }

    @Override
    public boolean remove(final int toRemove) {
        throw new UnsupportedOperationException("LargeSparseBitVector is immutable");
    }

    @Override
    public void removeAll(final int[] toRemove) {
        throw new UnsupportedOperationException("LargeSparseBitVector is immutable");
    }

    @Override
    public void removeAll(final IntSet toRemove) {
        throw new UnsupportedOperationException("LargeSparseBitVector is immutable");
    }

    @Override
    public boolean contains(final int i) {
        // TODO Since we maintain the elements in sorted order, we could use a binary search here
        // TODO: (nate) or you could hash the values
        for (final long element : elements) {
            if (element == i) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean contains(final long i) {
        // TODO Since we maintain the elements in sorted order, we could use a binary search here
        // TODO: (nate) or you could hash the values
        for (final long element : elements) {
            if (element == i) {
                return true;
            }
        }
        return false;
    }

    public long longArgMax() {
        throw new UnsupportedOperationException();
    }

    @Override
    public float dotProduct(final Vector v) {
        if (!(v instanceof LargeVector)) {
            if (length > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Argument must implement " + LargeVector.class.getName());
            }

            float dotProduct = 0f;
            for (final long i : elements) {
                dotProduct += v.getFloat((int) i);
            }
            return dotProduct;
        }

        final LargeVector lv = (LargeVector) v;

        float dotProduct = 0f;
        for (final long i : elements) {
            dotProduct += lv.getFloat(i);
        }
        return dotProduct;
    }

    @Override
    public boolean getBoolean(final int i) {
        return contains(i);
    }

    @Override
    public boolean getBoolean(final long i) {
        return contains(i);
    }

    @Override
    public float getFloat(final int i) {
        return contains(i) ? 1f : 0f;
    }

    @Override
    public float getFloat(final long i) {
        return contains(i) ? 1f : 0f;
    }

    @Override
    public int getInt(final int i) {
        return contains(i) ? 1 : 0;
    }

    @Override
    public int getInt(final long i) {
        return contains(i) ? 1 : 0;
    }

    @Override
    public void set(final long i, final int value) {
        throw new UnsupportedOperationException("LargeSparseBitVector is immutable");
    }

    @Override
    public void set(final long i, final float value) {
        throw new UnsupportedOperationException("LargeSparseBitVector is immutable");
    }

    @Override
    public void set(final long i, final boolean value) {
        throw new UnsupportedOperationException("LargeSparseBitVector is immutable");
    }

    @Override
    public void set(final long i, final String newValue) {
        throw new UnsupportedOperationException("LargeSparseBitVector is immutable");
    }

    @Override
    public float infinity() {
        return 1;
    }

    @Override
    public float negativeInfinity() {
        return 0;
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
        final MutableLargeSparseBitVector newVector = new MutableLargeSparseBitVector();
        for (int i = i0; i <= i1; i++) {
            if (contains(i)) {
                newVector.add(i - i0);
            }
        }
        return newVector;
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
        writer.write(String.format("vector type=large-sparse-bit length=%d sparse=true\n", length()));

        // Write Vector contents in sparse-vector notation
        for (int i = 0; i < elements.length; i++) {
            if (i < (elements.length - 1)) {
                writer.write(String.format("%d ", elements[i]));
            } else {
                writer.write(String.format("%d\n", elements[i]));
            }
        }
        writer.flush();
    }

    @Override
    public int[] values() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<Integer> valueIterator() {
        throw new UnsupportedOperationException();
    }

    public long[] longValues() {
        return elements;
    }

    @Override
    public Iterable<Long> longValueIterator() {
        return new LongArrayList(elements);
    }

    @Override
    public LargeSparseBitVector clone() {
        return new LargeSparseBitVector(length, elements);
    }
}

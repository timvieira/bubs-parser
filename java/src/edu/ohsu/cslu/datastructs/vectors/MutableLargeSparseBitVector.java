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
package edu.ohsu.cslu.datastructs.vectors;

import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongRBTreeSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.io.IOException;
import java.io.Writer;

/**
 * Implementation of the {@link BitVector} interface which stores the indices of populated bits in an {@link IntSet}.
 * 
 * This class is generally useful to store mutable binary feature vectors in which a few of the bits will be populated -
 * if a large number of bits are likely to be populated, {@link PackedBitVector} will likely be more efficient.
 * 
 * If a sparsely-populated bit vector is not expected to change, consider the immutable class {@link SparseBitVector},
 * which is generally more efficient.
 * 
 * @author Aaron Dunlop
 * @since Sep 11, 2008
 * 
 *        $Id$
 */

public class MutableLargeSparseBitVector extends BaseVector implements LargeBitVector, SparseVector {

    private final LongRBTreeSet bitSet;

    public MutableLargeSparseBitVector() {
        super(0);
        bitSet = new LongRBTreeSet();
    }

    /**
     * Constructs a {@link SparseBitVector} from an integer array. Note that the semantics of this constructor are
     * different from those of most other {@link Vector} constructors with the same signature. The array should consists
     * of index, boolean tuples.
     * 
     * @param array Index, boolean tuples
     */
    public MutableLargeSparseBitVector(final int[] array) {
        super(0);
        bitSet = new LongRBTreeSet();

        for (int i = 0; i < array.length; i = i + 2) {
            if (array[i + 1] != 0) {
                add(array[i]);
            }
        }
    }

    /**
     * Constructs a {@link SparseBitVector} from a long array.
     * 
     * @param array populated indices
     */
    public MutableLargeSparseBitVector(final long length, final long[] array) {
        super(length);
        bitSet = new LongRBTreeSet();

        for (int i = 0; i < array.length; i++) {
            add(array[i]);
        }
    }

    /**
     * Constructs a {@link MutableLargeSparseBitVector} from a boolean array.
     * 
     * @param array array of populated bits
     */
    public MutableLargeSparseBitVector(final boolean[] array) {
        super(array.length);

        bitSet = new LongRBTreeSet();
        for (int i = 0; i < array.length; i++) {
            if (array[i]) {
                bitSet.add(i);
            }
        }
    }

    /**
     * Constructs a {@link MutableLargeSparseBitVector} from a {@link LongSet}.
     * 
     * @param length Vector length
     * @param set Initial values
     */
    MutableLargeSparseBitVector(final long length, final LongSet set) {
        super(length);
        bitSet = new LongRBTreeSet(set);
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

        // If we're multiplying by a SparseBitVector, use SparseBitVector's implementation
        if (lv instanceof SparseBitVector) {
            return ((SparseBitVector) v).elementwiseMultiply(this);
        }

        // If we're multiplying two SparseBitVector instances, iterate through the smaller one.
        if (lv instanceof MutableLargeSparseBitVector
                && ((MutableLargeSparseBitVector) v).bitSet.size() < bitSet.size()) {
            return ((MutableLargeSparseBitVector) v).elementwiseMultiply(this);
        }

        final MutableLargeSparseBitVector newVector = new MutableLargeSparseBitVector();
        for (final long i : bitSet) {
            if (lv.getBoolean(i)) {
                newVector.add(i);
            }
        }

        return newVector;
    }

    @Override
    public boolean add(final int toAdd) {
        if ((toAdd + 1) > length) {
            length = toAdd + 1;
        }
        return bitSet.add(toAdd);
    }

    public boolean add(final long toAdd) {
        if ((toAdd + 1) > length) {
            length = toAdd + 1;
        }
        return bitSet.add(toAdd);
    }

    @Override
    public void addAll(final int[] toAdd) {
        for (final int i : toAdd) {
            bitSet.add(i);
        }
    }

    @Override
    public void addAll(final IntSet toAdd) {
        for (final int i : toAdd) {
            bitSet.add(i);
        }
        length = length();
    }

    public void addAll(final LongSet toAdd) {
        bitSet.addAll(toAdd);
        length = length();
    }

    @Override
    public boolean contains(final int i) {
        return bitSet.contains(i);
    }

    @Override
    public boolean contains(final long i) {
        return bitSet.contains(i);
    }

    @Override
    public boolean remove(final int toRemove) {
        final boolean result = bitSet.remove(toRemove);
        length = length();
        return result;
    }

    @Override
    public void removeAll(final int[] toRemove) {
        for (final int i : toRemove) {
            bitSet.remove(i);
        }
        length = length();
    }

    @Override
    public void removeAll(final IntSet toRemove) {
        for (final int i : toRemove) {
            bitSet.remove(i);
        }
        length = length();
    }

    public long longArgMax() {
        // If no index is populated, return 0
        if (bitSet.size() == 0) {
            return 0;
        }

        // Return the lowest populated index
        long minSetIndex = Long.MAX_VALUE;
        for (final long i : bitSet) {
            if (i < minSetIndex) {
                minSetIndex = i;
            }
        }
        return minSetIndex;
    }

    @Override
    public float dotProduct(final Vector v) {
        if (!(v instanceof LargeVector)) {
            if (length > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Argument must implement " + LargeVector.class.getName());
            }

            float dotProduct = 0f;
            for (final long i : bitSet) {
                dotProduct += v.getFloat((int) i);
            }
            return dotProduct;
        }

        final LargeVector lv = (LargeVector) v;

        float dotProduct = 0f;
        for (final long i : bitSet) {
            dotProduct += lv.getFloat(i);
        }
        return dotProduct;
    }

    @Override
    public boolean getBoolean(final int i) {
        return bitSet.contains(i);
    }

    @Override
    public boolean getBoolean(final long i) {
        return bitSet.contains(i);
    }

    @Override
    public float getFloat(final int i) {
        return bitSet.contains(i) ? 1f : 0f;
    }

    @Override
    public float getFloat(final long i) {
        return bitSet.contains(i) ? 1f : 0f;
    }

    @Override
    public int getInt(final int i) {
        return bitSet.contains(i) ? 1 : 0;
    }

    @Override
    public int getInt(final long i) {
        return bitSet.contains(i) ? 1 : 0;
    }

    @Override
    public void set(final long i, final int value) {
        if (value != 0) {
            bitSet.add(i);
        } else {
            bitSet.remove(i);
        }
    }

    @Override
    public void set(final long i, final float value) {
        if (value != 0) {
            bitSet.add(i);
        } else {
            bitSet.remove(i);
        }
    }

    @Override
    public void set(final long i, final boolean value) {
        if (value) {
            bitSet.add(i);
        } else {
            bitSet.remove(i);
        }
    }

    @Override
    public void set(final long i, final String newValue) {
        try {
            set(i, Integer.parseInt(newValue));
        } catch (final NumberFormatException e) {
            set(i, Boolean.parseBoolean(newValue));
        }
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
        return bitSet.size();
    }

    @Override
    public void write(final Writer writer) throws IOException {
        writer.write(String.format("vector type=mutable-large-sparse-bit length=%d sparse=true\n", length()));

        // Write Vector contents in sparse-vector notation
        for (final LongIterator iter = bitSet.iterator(); iter.hasNext();) {
            final long element = iter.nextLong();
            writer.write(String.format("%d", element));
            if (iter.hasNext()) {
                writer.write(' ');
            }
        }
        writer.write('\n');
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
        return bitSet.toLongArray();
    }

    @Override
    public Iterable<Long> longValueIterator() {
        return bitSet;
    }

    @Override
    public MutableLargeSparseBitVector clone() {
        // TODO Sizing the initial IntSet in the copy would make this more efficient
        final MutableLargeSparseBitVector newVector = new MutableLargeSparseBitVector();
        for (final long i : bitSet) {
            newVector.add(i);
        }
        return newVector;
    }
}

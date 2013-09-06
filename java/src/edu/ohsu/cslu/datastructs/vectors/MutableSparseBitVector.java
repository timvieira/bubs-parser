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

import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;

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

public final class MutableSparseBitVector extends BaseVector implements BitVector, SparseVector {

    private static final long serialVersionUID = 1L;

    private final IntRBTreeSet bitSet;

    public MutableSparseBitVector() {
        super(0);
        bitSet = new IntRBTreeSet();
    }

    /**
     * Constructs a {@link SparseBitVector} from an integer array.
     * 
     * @param elements Populated indices
     */
    public MutableSparseBitVector(final int[] elements) {
        super(0);
        bitSet = new IntRBTreeSet();

        for (int i = 0; i < elements.length; i++) {
            add(elements[i]);
        }
    }

    public MutableSparseBitVector(final long length, final long[] elements) {
        super(length);
        bitSet = new IntRBTreeSet();

        for (int i = 0; i < elements.length; i++) {
            add((int) elements[i]);
        }
    }

    /**
     * Constructs a {@link MutableSparseBitVector} from a boolean array.
     * 
     * @param array array of populated bits
     */
    public MutableSparseBitVector(final boolean[] array) {
        super(array.length);

        bitSet = new IntRBTreeSet();
        for (int i = 0; i < array.length; i++) {
            if (array[i]) {
                bitSet.add(i);
            }
        }
    }

    @Override
    public Vector elementwiseMultiply(final Vector v) {
        if (!(v instanceof BitVector)) {
            return super.elementwiseMultiply(v);
        }

        // If we're multiplying by a SparseBitVector, use SparseBitVector's implementation
        if (v instanceof SparseBitVector) {
            return ((SparseBitVector) v).elementwiseMultiply(this);
        }

        // If we're multiplying two SparseBitVector instances, iterate through the smaller one.
        if (v instanceof MutableSparseBitVector && ((MutableSparseBitVector) v).bitSet.size() < bitSet.size()) {
            return ((MutableSparseBitVector) v).elementwiseMultiply(this);
        }

        final MutableSparseBitVector newVector = new MutableSparseBitVector();
        for (final int i : bitSet) {
            if (v.getBoolean(i)) {
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

    @Override
    public void addAll(final int[] toAdd) {
        for (final int i : toAdd) {
            bitSet.add(i);
        }
    }

    @Override
    public void addAll(final IntSet toAdd) {
        bitSet.addAll(toAdd);
        length = length();
    }

    @Override
    public boolean contains(final int i) {
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
        bitSet.removeAll(toRemove);
        length = length();
    }

    @Override
    public float dotProduct(final Vector v) {
        try {
            float dotProduct = 0f;
            for (final int i : bitSet) {
                dotProduct += v.getFloat(i);
            }
            return dotProduct;
        } catch (final ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Vector length mismatch");
        }
    }

    @Override
    public boolean getBoolean(final int i) {
        return bitSet.contains(i);
    }

    @Override
    public float getFloat(final int i) {
        return bitSet.contains(i) ? 1f : 0f;
    }

    @Override
    public int getInt(final int i) {
        return bitSet.contains(i) ? 1 : 0;
    }

    @Override
    public float infinity() {
        return 1;
    }

    @Override
    public long length() {
        // Return the highest populated index + 1, or 0 if no index is populated
        return bitSet.isEmpty() ? 0 : bitSet.lastInt() + 1;
    }

    @Override
    public int l0Norm() {
        return bitSet.size();
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
        final MutableSparseBitVector newVector = new MutableSparseBitVector();
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
    public void trim() {
    }

    @Override
    public void write(final Writer writer) throws IOException {
        writer.write(String.format("vector type=mutable-sparse-bit length=%d sparse=true\n", length()));

        // Write Vector contents in sparse-vector notation
        for (final IntIterator iter = bitSet.iterator(); iter.hasNext();) {
            final int element = iter.nextInt();
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
        return bitSet.toIntArray();
    }

    @Override
    public Iterable<Integer> valueIterator() {
        return bitSet;
    }

    @Override
    public MutableSparseBitVector clone() {
        // TODO Sizing the initial IntSet in the copy would make this more efficient
        final MutableSparseBitVector newVector = new MutableSparseBitVector();
        for (final int i : bitSet) {
            newVector.add(i);
        }
        return newVector;
    }
}

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
public final class SparseBitVector extends BaseVector implements BitVector, SparseVector {

    private static final long serialVersionUID = 1L;

    // Allow direct access to other classes in the same package
    final int[] elements;

    public SparseBitVector() {
        super(0);
        elements = new int[0];
    }

    /**
     * Constructs a {@link SparseBitVector} from an integer array.
     * 
     * @param array populated indices
     */
    public SparseBitVector(final int[] array) {
        super(array.length == 0 ? 0 : array[array.length - 1] + 1);
        this.elements = Arrays.copyOf(array, array.length);
        Arrays.sort(elements);
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

    public SparseBitVector(final long length, final int[] elements) {
        super(length);
        this.elements = elements;
    }

    public SparseBitVector(final long length, final long[] elements) {
        super(length);
        this.elements = new int[elements.length];
        for (int i = 0; i < elements.length; i++) {
            this.elements[i] = (int) elements[i];
        }
    }

    /**
     * Returns the array of populated elements
     * 
     * @return populated elements
     */
    public int[] elements() {
        return elements;
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
        return Arrays.binarySearch(elements, i) >= 0;
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

    public int l0Norm() {
        return elements.length;
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
    public void trim() {
    }

    @Override
    public void write(final Writer writer) throws IOException {
        // Write Vector contents in sparse-vector notation
        writer.write(String.format("vector type=sparse-bit length=%d sparse=true\n", length()));

        for (int i = 0; i < elements.length; i++) {
            writer.write(String.format("%d", elements[i]));
            if (i < (elements.length - 1)) {
                writer.write(' ');
            }
        }
        writer.write('\n');
        writer.flush();
    }

    @Override
    public int[] values() {
        // Return the values in-order
        return elements;
    }

    @Override
    public Iterable<Integer> valueIterator() {
        return new IntArrayList(elements);
    }

    @Override
    public SparseBitVector clone() {
        final int[] newElements = new int[elements.length];
        System.arraycopy(elements, 0, newElements, 0, elements.length);
        return new SparseBitVector(length, newElements);
    }
}

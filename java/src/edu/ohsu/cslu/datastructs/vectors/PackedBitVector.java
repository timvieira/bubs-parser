/*
 * Copyright 2010-2014, Oregon Health & Science University
 * 
 * This file is part of the BUBS Parser.
 * 
 * The BUBS Parser is free software: you can redistribute it and/or 
 * modify  it under the terms of the GNU Affero General Public License 
 * as published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * The BUBS Parser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Further documentation and contact information is available at
 *   https://code.google.com/p/bubs-parser/ 
 */
package edu.ohsu.cslu.datastructs.vectors;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.Arrays;

/**
 * Implementation of the {@link BitVector} interface which stores single bits (logically booleans) packed into an array
 * of 32-bit ints.
 * 
 * This class is generally useful to store binary feature vectors in which a considerable portion of the bits will be
 * populated - if only a small number of bits are likely to be populated, {@link SparseBitVector} will likely be more
 * efficient.
 * 
 * @author Aaron Dunlop
 * @since Sep 11, 2008
 * 
 *        $Id$
 */
public final class PackedBitVector extends BaseVector implements BitVector, Serializable {

    private final static long serialVersionUID = 379752896212698724L;

    private final int[] packedVector;

    public PackedBitVector(final long length) {
        super(length);
        packedVector = new int[(int) (length >> 5) + 1];
    }

    public PackedBitVector(final int[] array) {
        super(array.length);
        this.packedVector = new int[(array.length >> 5) + 1];
        for (int i = 0; i < length; i++) {
            set(i, array[i] != 0);
        }
    }

    public PackedBitVector(final long length, final long[] array) {
        super(length);
        this.packedVector = new int[((int) length >> 5) + 1];
        for (int i = 0; i < array.length; i++) {
            set((int) array[i], true);
        }
    }

    /**
     * Constructs a {@link PackedBitVector} from a boolean array.
     * 
     * @param array array of populated bits
     */
    public PackedBitVector(final boolean[] array) {
        super(array.length);

        packedVector = new int[(array.length >> 5) + 1];
        for (int i = 0; i < length; i++) {
            set(i, array[i]);
        }
    }

    @Override
    public final boolean getBoolean(final int i) {
        final int index = i >> 5;
        final int shift = i & 0x1f;

        try {
            return ((packedVector[index] >> shift) & 0x01) != 0;
        } catch (final ArrayIndexOutOfBoundsException e) {
            return false;
        }
    }

    @Override
    public final float getFloat(final int i) {
        return getBoolean(i) ? 1 : 0;
    }

    @Override
    public final int getInt(final int i) {
        return getBoolean(i) ? 1 : 0;
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
        final int index = i >> 5;
        final int shift = (i & 0x1f);

        // Set
        if (value) {
            packedVector[index] = packedVector[index] | (1 << shift);
        } else {
            // Unset
            packedVector[index] = packedVector[index] & (~(1 << shift));
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
    public final float infinity() {
        return 1;
    }

    @Override
    public final float negativeInfinity() {
        return 0;
    }

    public final void clear() {
        Arrays.fill(packedVector, 0);
    }

    @Override
    public Vector elementwiseMultiply(final Vector v) {
        if (!(v instanceof BitVector)) {
            return super.elementwiseMultiply(v);
        }

        /**
         * {@link SparseBitVector} and {@link MutableSparseBitVector} have an efficient implementations
         */
        if (v instanceof SparseBitVector) {
            return ((SparseBitVector) v).elementwiseMultiply(this);
        }

        if (v instanceof MutableSparseBitVector) {
            return ((MutableSparseBitVector) v).elementwiseMultiply(this);
        }

        if (v.length() != length) {
            throw new IllegalArgumentException("Vector length mismatch");
        }

        final int[] vArray = ((PackedBitVector) v).packedVector;
        final PackedBitVector newVector = new PackedBitVector(length);
        final int[] newArray = newVector.packedVector;
        for (int i = 0; i < packedVector.length; i++) {
            newArray[i] = packedVector[i] & vArray[i];
        }

        return newVector;
    }

    @Override
    public final float dotProduct(final Vector v) {
        if (v.length() != length) {
            throw new IllegalArgumentException("Vector length mismatch");
        }

        // SparseBitVector dotProduct() implementation is more efficient.
        if (v instanceof SparseBitVector) {
            return v.dotProduct(this);
        }

        float dotProduct = 0f;
        for (int i = 0; i < length; i++) {
            if (contains(i)) {
                dotProduct += v.getFloat(i);
            }
        }
        return dotProduct;
    }

    @Override
    public Vector subVector(final int i0, final int i1) {
        final int subVectorLength = i1 - i0 + 1;
        final PackedBitVector subVector = new PackedBitVector(subVectorLength);
        for (int i = 0; i < subVectorLength; i++) {
            subVector.set(i, getBoolean(i0 + i));
        }
        return subVector;
    }

    @Override
    public void write(final Writer writer) throws IOException {
        writer.write(String.format("vector type=packed-bit length=%d sparse=false\n", length));

        // Write Vector contents
        for (int i = 0; i < length; i++) {
            if (contains(i)) {
                writer.write(String.format("%d", i));
                if (i < (length - 1)) {
                    writer.write(' ');
                }
            }
        }
        writer.write('\n');
        writer.flush();
    }

    /**
     * Set-convention convenience method
     * 
     * @param toAdd element to add to the set
     * @return true if this set did not already contain the specified element
     */
    public final boolean add(final int toAdd) {
        final int index = toAdd >> 5;
        final int shift = (toAdd & 0x1f);

        // Set
        final boolean alreadyPresent = ((packedVector[index] >> shift) & 0x01) != 0;
        packedVector[index] = packedVector[index] | (1 << shift);
        return alreadyPresent;
    }

    /**
     * Set-convention convenience method
     * 
     * @param toAdd elements to add to the set
     */
    public final void addAll(final int[] toAdd) {
        for (final int i : toAdd) {
            set(i, true);
        }
    }

    /**
     * Set-convention convenience method
     * 
     * @param toAdd elements to add to the set
     */
    public final void addAll(final IntSet toAdd) {
        for (final int i : toAdd) {
            set(i, true);
        }
    }

    /**
     * Set-convention convenience method
     * 
     * @param i element whose presence in this set is to be tested
     * @return True if the specified element is contained in this set
     */
    public final boolean contains(final int i) {
        return getBoolean(i);
    }

    /**
     * Set-convention convenience method
     * 
     * @param toRemove element to remove from the set
     * @return True if the specified element was contained in this set
     */
    public final boolean remove(final int toRemove) {
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
    public final void removeAll(final int[] toRemove) {
        for (final int i : toRemove) {
            set(i, false);
        }
    }

    /**
     * Set-convention convenience method
     * 
     * @param toRemove elements to remove from the set
     */
    public final void removeAll(final IntSet toRemove) {
        for (final int i : toRemove) {
            set(i, false);
        }
    }

    @Override
    public BitVector intersection(final BitVector v) {
        return (BitVector) elementwiseMultiply(v);
    }

    @Override
    public int l0Norm() {
        throw new UnsupportedOperationException("Not currently supported by PackedBitVector");
    }

    @Override
    public int[] values() {
        // Not very efficient, but we don't expect to use this method often
        return valueIterator().toIntArray();
    }

    @Override
    public IntList valueIterator() {
        // Not very efficient, but we don't expect to use this method often
        final IntList intList = new IntArrayList();
        for (int i = 0; i < length; i++) {
            if (getBoolean(i)) {
                intList.add(i);
            }
        }
        return intList;
    }

    @Override
    public PackedBitVector clone() {
        final PackedBitVector v = new PackedBitVector(length);
        System.arraycopy(packedVector, 0, v.packedVector, 0, packedVector.length);
        return v;
    }

    // TODO: union?
    // TODO: Implement size() ? This might turn out to be useful, but could also be inefficient
}

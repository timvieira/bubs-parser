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

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.io.IOException;
import java.io.Writer;

import edu.ohsu.cslu.datastructs.Semiring;

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
public class LargeSparseIntVector extends BaseNumericVector implements IntVector, LargeVector, SparseVector {

    private static final long serialVersionUID = 1L;

    private Long2IntOpenHashMap map = new Long2IntOpenHashMap();

    /**
     * Initializes all elements to a default value.
     * 
     * @param length The size of the vector
     * @param defaultValue The value assigned to all elements
     */
    public LargeSparseIntVector(final long length, final int defaultValue) {
        super(length);
        map.defaultReturnValue(defaultValue);
    }

    /**
     * Initializes all elements to 0.
     * 
     * @param length The size of the vector
     */
    public LargeSparseIntVector(final long length) {
        this(length, 0);
    }

    /**
     * Initializes a {@link LargeSparseIntVector} from an int array. Vector indices and values are assumed to be in
     * sorted order in adjacent array slots. (e.g, {2, 4, 5, 8} represents a Vector of length 6 in which 2 elements are
     * present). The length of the vector is the final populated index.
     * 
     * @param vector The populated vector elements in index, value tuples.
     */
    public LargeSparseIntVector(final int[] vector) {
        this(vector.length);
        for (int i = 0; i < vector.length; i++) {
            set(i, vector[i]);
        }
    }

    /**
     * Copies an {@link Int2IntOpenHashMap} into a new {@link LargeSparseIntVector}.
     * 
     * @param length
     * @param map
     */
    LargeSparseIntVector(final long length, final Long2IntMap map) {
        this(length, map.defaultReturnValue());
        this.map.putAll(map);
    }

    @Override
    public boolean getBoolean(final long i) {
        return map.containsKey(i);
    }

    @Override
    public float getFloat(final int i) {
        return map.get(i);
    }

    @Override
    public float getFloat(final long i) {
        return map.get(i);
    }

    @Override
    public int getInt(final int i) {
        return map.get(i);
    }

    @Override
    public int getInt(final long i) {
        return map.get(i);
    }

    @Override
    public float infinity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public float negativeInfinity() {
        return Integer.MIN_VALUE;
    }

    @Override
    public void set(final int i, final int value) {
        map.put(i, value);
    }

    @Override
    public void set(final int i, final float value) {
        map.put(i, Math.round(value));
    }

    @Override
    public void set(final int i, final boolean value) {
        map.put(i, value ? 1 : 0);
    }

    public void set(final long i, final int value) {
        map.put(i, value);
    }

    public void set(final long i, final float value) {
        map.put(i, Math.round(value));
    }

    public void set(final long i, final boolean value) {
        map.put(i, value ? 1 : 0);
    }

    @Override
    public void set(final int i, final String newValue) {
        map.put(i, Integer.parseInt(newValue));
    }

    @Override
    public void set(final long i, final String newValue) {
        map.put(i, Integer.parseInt(newValue));

    }

    @Override
    public LongSet populatedDimensions() {
        return map.keySet();
    }

    public LargeSparseIntVector inPlaceAdd(final Vector v) {
        // Special-case for SparseBitVector
        if (v instanceof SparseBitVector) {
            if (v.length() > length) {
                throw new IllegalArgumentException("Vector length mismatch");
            }

            for (final int i : ((SparseBitVector) v).values()) {
                set(i, getFloat(i) + 1);
            }
            return this;
        }

        // Special-case for MutableSparseBitVector
        if (v instanceof MutableSparseBitVector) {
            if (v.length() > length) {
                throw new IllegalArgumentException("Vector length mismatch");
            }

            for (final int i : ((MutableSparseBitVector) v).valueIterator()) {
                set(i, getFloat(i) + 1);
            }
            return this;
        }

        if (v.length() != length) {
            throw new IllegalArgumentException("Vector length mismatch");
        }

        for (int i = 0; i < length; i++) {
            set(i, getFloat(i) + v.getFloat(i));
        }
        return this;
    }

    public LargeSparseIntVector inPlaceAdd(final BitVector v, final float addend) {
        // Special-case for SparseBitVector
        if (v instanceof SparseBitVector) {
            if (v.length() > length) {
                throw new IllegalArgumentException("Vector length mismatch");
            }

            for (final int i : ((SparseBitVector) v).values()) {
                set(i, getFloat(i) + addend);
            }
            return this;
        }

        // Special-case for LargeSparseBitVector
        if (v instanceof MutableSparseBitVector) {
            if (v.length() > length) {
                throw new IllegalArgumentException("Vector length mismatch");
            }

            for (final int i : ((MutableSparseBitVector) v).valueIterator()) {
                set(i, getFloat(i) + addend);
            }
            return this;
        }

        if (v.length() != length) {
            throw new IllegalArgumentException("Vector length mismatch");
        }

        for (int i = 0; i < length; i++) {
            if (v.getBoolean(i)) {
                set(i, getFloat(i) + addend);
            }
        }
        return this;
    }

    @Override
    public Vector subVector(final int i0, final int i1) {
        final Long2IntOpenHashMap newMap = new Long2IntOpenHashMap();

        for (int i = i0; i <= i1; i++) {
            if (map.containsKey(i)) {
                newMap.put(i - i0, map.get(i));
            }
        }

        return new LargeSparseIntVector(i1 - i0 + 1, newMap);
    }

    public LargeSparseIntVector union(final LargeSparseIntVector other, final Semiring semiring) {
        if (semiring != Semiring.TROPICAL) {
            throw new UnsupportedOperationException("Only the tropical semiring is supported");
        }

        if (other.map.defaultReturnValue() != map.defaultReturnValue()) {
            throw new IllegalArgumentException("Default value of vectors must match");
        }

        final LargeSparseIntVector newVector = clone();
        final Long2IntOpenHashMap newMap = newVector.map;
        final Long2IntOpenHashMap otherMap = other.map;

        newVector.length = Math.max(length, other.length);
        for (final long key : otherMap.keySet()) {
            newMap.put(key, Math.max(map.get(key), otherMap.get(key)));
        }
        return newVector;
    }

    @Override
    protected LargeSparseIntVector createIntVector(final long newVectorLength) {
        return new LargeSparseIntVector(newVectorLength);
    }

    @Override
    protected LargeSparseFloatVector createFloatVector(final long newVectorLength) {
        return new LargeSparseFloatVector(newVectorLength);
    }

    @Override
    public void write(final Writer writer) throws IOException {
        writer.write(String.format("vector type=large-sparse-int length=%d sparse=true\n", length));

        // Write Vector contents
        for (final LongIterator i = new LongAVLTreeSet(map.keySet()).iterator(); i.hasNext();) {
            final long key = i.nextLong();
            writer.write(String.format("%d %d", key, getInt(key)));
            if (i.hasNext()) {
                writer.write(' ');
            }
        }
        writer.write('\n');
        writer.flush();
    }

    @Override
    public LargeSparseIntVector clone() {
        return new LargeSparseIntVector(length, map);
    }
}

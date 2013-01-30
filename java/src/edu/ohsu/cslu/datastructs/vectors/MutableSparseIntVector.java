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
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongRBTreeSet;
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
public final class MutableSparseIntVector extends BaseNumericVector implements IntVector, SparseVector {

    private static final long serialVersionUID = 1L;

    private Int2IntOpenHashMap map = new Int2IntOpenHashMap();

    /**
     * Initializes all elements to a default value.
     * 
     * @param length The size of the vector
     * @param defaultValue The value assigned to all elements
     */
    public MutableSparseIntVector(final long length, final int defaultValue) {
        super(length);
        map.defaultReturnValue(defaultValue);
    }

    /**
     * Initializes all elements to 0.
     * 
     * @param length The size of the vector
     */
    public MutableSparseIntVector(final long length) {
        this(length, 0);
    }

    /**
     * Initializes a {@link MutableSparseIntVector} from an int array.
     * 
     * @param vector The populated vector elements
     */
    public MutableSparseIntVector(final int[] vector) {
        this(vector.length);
        for (int i = 0; i < vector.length; i++) {
            set(i, vector[i]);
        }
    }

    /**
     * Copies an {@link Long2IntMap} into a new {@link MutableSparseIntVector}.
     * 
     * @param length
     * @param map
     */
    MutableSparseIntVector(final long length, final Long2IntMap map) {
        this(length, map.defaultReturnValue());
        for (final long key : map.keySet()) {
            this.map.put((int) key, map.get(key));
        }
    }

    @Override
    public float getFloat(final int i) {
        return map.get(i);
    }

    @Override
    public int getInt(final int i) {
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

    @Override
    public void set(final int i, final String newValue) {
        map.put(i, Integer.parseInt(newValue));
    }

    @Override
    public LongSet populatedDimensions() {
        final LongSet d = new LongRBTreeSet();
        for (final int i : map.keySet()) {
            d.add(i);
        }
        return d;
    }

    public MutableSparseIntVector inPlaceAdd(final Vector v) {
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

    public MutableSparseIntVector inPlaceAdd(final BitVector v, final float addend) {
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

        // Special-case for MutableSparseBitVector
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

        return new MutableSparseIntVector(i1 - i0 + 1, newMap);
    }

    public MutableSparseIntVector union(final MutableSparseIntVector other, final Semiring semiring) {
        if (semiring != Semiring.TROPICAL) {
            throw new UnsupportedOperationException("Only the tropical semiring is supported");
        }

        if (other.map.defaultReturnValue() != map.defaultReturnValue()) {
            throw new IllegalArgumentException("Default value of vectors must match");
        }

        final MutableSparseIntVector newVector = clone();
        final Int2IntOpenHashMap newMap = newVector.map;
        final Int2IntOpenHashMap otherMap = other.map;

        newVector.length = Math.max(length, other.length);
        for (final int key : otherMap.keySet()) {
            newMap.put(key, Math.max(map.get(key), otherMap.get(key)));
        }
        return newVector;
    }

    @Override
    public void trim() {
        map.trim();
    }

    @Override
    protected MutableSparseIntVector createIntVector(final long newVectorLength) {
        return new MutableSparseIntVector(newVectorLength);
    }

    @Override
    protected MutableSparseFloatVector createFloatVector(final long newVectorLength) {
        return new MutableSparseFloatVector(newVectorLength);
    }

    @Override
    public void write(final Writer writer) throws IOException {
        writer.write(String.format("vector type=mutable-sparse-int length=%d sparse=true\n", length));

        // Write Vector contents
        for (final IntIterator i = new IntAVLTreeSet(map.keySet()).iterator(); i.hasNext();) {
            final int key = i.nextInt();
            writer.write(String.format("%d %d", key, getInt(key)));
            if (i.hasNext()) {
                writer.write(' ');
            }
        }
        writer.write('\n');
        writer.flush();
    }

    @Override
    public MutableSparseIntVector clone() {
        final MutableSparseIntVector clone = new MutableSparseIntVector(length);
        for (final int key : map.keySet()) {
            clone.map.put(key, map.get(key));
        }
        return clone;
    }
}

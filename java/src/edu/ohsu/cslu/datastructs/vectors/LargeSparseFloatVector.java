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

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2FloatMap;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongRBTreeSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.io.IOException;
import java.io.Writer;

import edu.ohsu.cslu.datastructs.Semiring;

/**
 * Sparse float vector which stores entries in a long-> float hash, allowing very large keys. Applications which do not
 * need keys larger than 2^31 will generally be better served by {@link MutableSparseFloatVector}.
 * 
 * @author Aaron Dunlop
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class LargeSparseFloatVector extends BaseNumericVector implements FloatVector, LargeVector, SparseVector {

    private static final long serialVersionUID = 1L;

    private Long2FloatOpenHashMap map = new Long2FloatOpenHashMap();

    /**
     * Initializes all elements to a default value.
     * 
     * @param length The size of the vector
     * @param defaultValue The value assigned to all elements
     */
    public LargeSparseFloatVector(final long length, final float defaultValue) {
        super(length);
        map.defaultReturnValue(defaultValue);
    }

    /**
     * Initializes all elements to 0.
     * 
     * @param length The size of the vector
     */
    public LargeSparseFloatVector(final long length) {
        this(length, 0);
    }

    /**
     * Initializes a {@link LargeSparseFloatVector} from a floating-point array.
     * 
     * @param vector The populated vector elements
     */
    public LargeSparseFloatVector(final float[] vector) {
        this(vector.length);
        for (int i = 0; i < vector.length; i++) {
            set(i, vector[i]);
        }
    }

    /**
     * Copies an {@link Int2FloatOpenHashMap} into a new {@link LargeSparseFloatVector}.
     * 
     * @param length
     * @param map
     */
    LargeSparseFloatVector(final long length, final Long2FloatMap map) {
        this(length, map.defaultReturnValue());
        this.map.putAll(map);
    }

    @Override
    public boolean getBoolean(final long i) {
        return map.get(i) != 0;
    }

    @Override
    public float getFloat(final int i) {
        return map.get(i);
    }

    public float getFloat(final long i) {
        return map.get(i);
    }

    @Override
    public int getInt(final int i) {
        return Math.round(map.get(i));
    }

    public int getInt(final long i) {
        return Math.round(map.get(i));
    }

    @Override
    public float infinity() {
        return Float.POSITIVE_INFINITY;
    }

    @Override
    public float negativeInfinity() {
        return Float.NEGATIVE_INFINITY;
    }

    @Override
    public void set(final int i, final int value) {
        map.put(i, value);
    }

    public void set(final long i, final int value) {
        map.put(i, value);
    }

    @Override
    public void set(final int i, final float value) {
        map.put(i, value);
    }

    public void set(final long i, final float value) {
        map.put(i, value);
    }

    @Override
    public void set(final int i, final boolean value) {
        map.put(i, value ? 1 : 0);
    }

    public void set(final long i, final boolean value) {
        map.put(i, value ? 1 : 0);
    }

    @Override
    public void set(final int i, final String newValue) {
        map.put(i, Float.parseFloat(newValue));
    }

    public void set(final long i, final String newValue) {
        map.put(i, Float.parseFloat(newValue));
    }

    @Override
    public float max() {
        float max = Float.NEGATIVE_INFINITY;
        for (final float x : map.values()) {
            if (x > max) {
                max = x;
            }
        }
        return max;
    }

    @Override
    public float min() {
        float min = Float.POSITIVE_INFINITY;
        for (final float x : map.values()) {
            if (x < min) {
                min = x;
            }
        }
        return min;
    }

    @Override
    public LongSet populatedDimensions() {
        final LongSet d = new LongRBTreeSet();
        for (final long l : map.keySet()) {
            if (map.get(l) != 0) {
                d.add(l);
            }
        }
        return d;
    }

    @Override
    public Vector subVector(final int i0, final int i1) {
        final Long2FloatOpenHashMap newMap = new Long2FloatOpenHashMap();

        for (int i = i0; i <= i1; i++) {
            if (map.containsKey(i)) {
                newMap.put(i - i0, map.get(i));
            }
        }

        return new LargeSparseFloatVector(i1 - i0 + 1, newMap);
    }

    public LargeSparseFloatVector union(final LargeSparseFloatVector other, final Semiring semiring) {
        if (semiring != Semiring.TROPICAL) {
            throw new UnsupportedOperationException("Only the tropical semiring is supported");
        }

        if (other.map.defaultReturnValue() != map.defaultReturnValue()) {
            throw new IllegalArgumentException("Default value of vectors must match");
        }

        final LargeSparseFloatVector newVector = clone();
        final Long2FloatOpenHashMap newMap = newVector.map;
        final Long2FloatOpenHashMap otherMap = other.map;

        newVector.length = Math.max(length, other.length);
        for (final long key : otherMap.keySet()) {
            newMap.put(key, Math.max(map.get(key), otherMap.get(key)));
        }
        return newVector;
    }

    @Override
    protected LargeSparseFloatVector createIntVector(final long newVectorLength) {
        return new LargeSparseFloatVector(newVectorLength);
    }

    @Override
    protected LargeSparseFloatVector createFloatVector(final long newVectorLength) {
        return new LargeSparseFloatVector(newVectorLength);
    }

    @Override
    public void write(final Writer writer) throws IOException {
        writer.write(String.format("vector type=large-sparse-float length=%d sparse=true\n", length));

        // Write Vector contents
        for (final LongIterator i = new LongAVLTreeSet(map.keySet()).iterator(); i.hasNext();) {
            final long key = i.nextLong();
            writer.write(String.format("%d %.6f", key, getFloat(key)));
            if (i.hasNext()) {
                writer.write(' ');
            }
        }
        writer.write('\n');
        writer.flush();
    }

    @Override
    public LargeSparseFloatVector clone() {
        return new LargeSparseFloatVector(length, map);
    }

    /** Type-strengthen return-type */
    @Override
    public LargeSparseFloatVector elementwiseDivide(final NumericVector v) {

        if (v.length() != length) {
            throw new IllegalArgumentException("Vector length mismatch");
        }

        if (!(v instanceof LargeVector)) {
            throw new IllegalArgumentException("Vector must be a LargeVector");
        }

        final LargeVector lv = (LargeVector) v;
        final LargeSparseFloatVector newVector = new LargeSparseFloatVector(length);
        for (final long i : map.keySet()) {
            newVector.set(i, getFloat(i) / lv.getFloat(i));
        }

        return newVector;
    }

    /** Type-strengthen return-type */
    @Override
    public LargeSparseFloatVector elementwiseLog() {
        final LargeSparseFloatVector newVector = new LargeSparseFloatVector(length);
        for (final long i : map.keySet()) {
            newVector.set(i, (float) Math.log(getFloat(i)));
        }

        return newVector;
    }

    @Override
    public LargeSparseFloatVector inPlaceAdd(final Vector v) {
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

    @Override
    public LargeSparseFloatVector inPlaceAdd(final BitVector v, final float addend) {
        // Special-case for LargeSparseBitVector
        if (v instanceof LargeSparseBitVector) {
            if (v.length() > length) {
                throw new IllegalArgumentException("Vector length mismatch");
            }

            for (final long i : ((LargeSparseBitVector) v).longValues()) {
                set(i, getFloat(i) + addend);
            }
            return this;
        }

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
    public LargeSparseFloatVector inPlaceElementwiseMultiply(final Vector v) {
        if (v.length() != length && !(v instanceof SparseBitVector && v.length() <= length)) {
            throw new IllegalArgumentException("Vector length mismatch");
        }

        // TODO: This could be more efficient for SparseBitVectors

        for (int i = 0; i < length; i++) {
            set(i, getFloat(i) * v.getFloat(i));
        }
        return this;
    }

    @Override
    public LargeSparseFloatVector inPlaceElementwiseDivide(final Vector v) {
        if (v.length() != length) {
            throw new IllegalArgumentException("Vector length mismatch");
        }

        for (int i = 0; i < length; i++) {
            set(i, getFloat(i) / v.getFloat(i));
        }
        return this;
    }

    @Override
    public LargeSparseFloatVector inPlaceScalarAdd(final float addend) {
        for (int i = 0; i < length; i++) {
            set(i, getFloat(i) + addend);
        }
        return this;
    }

    @Override
    public LargeSparseFloatVector inPlaceScalarMultiply(final float multiplicand) {
        for (int i = 0; i < length; i++) {
            set(i, getFloat(i) * multiplicand);
        }
        return this;
    }

    @Override
    public LargeSparseFloatVector inPlaceElementwiseLog() {
        for (int i = 0; i < length; i++) {
            set(i, (float) Math.log(getFloat(i)));
        }
        return this;
    }

    @Override
    public void trim() {
        map.trim();
    }
}

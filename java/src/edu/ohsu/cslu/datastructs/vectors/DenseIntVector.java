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

import it.unimi.dsi.fastutil.longs.LongRBTreeSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

/**
 * A {@link Vector} implementation which stores 32-bit ints.
 * 
 * @author Aaron Dunlop
 * @since Dec 11, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class DenseIntVector extends BaseNumericVector implements IntVector {

    private final static long serialVersionUID = 379752896212698724L;

    private final int[] vector;

    public DenseIntVector(final long length) {
        super(length);
        this.vector = new int[(int) length];
    }

    public DenseIntVector(final long length, final int defaultValue) {
        super(length);
        this.vector = new int[(int) length];
        Arrays.fill(this.vector, defaultValue);
    }

    public DenseIntVector(final int[] vector) {
        super(vector.length);
        this.vector = vector;
    }

    public DenseIntVector(final float[] vector) {
        super(vector.length);
        this.vector = new int[vector.length];
        for (int i = 0; i < vector.length; i++) {
            set(i, vector[i]);
        }
    }

    public final void fill(final int value) {
        Arrays.fill(vector, value);
    }

    @Override
    public final float getFloat(final int i) {
        return vector[i];
    }

    @Override
    public final int getInt(final int i) {
        return vector[i];
    }

    @Override
    public void set(final int i, final int value) {
        vector[i] = value;
    }

    @Override
    public void set(final int i, final float value) {
        set(i, Math.round(value));
    }

    @Override
    public void set(final int i, final boolean value) {
        set(i, value ? 1 : 0);
    }

    @Override
    public void set(final int i, final String newValue) {
        set(i, Float.parseFloat(newValue));
    }

    @Override
    public LongSet populatedDimensions() {
        final LongSet d = new LongRBTreeSet();
        for (long i = 0; i < length; i++) {
            d.add(i);
        }
        return d;
    }

    @Override
    public final float infinity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public final float negativeInfinity() {
        return Integer.MIN_VALUE;
    }

    /**
     * Adds an addend to the elements of this vector indicated by the populated indices of a bit vector, returning a
     * reference to this vector
     * 
     * Caution: This method changes the contents of this vector
     * 
     * @param v a {@link BitVector} of the same length.
     * @param addend the float to be added to the elements indicated by populated elements in v
     * @return a reference to this vector.
     */
    public DenseIntVector inPlaceAdd(final BitVector v, final int addend) {
        // Special-case for SparseBitVector
        if (v instanceof SparseBitVector) {
            if (v.length() > length) {
                throw new IllegalArgumentException("Vector length mismatch");
            }

            for (final int i : ((SparseBitVector) v).values()) {
                vector[i] += addend;
            }
            return this;
        }

        // Special-case for MutableSparseBitVector
        if (v instanceof MutableSparseBitVector) {
            if (v.length() > length) {
                throw new IllegalArgumentException("Vector length mismatch");
            }

            for (final int i : ((MutableSparseBitVector) v).valueIterator()) {
                vector[i] += addend;
            }
            return this;
        }

        if (v.length() != length) {
            throw new IllegalArgumentException("Vector length mismatch");
        }

        for (int i = 0; i < length; i++) {
            if (v.getBoolean(i)) {
                vector[i] += addend;
            }
        }
        return this;
    }

    @Override
    public Vector subVector(final int i0, final int i1) {
        final int[] newVector = new int[i1 - i0 + 1];
        System.arraycopy(vector, i0, newVector, 0, newVector.length);
        return new DenseIntVector(newVector);
    }

    @Override
    public DenseIntVector clone() {
        final int[] newVector = new int[(int) length];
        System.arraycopy(vector, 0, newVector, 0, (int) length);
        return new DenseIntVector(newVector);
    }

    @Override
    public void write(final Writer writer) throws IOException {
        write(writer, String.format("vector type=int length=%d sparse=false\n", length));
    }
}

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
import java.util.Random;

/**
 * A {@link Vector} implementation which stores 32-bit floats. For most NLP tasks requiring floating-point numbers, a
 * 32-bit float is sufficient, and requires half the memory of a 64-bit double.
 * 
 * @author Aaron Dunlop
 * @since Dec 11, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
public final class DenseFloatVector extends BaseNumericVector implements FloatVector {

    private final static long serialVersionUID = 379752896212698724L;

    private final float[] vector;

    public DenseFloatVector(final long length) {
        super(length);
        this.vector = new float[(int) length];
    }

    /**
     * Initializes all elements to a default value.
     * 
     * @param length The size of the vector
     * @param defaultValue The value assigned to all elements
     */
    public DenseFloatVector(final long length, final float defaultValue) {
        super(length);
        this.vector = new float[(int) length];
        Arrays.fill(vector, defaultValue);
    }

    /**
     * Initializes all elements to random values between minValue and maxValue
     * 
     * @param length The size of the vector
     * @param minValue The lowest value in the initialization range (inclusive)
     * @param maxValue The highest value in the initialization range (exclusive)
     */
    public DenseFloatVector(final long length, final float minValue, final float maxValue) {
        super(length);
        this.vector = new float[(int) length];
        final Random r = new Random();
        for (int i = 0; i < length; i++) {
            vector[i] = minValue + r.nextFloat() * (maxValue - minValue);
        }
    }

    public DenseFloatVector(final float[] vector) {
        super(vector.length);
        this.vector = vector;
    }

    /** Type-strengthen return-type */
    @Override
    public DenseFloatVector add(final Vector v) {
        return (DenseFloatVector) super.add(v);
    }

    /** Type-strengthen return-type */
    @Override
    public DenseFloatVector elementwiseDivide(final NumericVector v) {

        if (v.length() != length) {
            throw new IllegalArgumentException("Vector length mismatch");
        }

        final DenseFloatVector newVector = new DenseFloatVector(length);
        for (int i = 0; i < length; i++) {
            newVector.set(i, getFloat(i) / v.getFloat(i));
        }

        return newVector;
    }

    /** Type-strengthen return-type */
    @Override
    public DenseFloatVector elementwiseLog() {
        final DenseFloatVector newVector = new DenseFloatVector(length);
        for (int i = 0; i < length; i++) {
            newVector.set(i, (float) Math.log(getFloat(i)));
        }

        return newVector;
    }

    @Override
    public DenseFloatVector inPlaceAdd(final Vector v) {
        // Special-case for SparseBitVector
        if (v instanceof SparseBitVector) {
            if (v.length() > length) {
                throw new IllegalArgumentException("Vector length mismatch");
            }

            for (final int i : ((SparseBitVector) v).values()) {
                vector[i] += 1;
            }
            return this;
        }

        // Special-case for MutableSparseBitVector
        if (v instanceof MutableSparseBitVector) {
            if (v.length() > length) {
                throw new IllegalArgumentException("Vector length mismatch");
            }

            for (final int i : ((MutableSparseBitVector) v).valueIterator()) {
                vector[i] += 1;
            }
            return this;
        }

        if (v.length() != length) {
            throw new IllegalArgumentException("Vector length mismatch");
        }

        for (int i = 0; i < length; i++) {
            vector[i] += v.getFloat(i);
        }
        return this;
    }

    @Override
    public DenseFloatVector inPlaceAdd(final BitVector v, final float addend) {
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
    public DenseFloatVector inPlaceElementwiseMultiply(final Vector v) {
        if (v.length() != length && !(v instanceof SparseBitVector && v.length() <= length)) {
            throw new IllegalArgumentException("Vector length mismatch");
        }

        // TODO: This could be more efficient for SparseBitVectors

        for (int i = 0; i < length; i++) {
            vector[i] *= v.getFloat(i);
        }
        return this;
    }

    @Override
    public DenseFloatVector inPlaceElementwiseDivide(final Vector v) {
        if (v.length() != length) {
            throw new IllegalArgumentException("Vector length mismatch");
        }

        for (int i = 0; i < length; i++) {
            vector[i] /= v.getFloat(i);
        }
        return this;
    }

    @Override
    public DenseFloatVector inPlaceScalarAdd(final float addend) {
        for (int i = 0; i < length; i++) {
            vector[i] += addend;
        }
        return this;
    }

    @Override
    public DenseFloatVector inPlaceScalarMultiply(final float multiplicand) {
        for (int i = 0; i < length; i++) {
            vector[i] *= multiplicand;
        }
        return this;
    }

    @Override
    public DenseFloatVector inPlaceElementwiseLog() {
        for (int i = 0; i < length; i++) {
            vector[i] = (float) Math.log(vector[i]);
        }
        return this;
    }

    @Override
    public final float getFloat(final int i) {
        return vector[i];
    }

    @Override
    public final int getInt(final int i) {
        return Math.round(vector[i]);
    }

    @Override
    public void set(final int i, final int value) {
        vector[i] = value;
    }

    @Override
    public void set(final int i, final float value) {
        vector[i] = value;
    }

    @Override
    public void set(final int i, final boolean value) {
        set(i, value ? 1f : 0f);
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
        return Float.POSITIVE_INFINITY;
    }

    @Override
    public final float negativeInfinity() {
        return Float.NEGATIVE_INFINITY;
    }

    @Override
    public DenseFloatVector createIntVector(final long newVectorLength) {
        return new DenseFloatVector(newVectorLength);
    }

    @Override
    public DenseFloatVector createFloatVector(final long newVectorLength) {
        return new DenseFloatVector(newVectorLength);
    }

    @Override
    public Vector subVector(final int i0, final int i1) {
        final float[] newVector = new float[i1 - i0 + 1];
        System.arraycopy(vector, i0, newVector, 0, newVector.length);
        return new DenseFloatVector(newVector);
    }

    @Override
    public DenseFloatVector clone() {
        final float[] newVector = new float[(int) length];
        System.arraycopy(vector, 0, newVector, 0, (int) length);
        return new DenseFloatVector(newVector);
    }

    @Override
    public void write(final Writer writer) throws IOException {
        writer.write(String.format("vector type=float length=%d sparse=false\n", length));

        // Write Vector contents
        for (int i = 0; i < length - 1; i++) {
            writer.write(String.format("%f ", getFloat(i)));
        }
        writer.write(String.format("%f\n", getFloat((int) (length - 1))));
        writer.flush();
    }
}

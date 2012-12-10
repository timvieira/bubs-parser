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

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Implements functionality common to all {@link Vector} implementations.
 * 
 * @author Aaron Dunlop
 * @since Apr 1, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public abstract class BaseVector implements Vector, Serializable {

    private static final long serialVersionUID = 1L;
    private final static float EPSILON = 0.0001f;

    protected long length;

    BaseVector(final long length) {
        this.length = length;
    }

    @Override
    public boolean getBoolean(final int i) {
        return getInt(i) != 0;
    }

    public boolean getBoolean(final long i) {
        return getInt((int) i) != 0;
    }

    @Override
    public long length() {
        return length;
    }

    public NumericVector add(final Vector v) {
        if (v.length() != length) {
            throw new IllegalArgumentException("Vector length mismatch");
        }

        NumericVector newVector;
        if ((this instanceof IntVector || this instanceof BitVector)
                && (v instanceof IntVector || v instanceof BitVector)) {
            newVector = createIntVector();
        } else {
            newVector = createFloatVector();
        }

        // TODO Handle SparseVector classes here

        for (int i = 0; i < length; i++) {
            newVector.set(i, getFloat(i) + v.getFloat(i));
        }

        return newVector;
    }

    public Vector elementwiseMultiply(final Vector v) {
        // TODO: The return-types of various Vector class's elementwiseMultiply methods are somewhat
        // inconsistent. Think through the appropriate return-types for add(Vector),
        // elementwiseMultiply, and elementwiseDivide.

        if (v.length() != length && !(v instanceof SparseBitVector && v.length() < length)) {
            throw new IllegalArgumentException("Vector length mismatch");
        }

        final NumericVector newVector = (v instanceof FloatVector) ? createFloatVector() : createIntVector();

        for (int i = 0; i < length; i++) {
            newVector.set(i, getFloat(i) * v.getFloat(i));
        }

        return newVector;
    }

    /**
     * Creates a new {@link Vector} of the specified length and of a type appropriate to return from integer operations
     * This method will be overridden by floating-point {@link Vector} implementations, by {@link PackedIntVector}, and
     * by sparse storage implementations.
     * 
     * @return Vector
     */
    protected NumericVector createIntVector(final long newVectorLength) {
        return new DenseIntVector(newVectorLength);
    }

    /**
     * Creates a new {@link Vector} of the same length and of a type appropriate to return from integer operations.
     * 
     * @return Vector
     */
    protected NumericVector createIntVector() {
        return createIntVector(length);
    }

    /**
     * Creates a new {@link Vector} of the specified length and of a type appropriate to return from floating-point
     * operations. This method may be overridden by some {@link Vector} implementations.
     * 
     * @return Vector
     */
    protected NumericVector createFloatVector(final long newVectorLength) {
        return new DenseFloatVector(newVectorLength);
    }

    /**
     * Creates a new {@link Vector} of the same length and of a type appropriate to return from floating-point
     * operations. This method may be overridden by some {@link Vector} implementations.
     * 
     * @return Vector
     */
    protected NumericVector createFloatVector() {
        return createFloatVector(length);
    }

    @Override
    public float dotProduct(final Vector v) {
        // BitVector dotProduct() implementations are generally more efficient.
        if (v instanceof BitVector) {
            return v.dotProduct(this);
        }

        if (v.length() != length) {
            throw new IllegalArgumentException("Vector length mismatch");
        }

        float dotProduct = 0f;
        for (int i = 0; i < length; i++) {
            dotProduct += getFloat(i) * v.getFloat(i);
        }
        return dotProduct;
    }

    @Override
    public float sum() {
        float sum = 0f;
        for (int i = 0; i < length; i++) {
            sum += getFloat(i);
        }
        return sum;
    }

    public void write(final Writer writer, final String headerLine) throws IOException {
        writer.write(headerLine);

        // Write Vector contents
        for (int i = 0; i < length - 1; i++) {
            writer.write(String.format("%d ", getInt(i)));
        }
        writer.write(String.format("%d\n", getInt((int) (length - 1))));
        writer.flush();
    }

    /**
     * Type-strengthen return-type
     * 
     * @return a copy of this vector
     */
    @Override
    public abstract Vector clone();

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null) {
            return false;
        }

        if (o.getClass() != this.getClass()) {
            return false;
        }

        final BaseVector other = (BaseVector) o;

        if (other.length != length) {
            return false;
        }

        for (int i = 0; i < length; i++) {
            if (Math.abs(getFloat(i) - other.getFloat(i)) > EPSILON) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        try {
            final Writer writer = new StringWriter(length > 0 && length < 10000 ? (int) (length * 10) : 10240);
            write(writer);
            return writer.toString();
        } catch (final IOException e) {
            return "Caught IOException in StringWriter";
        }
    }

}

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

import edu.ohsu.cslu.datastructs.matrices.FixedPointShortMatrix;
import edu.ohsu.cslu.datastructs.matrices.FloatMatrix;
import edu.ohsu.cslu.datastructs.matrices.HashSparseFloatMatrix;
import edu.ohsu.cslu.datastructs.matrices.Matrix;

public abstract class BaseNumericVector extends BaseVector implements NumericVector {

    private static final long serialVersionUID = 1L;

    BaseNumericVector(final long length) {
        super(length);
    }

    public NumericVector multiply(final Matrix m) {
        // Relatively inefficient implementation. Could be overridden more efficiently in child
        // classes.

        final int columns = m.columns();
        if (columns != length) {
            throw new IllegalArgumentException("Vector length mismatch");
        }

        final int rows = m.rows();

        NumericVector newVector;
        final Class<?> vClass = getClass();
        final Class<?> mClass = m.getClass();
        // If either the matrix or vector is a floating-point type, we have to return a floating-point vector. Otherwise
        // (for the moment, at least), return an IntVector.
        if (mClass == FloatMatrix.class || mClass == FixedPointShortMatrix.class
                || mClass == HashSparseFloatMatrix.class || vClass == DenseFloatVector.class
                && vClass == LargeSparseFloatVector.class) {
            newVector = createFloatVector(rows);
        } else {
            newVector = createIntVector(rows);
        }

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                newVector.set(i, newVector.getFloat(i) + getFloat(j) * m.getFloat(i, j));
            }
        }

        return newVector;
    }

    @Override
    public NumericVector scalarAdd(final float addend) {
        final NumericVector newVector = new DenseFloatVector(length);
        for (int i = 0; i < newVector.length(); i++) {
            newVector.set(i, getFloat(i) + addend);
        }
        return newVector;
    }

    @Override
    public NumericVector scalarAdd(final int addend) {
        final NumericVector newVector = createIntVector();
        for (int i = 0; i < newVector.length(); i++) {
            newVector.set(i, getFloat(i) + addend);
        }
        return newVector;
    }

    @Override
    public FloatVector scalarMultiply(final float multiplier) {
        final DenseFloatVector newVector = new DenseFloatVector(length);
        for (int i = 0; i < newVector.length(); i++) {
            newVector.set(i, getFloat(i) * multiplier);
        }
        return newVector;
    }

    @Override
    public NumericVector scalarMultiply(final int multiplier) {
        final NumericVector newVector = createIntVector();
        for (int i = 0; i < newVector.length(); i++) {
            newVector.set(i, getFloat(i) * multiplier);
        }
        return newVector;
    }

    @Override
    public float max() {
        float max = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < length; i++) {
            final float x = getFloat(i);
            if (x > max) {
                max = x;
            }
        }
        return max;
    }

    @Override
    public int intMax() {
        return Math.round(max());
    }

    @Override
    public float min() {
        float min = Float.POSITIVE_INFINITY;

        for (int i = 0; i < length; i++) {
            final float x = getFloat(i);
            if (x < min) {
                min = x;
            }
        }
        return min;
    }

    @Override
    public int intMin() {
        return Math.round(min());
    }

    @Override
    public int argMax() {
        int maxI = 0;
        float max = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < length; i++) {
            final float x = getFloat(i);
            if (x > max) {
                max = x;
                maxI = i;
            }
        }
        return maxI;
    }

    @Override
    public int argMin() {
        int minI = 0;
        float min = Float.POSITIVE_INFINITY;

        for (int i = 0; i < length; i++) {
            final float x = getFloat(i);
            if (x < min) {
                min = x;
                minI = i;
            }
        }
        return minI;
    }
}

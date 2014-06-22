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
package edu.ohsu.cslu.datastructs.matrices;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;

import edu.ohsu.cslu.datastructs.vectors.NumericVector;

/**
 * Basic functionality required by all classes implementing the {@link Matrix} interface.
 * 
 * @author Aaron Dunlop
 * @since Sep 30, 2008
 * 
 *        $Id$
 */
public abstract class BaseMatrix implements Matrix, Serializable {

    private static final long serialVersionUID = 1L;

    /** Number of rows */
    protected final int m;

    /** Number of columns */
    protected final int n;

    /**
     * Is this matrix symmetric? (Symmetric matrices are reflected across the diagonal, and can thus be stored in 1/2
     * the space)
     */
    protected final boolean symmetric;

    BaseMatrix(final int m, final int n, final boolean symmetric) {
        this.m = m;
        this.n = n;
        this.symmetric = symmetric;
    }

    @Override
    public final int rows() {
        return m;
    }

    @Override
    public final int columns() {
        return n;
    }

    @Override
    public boolean isSquare() {
        return m == n;
    }

    @Override
    public boolean isSymmetric() {
        return symmetric;
    }

    @Override
    public float max() {
        float max = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < m; i++) {
            final int jBound = symmetric ? i + 1 : n;
            for (int j = 0; j < jBound; j++) {
                final float x = getFloat(i, j);
                if (x > max) {
                    max = x;
                }
            }
        }
        return max;
    }

    @Override
    public int intMax() {
        return Math.round(max());
    }

    @Override
    public int[] argMax() {
        int maxI = 0, maxJ = 0;
        float max = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < m; i++) {
            final int jBound = symmetric ? i + 1 : n;
            for (int j = 0; j < jBound; j++) {
                final float x = getFloat(i, j);
                if (x > max) {
                    max = x;
                    maxI = i;
                    maxJ = j;
                }
            }
        }
        return new int[] { maxI, maxJ };
    }

    @Override
    public int rowArgMax(final int i) {
        int maxJ = 0;
        float max = Float.NEGATIVE_INFINITY;

        for (int j = 0; j < n; j++) {
            final float x = getFloat(i, j);
            if (x > max) {
                max = x;
                maxJ = j;
            }
        }
        return maxJ;
    }

    @Override
    public float min() {
        float min = Float.POSITIVE_INFINITY;

        for (int i = 0; i < m; i++) {
            final int jBound = symmetric ? i + 1 : n;
            for (int j = 0; j < jBound; j++) {
                final float x = getFloat(i, j);
                if (x < min) {
                    min = x;
                }
            }
        }
        return min;
    }

    @Override
    public int intMin() {
        return Math.round(min());
    }

    @Override
    public int[] argMin() {
        int minI = 0, minJ = 0;
        float min = Float.POSITIVE_INFINITY;

        for (int i = 0; i < m; i++) {
            final int jBound = symmetric ? i + 1 : n;
            for (int j = 0; j < jBound; j++) {
                final float x = getFloat(i, j);
                if (x < min) {
                    min = x;
                    minI = i;
                    minJ = j;
                }
            }
        }
        return new int[] { minI, minJ };
    }

    @Override
    public int rowArgMin(final int i) {
        int minJ = 0;
        float min = Float.POSITIVE_INFINITY;

        for (int j = 0; j < n; j++) {
            final float x = getFloat(i, j);
            if (x < min) {
                min = x;
                minJ = j;
            }
        }
        return minJ;
    }

    @Override
    public DenseMatrix scalarAdd(final float addend) {
        // Relatively inefficient implementation. Could be overridden in subclasses
        final DenseMatrix newMatrix = (DenseMatrix) clone();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                newMatrix.set(i, j, getFloat(i, j) + addend);
            }
        }
        return newMatrix;
    }

    @Override
    public DenseMatrix scalarAdd(final int addend) {
        // Relatively inefficient implementation. Could be overridden in subclasses
        final DenseMatrix newMatrix = (DenseMatrix) clone();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                newMatrix.set(i, j, getFloat(i, j) + addend);
            }
        }
        return newMatrix;
    }

    @Override
    public Matrix scalarMultiply(final float multiplier) {
        // Relatively inefficient implementation. Could be overridden in subclasses
        final Matrix newMatrix = clone();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                newMatrix.set(i, j, getFloat(i, j) * multiplier);
            }
        }
        return newMatrix;
    }

    @Override
    public Matrix scalarMultiply(final int multiplier) {
        // Relatively inefficient implementation. Could be overridden in subclasses
        final Matrix newMatrix = clone();
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                newMatrix.set(i, j, getFloat(i, j) * multiplier);
            }
        }
        return newMatrix;
    }

    @Override
    public NumericVector multiply(final NumericVector v) {
        return v.multiply(this);
    }

    @Override
    public float sum() {
        float sum = 0f;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                sum += getFloat(i, j);
            }
        }
        return sum;
    }

    /**
     * Type-strengthen return-type
     * 
     * @return a copy of this matrix
     */
    @Override
    public abstract Matrix clone();

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o.getClass() != this.getClass()) {
            return false;
        }

        final Matrix other = (Matrix) o;

        for (int i = 0; i < m; i++) {
            final int maxJ = symmetric ? i + 1 : n;
            for (int j = 0; j < maxJ; j++) {
                // TODO: Should this use an epsilon comparison instead of an exact float comparison?
                if (getFloat(i, j) != other.getFloat(i, j)) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public String toString() {
        try {
            final Writer writer = new StringWriter(m * n * 10);
            write(writer);
            return writer.toString();
        } catch (final IOException e) {
            return "Caught IOException in StringWriter";
        }
    }
}

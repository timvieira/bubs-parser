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
package edu.ohsu.cslu.datastructs.matrices;

import java.io.IOException;
import java.io.Writer;

import edu.ohsu.cslu.util.Math;

/**
 * Fixed-point implementation of the {@link Matrix} interface, which stores values as shorts for space efficiency
 * (16-bit instead of 32-bit floats), at the expense of some runtime efficiency due to conversions.
 * 
 * Reads and writes matrices to a standard human-readable storage format as well as to java serialized objects.
 * 
 * Like most {@link Matrix} implementations, this implementation includes special handling of symmetric matrices,
 * allowing them to be stored in a less memory-intensive manner.
 * 
 * @author Aaron Dunlop
 * @since Sep 17, 2008
 * 
 *        $Id$
 */
public class FixedPointShortMatrix extends BaseDenseMatrix {

    private final static long serialVersionUID = 369752896212698723L;

    private final short[][] matrix;
    private final int precision;
    private final float precisionFactor;
    private final float maxValue;
    private final float minValue;

    /**
     * Construct a FixedPointShortMatrix
     * 
     * @param matrix the float array
     * @param precision the number of digits to the right of the decimal
     * @param symmetric Is this matrix symmetric? (Symmetric matrices can be stored more efficiently)
     */
    public FixedPointShortMatrix(final short[][] matrix, final int precision, final boolean symmetric) {
        super(matrix.length, matrix.length == 0 ? 0 : matrix[matrix.length - 1].length, symmetric);
        this.precision = precision;
        this.precisionFactor = Math.pow(10, precision);
        maxValue = 32767f / precisionFactor;
        minValue = -32768f / precisionFactor;
        this.matrix = matrix;
    }

    /**
     * Construct a FixedPointShortMatrix
     * 
     * @param matrix the float array
     * @param precision the number of digits to the right of the decimal
     * @param symmetric Is this matrix symmetric? (Symmetric matrices can be stored more efficiently)
     */
    public FixedPointShortMatrix(final float[][] matrix, final int precision, final boolean symmetric) {
        super(matrix.length, matrix[matrix.length - 1].length, symmetric);
        this.precision = precision;
        this.precisionFactor = Math.pow(10, precision);
        maxValue = 32767f / precisionFactor;
        minValue = -32768f / precisionFactor;

        this.matrix = createArray(symmetric);
        for (int i = 0; i < m; i++) {
            final int maxJ = symmetric ? (i + 1) : n;
            for (int j = 0; j < maxJ; j++) {
                set(i, j, matrix[i][j]);
            }
        }
    }

    /**
     * Construct a FixedPointShortMatrix
     * 
     * @param m rows
     * @param n columns
     * @param precision the number of digits to the right of the decimal
     * @param symmetric Is this matrix symmetric? (Symmetric matrices can be stored more efficiently)
     */
    public FixedPointShortMatrix(final int m, final int n, final int precision, final boolean symmetric) {
        super(m, n, symmetric);
        this.precision = precision;
        this.precisionFactor = Math.pow(10, precision);
        maxValue = 32767f / precisionFactor;
        minValue = -32768f / precisionFactor;
        this.matrix = createArray(symmetric);
    }

    /**
     * Construct a FixedPointShortMatrix
     * 
     * @param matrix the float array
     * @param precision the number of digits to the right of the decimal
     */
    public FixedPointShortMatrix(final float[][] matrix, final int precision) {
        this(matrix, precision, false);
    }

    /**
     * Construct a FixedPointShortMatrix
     * 
     * @param matrix the short array
     * @param precision the number of digits to the right of the decimal
     */
    public FixedPointShortMatrix(final short[][] matrix, final int precision) {
        this(matrix, precision, false);
    }

    private short[][] createArray(final boolean symmetricArray) {
        if (symmetricArray) {
            final short[][] array = new short[m][];
            for (int i = 0; i < m; i++) {
                array[i] = new short[i + 1];
            }
            return array;
        }

        return new short[m][n];
    }

    @Override
    public float getFloat(final int i, final int j) {
        if (symmetric && j > i) {
            return matrix[j][i] / precisionFactor;
        }

        return matrix[i][j] / precisionFactor;
    }

    @Override
    public int getInt(final int i, final int j) {
        return java.lang.Math.round(getFloat(i, j));
    }

    @Override
    public void set(final int i, final int j, final float value) {
        if (value > maxValue || value < minValue) {
            throw new IllegalArgumentException("value out of range");
        }

        if (symmetric && j > i) {
            matrix[j][i] = (short) java.lang.Math.round(value * precisionFactor);
        } else {
            matrix[i][j] = (short) java.lang.Math.round(value * precisionFactor);
        }
    }

    @Override
    public void set(final int i, final int j, final int value) {
        set(i, j, (float) value);
    }

    @Override
    public void set(final int i, final int j, final String newValue) {
        set(i, j, Float.parseFloat(newValue));
    }

    /**
     * Type-strengthen {@link DenseMatrix#subMatrix(int, int, int, int)}
     * 
     * Note that a submatrix of a symmetric matrix may or may not be symmetric as well. This implementation correctly
     * creates a symmetric submatrix when the specified indices indicate a submatrix reflected across the diagonal of
     * the original matrix. Note that it does no element value comparisons, so a submatrix of a non-symmetric matrix
     * will _never_ be labeled as symmetric (even if the submatrix is in fact mathematically a symmetric matrix).
     */
    @Override
    public FixedPointShortMatrix subMatrix(final int i0, final int i1, final int j0, final int j1) {
        if (symmetric) {
            if (((i1 - i0) == (j1 - j0)) && (i1 == j1)) {
                // The resulting matrix will still be symmetric
                final short[][] submatrix = new short[i1 - i0 + 1][];
                for (int i = i0; i <= i1; i++) {
                    final int rowLength = i - i0 + 1;
                    final short[] row = submatrix[i - i0] = new short[rowLength];
                    System.arraycopy(matrix[i], j0, row, 0, rowLength);
                }
                return new FixedPointShortMatrix(submatrix, precision, true);
            }

            // The resulting matrix will _not_ be symmetric
            final float[][] submatrix = new float[i1 - i0 + 1][];
            for (int i = i0; i <= i1; i++) {
                final int rowLength = j1 - j0 + 1;
                final float[] row = submatrix[i - i0] = new float[rowLength];
                for (int j = j0; j <= j1; j++) {
                    row[j - j0] = getFloat(i, j);
                }
            }
            return new FixedPointShortMatrix(submatrix, precision, false);
        }

        final short[][] submatrix = new short[i1 - i0 + 1][];
        for (int i = i0; i <= i1; i++) {
            final short[] row = new short[j1 - j0 + 1];
            System.arraycopy(matrix[i], j0, row, 0, row.length);
            submatrix[i - i0] = row;
        }
        return new FixedPointShortMatrix(submatrix, precision);
    }

    /**
     * Type-strengthen {@link Matrix#transpose()}
     */
    @Override
    public FixedPointShortMatrix transpose() {
        if (isSymmetric() || n == 0) {
            return clone();
        }

        final short[][] array = new short[n][m];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                array[j][i] = matrix[i][j];
            }
        }
        return new FixedPointShortMatrix(array, precision);
    }

    @Override
    public float infinity() {
        return Short.MAX_VALUE / precisionFactor;
    }

    @Override
    public float negativeInfinity() {
        return Short.MIN_VALUE / precisionFactor;
    }

    @Override
    public FixedPointShortMatrix clone() {
        final short[][] newMatrix = new short[m][];
        for (int i = 0; i < m; i++) {
            final int rowLength = matrix[i].length;
            newMatrix[i] = new short[rowLength];
            System.arraycopy(matrix[i], 0, newMatrix[i], 0, rowLength);
        }
        return new FixedPointShortMatrix(newMatrix, precision, symmetric);
    }

    @Override
    public void write(final Writer writer) throws IOException {
        // Header line:
        writer.write(String.format("matrix type=" + Matrix.Factory.ATTRIBUTE_TYPE_FIXED_POINT_SHORT
                + " precision=%d rows=%d columns=%d symmetric=%s\n", precision, m, n, symmetric));

        // Length of each cell = 'maximum number of digits to the left of the decimal' + precision +
        // decimal
        final int maxInt = (int) (max() / precisionFactor);
        final int length = Integer.toString(maxInt).length() + precision + 1;
        write(writer, "%-" + length + "." + precision + "f");
    }
}

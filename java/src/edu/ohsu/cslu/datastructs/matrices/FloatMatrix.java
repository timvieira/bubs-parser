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
import java.util.Arrays;

/**
 * Single-precision floating-point implementation of the {@link Matrix} interface.
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
public final class FloatMatrix extends BaseDenseMatrix {

    private final static long serialVersionUID = 369752896212698723L;

    private final float[][] matrix;

    /**
     * Construct a FloatMatrix
     * 
     * @param matrix the float array
     * @param symmetric Is this matrix symmetric? (Symmetric matrices can be stored more efficiently)
     */
    public FloatMatrix(final float[][] matrix, final boolean symmetric) {
        super(matrix.length, matrix.length == 0 ? 0 : matrix[matrix.length - 1].length, symmetric);
        this.matrix = matrix;
    }

    /**
     * Construct a FloatMatrix
     * 
     * @param m rows
     * @param n columns
     * @param symmetric Is this matrix symmetric? (Symmetric matrices can be stored more efficiently)
     */
    public FloatMatrix(final int m, final int n, final boolean symmetric) {
        super(m, n, symmetric);
        if (symmetric) {
            matrix = new float[m][];
            for (int i = 0; i < m; i++) {
                matrix[i] = new float[i + 1];
            }
        } else {
            matrix = new float[m][n];
        }
    }

    /**
     * Construct a FloatMatrix
     * 
     * @param m rows
     * @param n columns
     */
    public FloatMatrix(final int m, final int n) {
        this(m, n, false);
    }

    /**
     * Construct a FloatMatrix
     * 
     * @param matrix the float array
     */
    public FloatMatrix(final float[][] matrix) {
        this(matrix, false);
    }

    @Override
    public int getInt(final int i, final int j) {
        if (symmetric && j > i) {
            return Math.round(matrix[j][i]);
        }

        return Math.round(matrix[i][j]);
    }

    @Override
    public float getFloat(final int i, final int j) {
        if (symmetric && j > i) {
            return matrix[j][i];
        }

        return matrix[i][j];
    }

    /**
     * Override BaseMatrix implementation with a more efficient one.
     */
    @Override
    public float[] getRow(final int i) {
        if (!symmetric) {
            return matrix[i];
        }

        // For symmetric matrices, we have to copy and 'extend' the row
        final float[] row = new float[n];
        for (int j = 0; j < n; j++) {
            row[j] = getFloat(i, j);
        }
        return row;
    }

    @Override
    public void set(final int i, final int j, final int value) {
        set(i, j, (float) value);
    }

    @Override
    public void set(final int i, final int j, final float value) {
        if (symmetric && j > i) {
            matrix[j][i] = value;
        } else {
            matrix[i][j] = value;
        }
    }

    @Override
    public void set(final int i, final int j, final String newValue) {
        matrix[i][j] = Float.parseFloat(newValue);
    }

    public FloatMatrix add(final Matrix addend) {

        if (addend.rows() != rows() || addend.columns() != columns()) {
            throw new IllegalArgumentException("Matrix dimensions must match");
        }

        final FloatMatrix sum = new FloatMatrix(rows(), columns());
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                sum.set(i, j, getFloat(i, j) + addend.getFloat(i, j));
            }
        }
        return sum;
    }

    /**
     * Override BaseMatrix implementation with a more efficient one.
     */
    @Override
    public void setRow(final int i, final float value) {
        Arrays.fill(matrix[i], value);
    }

    /**
     * Override BaseMatrix implementation with a more efficient one.
     */
    @Override
    public void setRow(final int i, final int value) {
        setRow(i, (float) value);
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
    public FloatMatrix subMatrix(final int i0, final int i1, final int j0, final int j1) {
        if (symmetric) {
            if (((i1 - i0) == (j1 - j0)) && (i1 == j1)) {
                // The resulting matrix will still be symmetric
                final float[][] submatrix = new float[i1 - i0 + 1][];
                for (int i = i0; i <= i1; i++) {
                    final int rowLength = i - i0 + 1;
                    final float[] row = submatrix[i - i0] = new float[rowLength];
                    System.arraycopy(matrix[i], j0, row, 0, rowLength);
                }
                return new FloatMatrix(submatrix, true);
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
            return new FloatMatrix(submatrix, false);
        }

        final float[][] submatrix = new float[i1 - i0 + 1][];
        for (int i = i0; i <= i1; i++) {
            final float[] row = new float[j1 - j0 + 1];
            System.arraycopy(matrix[i], j0, row, 0, row.length);
            submatrix[i - i0] = row;
        }
        return new FloatMatrix(submatrix);
    }

    /**
     * Type-strengthen {@link Matrix#transpose()}
     */
    @Override
    public FloatMatrix transpose() {
        if (isSymmetric() || n == 0) {
            return clone();
        }

        final float[][] array = new float[n][m];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                array[j][i] = matrix[i][j];
            }
        }
        return new FloatMatrix(array);
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
    public FloatMatrix clone() {
        final float[][] newMatrix = new float[m][];
        for (int i = 0; i < m; i++) {
            final int rowLength = matrix[i].length;
            newMatrix[i] = new float[rowLength];
            System.arraycopy(matrix[i], 0, newMatrix[i], 0, rowLength);
        }
        return new FloatMatrix(newMatrix, symmetric);
    }

    @Override
    public void write(final Writer writer) throws IOException {
        write(writer, 4);
    }

    public void write(final Writer writer, final int precision) throws IOException {
        // Header line:
        writer.write(String.format("matrix type=float rows=%d columns=%d symmetric=%s\n", m, n, symmetric));

        // Length of each cell = 'maximum number of digits to the left of the decimal' + precision +
        // decimal
        final int length = Integer.toString((int) max()).length() + precision + 1;
        write(writer, "%-" + length + "." + precision + "f");
    }
}

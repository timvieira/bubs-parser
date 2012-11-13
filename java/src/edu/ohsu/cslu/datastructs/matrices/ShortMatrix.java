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
 * Implementation of the {@link Matrix} interface which stores shorts instead of floating-point. This implementation is
 * more memory-efficient than {@link IntMatrix} for applications which do not need the full range of a 32-bit integer.
 * 
 * Reads and writes matrices to a standard human-readable storage format as well as to java serialized objects.
 * 
 * Like most {@link Matrix} implementations, this implementation includes special handling of symmetric matrices,
 * allowing them to be stored in a less memory-intensive manner.
 * 
 * 
 * @author Aaron Dunlop
 * @since Sep 17, 2008
 * 
 *        $Id$
 */
public class ShortMatrix extends BaseDenseMatrix {

    private final static long serialVersionUID = 369752896212698723L;

    private final short[][] matrix;

    /**
     * Construct a ShortMatrix
     * 
     * @param matrix the short array
     */
    public ShortMatrix(final short[][] matrix) {
        this(matrix, false);
    }

    /**
     * Construct a ShortMatrix
     * 
     * @param m rows
     * @param n columns
     * @param symmetric Is this matrix symmetric? (Symmetric matrices can be stored more efficiently)
     */
    public ShortMatrix(final int m, final int n, final boolean symmetric) {
        super(m, n, symmetric);
        if (symmetric) {
            matrix = new short[m][];
            for (int i = 0; i < m; i++) {
                matrix[i] = new short[i + 1];
            }
        } else {
            matrix = new short[m][n];
        }
    }

    /**
     * Construct a ShortMatrix
     * 
     * @param m rows
     * @param n columns
     */
    public ShortMatrix(final int m, final int n) {
        this(m, n, false);
    }

    /**
     * Construct a ShortMatrix
     * 
     * @param matrix the short array
     * @param symmetric Is this matrix symmetric? (Symmetric matrices can be stored more efficiently)
     */
    public ShortMatrix(final short[][] matrix, final boolean symmetric) {
        super(matrix.length, matrix.length == 0 ? 0 : matrix[matrix.length - 1].length, symmetric);
        this.matrix = matrix;
    }

    @Override
    public int getInt(final int i, final int j) {
        if (symmetric && j > i) {
            return matrix[j][i];
        }

        return matrix[i][j];
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
    public int[] getIntRow(final int i) {
        if (!symmetric) {
            final int[] row = new int[n];
            for (int j = 0; j < n; j++) {
                row[j] = matrix[i][j];
            }
            return row;
        }

        // For symmetric matrices, we have to copy and 'extend' the row
        final int[] row = new int[n];
        for (int j = 0; j < n; j++) {
            row[j] = getInt(i, j);
        }
        return row;
    }

    @Override
    public void set(final int i, final int j, final int value) {
        if (symmetric && j > i) {
            matrix[j][i] = (short) value;
        } else {
            matrix[i][j] = (short) value;
        }
    }

    @Override
    public void set(final int i, final int j, final float value) {
        set(i, j, Math.round(value));
    }

    @Override
    public void set(final int i, final int j, final String newValue) {
        matrix[i][j] = Short.parseShort(newValue);
    }

    public void increment(final int i, final int j) {
        matrix[i][j]++;
    }

    public void add(final int i, final int j, final int addend) {
        matrix[i][j] += addend;
    }

    /**
     * Override BaseMatrix implementation with a more efficient one.
     */
    @Override
    public void setRow(final int i, final float value) {
        setRow(i, (int) value);
    }

    /**
     * Override BaseMatrix implementation with a more efficient one.
     */
    @Override
    public void setRow(final int i, final int value) {
        if (symmetric) {
            super.setRow(i, value);
        } else {
            Arrays.fill(matrix[i], (short) value);
        }
    }

    /**
     * Override BaseMatrix implementation to avoid repeated int->float conversions
     */
    @Override
    public int[] argMax() {
        int maxI = 0, maxJ = 0;
        int max = Integer.MIN_VALUE;

        for (int i = 0; i < m; i++) {
            final int jBound = symmetric ? i + 1 : n;
            for (int j = 0; j < jBound; j++) {
                final int x = matrix[i][j];
                if (x > max) {
                    max = x;
                    maxI = i;
                    maxJ = j;
                }
            }
        }
        return new int[] { maxI, maxJ };
    }

    /**
     * Override BaseMatrix implementation to avoid repeated int->float conversions
     */
    @Override
    public int rowArgMax(final int i) {
        int maxJ = 0;
        int max = Integer.MIN_VALUE;

        for (int j = 0; j < n; j++) {
            final int x = getInt(i, j);
            if (x > max) {
                max = x;
                maxJ = j;
            }
        }
        return maxJ;
    }

    /**
     * Override BaseMatrix implementation to avoid repeated int->float conversions
     */
    @Override
    public int[] argMin() {
        int minI = 0, minJ = 0;
        int min = Integer.MAX_VALUE;

        for (int i = 0; i < m; i++) {
            final int jBound = symmetric ? i + 1 : n;
            for (int j = 0; j < jBound; j++) {
                final int x = matrix[i][j];
                if (x < min) {
                    min = x;
                    minI = i;
                    minJ = j;
                }
            }
        }
        return new int[] { minI, minJ };
    }

    /**
     * Override BaseMatrix implementation to avoid repeated int->float conversions
     */
    @Override
    public int rowArgMin(final int i) {
        int minJ = 0;
        int min = Integer.MAX_VALUE;

        for (int j = 0; j < n; j++) {
            final int x = getInt(i, j);
            if (x < min) {
                min = x;
                minJ = j;
            }
        }
        return minJ;
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
    public ShortMatrix subMatrix(final int i0, final int i1, final int j0, final int j1) {
        if (symmetric) {
            if (((i1 - i0) == (j1 - j0)) && (i1 == j1)) {
                // The resulting matrix will still be symmetric
                final short[][] submatrix = new short[i1 - i0 + 1][];
                for (int i = i0; i <= i1; i++) {
                    final int rowLength = i - i0 + 1;
                    final short[] row = submatrix[i - i0] = new short[rowLength];
                    System.arraycopy(matrix[i], j0, row, 0, rowLength);
                }
                return new ShortMatrix(submatrix, true);
            }

            // The resulting matrix will _not_ be symmetric
            final short[][] submatrix = new short[i1 - i0 + 1][];
            for (int i = i0; i <= i1; i++) {
                final int rowLength = j1 - j0 + 1;
                final short[] row = submatrix[i - i0] = new short[rowLength];
                for (int j = j0; j <= j1; j++) {
                    row[j - j0] = (short) getInt(i, j);
                }
            }
            return new ShortMatrix(submatrix, false);
        }

        final short[][] submatrix = new short[i1 - i0 + 1][];
        for (int i = i0; i <= i1; i++) {
            final short[] row = new short[j1 - j0 + 1];
            System.arraycopy(matrix[i], j0, row, 0, row.length);
            submatrix[i - i0] = row;
        }
        return new ShortMatrix(submatrix);
    }

    /**
     * Type-strengthen {@link Matrix#transpose()}
     */
    @Override
    public ShortMatrix transpose() {
        if (isSymmetric() || n == 0) {
            return clone();
        }

        final short[][] array = new short[n][m];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                array[j][i] = matrix[i][j];
            }
        }
        return new ShortMatrix(array);
    }

    @Override
    public DenseMatrix scalarAdd(final float addend) {
        // scalarAdd() and scalarMultiply() with floats should return float matrices
        final float[][] array = new float[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                array[i][j] = matrix[i][j] + addend;
            }
        }
        return new FloatMatrix(array);
    }

    @Override
    public DenseMatrix scalarMultiply(final float multiplier) {
        // scalarAdd() and scalarMultiply() with floats should return float matrices
        final float[][] array = new float[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                array[i][j] = matrix[i][j] * multiplier;
            }
        }
        return new FloatMatrix(array);
    }

    @Override
    public float infinity() {
        return Short.MAX_VALUE;
    }

    @Override
    public float negativeInfinity() {
        return Short.MIN_VALUE;
    }

    @Override
    public ShortMatrix clone() {
        final short[][] newMatrix = new short[m][];
        for (int i = 0; i < m; i++) {
            final int rowLength = matrix[i].length;
            newMatrix[i] = new short[rowLength];
            System.arraycopy(matrix[i], 0, newMatrix[i], 0, rowLength);
        }
        return new ShortMatrix(newMatrix, symmetric);
    }

    @Override
    public void write(final Writer writer) throws IOException {
        // Header line
        writer.write(String.format("matrix type=short rows=%d columns=%d symmetric=%s\n", m, n, symmetric));

        // Matrix contents
        final int length = Integer.toString(intMax()).length();
        final String format = "%-" + length + "d ";
        final String eolFormat = "%d\n";

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < matrix[i].length - 1; j++) {
                writer.write(String.format(format, matrix[i][j]));
            }
            writer.write(String.format(eolFormat, matrix[i][matrix[i].length - 1]));
        }
        writer.flush();
    }
}

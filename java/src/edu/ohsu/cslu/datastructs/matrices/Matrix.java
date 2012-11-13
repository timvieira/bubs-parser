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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import edu.ohsu.cslu.datastructs.vectors.NumericVector;
import edu.ohsu.cslu.util.Strings;

/**
 * Base interface for all matrix classes.
 * 
 * @author Aaron Dunlop
 * @since Sep 17, 2008
 * 
 *        $Id$
 */
public interface Matrix extends Serializable, Cloneable {

    /**
     * Get row dimension.
     * 
     * @return m, the number of rows.
     */
    public int rows();

    /**
     * Get column dimension.
     * 
     * @return n, the number of columns.
     */
    public int columns();

    /**
     * Retrieves a matrix element
     * 
     * @param i row
     * @param j column
     * @return the matrix element as an integer
     */
    public int getInt(final int i, final int j);

    /**
     * Retrieves a matrix element
     * 
     * @param i row
     * @param j column
     * @return the matrix element as a float
     */
    public float getFloat(final int i, final int j);

    /**
     * Sets a matrix element
     * 
     * @param i row
     * @param j column
     * @param value new value
     */
    public void set(final int i, final int j, final int value);

    /**
     * Sets a matrix element
     * 
     * @param i row
     * @param j column
     * @param value new value
     */
    public void set(final int i, final int j, final float value);

    /**
     * Parses and sets a matrix element
     * 
     * @param i row
     * @param j column
     * @param newValue new value
     */
    public void set(final int i, final int j, final String newValue);

    /**
     * @return the maximum value which can be stored by this matrix
     */
    public float infinity();

    /**
     * @return the minimum value which can be stored by this matrix
     */
    public float negativeInfinity();

    /**
     * @return the maximum value present in the matrix
     */
    public float max();

    /**
     * @return the maximum value present in the matrix as an int
     */
    public int intMax();

    /**
     * @return the coordinates of the maximum value present in the matrix; i,j such that {@link #getFloat(int, int)}
     *         will return the same value as {@link #max()}.
     */
    public int[] argMax();

    /**
     * @return the minimum value present in the matrix
     */
    public float min();

    /**
     * @return the minimum value present in the matrix as an int
     */
    public int intMin();

    /**
     * @return the coordinates of the minimum value present in the matrix; i,j such that {@link #getFloat(int, int)}
     *         will return the same value as {@link #min()}.
     */
    public int[] argMin();

    /**
     * @return the index of the maximum value in the specified row.
     */
    public int rowArgMax(int i);

    /**
     * @return the index of the minimum value in the specified row.
     */
    public int rowArgMin(int i);

    /**
     * Returns a new matrix scaled by the provided multiplier. Note that a dense matrix is returned even if the original
     * matrix is sparse, since addition will populate previously empty entries.
     * 
     * @param addend
     * @return a new matrix scaled by the provided multiplier
     */
    public DenseMatrix scalarAdd(float addend);

    /**
     * Returns a new matrix scaled by the provided multiplier. Note that a dense matrix is returned even if the original
     * matrix is sparse, since addition will populate previously empty entries.
     * 
     * @param addend
     * @return a new matrix scaled by the provided addend
     */
    public DenseMatrix scalarAdd(int addend);

    /**
     * Returns a new matrix scaled by the provided multiplier. Note that (unlike {@link #scalarAdd(float)}),
     * multiplication does not require populating previously empty entries, so the result may be a sparse matrix.
     * 
     * @param multiplier
     * @return a new matrix scaled by the provided multiplier
     */
    public Matrix scalarMultiply(float multiplier);

    /**
     * Returns a new matrix scaled by the provided multiplier. Note that (unlike {@link #scalarAdd(float)}),
     * multiplication does not require populating previously empty entries, so the result may be a sparse matrix.
     * 
     * @param multiplier
     * @return a new matrix scaled by the provided multiplier
     */
    public Matrix scalarMultiply(int multiplier);

    /**
     * @return true if this matrix is square (m == n)
     */
    public boolean isSquare();

    /**
     * @return true if this matrix is symmetric (that is, if its values are mirrored around the diagonal such that it is
     *         equal to its own transposition)
     */
    public boolean isSymmetric();

    /**
     * Not terribly useful mathematically, but useful as a simple single statistic about a matrix for unit testing (at
     * least until we bite the bullet and implement a determinant function). Note that we don't test this method, since
     * it's not really meaningful anyway.
     * 
     * TODO: Implement determinant instead.
     * 
     * @return the sum of all entries in this matrix
     */
    public float sum();

    /**
     * Returns the result of multiplying this matrix by the supplied vector.
     * 
     * @param v Vector
     * @return m x v
     */
    public NumericVector multiply(NumericVector v);

    /**
     * @return A' (the matrix's transposition)
     */
    public Matrix transpose();

    /**
     * Write this matrix to the specified writer
     * 
     * @param writer the output location
     * @throws IOException if the write fails
     */
    public void write(Writer writer) throws IOException;

    /**
     * Type-strengthen return-type
     * 
     * @return a copy of this matrix
     */
    public Matrix clone();

    /**
     * Factory class for all matrix types
     */
    public static class Factory {

        private final static String ATTRIBUTE_TYPE = "type";
        private final static String ATTRIBUTE_ROWS = "rows";
        private final static String ATTRIBUTE_COLUMNS = "columns";
        private final static String ATTRIBUTE_SYMMETRIC = "symmetric";
        /** Used for fixed-point implementations */
        private final static String ATTRIBUTE_PRECISION = "precision";

        public final static String ATTRIBUTE_TYPE_INT = "int";
        public final static String ATTRIBUTE_TYPE_SHORT = "short";
        public final static String ATTRIBUTE_TYPE_BYTE = "byte";
        public final static String ATTRIBUTE_TYPE_FLOAT = "float";
        public final static String ATTRIBUTE_TYPE_FIXED_POINT_SHORT = "fixed-point-short";
        public final static String ATTRIBUTE_TYPE_HASH_SPARSE_FLOAT = "hash-sparse-float";

        /**
         * Returns a new {@link IntMatrix} of m rows x n columns, in which all cells are initialized to the specified
         * initial value.
         * 
         * @param m rows
         * @param n columns
         * @param initialValue cell initialization value
         * @return IntMatrix
         */
        public static IntMatrix newIntMatrix(final int m, final int n, final int initialValue) {
            final IntMatrix matrix = new IntMatrix(new int[m][n]);
            for (int i = 0; i < m; i++) {
                matrix.setRow(i, initialValue);
            }
            return matrix;
        }

        /**
         * @param m matrix size
         * @param defaultValue
         * @param identityValue
         * @return an IntMatrix in which all cells such that i == j are pre-populated with identityValue and all others
         *         with defaultValue
         */
        public static IntMatrix newIdentityIntMatrix(final int m, final int defaultValue, final int identityValue) {
            final int[][] array = new int[m][m];
            for (int i = 0; i < m; i++) {
                Arrays.fill(array[i], defaultValue);
                array[i][i] = identityValue;
            }
            return new IntMatrix(array, false);
        }

        /**
         * @param m matrix size
         * @param defaultValue
         * @param identityValue
         * @return a FloatMatrix in which all cells such that i == j are pre-populated with identityValue and all others
         *         with defaultValue
         */
        public static FloatMatrix newIdentityFloatMatrix(final int m, final float defaultValue,
                final float identityValue) {
            final float[][] array = new float[m][m];
            for (int i = 0; i < m; i++) {
                Arrays.fill(array[i], defaultValue);
                array[i][i] = identityValue;
            }
            return new FloatMatrix(array, false);
        }

        /**
         * @param m matrix size
         * @param defaultValue
         * @param identityValue
         * @return a symmetric FloatMatrix in which all cells such that i == j are pre-populated with identityValue and
         *         all others with defaultValue
         */
        public static FloatMatrix newSymmetricIdentityFloatMatrix(final int m, final float defaultValue,
                final float identityValue) {
            final FloatMatrix matrix = new FloatMatrix(m, m, true);
            for (int i = 0; i < m; i++) {
                matrix.setRow(i, defaultValue);
                matrix.set(i, i, identityValue);
            }
            return matrix;
        }

        public static Matrix read(final String s) throws IOException {
            return read(new StringReader(s));
        }

        public static Matrix read(final File f) throws IOException {
            InputStream is = new FileInputStream(f);
            if (f.getName().endsWith(".gz")) {
                is = new GZIPInputStream(is);
            }
            return read(is);
        }

        public static Matrix read(final InputStream inputStream) throws IOException {
            return read(new InputStreamReader(inputStream));
        }

        public static Matrix read(final Reader reader) throws IOException {
            final BufferedReader br = new BufferedReader(reader);
            final Map<String, String> attributes = Strings.headerAttributes(br.readLine());

            final String type = attributes.get(ATTRIBUTE_TYPE);

            final int rows = Integer.parseInt(attributes.get(ATTRIBUTE_ROWS));
            final int columns = Integer.parseInt(attributes.get(ATTRIBUTE_COLUMNS));

            final boolean symmetric = attributes.containsKey(ATTRIBUTE_SYMMETRIC) ? Boolean.parseBoolean(attributes
                    .get(ATTRIBUTE_SYMMETRIC)) : false;

            Matrix matrix = null;
            if (type.equals(ATTRIBUTE_TYPE_INT)) {
                matrix = new IntMatrix(rows, columns, symmetric);
            } else if (type.equals(ATTRIBUTE_TYPE_FLOAT)) {
                matrix = new FloatMatrix(rows, columns, symmetric);
            } else if (type.equals(ATTRIBUTE_TYPE_FIXED_POINT_SHORT)) {
                final int precision = Integer.parseInt(attributes.get(ATTRIBUTE_PRECISION));
                matrix = new FixedPointShortMatrix(rows, columns, precision, symmetric);
            } else if (type.equals(ATTRIBUTE_TYPE_SHORT)) {
                matrix = new ShortMatrix(rows, columns, symmetric);
            } else if (type.equals(ATTRIBUTE_TYPE_BYTE)) {
                matrix = new ByteMatrix(rows, columns, symmetric);
            } else if (type.equals(ATTRIBUTE_TYPE_HASH_SPARSE_FLOAT)) {
                matrix = new HashSparseFloatMatrix(rows, columns, symmetric);
            } else if (type.equals("double")) {
                throw new RuntimeException(type + " type is not supported");
            } else {
                throw new RuntimeException(type + " type is not supported");
            }

            // Read and initialize matrix
            for (int i = 0; i < rows; i++) {
                final String line = br.readLine();
                final String[] split = line.split(" +");
                for (int j = 0; j < split.length; j++) {
                    matrix.set(i, j, split[j]);
                }
            }

            return matrix;

        }
    }
}

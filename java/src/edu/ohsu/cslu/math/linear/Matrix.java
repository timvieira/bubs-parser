package edu.ohsu.cslu.math.linear;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import edu.ohsu.cslu.util.Strings;


/**
 * Base interface for all matrix classes.
 * 
 * @author Aaron Dunlop
 * @since Sep 17, 2008
 * 
 *        $Id$
 */
public interface Matrix extends Cloneable
{
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
     * Retrieves a row. Note that {@link Matrix} implementations may return a copy of the actual row
     * or may return a reference to the row itself. Consumers should make their own copy before
     * altering the returned array. Read-only access is safe.
     * 
     * @param i row
     * @return the row as an array of ints
     */
    public int[] getIntRow(final int i);

    /**
     * Retrieves a row. Note that {@link Matrix} implementations may return a copy of the actual row
     * or may return a reference to the row itself. Consumers should make their own copy before
     * altering the returned array. Read-only access is safe.
     * 
     * @param i row
     * @return the row as an array of floats
     */
    public float[] getRow(final int i);

    /**
     * Retrieves a column. Note that {@link Matrix} implementations may return a copy of the actual
     * row or may return a reference to the row itself. Consumers should make their own copy before
     * altering the returned array. Read-only access is safe.
     * 
     * @param j column
     * @return the column as an array of ints
     */
    public int[] getIntColumn(final int j);

    /**
     * Retrieves a column. Note that {@link Matrix} implementations may return a copy of the actual
     * row or may return a reference to the row itself. Consumers should make their own copy before
     * altering the returned array. Read-only access is safe.
     * 
     * @param j column
     * @return the column as an array of floats
     */
    public float[] getColumn(final int j);

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
     * @param value new value
     */
    public void set(final int i, final int j, final String newValue);

    /**
     * Sets new values in a row
     * 
     * @param i row
     * @param newRow new row values
     */
    public void setRow(final int i, float[] newRow);

    /**
     * Fills in a row with the specified value
     * 
     * @param i row
     * @param value value with which to fill the specified row
     */
    public void setRow(final int i, float value);

    /**
     * Sets new values in a row
     * 
     * @param i row
     * @param newRow new row values
     */
    public void setRow(final int i, int[] newRow);

    /**
     * Fills in a row with the specified value
     * 
     * @param i row
     * @param value value with which to fill the specified row
     */
    public void setRow(final int i, int value);

    /**
     * Sets new values in a column
     * 
     * @param j column
     * @param newColumn new column values
     */
    public void setColumn(final int j, float[] newColumn);

    /**
     * Fills in a column with the specified value
     * 
     * @param j column
     * @param value value with which to fill the specified column
     */
    public void setColumn(final int j, float value);

    /**
     * Sets new values in a column
     * 
     * @param j column
     * @param newColumn new column values
     */
    public void setColumn(final int j, int[] newColumn);

    /**
     * Fills in a column with the specified value
     * 
     * @param j column
     * @param value value with which to fill the specified column
     */
    public void setColumn(final int j, int value);

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
     * @return the coordinates of the maximum value present in the matrix; i,j such that
     *         {@link #get(i, j)} will return the same value as {@link #max()}.
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
     * @return the coordinates of the minimum value present in the matrix; i,j such that
     *         {@link #get(i, j)} will return the same value as {@link #min()}.
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
     * @param addend
     * @return a new matrix scaled by the provided multiplier
     */
    public Matrix scalarAdd(float addend);

    /**
     * @param addend
     * @return a new matrix scaled by the provided multiplier
     */
    public Matrix scalarAdd(int addend);

    /**
     * @param multiplier
     * @return a new matrix scaled by the provided multiplier
     */
    public Matrix scalarMultiply(float multiplier);

    /**
     * @param multiplier
     * @return a new matrix scaled by the provided multiplier
     */
    public Matrix scalarMultiply(int multiplier);

    /**
     * Get a submatrix.
     * 
     * @param i0 Initial row index, inclusive.
     * @param i1 Final row index, inclusive
     * @param j0 Initial column index, inclusive
     * @param j1 Final column index, inclusive
     * @return A(i0:i1,j0:j1)
     * @throws ArrayIndexOutOfBoundsException Submatrix indices
     */
    public Matrix subMatrix(final int i0, final int i1, final int j0, final int j1);

    /**
     * @return true if this matrix is square (m == n)
     */
    public boolean isSquare();

    /**
     * @return true if this matrix is symmetric (that is, if its values are mirrored around the
     *         diagonal such that it is equal to its own transposition)
     */
    public boolean isSymmetric();

    /**
     * Not terribly useful mathematically, but useful as a simple single statistic about a matrix
     * for unit testing (at least until we bite the bullet and implement a determinant function).
     * Note that we don't test this method, since it's not really meaningful anyway.
     * 
     * TODO: Implement determinant instead.
     * 
     * @return the sum of all entries in this matrix
     */
    public float sum();

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
    public static class Factory
    {
        private final static String ATTRIBUTE_TYPE = "type";
        private final static String ATTRIBUTE_ROWS = "rows";
        private final static String ATTRIBUTE_COLUMNS = "columns";
        private final static String ATTRIBUTE_SYMMETRIC = "symmetric";
        /** Used for fixed-point implementations */
        private final static String ATTRIBUTE_PRECISION = "precision";

        public final static String ATTRIBUTE_TYPE_INT = "int";
        public final static String ATTRIBUTE_TYPE_FLOAT = "float";
        public final static String ATTRIBUTE_TYPE_FIXED_POINT_SHORT = "fixed-point-short";

        /**
         * Returns a new {@link IntMatrix} of m rows x n columns, in which all cells are initialized
         * to the specified initial value.
         * 
         * @param m rows
         * @param n columns
         * @param initialValue cell initialization value
         * @return IntMatrix
         */
        public static IntMatrix newIntMatrix(final int m, final int n, final int initialValue)
        {
            IntMatrix matrix = new IntMatrix(new int[m][n]);
            for (int i = 0; i < m; i++)
            {
                matrix.setRow(i, initialValue);
            }
            return matrix;
        }

        /**
         * @param m matrix size
         * @param defaultValue
         * @param identityValue
         * @return an IntMatrix in which all cells such that i == j are pre-populated with
         *         identityValue and all others with defaultValue
         */
        public static IntMatrix newIdentityIntMatrix(final int m, final int defaultValue, final int identityValue)
        {
            int[][] array = new int[m][m];
            for (int i = 0; i < m; i++)
            {
                Arrays.fill(array[i], defaultValue);
                array[i][i] = identityValue;
            }
            return new IntMatrix(array, false);
        }

        /**
         * @param m matrix size
         * @param defaultValue
         * @param identityValue
         * @return a FloatMatrix in which all cells such that i == j are pre-populated with
         *         identityValue and all others with defaultValue
         */
        public static FloatMatrix newIdentityFloatMatrix(final int m, final float defaultValue,
            final float identityValue)
        {
            float[][] array = new float[m][m];
            for (int i = 0; i < m; i++)
            {
                Arrays.fill(array[i], defaultValue);
                array[i][i] = identityValue;
            }
            return new FloatMatrix(array, false);
        }

        /**
         * @param m matrix size
         * @param defaultValue
         * @param identityValue
         * @return a symmetric FloatMatrix in which all cells such that i == j are pre-populated
         *         with identityValue and all others with defaultValue
         */
        public static FloatMatrix newSymmetricIdentityFloatMatrix(final int m, final float defaultValue,
            final float identityValue)
        {
            FloatMatrix matrix = new FloatMatrix(m, m, true);
            for (int i = 0; i < m; i++)
            {
                matrix.setRow(i, defaultValue);
                matrix.set(i, i, identityValue);
            }
            return matrix;
        }

        public static Matrix read(String s) throws IOException
        {
            return read(new StringReader(s));
        }

        public static Matrix read(File f) throws IOException
        {
            InputStream is = new FileInputStream(f);
            if (f.getName().endsWith(".gz"))
            {
                is = new GZIPInputStream(is);
            }
            return read(is);
        }

        public static Matrix read(InputStream inputStream) throws IOException
        {
            return read(new InputStreamReader(inputStream));
        }

        public static Matrix read(Reader reader) throws IOException
        {
            BufferedReader br = new BufferedReader(reader);
            Map<String, String> attributes = Strings.headerAttributes(br.readLine());

            String type = attributes.get(ATTRIBUTE_TYPE);

            int rows = Integer.parseInt(attributes.get(ATTRIBUTE_ROWS));
            int columns = Integer.parseInt(attributes.get(ATTRIBUTE_COLUMNS));

            boolean symmetric = attributes.containsKey(ATTRIBUTE_SYMMETRIC) ? Boolean.parseBoolean(attributes
                .get(ATTRIBUTE_SYMMETRIC)) : false;

            Matrix matrix = null;
            if (type.equals(ATTRIBUTE_TYPE_INT))
            {
                matrix = new IntMatrix(rows, columns, symmetric);
            }
            else if (type.equals(ATTRIBUTE_TYPE_FLOAT))
            {
                matrix = new FloatMatrix(rows, columns, symmetric);
            }
            else if (type.equals(ATTRIBUTE_TYPE_FIXED_POINT_SHORT))
            {
                int precision = Integer.parseInt(attributes.get(ATTRIBUTE_PRECISION));
                matrix = new FixedPointShortMatrix(rows, columns, precision, symmetric);
            }
            else if (type.equals("double"))
            {
                throw new RuntimeException(type + " type is not supported");
            }
            else
            {
                throw new RuntimeException(type + " type is not supported");
            }

            // Read and initialize matrix
            for (int i = 0; i < rows; i++)
            {
                String line = br.readLine();
                String[] split = line.split(" +");
                for (int j = 0; j < split.length; j++)
                {
                    matrix.set(i, j, split[j]);
                }
            }

            return matrix;

        }
    }
}

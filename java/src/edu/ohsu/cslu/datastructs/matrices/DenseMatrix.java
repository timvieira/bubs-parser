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

public interface DenseMatrix extends Matrix {

    /**
     * Retrieves a row. Note that {@link Matrix} implementations may return a copy of the actual row or may return a
     * reference to the row itself. Consumers should make their own copy before altering the returned array. Read-only
     * access is safe.
     * 
     * @param i row
     * @return the row as an array of ints
     */
    public int[] getIntRow(final int i);

    /**
     * Retrieves a row. Note that {@link Matrix} implementations may return a copy of the actual row or may return a
     * reference to the row itself. Consumers should make their own copy before altering the returned array. Read-only
     * access is safe.
     * 
     * @param i row
     * @return the row as an array of floats
     */
    public float[] getRow(final int i);

    /**
     * Retrieves a column. Note that {@link Matrix} implementations may return a copy of the actual row or may return a
     * reference to the row itself. Consumers should make their own copy before altering the returned array. Read-only
     * access is safe.
     * 
     * @param j column
     * @return the column as an array of ints
     */
    public int[] getIntColumn(final int j);

    /**
     * Retrieves a column. Note that {@link Matrix} implementations may return a copy of the actual row or may return a
     * reference to the row itself. Consumers should make their own copy before altering the returned array. Read-only
     * access is safe.
     * 
     * @param j column
     * @return the column as an array of floats
     */
    public float[] getColumn(final int j);

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
     * Get a submatrix.
     * 
     * @param i0 Initial row index, inclusive.
     * @param i1 Final row index, inclusive
     * @param j0 Initial column index, inclusive
     * @param j1 Final column index, inclusive
     * @return A(i0:i1,j0:j1)
     * 
     * @throws ArrayIndexOutOfBoundsException if a submatrix index is outside of this matrix
     */
    public DenseMatrix subMatrix(final int i0, final int i1, final int j0, final int j1);

    /**
     * Type-strengthen return type.
     */
    public DenseMatrix transpose();

    /**
     * Type-strengthen return type.
     */
    public DenseMatrix clone();

}

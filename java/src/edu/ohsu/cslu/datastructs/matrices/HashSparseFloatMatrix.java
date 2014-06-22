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

import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

/**
 * Stores a sparse float matrix in a hashtable.
 * 
 * @author Aaron Dunlop
 * @since Dec 16, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class HashSparseFloatMatrix extends BaseMatrix implements SparseMatrix {

    private static final long serialVersionUID = 1L;

    private Long2ObjectOpenHashMap<Long2FloatOpenHashMap> maps;
    private float defaultValue;

    HashSparseFloatMatrix(final int m, final int n, final boolean symmetric, final float defaultValue) {
        super(m, n, symmetric);
        maps = new Long2ObjectOpenHashMap<Long2FloatOpenHashMap>();
        this.defaultValue = defaultValue;
    }

    HashSparseFloatMatrix(final int m, final int n, final boolean symmetric) {
        this(m, n, symmetric, 0);
    }

    private HashSparseFloatMatrix(final int m, final int n, final boolean symmetric,
            final Long2ObjectOpenHashMap<Long2FloatOpenHashMap> maps, final float defaultValue) {
        this(m, n, symmetric, defaultValue);
        this.maps = maps;
    }

    HashSparseFloatMatrix(final float[][] matrix, final boolean symmetric) {
        this(matrix.length, matrix[matrix.length - 1].length, symmetric);
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                set(i, j, matrix[i][j]);
            }
        }
    }

    /**
     * Type-strengthen {@link Matrix#clone()}
     */
    @Override
    public HashSparseFloatMatrix clone() {
        final Long2ObjectOpenHashMap<Long2FloatOpenHashMap> newMaps = new Long2ObjectOpenHashMap<Long2FloatOpenHashMap>();
        for (final long key : maps.keySet()) {
            newMaps.put(key, maps.get(key).clone());
        }
        return new HashSparseFloatMatrix(m, n, symmetric, newMaps, defaultValue);
    }

    public float getFloat(final long i, final long j) {
        if (symmetric && j > i) {
            return getFloat(j, i);
        }
        final Long2FloatOpenHashMap map = maps.get(i);
        return map == null ? defaultValue : map.get(j);
    }

    @Override
    public float getFloat(final int i, final int j) {
        return getFloat((long) i, (long) j);
    }

    @Override
    public int getInt(final int i, final int j) {
        return Math.round(getFloat(i, j));
    }

    public void set(final long i, final long j, final float value) {
        if (symmetric && j > i) {
            set(j, i, value);
        }
        Long2FloatOpenHashMap map = maps.get(i);
        if (map == null) {
            map = new Long2FloatOpenHashMap();
            maps.put(i, map);
        }
        map.put(j, value);
    }

    @Override
    public void set(final int i, final int j, final float value) {
        set((long) i, (long) j, value);
    }

    @Override
    public void set(final int i, final int j, final int value) {
        set(i, j, (float) value);
    }

    public void set(final long i, final long j, final String value) {
        set(i, j, Float.parseFloat(value));
    }

    @Override
    public void set(final int i, final int j, final String value) {
        set((long) i, (long) j, Float.parseFloat(value));
    }

    @Override
    public float infinity() {
        return Float.POSITIVE_INFINITY;
    }

    @Override
    public float negativeInfinity() {
        return Float.NEGATIVE_INFINITY;
    }

    public Matrix add(final Matrix addend) {

        if (addend.rows() != rows() || addend.columns() != columns()) {
            throw new IllegalArgumentException("Matrix dimensions must match");
        }

        // TODO Handle adding a dense matrix to a sparse matrix
        if (!(addend instanceof HashSparseFloatMatrix)) {
            throw new UnsupportedOperationException("Adding " + addend.getClass()
                    + " to HashSparseFloatMatrix is not currently supported");
        }

        final HashSparseFloatMatrix sparseAddend = (HashSparseFloatMatrix) addend;
        final HashSparseFloatMatrix sum = new HashSparseFloatMatrix(rows(), columns(), isSymmetric()
                && addend.isSymmetric());

        // Iterate over populated entries in both matrices
        for (final long row : maps.keySet()) {
            final Long2FloatOpenHashMap columnMap = maps.get(row);
            for (final long column : columnMap.keySet()) {
                sum.set(row, column, getFloat(row, column) + sparseAddend.getFloat(row, column));
            }
        }

        // We already covered all entries populated in this matrix, but need to add any present in the addend which
        // aren't populated in this
        for (final long row : sparseAddend.maps.keySet()) {
            final Long2FloatOpenHashMap thisColumnMap = maps.get(row);
            final Long2FloatOpenHashMap addendColumnMap = sparseAddend.maps.get(row);
            for (final long column : addendColumnMap.keySet()) {
                if (thisColumnMap == null || !thisColumnMap.containsKey(column)) {
                    sum.set(row, column, sparseAddend.getFloat(row, column));
                }
            }
        }

        return sum;
    }

    @Override
    public DenseMatrix scalarAdd(final float addend) {
        final DenseMatrix newMatrix = new FloatMatrix(m, n, symmetric);
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                newMatrix.set(i, j, getFloat(i, j) + addend);
            }
        }
        return newMatrix;
    }

    @Override
    public DenseMatrix scalarAdd(final int addend) {
        return scalarAdd((float) addend);
    }

    /**
     * Type-strengthen {@link Matrix#transpose()}
     */
    @Override
    public HashSparseFloatMatrix transpose() {
        if (isSymmetric() || n == 0) {
            return clone();
        }

        final HashSparseFloatMatrix transposed = new HashSparseFloatMatrix(n, m, false);

        final long[] rows = maps.keySet().toLongArray();
        Arrays.sort(rows);

        // Write Matrix contents
        for (int i = 0; i < rows.length; i++) {
            final long row = rows[i];
            final Long2FloatOpenHashMap map = maps.get(row);
            final long[] columns = map.keySet().toLongArray();
            Arrays.sort(columns);

            for (int j = 0; j < columns.length; j++) {
                transposed.set(j, i, getFloat(i, j));
            }
        }
        return transposed;
    }

    @Override
    public void write(final Writer writer) throws IOException {
        write(writer, 4);
    }

    public void write(final Writer writer, final int precision) throws IOException {
        // Header line:
        writer.write(String.format("matrix type=hash-sparse-float sparse=true rows=%d columns=%d symmetric=%s\n", m, n,
                symmetric));

        // Length of each cell = 'maximum number of digits to the left of the decimal' + precision +
        // decimal
        final int length = Integer.toString((int) max()).length() + precision + 1;
        final String format = "%-" + length + "." + precision + "f";

        final long[] rows = maps.keySet().toLongArray();
        Arrays.sort(rows);

        // Write Matrix contents
        for (int i = 0; i < rows.length; i++) {
            final long row = rows[i];
            final Long2FloatOpenHashMap map = maps.get(row);
            final long[] columns = map.keySet().toLongArray();
            Arrays.sort(columns);

            for (int j = 0; j < columns.length - 1; j++) {
                writer.write(String.format(format, getFloat(rows[i], columns[j])));
                writer.write(' ');
            }
            writer.write(String.format(format, getFloat(rows[i], columns[columns.length - 1])).trim());
            writer.write('\n');
        }
        writer.flush();
    }
}

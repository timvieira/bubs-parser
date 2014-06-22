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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.cjunit.FilteredRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit tests for {@link ByteMatrix}
 * 
 * @author Aaron Dunlop
 * @since May 6, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
@RunWith(FilteredRunner.class)
public class TestByteMatrix extends DenseIntMatrixTestCase {

    @Override
    protected String matrixType() {
        return "byte";
    }

    @Override
    protected Matrix create(final float[][] array, final boolean symmetric) {
        final byte[][] byteArray = new byte[array.length][];
        for (int i = 0; i < array.length; i++) {
            byteArray[i] = new byte[array[i].length];
            for (int j = 0; j < array[i].length; j++) {
                byteArray[i][j] = (byte) Math.round(array[i][j]);
            }
        }
        return new ByteMatrix(byteArray);
    }

    @Override
    protected Matrix create(final int[][] array, final boolean symmetric) {
        final byte[][] byteArray = new byte[array.length][];
        for (int i = 0; i < array.length; i++) {
            byteArray[i] = new byte[array[i].length];
            for (int j = 0; j < array[i].length; j++) {
                byteArray[i][j] = (byte) Math.round(array[i][j]);
            }
        }
        return new ByteMatrix(byteArray, symmetric);
    }

    /**
     * Tests scalar multiplication. Overrides implementation in {@link IntMatrixTestCase} because the multiplication
     * exceeded the range of a byte.
     * 
     * @throws Exception if something bad happens
     */
    @Override
    @Test
    public void testScalarMultiply() throws Exception {
        Matrix m = sampleMatrix.scalarMultiply(3);
        assertEquals(matrixClass, m.getClass());
        assertEquals(-22, m.getInt(1, 2));
        assertEquals(36, m.getInt(2, 3));

        m = sampleMatrix.scalarMultiply(3.6f);
        assertEquals(FloatMatrix.class, m.getClass());
        assertEquals(280.8, m.getFloat(1, 2), .01f);
        assertEquals(43.2, m.getFloat(2, 3), .01f);
    }

    /**
     * Override, since adding sampleMatrix to itself overflows a byte
     */
    @Override
    @Test
    public void testAdd() {
        // Verify that add() throws IllegalArgumentException if attempting to add matrices of different dimensions
        try {
            sampleMatrix.add(symmetricMatrix);
            fail("Expected IllegalArgumentException");
        } catch (final IllegalArgumentException expected) {
        }

        final byte[][] tmp = new byte[sampleMatrix.rows()][sampleMatrix.columns()];
        for (int i = 0; i < tmp.length; i++) {
            Arrays.fill(tmp[i], (byte) -1);
        }
        final Matrix addend = new ByteMatrix(tmp);

        // Add sampleMatrix to itself
        final Matrix sum = sampleMatrix.add(addend);
        assertTrue("Expected " + sampleMatrix.getClass(), sum.getClass() == sampleMatrix.getClass());
        for (int i = 0; i < sampleMatrix.rows(); i++) {
            for (int j = 0; j < sampleMatrix.columns(); j++) {
                assertEquals(sampleMatrix.getInt(i, j) - 1, sum.getInt(i, j));
            }
        }
    }

    @Override
    public void testInfinity() throws Exception {
        assertEquals(Byte.MAX_VALUE, sampleMatrix.infinity(), .01f);
    }

    @Override
    public void testNegativeInfinity() throws Exception {
        assertEquals(Byte.MIN_VALUE, sampleMatrix.negativeInfinity(), .01f);
    }
}

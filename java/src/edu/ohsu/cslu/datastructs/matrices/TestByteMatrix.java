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

import static junit.framework.Assert.assertEquals;

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
    protected Matrix create(final float[][] array, boolean symmetric) {
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

    @Override
    public void testInfinity() throws Exception {
        assertEquals(Byte.MAX_VALUE, sampleMatrix.infinity(), .01f);
    }

    @Override
    public void testNegativeInfinity() throws Exception {
        assertEquals(Byte.MIN_VALUE, sampleMatrix.negativeInfinity(), .01f);
    }
}

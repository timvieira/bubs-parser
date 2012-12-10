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
import org.junit.runner.RunWith;

/**
 * Unit Tests for {@link ShortMatrix}.
 * 
 * @author Aaron Dunlop
 * @since May 6, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
@RunWith(FilteredRunner.class)
public class TestShortMatrix extends DenseIntMatrixTestCase {

    @Override
    protected String matrixType() {
        return "short";
    }

    @Override
    protected Matrix create(final float[][] array, boolean symmetric) {
        final short[][] shortArray = new short[array.length][array[0].length];
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[0].length; j++) {
                shortArray[i][j] = (short) Math.round(array[i][j]);
            }
        }
        return new ShortMatrix(shortArray);
    }

    @Override
    protected Matrix create(final int[][] array, final boolean symmetric) {
        final short[][] shortArray = new short[array.length][];
        for (int i = 0; i < array.length; i++) {
            shortArray[i] = new short[array[i].length];
            for (int j = 0; j < array[i].length; j++) {
                shortArray[i][j] = (short) Math.round(array[i][j]);
            }
        }
        return new ShortMatrix(shortArray, symmetric);
    }

    @Override
    public void testInfinity() throws Exception {
        assertEquals(Short.MAX_VALUE, sampleMatrix.infinity(), .01f);
    }

    @Override
    public void testNegativeInfinity() throws Exception {
        assertEquals(Short.MIN_VALUE, sampleMatrix.negativeInfinity(), .01f);
    }
}

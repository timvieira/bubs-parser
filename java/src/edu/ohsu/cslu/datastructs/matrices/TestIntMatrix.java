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

import org.cjunit.FilteredRunner;
import org.junit.runner.RunWith;

/**
 * Tests for the IntMatrix class
 * 
 * @author Aaron Dunlop
 * @since Sep 18, 2008
 * 
 *        $Id$
 */
@RunWith(FilteredRunner.class)
public class TestIntMatrix extends DenseIntMatrixTestCase {

    @Override
    protected String matrixType() {
        return "int";
    }

    @Override
    protected Matrix create(final float[][] array, boolean symmetric) {
        final int[][] intArray = new int[array.length][array[0].length];
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[0].length; j++) {
                intArray[i][j] = Math.round(array[i][j]);
            }
        }
        return new IntMatrix(intArray);
    }

    @Override
    protected Matrix create(final int[][] array, final boolean symmetric) {
        return new IntMatrix(array, symmetric);
    }

    @Override
    public void testInfinity() throws Exception {
        assertEquals(Integer.MAX_VALUE, sampleMatrix.infinity(), .01f);
    }

    @Override
    public void testNegativeInfinity() throws Exception {
        assertEquals(Integer.MIN_VALUE, sampleMatrix.negativeInfinity(), .01f);
    }
}

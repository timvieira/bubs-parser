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
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the {@link FloatMatrix} class
 * 
 * @author Aaron Dunlop
 * @since Sep 18, 2008
 * 
 *        $Id$
 */
@RunWith(FilteredRunner.class)
public class TestFloatMatrix extends DenseFloatingPointMatrixTestCase {

    @Override
    protected String matrixType() {
        return "float";
    }

    @Override
    protected Matrix create(final float[][] array, final boolean symmetric) {
        return new FloatMatrix(array, symmetric);
    }

    @Override
    public void testInfinity() throws Exception {
        assertEquals(Float.POSITIVE_INFINITY, sampleMatrix.infinity(), .01f);
        assertEquals(Float.POSITIVE_INFINITY, sampleMatrix2.infinity(), .01f);
    }

    @Override
    public void testNegativeInfinity() throws Exception {
        assertEquals(Float.NEGATIVE_INFINITY, sampleMatrix.negativeInfinity(), .01f);
        assertEquals(Float.NEGATIVE_INFINITY, sampleMatrix2.negativeInfinity(), .01f);
    }

    /**
     * Tests equals() method
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testEquals() throws Exception {
        assertEquals(sampleMatrix2, Matrix.Factory.read(sampleMatrix2.toString()));
        assertEquals(symmetricMatrix2, Matrix.Factory.read(symmetricMatrix2.toString()));
    }
}

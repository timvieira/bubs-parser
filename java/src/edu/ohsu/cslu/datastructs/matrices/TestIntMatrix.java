package edu.ohsu.cslu.datastructs.matrices;

import static junit.framework.Assert.assertEquals;

import org.junit.runner.RunWith;

import edu.ohsu.cslu.tests.FilteredRunner;

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

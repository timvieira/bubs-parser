package edu.ohsu.cslu.datastructs.matrices;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.tests.FilteredRunner;

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
     * @throws Exception
     *             if something bad happens
     */
    @Test
    public void testEquals() throws Exception {
        assertEquals(sampleMatrix2, Matrix.Factory.read(sampleMatrix2.toString()));
        assertEquals(symmetricMatrix2, Matrix.Factory.read(symmetricMatrix2.toString()));
    }
}

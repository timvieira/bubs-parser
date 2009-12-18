package edu.ohsu.cslu.datastructs.matrices;

import static org.junit.Assert.assertEquals;

public class TestHashSparseFloatMatrix extends SparseFloatingPointMatrixTestCase {

    @Override
    protected String matrixType() {
        return "hash-sparse-float sparse=true";
    }

    @Override
    protected Matrix create(final float[][] array, final boolean symmetric) {
        return new HashSparseFloatMatrix(array, symmetric);
    }

    @Override
    public void testInfinity() throws Exception {
        assertEquals(Float.POSITIVE_INFINITY, sampleMatrix.infinity(), 0.01f);
    }

    @Override
    public void testNegativeInfinity() throws Exception {
        assertEquals(Float.NEGATIVE_INFINITY, sampleMatrix.negativeInfinity(), 0.01f);
    }
}

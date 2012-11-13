package edu.ohsu.cslu.datastructs.vectors;

import static junit.framework.Assert.assertEquals;

public class TestMutableLargeSparseBitVector extends BitVectorTestCase<MutableLargeSparseBitVector> {

    @Override
    protected String serializedName() {
        return "mutable-large-sparse-bit";
    }

    @Override
    public void testLength() throws Exception {
        super.testLength();

        // Add another element and re-test length
        ((MutableLargeSparseBitVector) sampleVector).add(56);
        assertEquals("Wrong length", 57, sampleVector.length());
    }
}

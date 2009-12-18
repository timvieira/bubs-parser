package edu.ohsu.cslu.datastructs.vectors;

import org.junit.Before;

import static junit.framework.Assert.assertEquals;

/**
 * Unit test for {@link MutableSparseBitVector}
 * 
 * @author Aaron Dunlop
 * @since May 28, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestMutableSparseBitVector extends BitVectorTestCase {

    @Override
    @Before
    public void setUp() throws Exception {
        final StringBuilder sb = new StringBuilder();
        sb.append("vector type=mutable-sparse-bit length=35 sparse=true\n");
        sb
            .append("0 1 2 1 3 1 4 1 7 1 8 1 10 1 11 1 12 1 14 1 18 1 20 1 21 1 23 1 24 1 25 1 26 1 29 1 32 1 33 1 34 1\n");
        stringSampleVector = sb.toString();

        final int[] sampleArray = new int[] { 0, 1, 2, 1, 3, 1, 4, 1, 7, 1, 8, 1, 10, 1, 11, 1, 12, 1, 14, 1,
                18, 1, 20, 1, 21, 1, 23, 1, 24, 1, 25, 1, 26, 1, 29, 1, 32, 1, 33, 1, 34, 1 };
        sampleVector = new MutableSparseBitVector(sampleArray);

        vectorClass = MutableSparseBitVector.class;
        elementwiseMultiplyResultClass = MutableSparseBitVector.class;
    }

    @Override
    protected BitVector createEmptyBitVector() {
        return new MutableSparseBitVector();
    }

    @Override
    public void testLength() throws Exception {
        assertEquals("Wrong length", 35, sampleVector.length());

        // If we add
        ((MutableSparseBitVector) sampleVector).add(56);
        assertEquals("Wrong length", 57, sampleVector.length());
    }

}

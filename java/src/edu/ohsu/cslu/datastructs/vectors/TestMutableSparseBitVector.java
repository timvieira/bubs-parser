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
public class TestMutableSparseBitVector extends BitVectorTestCase
{
    @Override
    @Before
    public void setUp() throws Exception
    {
        StringBuilder sb = new StringBuilder();
        sb.append("vector type=mutable-sparse-bit length=35\n");
        sb.append("0 2 3 4 7 8 10 11 12 14 18 20 21 23 24 25 26 29 32 33 34\n");
        stringSampleVector = sb.toString();

        int[] sampleArray = new int[] {0, 2, 3, 4, 7, 8, 10, 11, 12, 14, 18, 20, 21, 23, 24, 25, 26, 29, 32, 33, 34};
        sampleVector = new MutableSparseBitVector(sampleArray);

        vectorClass = MutableSparseBitVector.class;
        elementwiseMultiplyResultClass = MutableSparseBitVector.class;
    }

    @Override
    protected BitVector createEmptyBitVector()
    {
        return new MutableSparseBitVector();
    }

    @Override
    public void testLength() throws Exception
    {
        assertEquals("Wrong length", 35, sampleVector.length());

        // If we add
        ((MutableSparseBitVector) sampleVector).add(56);
        assertEquals("Wrong length", 57, sampleVector.length());
    }

}

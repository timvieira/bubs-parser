package edu.ohsu.cslu.math.linear;

import org.junit.Before;

/**
 * Unit tests for {@link SparseBitVector}.
 * 
 * @author Aaron Dunlop
 * @since Mar 23, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestSparseBitVector extends BitVectorTestCase
{
    @Override
    @Before
    public void setUp() throws Exception
    {
        StringBuilder sb = new StringBuilder();
        sb.append("vector type=sparse-bit length=35\n");
        sb.append("1 0 1 1 1 0 0 1 1 0 1 1 1 0 1 0 0 0 1 0 1 1 0 1 1 1 1 0 0 1 0 0 1 1 1\n");
        stringSampleVector = sb.toString();

        int[] sampleArray = new int[] {1, 0, 1, 1, 1, 0, 0, 1, 1, 0, 1, 1, 1, 0, 1, 0, 0, 0, 1, 0, 1, 1, 0, 1, 1, 1, 1,
                                       0, 0, 1, 0, 0, 1, 1, 1};
        sampleVector = new SparseBitVector(sampleArray);

        vectorClass = SparseBitVector.class;
    }

    @Override
    protected BitVector createEmptyBitVector()
    {
        return new SparseBitVector();
    }
}

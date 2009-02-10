package edu.ohsu.cslu.math.linear;

import org.junit.Before;

import static junit.framework.Assert.assertEquals;

/**
 * Unit tests for {@link IntVector}
 * 
 * @author Aaron Dunlop
 * @since Dec 11, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestIntVector extends IntVectorTestCase
{
    private int[] sampleArray;

    @Override
    @Before
    public void setUp() throws Exception
    {
        StringBuilder sb = new StringBuilder();
        sb.append("vector type=int length=11\n");
        sb.append("-11 0 11 22 33 44 56 67 78 89 100\n");
        stringSampleVector = sb.toString();

        sampleArray = new int[] {-11, 0, 11, 22, 33, 44, 56, 67, 78, 89, 100};
        sampleVector = new IntVector(sampleArray);

        vectorClass = IntVector.class;
    }

    @Override
    protected Vector create(float[] array)
    {
        int[] intArray = new int[array.length];
        for (int i = 0; i < array.length; i++)
        {
            intArray[i] = Math.round(array[i]);
        }
        return new IntVector(intArray);
    }

    @Override
    public void testInfinity() throws Exception
    {
        assertEquals(Integer.MAX_VALUE, sampleVector.infinity(), .01f);
    }

    @Override
    public void testNegativeInfinity() throws Exception
    {
        assertEquals(Integer.MIN_VALUE, sampleVector.negativeInfinity(), .01f);
    }
}

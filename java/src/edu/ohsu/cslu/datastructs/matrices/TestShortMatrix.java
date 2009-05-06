package edu.ohsu.cslu.datastructs.matrices;

import static junit.framework.Assert.assertEquals;

/**
 * Unit Tests for {@link ShortMatrix}.
 * 
 * @author Aaron Dunlop
 * @since May 6, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestShortMatrix extends IntMatrixTestCase
{
    @Override
    protected String matrixType()
    {
        return "short";
    }

    @Override
    protected Matrix create(float[][] array)
    {
        short[][] shortArray = new short[array.length][array[0].length];
        for (int i = 0; i < array.length; i++)
        {
            for (int j = 0; j < array[0].length; j++)
            {
                shortArray[i][j] = (short) Math.round(array[i][j]);
            }
        }
        return new ShortMatrix(shortArray);
    }

    @Override
    protected Matrix create(int[][] array, boolean symmetric)
    {
        short[][] shortArray = new short[array.length][];
        for (int i = 0; i < array.length; i++)
        {
            shortArray[i] = new short[array[i].length];
            for (int j = 0; j < array[i].length; j++)
            {
                shortArray[i][j] = (short) Math.round(array[i][j]);
            }
        }
        return new ShortMatrix(shortArray, symmetric);
    }

    @Override
    public void testInfinity() throws Exception
    {
        assertEquals(Short.MAX_VALUE, sampleMatrix.infinity(), .01f);
    }

    @Override
    public void testNegativeInfinity() throws Exception
    {
        assertEquals(Short.MIN_VALUE, sampleMatrix.negativeInfinity(), .01f);
    }
}

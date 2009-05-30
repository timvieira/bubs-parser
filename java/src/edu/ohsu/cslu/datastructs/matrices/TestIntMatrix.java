package edu.ohsu.cslu.datastructs.matrices;

import org.junit.runner.RunWith;

import edu.ohsu.cslu.tests.FilteredRunner;

import static junit.framework.Assert.assertEquals;

/**
 * Tests for the IntMatrix class
 * 
 * @author Aaron Dunlop
 * @since Sep 18, 2008
 * 
 *        $Id$
 */
@RunWith(FilteredRunner.class)
public class TestIntMatrix extends IntMatrixTestCase
{
    @Override
    protected String matrixType()
    {
        return "int";
    }

    @Override
    protected Matrix create(float[][] array)
    {
        int[][] intArray = new int[array.length][array[0].length];
        for (int i = 0; i < array.length; i++)
        {
            for (int j = 0; j < array[0].length; j++)
            {
                intArray[i][j] = Math.round(array[i][j]);
            }
        }
        return new IntMatrix(intArray);
    }

    @Override
    protected Matrix create(int[][] array, boolean symmetric)
    {
        return new IntMatrix(array, symmetric);
    }

    @Override
    public void testInfinity() throws Exception
    {
        assertEquals(Integer.MAX_VALUE, sampleMatrix.infinity(), .01f);
    }

    @Override
    public void testNegativeInfinity() throws Exception
    {
        assertEquals(Integer.MIN_VALUE, sampleMatrix.negativeInfinity(), .01f);
    }
}

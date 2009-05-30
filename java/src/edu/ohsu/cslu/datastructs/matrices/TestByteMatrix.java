package edu.ohsu.cslu.datastructs.matrices;

import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.tests.FilteredRunner;

import static junit.framework.Assert.assertEquals;

/**
 * Unit tests for {@link ByteMatrix}
 * 
 * @author Aaron Dunlop
 * @since May 6, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
@RunWith(FilteredRunner.class)
public class TestByteMatrix extends IntMatrixTestCase
{
    @Override
    protected String matrixType()
    {
        return "byte";
    }

    @Override
    protected Matrix create(float[][] array)
    {
        byte[][] byteArray = new byte[array.length][];
        for (int i = 0; i < array.length; i++)
        {
            byteArray[i] = new byte[array[i].length];
            for (int j = 0; j < array[i].length; j++)
            {
                byteArray[i][j] = (byte) Math.round(array[i][j]);
            }
        }
        return new ByteMatrix(byteArray);
    }

    @Override
    protected Matrix create(int[][] array, boolean symmetric)
    {
        byte[][] byteArray = new byte[array.length][];
        for (int i = 0; i < array.length; i++)
        {
            byteArray[i] = new byte[array[i].length];
            for (int j = 0; j < array[i].length; j++)
            {
                byteArray[i][j] = (byte) Math.round(array[i][j]);
            }
        }
        return new ByteMatrix(byteArray, symmetric);
    }

    /**
     * Tests scalar multiplication. Overrides implementation in {@link IntMatrixTestCase} because
     * the multiplication exceeded the range of a byte.
     * 
     * @throws Exception if something bad happens
     */
    @Override
    @Test
    public void testScalarMultiply() throws Exception
    {
        Matrix m = sampleMatrix.scalarMultiply(3);
        assertEquals(matrixClass, m.getClass());
        assertEquals(-22, m.getInt(1, 2));
        assertEquals(36, m.getInt(2, 3));

        m = sampleMatrix.scalarMultiply(3.6f);
        assertEquals(FloatMatrix.class, m.getClass());
        assertEquals(280.8, m.getFloat(1, 2), .01f);
        assertEquals(43.2, m.getFloat(2, 3), .01f);
    }

    @Override
    public void testInfinity() throws Exception
    {
        assertEquals(Byte.MAX_VALUE, sampleMatrix.infinity(), .01f);
    }

    @Override
    public void testNegativeInfinity() throws Exception
    {
        assertEquals(Byte.MIN_VALUE, sampleMatrix.negativeInfinity(), .01f);
    }
}

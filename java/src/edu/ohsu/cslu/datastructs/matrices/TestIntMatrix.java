package edu.ohsu.cslu.datastructs.matrices;

import org.junit.Before;
import org.junit.Test;

import edu.ohsu.cslu.tests.SharedNlpTests;


import static junit.framework.Assert.assertEquals;

/**
 * Tests for the IntMatrix class
 * 
 * @author Aaron Dunlop
 * @since Sep 18, 2008
 * 
 *        $Id$
 */
public class TestIntMatrix extends MatrixTestCase
{
    private int[][] sampleArray;
    private int[][] symmetricArray;

    @Before
    public void setUp() throws Exception
    {
        StringBuilder sb = new StringBuilder();
        sb.append("matrix type=int rows=3 columns=4 symmetric=false\n");
        sb.append("11  22  33  44\n");
        sb.append("56  67  78  89\n");
        sb.append("100 10  11  12\n");
        stringSampleMatrix = sb.toString();

        sampleArray = new int[][] { {11, 22, 33, 44}, {56, 67, 78, 89}, {100, 10, 11, 12}};
        sampleMatrix = new IntMatrix(sampleArray);

        sb = new StringBuilder();
        sb.append("matrix type=int rows=5 columns=5 symmetric=true\n");
        sb.append("0\n");
        sb.append("11  22\n");
        sb.append("33  44  56\n");
        sb.append("67  78  89  100\n");
        sb.append("10  11  12  13  14\n");
        stringSymmetricMatrix = sb.toString();

        symmetricArray = new int[][] { {0}, {11, 22}, {33, 44, 56}, {67, 78, 89, 100}, {10, 11, 12, 13, 14}};
        symmetricMatrix = new IntMatrix(symmetricArray, true);

        matrixClass = IntMatrix.class;
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

    /**
     * Tests deserializing a matrix using a Reader
     * 
     * TODO: Combine with MatrixTestCase.testReadFromReader?
     * 
     * @throws Exception if something bad happens
     */
    @Override
    @Test
    public void testReadfromReader() throws Exception
    {
        Matrix m1 = Matrix.Factory.read(stringSampleMatrix);
        assertEquals(matrixClass, m1.getClass());
        assertEquals(3, m1.rows());
        assertEquals(4, m1.columns());
        assertEquals(11, m1.getInt(0, 0));
        assertEquals(22, m1.getInt(0, 1));
        assertEquals(67, m1.getInt(1, 1));
        assertEquals(78, m1.getInt(1, 2));
        assertEquals(11, m1.getInt(2, 2));
        assertEquals(12, m1.getInt(2, 3));

        Matrix m2 = Matrix.Factory.read(stringSymmetricMatrix);
        assertEquals(matrixClass, m2.getClass());
        assertEquals("Wrong number of rows", 5, m2.rows());
        assertEquals("Wrong number of columns", 5, m2.columns());
        assertEquals("Wrong value", 0, m2.getInt(0, 0));
        assertEquals("Wrong value", 11, m2.getInt(1, 0));
        assertEquals("Wrong value", 67, m2.getInt(3, 0));
        assertEquals("Wrong value", 78, m2.getInt(3, 1));
        assertEquals("Wrong value", 100, m2.getInt(3, 3));
        assertEquals("Wrong value", 14, m2.getInt(4, 4));
    }

    /**
     * Tests 'getFloat', including reflection across the diagonal in symmetric matrices.
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testGetFloat() throws Exception
    {
        assertEquals("Wrong value", 11f, sampleMatrix.getFloat(0, 0), .0001f);
        assertEquals("Wrong value", 22f, sampleMatrix.getFloat(0, 1), .0001f);
        assertEquals("Wrong value", 56f, sampleMatrix.getFloat(1, 0), .0001f);
        assertEquals("Wrong value", 89f, sampleMatrix.getFloat(1, 3), .0001f);
        assertEquals("Wrong value", 100f, sampleMatrix.getFloat(2, 0), .0001f);
        assertEquals("Wrong value", 12f, sampleMatrix.getFloat(2, 3), .0001f);

        assertEquals("Wrong value", 0f, symmetricMatrix.getFloat(0, 0), .0001f);
        assertEquals("Wrong value", 11f, symmetricMatrix.getFloat(1, 0), .0001f);
        assertEquals("Wrong value", 67f, symmetricMatrix.getFloat(3, 0), .0001f);
        assertEquals("Wrong value", 78f, symmetricMatrix.getFloat(3, 1), .0001f);
        assertEquals("Wrong value", 100f, symmetricMatrix.getFloat(3, 3), .0001f);
        assertEquals("Wrong value", 14f, symmetricMatrix.getFloat(4, 4), .0001f);

        // And a couple values that are out of the storage area, but should be reflected about the
        // diagonal
        assertEquals("Wrong value", 67f, symmetricMatrix.getFloat(0, 3), .0001f);
        assertEquals("Wrong value", 78f, symmetricMatrix.getFloat(1, 3), .0001f);
        assertEquals("Wrong value", 89f, symmetricMatrix.getFloat(2, 3), .0001f);
    }

    /**
     * Tests setting matrix elements
     * 
     * @throws Exception if something bad happens
     */
    @Override
    @Test
    public void testSet() throws Exception
    {
        assertEquals("Wrong value", 100, sampleMatrix.getInt(2, 0));
        sampleMatrix.set(2, 0, 1);
        assertEquals("Wrong value", 1, sampleMatrix.getInt(2, 0));

        assertEquals("Wrong value", 67, symmetricMatrix.getInt(0, 3));
        symmetricMatrix.set(0, 3, 15);
        assertEquals("Wrong value", 15, symmetricMatrix.getInt(0, 3));

        // We expect float values to be rounded
        symmetricMatrix.set(0, 3, 13.6f);
        symmetricMatrix.set(1, 3, 13.4f);
        assertEquals("Wrong value", 14, symmetricMatrix.getInt(0, 3));
        assertEquals("Wrong value", 13, symmetricMatrix.getInt(1, 3));
    }

    @Override
    @Test
    public void testSetRow()
    {
        sampleMatrix.setRow(0, new int[] {13, 14, 15, 16});
        SharedNlpTests.assertEquals(new int[] {13, 14, 15, 16}, sampleMatrix.getIntRow(0));
        sampleMatrix.setRow(2, new int[] {0, 1, 2, 3});
        SharedNlpTests.assertEquals(new int[] {0, 1, 2, 3}, sampleMatrix.getIntRow(2));

        sampleMatrix.setRow(2, 3);
        SharedNlpTests.assertEquals(new int[] {3, 3, 3, 3}, sampleMatrix.getIntRow(2));

        symmetricMatrix.setRow(0, new int[] {1, 2, 3, 4, 5});
        SharedNlpTests.assertEquals(new int[] {1, 2, 3, 4, 5}, symmetricMatrix.getIntRow(0));
        symmetricMatrix.setRow(4, new int[] {10, 11, 12, 13, 14});
        SharedNlpTests.assertEquals(new int[] {10, 11, 12, 13, 14}, symmetricMatrix.getIntRow(4));

        symmetricMatrix.setRow(0, 10);
        SharedNlpTests.assertEquals(new int[] {10, 10, 10, 10, 10}, symmetricMatrix.getIntRow(0));
        symmetricMatrix.setRow(4, 15);
        SharedNlpTests.assertEquals(new int[] {15, 15, 15, 15, 15}, symmetricMatrix.getIntRow(4));
    }

    @Override
    @Test
    public void testSetColumn()
    {
        sampleMatrix.setColumn(0, new int[] {13, 14, 15});
        SharedNlpTests.assertEquals(new int[] {13, 14, 15}, sampleMatrix.getIntColumn(0));
        sampleMatrix.setColumn(2, new int[] {0, 1, 2});
        SharedNlpTests.assertEquals(new int[] {0, 1, 2}, sampleMatrix.getIntColumn(2));

        sampleMatrix.setColumn(2, 3);
        SharedNlpTests.assertEquals(new int[] {3, 3, 3}, sampleMatrix.getIntColumn(2));

        symmetricMatrix.setColumn(0, new int[] {1, 2, 3, 4, 5});
        SharedNlpTests.assertEquals(new int[] {1, 2, 3, 4, 5}, symmetricMatrix.getIntColumn(0));
        symmetricMatrix.setColumn(4, new int[] {10, 11, 12, 13, 14});
        SharedNlpTests.assertEquals(new int[] {10, 11, 12, 13, 14}, symmetricMatrix.getIntColumn(4));

        symmetricMatrix.setColumn(0, 10);
        SharedNlpTests.assertEquals(new int[] {10, 10, 10, 10, 10}, symmetricMatrix.getIntColumn(0));
        symmetricMatrix.setColumn(4, 15);
        SharedNlpTests.assertEquals(new int[] {15, 15, 15, 15, 15}, symmetricMatrix.getIntColumn(4));
    }

    @Override
    @Test
    public void testScalarAdd() throws Exception
    {
        Matrix m = sampleMatrix.scalarAdd(3);
        assertEquals(IntMatrix.class, m.getClass());
        assertEquals(81, m.getInt(1, 2));
        assertEquals(15, m.getInt(2, 3));

        m = sampleMatrix.scalarAdd(2.6f);
        assertEquals(FloatMatrix.class, m.getClass());
        assertEquals(80.6, m.getFloat(1, 2), .01f);
        assertEquals(14.6, m.getFloat(2, 3), .01f);
    }

    /**
     * Tests scalar multiplication
     * 
     * @throws Exception if something bad happens
     */
    @Override
    @Test
    public void testScalarMultiply() throws Exception
    {
        Matrix m = sampleMatrix.scalarMultiply(3);
        assertEquals(IntMatrix.class, m.getClass());
        assertEquals(234, m.getInt(1, 2));
        assertEquals(36, m.getInt(2, 3));

        m = sampleMatrix.scalarMultiply(3.6f);
        assertEquals(FloatMatrix.class, m.getClass());
        assertEquals(280.8, m.getFloat(1, 2), .01f);
        assertEquals(43.2, m.getFloat(2, 3), .01f);
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

    /**
     * Tests equals() method
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testEquals() throws Exception
    {
        assertEquals(sampleMatrix, new IntMatrix(sampleArray));
        assertEquals(symmetricMatrix, new IntMatrix(symmetricArray, true));
    }
}

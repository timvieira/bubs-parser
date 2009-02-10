package edu.ohsu.cslu.math.linear;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

/**
 * Unit tests for {@link FixedPointShortMatrix}
 * 
 * @author Aaron Dunlop
 * @since Oct 4, 2008
 * 
 *        $Id$
 */
public class TestFixedPointShortMatrix extends FloatingPointMatrixTestCase
{
    private float[][] sampleArray;
    private float[][] sampleArray2;

    private float[][] symmetricArray;
    private float[][] symmetricArray2;

    @Override
    protected Matrix create(float[][] array)
    {
        return new FixedPointShortMatrix(array, 2);
    }

    @Before
    public void setUp() throws Exception
    {
        StringBuilder sb = new StringBuilder();
        sb.append("matrix type=fixed-point-short precision=2 rows=3 columns=4 symmetric=false\n");
        sb.append("11.11 22.22 33.33 44.44\n");
        sb.append("55.55 66.66 77.77 88.88\n");
        sb.append("99.99 10.00 11.11 12.11\n");
        stringSampleMatrix = sb.toString();

        sampleArray = new float[][] { {11.11f, 22.22f, 33.33f, 44.44f}, {55.55f, 66.66f, 77.77f, 88.88f},
                                     {99.99f, 10.00f, 11.11f, 12.11f}};
        sampleMatrix = new FixedPointShortMatrix(sampleArray, 2);

        sb = new StringBuilder();
        sb.append("matrix type=fixed-point-short precision=4 rows=3 columns=4 symmetric=false\n");
        sb.append("0.1111 0.2222 0.3333 0.4444\n");
        sb.append("0.5555 0.6666 0.7777 0.8888\n");
        sb.append("0.9999 0.1000 0.1111 0.1222\n");
        stringSampleMatrix2 = sb.toString();

        sampleArray2 = new float[][] { {0.1111f, 0.2222f, 0.3333f, 0.4444f}, {0.5555f, 0.6666f, 0.7777f, 0.8888f},
                                      {0.9999f, 0.1000f, 0.1111f, 0.1222f}};
        sampleMatrix2 = new FixedPointShortMatrix(sampleArray2, 4);

        sb = new StringBuilder();
        sb.append("matrix type=fixed-point-short precision=2 rows=5 columns=5 symmetric=true\n");
        sb.append("0.00\n");
        sb.append("11.11 22.22\n");
        sb.append("33.33 44.44 55.55\n");
        sb.append("66.66 77.77 88.88 99.99\n");
        sb.append("10.00 11.11 12.22 13.33 14.44\n");
        stringSymmetricMatrix = sb.toString();

        symmetricArray = new float[][] { {0f}, {11.11f, 22.22f}, {33.33f, 44.44f, 55.55f},
                                        {66.66f, 77.77f, 88.88f, 99.99f}, {10.00f, 11.11f, 12.22f, 13.33f, 14.44f}};
        symmetricMatrix = new FixedPointShortMatrix(symmetricArray, 2, true);

        sb = new StringBuilder();
        sb.append("matrix type=fixed-point-short precision=4 rows=5 columns=5 symmetric=true\n");
        sb.append("0.0000\n");
        sb.append("0.1111 0.2222\n");
        sb.append("0.3333 0.4444 0.5555\n");
        sb.append("0.6666 0.7777 0.8888 0.9999\n");
        sb.append("0.1000 0.1111 0.1222 0.1333 0.1444\n");
        stringSymmetricMatrix2 = sb.toString();

        symmetricArray2 = new float[][] { {0}, {0.1111f, 0.2222f}, {0.3333f, 0.4444f, 0.5555f},
                                         {0.6666f, 0.7777f, 0.8888f, 0.9999f},
                                         {0.1000f, 0.1111f, 0.1222f, 0.1333f, 0.1444f}};
        symmetricMatrix2 = new FixedPointShortMatrix(symmetricArray2, 4, true);

        matrixClass = FixedPointShortMatrix.class;
    }

    /**
     * Tests constructing a FixedPointShortMatrix - specifically, verifies that out-of-range values
     * will throw {@link IllegalArgumentException}
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testConstructors() throws Exception
    {
        float[][] floatArray = new float[][] {{11.11f, 22.22f, 33.33f, 44.44f}};
        // This should work
        new FixedPointShortMatrix(floatArray, 2);

        // But 33.33 and 44.44 are out-of-range for a matrix of precision 4
        try
        {
            new FixedPointShortMatrix(floatArray, 4);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected)
        {
            assertEquals("value out of range", expected.getMessage());
        }

        // And similarly when reading in a serialized matrix
        StringBuilder sb = new StringBuilder();
        sb = new StringBuilder();
        sb.append("matrix type=fixed-point-short precision=4 rows=3 columns=4 symmetric=false\n");
        sb.append("11.11  22.22\n");
        sb.append("33.33  44.44\n");
        String badMatrix = sb.toString();

        try
        {
            Matrix.Factory.read(badMatrix);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected)
        {
            assertEquals("value out of range", expected.getMessage());
        }
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
        super.testSet();

        // Verify IllegalArgumentException if the value is out of range
        try
        {
            sampleMatrix2.set(2, 0, 4);
        }
        catch (IllegalArgumentException expected)
        {
            assertEquals("value out of range", expected.getMessage());
        }

        try
        {
            sampleMatrix2.set(2, 0, 3.5f);
        }
        catch (IllegalArgumentException expected)
        {
            assertEquals("value out of range", expected.getMessage());
        }
    }

    @Override
    public void testInfinity() throws Exception
    {
        assertEquals(Short.MAX_VALUE / 100f, sampleMatrix.infinity(), .01f);
        assertEquals(Short.MAX_VALUE / 10000f, sampleMatrix2.infinity(), .01f);
    }

    @Override
    public void testNegativeInfinity() throws Exception
    {
        assertEquals(Short.MIN_VALUE / 100f, sampleMatrix.negativeInfinity(), .01f);
        assertEquals(Short.MIN_VALUE / 10000f, sampleMatrix2.negativeInfinity(), .01f);
    }

    /**
     * Tests equals() method
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testEquals() throws Exception
    {
        assertEquals(sampleMatrix, new FixedPointShortMatrix(sampleArray, 2));
        assertEquals(sampleMatrix2, new FixedPointShortMatrix(sampleArray2, 4));
        assertEquals(symmetricMatrix, new FixedPointShortMatrix(symmetricArray, 2, true));
        assertEquals(symmetricMatrix2, new FixedPointShortMatrix(symmetricArray2, 4, true));
    }
}

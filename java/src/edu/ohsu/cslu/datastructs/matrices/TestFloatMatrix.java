package edu.ohsu.cslu.datastructs.matrices;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Tests for the {@link FloatMatrix} class
 * 
 * @author Aaron Dunlop
 * @since Sep 18, 2008
 * 
 *        $Id$
 */
public class TestFloatMatrix extends FloatingPointMatrixTestCase
{
    private float[][] sampleArray;
    private float[][] sampleArray2;

    private float[][] symmetricArray;
    private float[][] symmetricArray2;

    @Override
    protected Matrix create(float[][] array)
    {
        return new FloatMatrix(array);
    }

    @Before
    public void setUp() throws Exception
    {
        StringBuilder sb = new StringBuilder();
        sb.append("matrix type=float rows=3 columns=4 symmetric=false\n");
        sb.append("11.1100 22.2200 33.3300 44.4400\n");
        sb.append("55.5500 66.6600 77.7700 88.8800\n");
        sb.append("99.9900 10.0000 11.1100 12.1100\n");
        stringSampleMatrix = sb.toString();

        sampleArray = new float[][] { {11.11f, 22.22f, 33.33f, 44.44f}, {55.55f, 66.66f, 77.77f, 88.88f},
                                     {99.99f, 10.00f, 11.11f, 12.11f}};
        sampleMatrix = new FloatMatrix(sampleArray);

        sb = new StringBuilder();
        sb.append("matrix type=float rows=3 columns=4 symmetric=false\n");
        sb.append("0.1111 0.2222 0.3333 0.4444\n");
        sb.append("0.5555 0.6666 0.7777 0.8888\n");
        sb.append("0.9999 0.1000 0.1111 0.1222\n");
        stringSampleMatrix2 = sb.toString();

        sampleArray2 = new float[][] { {0.1111f, 0.2222f, 0.3333f, 0.4444f}, {0.5555f, 0.6666f, 0.7777f, 0.8888f},
                                      {0.9999f, 0.1000f, 0.1111f, 0.1222f}};
        sampleMatrix2 = new FloatMatrix(sampleArray2);

        sb = new StringBuilder();
        sb.append("matrix type=float rows=5 columns=5 symmetric=true\n");
        sb.append("0.0000\n");
        sb.append("11.1100 22.2200\n");
        sb.append("33.3300 44.4400 55.5500\n");
        sb.append("66.6600 77.7700 88.8800 99.9900\n");
        sb.append("10.0000 11.1100 12.2200 13.3300 14.4400\n");
        stringSymmetricMatrix = sb.toString();

        symmetricArray = new float[][] { {0f}, {11.11f, 22.22f}, {33.33f, 44.44f, 55.55f},
                                        {66.66f, 77.77f, 88.88f, 99.99f}, {10.00f, 11.11f, 12.22f, 13.33f, 14.44f}};
        symmetricMatrix = new FloatMatrix(symmetricArray, true);
        matrixClass = FloatMatrix.class;

        sb = new StringBuilder();
        sb.append("matrix type=float rows=5 columns=5 symmetric=true\n");
        sb.append("0.0000\n");
        sb.append("0.1111 0.2222\n");
        sb.append("0.3333 0.4444 0.5555\n");
        sb.append("0.6666 0.7777 0.8888 0.9999\n");
        sb.append("0.1000 0.1111 0.1222 0.1333 0.1444\n");
        stringSymmetricMatrix2 = sb.toString();

        symmetricArray2 = new float[][] { {0}, {0.1111f, 0.2222f}, {0.3333f, 0.4444f, 0.5555f},
                                         {0.6666f, 0.7777f, 0.8888f, 0.9999f},
                                         {0.1000f, 0.1111f, 0.1222f, 0.1333f, 0.1444f}};
        symmetricMatrix2 = new FloatMatrix(symmetricArray2, true);
    }

    @Override
    public void testInfinity() throws Exception
    {
        assertEquals(Float.POSITIVE_INFINITY, sampleMatrix.infinity(), .01f);
        assertEquals(Float.POSITIVE_INFINITY, sampleMatrix2.infinity(), .01f);
    }

    @Override
    public void testNegativeInfinity() throws Exception
    {
        assertEquals(Float.NEGATIVE_INFINITY, sampleMatrix.negativeInfinity(), .01f);
        assertEquals(Float.NEGATIVE_INFINITY, sampleMatrix2.negativeInfinity(), .01f);
    }

    /**
     * Tests equals() method
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testEquals() throws Exception
    {
        assertEquals(sampleMatrix2, new FloatMatrix(sampleArray2));
        assertEquals(symmetricMatrix2, new FloatMatrix(symmetricArray2, true));
    }
}

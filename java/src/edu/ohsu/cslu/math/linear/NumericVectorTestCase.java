package edu.ohsu.cslu.math.linear;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

import org.junit.Test;

/**
 * Unit tests common to all {@link NumericVector} implementations
 * 
 * @author Aaron Dunlop
 * @since Apr 1, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public abstract class NumericVectorTestCase extends VectorTestCase
{
    /**
     * Tests element-wise division
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testElementwiseDivide() throws Exception
    {
        NumericVector vector = (NumericVector) create(new float[] {1, 2, 3, 4});

        try
        {
            vector.elementwiseDivide((NumericVector) create(new float[] {1}));
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected)
        {
            assertEquals("Vector length mismatch", expected.getMessage());
        }

        // Divide by an {@link IntVector}
        FloatVector quotient = vector.elementwiseDivide(new IntVector(new int[] {1, 3, 6, 10}));
        assertEquals("Wrong length", 4, quotient.length());
        assertEquals("Wrong value", 1f, quotient.getInt(0), .01f);
        assertEquals("Wrong value", .666f, quotient.getFloat(1), .01f);
        assertEquals("Wrong value", .5f, quotient.getFloat(2), .01f);
        assertEquals("Wrong value", .4f, quotient.getFloat(3), .01f);

        // Divide by a {@link FloatVector}
        quotient = vector.elementwiseDivide(new FloatVector(new float[] {1, 3, 6, 10}));
        assertEquals("Wrong length", 4, quotient.length());
        assertEquals("Wrong value", 1f, quotient.getInt(0), .01f);
        assertEquals("Wrong value", .666f, quotient.getFloat(1), .01f);
        assertEquals("Wrong value", .5f, quotient.getFloat(2), .01f);
        assertEquals("Wrong value", .4f, quotient.getFloat(3), .01f);

        // Divide by a {@link PackedIntVector}
        quotient = vector.elementwiseDivide(new PackedIntVector(new int[] {1, 3, 6, 10}, 4));
        assertEquals("Wrong length", 4, quotient.length());
        assertEquals("Wrong value", 1f, quotient.getInt(0), .01f);
        assertEquals("Wrong value", .666f, quotient.getFloat(1), .01f);
        assertEquals("Wrong value", .5f, quotient.getFloat(2), .01f);
        assertEquals("Wrong value", .4f, quotient.getFloat(3), .01f);
    }

    @Test
    public void testElementwiseLog() throws Exception
    {
        NumericVector vector = (NumericVector) create(new float[] {1, 10, 20, 30});

        try
        {
            vector.elementwiseDivide((NumericVector) create(new float[] {1}));
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected)
        {
            assertEquals("Vector length mismatch", expected.getMessage());
        }

        FloatVector log = vector.elementwiseLog();
        assertEquals("Wrong length", 4, log.length());
        assertEquals("Wrong value", Math.log(1), log.getInt(0), .01f);
        assertEquals("Wrong value", Math.log(10), log.getFloat(1), .01f);
        assertEquals("Wrong value", Math.log(20), log.getFloat(2), .01f);
        assertEquals("Wrong value", Math.log(30), log.getFloat(3), .01f);
    }
}

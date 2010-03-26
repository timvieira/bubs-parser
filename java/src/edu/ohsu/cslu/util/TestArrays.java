package edu.ohsu.cslu.util;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link Arrays}.
 * 
 * @author Aaron Dunlop
 * @since Mar 25, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestArrays {

    private int[] intArray = new int[] { 2, 5, 0, 11, 4, 4, 0, 3, 7 };
    private float[] floatArray = new float[intArray.length];
    private short[] shortArray = new short[intArray.length];

    @Before
    public void setUp() throws Exception {
        for (int i = 0; i < intArray.length; i++) {
            floatArray[i] = intArray[i];
            shortArray[i] = (short) intArray[i];
        }
    }

    @Test
    public void testNonDestructiveRadixSort() throws Exception {
        assertArrayEquals(new int[] { 0, 0, 2, 3, 4, 4, 5, 7, 11 }, Arrays.nonDestructiveRadixSort(intArray));
    }

    @Test
    public void testParallelArrayRadixSort() throws Exception {
        Arrays.radixSort(intArray, floatArray);

        assertArrayEquals(new int[] { 0, 0, 2, 3, 4, 4, 5, 7, 11 }, intArray);
        assertArrayEquals(new float[] { 0, 0, 2, 3, 4, 4, 5, 7, 11 }, floatArray, 0.01f);
    }

    @Test
    public void testParallelArrayRadixSort2() throws Exception {
        Arrays.radixSort(intArray, floatArray, shortArray);

        assertArrayEquals(new int[] { 0, 0, 2, 3, 4, 4, 5, 7, 11 }, intArray);
        assertArrayEquals(new float[] { 0, 0, 2, 3, 4, 4, 5, 7, 11 }, floatArray, 0.01f);
        assertArrayEquals(new short[] { 0, 0, 2, 3, 4, 4, 5, 7, 11 }, shortArray);
    }

}

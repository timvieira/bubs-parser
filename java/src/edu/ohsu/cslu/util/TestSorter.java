package edu.ohsu.cslu.util;

import static org.junit.Assert.assertArrayEquals;

import java.lang.reflect.ParameterizedType;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for various {@link Sorter} implementations
 * 
 * @author Aaron Dunlop
 * @since Mar 26, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public abstract class TestSorter<S extends Sorter> {

    private S sorter;

    private int[] intArray = new int[] { 2, 5, 0, 11, 4, 4, 0, 3, 7 };
    private float[] floatArray = new float[intArray.length];
    private short[] shortArray = new short[intArray.length];

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        sorter = ((Class<S>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0]).getConstructor().newInstance();

        for (int i = 0; i < intArray.length; i++) {
            floatArray[i] = intArray[i];
            shortArray[i] = (short) intArray[i];
        }
    }

    @Test
    public void testNonDestructiveSort() throws Exception {
        assertArrayEquals(new int[] { 0, 0, 2, 3, 4, 4, 5, 7, 11 }, sorter.nonDestructiveSort(intArray));
    }

    @Test
    public void testParallelArraySort() throws Exception {
        sorter.sort(intArray, floatArray);

        assertArrayEquals(new int[] { 0, 0, 2, 3, 4, 4, 5, 7, 11 }, intArray);
        assertArrayEquals(new float[] { 0, 0, 2, 3, 4, 4, 5, 7, 11 }, floatArray, 0.01f);
    }

    @Test
    public void testParallelArraySort2() throws Exception {
        sorter.sort(intArray, floatArray, shortArray);

        assertArrayEquals(new int[] { 0, 0, 2, 3, 4, 4, 5, 7, 11 }, intArray);
        assertArrayEquals(new float[] { 0, 0, 2, 3, 4, 4, 5, 7, 11 }, floatArray, 0.01f);
        assertArrayEquals(new short[] { 0, 0, 2, 3, 4, 4, 5, 7, 11 }, shortArray);
    }

    /**
     * Unit tests for {@link RadixSorter}
     */
    public static class TestRadixSorter extends TestSorter<RadixSorter> {
    }
}

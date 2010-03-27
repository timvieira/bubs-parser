package edu.ohsu.cslu.util;

import java.lang.reflect.ParameterizedType;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import edu.ohsu.cslu.tests.PerformanceTest;
import static org.junit.Assert.*;

/**
 * Unit tests for various {@link Sort} implementations
 * 
 * @author Aaron Dunlop
 * @since Mar 26, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public abstract class TestSort<S extends Sort> {

    private S sort;
    private Random random = new Random(System.currentTimeMillis());

    private int[] intArray = new int[] { 2, 5, 0, 11, 4, 4, 0, 3, 7 };
    private float[] floatArray = new float[intArray.length];
    private short[] shortArray = new short[intArray.length];

    private int[] intArray2;
    private float[] floatArray2;
    private short[] shortArray2;

    private int[] segmentedIntArray;
    private float[] segmentedFloatArray;
    private short[] segmentedShortArray;
    private int[] segmentBoundaries;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        sort = ((Class<S>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0]).getConstructor().newInstance();

        for (int i = 0; i < intArray.length; i++) {
            floatArray[i] = intArray[i];
            shortArray[i] = (short) intArray[i];
        }
        final Object[] o = createParallelArray(1000000);
        intArray2 = (int[]) o[0];
        floatArray2 = (float[]) o[1];
        shortArray2 = (short[]) o[2];

        segmentedIntArray = new int[100];
        segmentedFloatArray = new float[segmentedIntArray.length];
        segmentedShortArray = new short[segmentedIntArray.length];

        final int segments = 10;
        final int segmentSize = segmentedIntArray.length / segments;
        segmentBoundaries = new int[segments - 1];

        for (int i = 0; i < segments; i++) {
            for (int j = 0; j < segmentSize; j++) {
                segmentedIntArray[i * segmentSize + j] = j;
                segmentedFloatArray[i * segmentSize + j] = j;
                segmentedShortArray[i * segmentSize + j] = (short) j;
            }
        }
        for (int i = 1; i < segments; i++) {
            segmentBoundaries[i - 1] = segmentSize * i - 1;
        }
    }

    private Object[] createParallelArray(final int size) {
        final int[] ints = new int[size];
        final float[] floats = new float[size];
        final short[] shorts = new short[size];

        for (int i = 0; i < ints.length; i++) {
            ints[i] = random.nextInt(32767);
            floats[i] = ints[i];
            shorts[i] = (short) ints[i];
        }

        return new Object[] { ints, floats, shorts };
    }

    @Test
    public void testSort() throws Exception {
        sort.sort(intArray);
        verifySort(intArray, null, null);

        try {
            verifySort(intArray2, null, null);
            fail("Expected AssertionError");
        } catch (final AssertionError expected) {
        }

        sort.sort(intArray2);
        verifySort(intArray2, null, null);
    }

    @Test
    public void testNonDestructiveSort() throws Exception {
        assertArrayEquals(new int[] { 0, 0, 2, 3, 4, 4, 5, 7, 11 }, sort.nonDestructiveSort(intArray));

        verifySort(sort.nonDestructiveSort(intArray2), null, null);
    }

    @Test
    public void testParallelArraySortWithFloatValues() throws Exception {
        sort.sort(intArray, floatArray);

        assertArrayEquals(new int[] { 0, 0, 2, 3, 4, 4, 5, 7, 11 }, intArray);
        assertArrayEquals(new float[] { 0, 0, 2, 3, 4, 4, 5, 7, 11 }, floatArray, 0.01f);

        try {
            verifySort(intArray2, floatArray2, null);
            fail("Expected AssertionError");
        } catch (final AssertionError expected) {
        }

        sort.sort(intArray2, floatArray2);
        verifySort(sort.nonDestructiveSort(intArray2), floatArray2, null);
    }

    @Test
    public void testParallelArraySortWithShortValues() throws Exception {
        sort.sort(intArray, floatArray, shortArray);

        assertArrayEquals(new int[] { 0, 0, 2, 3, 4, 4, 5, 7, 11 }, intArray);
        assertArrayEquals(new float[] { 0, 0, 2, 3, 4, 4, 5, 7, 11 }, floatArray, 0.01f);
        assertArrayEquals(new short[] { 0, 0, 2, 3, 4, 4, 5, 7, 11 }, shortArray);

        try {
            verifySort(intArray2, floatArray2, shortArray2);
            fail("Expected AssertionError");
        } catch (final AssertionError expected) {
        }

        sort.sort(intArray2, floatArray2, shortArray2);
        verifySort(sort.nonDestructiveSort(intArray2), floatArray2, shortArray2);
    }

    @Test
    public void testSegmentedSort() throws Exception {
        sort.sort(segmentedIntArray, segmentedFloatArray, segmentedShortArray, segmentBoundaries);
        verifySort(segmentedIntArray, segmentedFloatArray, segmentedShortArray);
    }

    @Test
    @PerformanceTest
    public void profileSort() throws Exception {
        profileSort(100 * 1024);
        profileSort(1024 * 1024);
        profileSort(8 * 1024 * 1024);
    }

    private void profileSort(final int size) {
        final Object[] o = createParallelArray(size);
        final long startTime = System.currentTimeMillis();
        sort.sort((int[]) o[0], (float[]) o[1], (short[]) o[2]);
        final long totalTime = System.currentTimeMillis() - startTime;
        verifySort((int[]) o[0], (float[]) o[1], (short[]) o[2]);

        System.out.format("%s sorted %d entries in %d ms (%.0f entries/ms)\n", sort.getClass().toString(), size, totalTime, totalTime == 0 ? Float.POSITIVE_INFINITY : size
                / totalTime);
    }

    private void verifySort(final int[] keys, final float[] floatValues, final short[] shortValues) {
        int lastKey = Integer.MIN_VALUE;
        for (int i = 0; i < keys.length; i++) {
            assertTrue(keys[i] >= lastKey);
            lastKey = keys[i];

            if (floatValues != null) {
                assertEquals(keys[i], floatValues[i], .01f);
            }

            if (shortValues != null) {
                assertEquals((short) (keys[i]), shortValues[i]);
            }
        }
    }

    /**
     * Unit tests for {@link RadixSort}
     */
    public static class TestRadixSort extends TestSort<RadixSort> {
    }

    /**
     * Unit tests for {@link BitonicSort}
     */
    public static class TestBitonicSort extends TestSort<BitonicSort> {
    }

    /**
     * Unit tests for {@link FlashSort}
     */
    public static class TestFlashSort extends TestSort<FlashSort> {
    }

    /**
     * Unit tests for {@link QuickSort}
     */
    public static class TestQuickSort extends TestSort<QuickSort> {
    }

    /**
     * Unit tests for {@link MergeSort}
     */
    public static class TestMergeSort extends TestSort<MergeSort> {
    }
}

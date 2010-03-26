package edu.ohsu.cslu.util;

import static org.junit.Assert.assertArrayEquals;

import java.lang.reflect.ParameterizedType;

import org.junit.Before;
import org.junit.Test;

import edu.ohsu.cslu.util.Scanner.Operator;

public abstract class TestScanner<S extends Scanner> {

    private S scanner;

    private int[] intArray1 = new int[] { 2, 5, 0, 11, 4, 4, 0, 3, 7 };
    private float[] floatArray1 = new float[intArray1.length];
    private short[] shortArray1 = new short[intArray1.length];

    // Similar arrays, divided into 3 segments; the last entry in each segment is flagged, except for the
    // final segment (to ensure we don't run off the end)
    private int[] intArray2 = new int[] { 2, 5, 0, 11, 4, 4, 0, 3, 7, 2, 5, 0, 11, 4, 4, 0, 3, 7, 2, 5, 0, 11, 4, 4, 0, 3, 7 };
    private float[] floatArray2 = new float[intArray2.length];
    private short[] shortArray2 = new short[intArray2.length];
    private byte[] flags2 = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        scanner = ((Class<S>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0]).getConstructor().newInstance();

        for (int i = 0; i < intArray1.length; i++) {
            floatArray1[i] = intArray1[i];
            shortArray1[i] = (short) intArray1[i];
        }

        for (int i = 0; i < intArray2.length; i++) {
            floatArray2[i] = intArray2[i];
            shortArray2[i] = (short) intArray2[i];
        }
    }

    @Test
    public void testPrefixScanSum() throws Exception {
        // Exclusive
        assertArrayEquals(new int[] { 0, 2, 7, 7, 18, 22, 26, 26, 29 }, scanner.exclusiveScan(intArray1, Operator.SUM));
        assertArrayEquals(new float[] { 0, 2, 7, 7, 18, 22, 26, 26, 29 }, scanner.exclusiveScan(floatArray1, Operator.SUM), .01f);

        assertArrayEquals(new int[] { 0, 5, 5, 16 }, scanner.exclusiveScan(intArray1, 1, 5, Operator.SUM));
        assertArrayEquals(new float[] { 0, 5, 5, 16 }, scanner.exclusiveScan(floatArray1, 1, 5, Operator.SUM), .01f);

        // Inclusive
        assertArrayEquals(new int[] { 2, 7, 7, 18, 22, 26, 26, 29, 36 }, scanner.inclusiveScan(intArray1, Operator.SUM));
        assertArrayEquals(new float[] { 2, 7, 7, 18, 22, 26, 26, 29, 36 }, scanner.inclusiveScan(floatArray1, Operator.SUM), .01f);

        assertArrayEquals(new int[] { 5, 5, 16, 20 }, scanner.inclusiveScan(intArray1, 1, 5, Operator.SUM));
        assertArrayEquals(new float[] { 5, 5, 16, 20 }, scanner.inclusiveScan(floatArray1, 1, 5, Operator.SUM), .01f);
    }

    @Test
    public void testPrefixScanLogicalAnd() throws Exception {
        // Exclusive
        assertArrayEquals(new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0 }, scanner.exclusiveScan(intArray1, Operator.LOGICAL_AND));
        assertArrayEquals(new float[] { 0, 0, 0, 0, 0, 0, 0, 0, 0 }, scanner.exclusiveScan(floatArray1, Operator.LOGICAL_AND), .01f);

        assertArrayEquals(new int[] { 0, 0, 0, 0 }, scanner.exclusiveScan(intArray1, 1, 5, Operator.LOGICAL_AND));
        assertArrayEquals(new float[] { 0, 0, 0, 0 }, scanner.exclusiveScan(floatArray1, 1, 5, Operator.LOGICAL_AND), .01f);

        // Inclusive
        assertArrayEquals(new int[] { 1, 1, 0, 0, 0, 0, 0, 0, 0 }, scanner.inclusiveScan(intArray1, Operator.LOGICAL_AND));
        assertArrayEquals(new float[] { 1, 1, 0, 0, 0, 0, 0, 0, 0 }, scanner.inclusiveScan(floatArray1, Operator.LOGICAL_AND), .01f);

        assertArrayEquals(new int[] { 1, 0, 0, 0 }, scanner.inclusiveScan(intArray1, 1, 5, Operator.LOGICAL_AND));
        assertArrayEquals(new float[] { 1, 0, 0, 0 }, scanner.inclusiveScan(floatArray1, 1, 5, Operator.LOGICAL_AND), .01f);
    }

    @Test
    public void testPrefixScanLogicalNand() throws Exception {
        // Exclusive
        assertArrayEquals(new int[] { 0, 1, 0, 1, 0, 1, 0, 1, 0 }, scanner.exclusiveScan(intArray1, Operator.LOGICAL_NAND));
        assertArrayEquals(new float[] { 0, 1, 0, 1, 0, 1, 0, 1, 0 }, scanner.exclusiveScan(floatArray1, Operator.LOGICAL_NAND), .01f);

        assertArrayEquals(new int[] { 0, 1, 1, 0 }, scanner.exclusiveScan(intArray1, 1, 5, Operator.LOGICAL_NAND));
        assertArrayEquals(new float[] { 0, 1, 1, 0 }, scanner.exclusiveScan(floatArray1, 1, 5, Operator.LOGICAL_NAND), .01f);

        // Inclusive
        assertArrayEquals(new int[] { 0, 1, 1, 0, 1, 0, 1, 0, 1 }, scanner.inclusiveScan(intArray1, Operator.LOGICAL_NAND));
        assertArrayEquals(new float[] { 0, 1, 1, 0, 1, 0, 1, 0, 1 }, scanner.inclusiveScan(floatArray1, Operator.LOGICAL_NAND), .01f);

        assertArrayEquals(new int[] { 0, 1, 0, 1 }, scanner.inclusiveScan(intArray1, 1, 5, Operator.LOGICAL_NAND));
        assertArrayEquals(new float[] { 0, 1, 0, 1 }, scanner.inclusiveScan(floatArray1, 1, 5, Operator.LOGICAL_NAND), .01f);
    }

    @Test
    public void testPrefixScanMax() throws Exception {
        // Exclusive
        assertArrayEquals(new int[] { 0, 2, 5, 5, 11, 11, 11, 11, 11 }, scanner.exclusiveScan(intArray1, Operator.MAX));
        assertArrayEquals(new float[] { 0, 2, 5, 5, 11, 11, 11, 11, 11 }, scanner.exclusiveScan(floatArray1, Operator.MAX), .01f);

        assertArrayEquals(new int[] { 0, 5, 5, 11 }, scanner.exclusiveScan(intArray1, 1, 5, Operator.MAX));
        assertArrayEquals(new float[] { 0, 5, 5, 11 }, scanner.exclusiveScan(floatArray1, 1, 5, Operator.MAX), .01f);

        // Inclusive
        assertArrayEquals(new int[] { 2, 5, 5, 11, 11, 11, 11, 11, 11 }, scanner.inclusiveScan(intArray1, Operator.MAX));
        assertArrayEquals(new float[] { 2, 5, 5, 11, 11, 11, 11, 11, 11 }, scanner.inclusiveScan(floatArray1, Operator.MAX), .01f);

        assertArrayEquals(new int[] { 5, 5, 11, 11 }, scanner.inclusiveScan(intArray1, 1, 5, Operator.MAX));
        assertArrayEquals(new float[] { 5, 5, 11, 11 }, scanner.inclusiveScan(floatArray1, 1, 5, Operator.MAX), .01f);
    }

    @Test
    public void testPrefixScanMin() throws Exception {
        // Exclusive
        assertArrayEquals(new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0 }, scanner.exclusiveScan(intArray1, Operator.MIN));
        assertArrayEquals(new float[] { 0, 0, 0, 0, 0, 0, 0, 0, 0 }, scanner.exclusiveScan(floatArray1, Operator.MIN), .01f);

        assertArrayEquals(new int[] { 0, 0, 0, 0 }, scanner.exclusiveScan(intArray1, 1, 5, Operator.MIN));
        assertArrayEquals(new float[] { 0, 0, 0, 0 }, scanner.exclusiveScan(floatArray1, 1, 5, Operator.MIN), .01f);

        // Inclusive
        assertArrayEquals(new int[] { 2, 2, 0, 0, 0, 0, 0, 0, 0 }, scanner.inclusiveScan(intArray1, Operator.MIN));
        assertArrayEquals(new float[] { 2, 2, 0, 0, 0, 0, 0, 0, 0 }, scanner.inclusiveScan(floatArray1, Operator.MIN), .01f);

        assertArrayEquals(new int[] { 5, 0, 0, 0 }, scanner.inclusiveScan(intArray1, 1, 5, Operator.MIN));
        assertArrayEquals(new float[] { 5, 0, 0, 0 }, scanner.inclusiveScan(floatArray1, 1, 5, Operator.MIN), .01f);
    }

    @Test
    public void testScanEqual() throws Exception {
        // Exclusive
        assertArrayEquals(new int[] { 0, 0, 0, 0, 0, 0, 1, 0, 0 }, scanner.exclusiveScan(intArray1, Operator.EQUAL));
        assertArrayEquals(new float[] { 0, 0, 0, 0, 0, 0, 1, 0, 0 }, scanner.exclusiveScan(floatArray1, Operator.EQUAL), .01f);

        // Inclusive
        assertArrayEquals(new int[] { 0, 0, 0, 0, 0, 1, 0, 0, 0 }, scanner.inclusiveScan(intArray1, Operator.EQUAL));
        assertArrayEquals(new float[] { 0, 0, 0, 0, 0, 1, 0, 0, 0 }, scanner.inclusiveScan(floatArray1, Operator.EQUAL), .01f);
    }

    @Test
    public void testScanNotEquals() throws Exception {
        // Exclusive
        assertArrayEquals(new int[] { 0, 1, 1, 1, 1, 1, 0, 1, 1 }, scanner.exclusiveScan(intArray1, Operator.NOT_EQUAL));
        assertArrayEquals(new float[] { 0, 1, 1, 1, 1, 1, 0, 1, 1 }, scanner.exclusiveScan(floatArray1, Operator.NOT_EQUAL), .01f);

        // Inclusive
        assertArrayEquals(new int[] { 1, 1, 1, 1, 1, 0, 1, 1, 1 }, scanner.inclusiveScan(intArray1, Operator.NOT_EQUAL));
        assertArrayEquals(new float[] { 1, 1, 1, 1, 1, 0, 1, 1, 1 }, scanner.inclusiveScan(floatArray1, Operator.NOT_EQUAL), .01f);
    }

    @Test
    public void testSegmentedScanSum() throws Exception {
        // Exclusive
        assertArrayEquals(new int[] { 0, 2, 7, 7, 18, 22, 26, 26, 29, 0, 2, 7, 7, 18, 22, 26, 26, 29, 0, 2, 7, 7, 18, 22, 26, 26, 29 }, scanner.exclusiveSegmentedScan(intArray2,
                flags2, Operator.SUM));
        assertArrayEquals(new float[] { 0, 2, 7, 7, 18, 22, 26, 26, 29, 0, 2, 7, 7, 18, 22, 26, 26, 29, 0, 2, 7, 7, 18, 22, 26, 26, 29 }, scanner.exclusiveSegmentedScan(
                floatArray2, flags2, Operator.SUM), .01f);

        // Inclusive
        assertArrayEquals(new int[] { 2, 7, 7, 18, 22, 26, 26, 29, 36, 2, 7, 7, 18, 22, 26, 26, 29, 36, 2, 7, 7, 18, 22, 26, 26, 29, 36 }, scanner.inclusiveSegmentedScan(
                intArray2, flags2, Operator.SUM));
        assertArrayEquals(new float[] { 2, 7, 7, 18, 22, 26, 26, 29, 36, 2, 7, 7, 18, 22, 26, 26, 29, 36, 2, 7, 7, 18, 22, 26, 26, 29, 36 }, scanner.inclusiveSegmentedScan(
                floatArray2, flags2, Operator.SUM), .01f);
    }

    @Test
    public void testSegmentedScanLogicalAnd() throws Exception {
        // Exclusive
        assertArrayEquals(new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, scanner.exclusiveSegmentedScan(intArray2, flags2,
                Operator.LOGICAL_AND));
        assertArrayEquals(new float[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, scanner.exclusiveSegmentedScan(floatArray2, flags2,
                Operator.LOGICAL_AND), .01f);

        // Inclusive
        assertArrayEquals(new int[] { 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0 }, scanner.inclusiveSegmentedScan(intArray2, flags2,
                Operator.LOGICAL_AND));
        assertArrayEquals(new float[] { 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0 }, scanner.inclusiveSegmentedScan(floatArray2, flags2,
                Operator.LOGICAL_AND), .01f);
    }

    @Test
    public void testSegmentedScanLogicalNand() throws Exception {
        // Exclusive
        assertArrayEquals(new int[] { 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 1, 0, 1, 0 }, scanner.exclusiveSegmentedScan(intArray2, flags2,
                Operator.LOGICAL_NAND));
        assertArrayEquals(new float[] { 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 1, 0, 1, 0 }, scanner.exclusiveSegmentedScan(floatArray2, flags2,
                Operator.LOGICAL_NAND), .01f);

        // Inclusive
        assertArrayEquals(new int[] { 0, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 0, 1 }, scanner.inclusiveSegmentedScan(intArray2, flags2,
                Operator.LOGICAL_NAND));
        assertArrayEquals(new float[] { 0, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 0, 1 }, scanner.inclusiveSegmentedScan(floatArray2, flags2,
                Operator.LOGICAL_NAND), .01f);
    }

    @Test
    public void testSegmentedScanMax() throws Exception {
        // Exclusive
        assertArrayEquals(new int[] { 0, 2, 5, 5, 11, 11, 11, 11, 11, 0, 2, 5, 5, 11, 11, 11, 11, 11, 0, 2, 5, 5, 11, 11, 11, 11, 11 }, scanner.exclusiveSegmentedScan(intArray2,
                flags2, Operator.MAX));
        assertArrayEquals(new float[] { 0, 2, 5, 5, 11, 11, 11, 11, 11, 0, 2, 5, 5, 11, 11, 11, 11, 11, 0, 2, 5, 5, 11, 11, 11, 11, 11 }, scanner.exclusiveSegmentedScan(
                floatArray2, flags2, Operator.MAX), .01f);

        // Inclusive
        assertArrayEquals(new int[] { 2, 5, 5, 11, 11, 11, 11, 11, 11, 2, 5, 5, 11, 11, 11, 11, 11, 11, 2, 5, 5, 11, 11, 11, 11, 11, 11 }, scanner.inclusiveSegmentedScan(
                intArray2, flags2, Operator.MAX));
        assertArrayEquals(new float[] { 2, 5, 5, 11, 11, 11, 11, 11, 11, 2, 5, 5, 11, 11, 11, 11, 11, 11, 2, 5, 5, 11, 11, 11, 11, 11, 11 }, scanner.inclusiveSegmentedScan(
                floatArray2, flags2, Operator.MAX), .01f);
    }

    @Test
    public void testSegmentedScanMin() throws Exception {
        // Exclusive
        assertArrayEquals(new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, scanner.exclusiveSegmentedScan(intArray2, flags2,
                Operator.MIN));
        assertArrayEquals(new float[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, scanner.exclusiveSegmentedScan(floatArray2, flags2,
                Operator.MIN), .01f);

        // Inclusive
        assertArrayEquals(new int[] { 2, 2, 0, 0, 0, 0, 0, 0, 0, 2, 2, 0, 0, 0, 0, 0, 0, 0, 2, 2, 0, 0, 0, 0, 0, 0, 0 }, scanner.inclusiveSegmentedScan(intArray2, flags2,
                Operator.MIN));
        assertArrayEquals(new float[] { 2, 2, 0, 0, 0, 0, 0, 0, 0, 2, 2, 0, 0, 0, 0, 0, 0, 0, 2, 2, 0, 0, 0, 0, 0, 0, 0 }, scanner.inclusiveSegmentedScan(floatArray2, flags2,
                Operator.MIN), .01f);
    }

    @Test
    public void testPack() throws Exception {

        assertArrayEquals(new int[0], scanner.pack(intArray1, new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0 }));
        assertArrayEquals(new int[0], scanner.pack(intArray1, new byte[] { 1, 1, 1, 1, 1, 1, 1, 1, 1 }, 0, 0));
        assertArrayEquals(new int[0], scanner.pack(intArray1, new byte[] { 1, 1, 1, 1, 1, 1, 1, 1, 1 }, 5, 5));
        assertArrayEquals(new int[] { 4 }, scanner.pack(intArray1, new byte[] { 1, 1, 1, 1, 1, 1, 1, 1, 1 }, 5, 6));

        assertArrayEquals(new int[] { 11 }, scanner.pack(intArray1, new byte[] { 0, 0, 0, 1, 0, 0, 0, 0, 0 }));
        assertArrayEquals(intArray1, scanner.pack(intArray1, new byte[] { 1, 1, 1, 1, 1, 1, 1, 1, 1 }));
        assertArrayEquals(new int[] { 7, 7 }, scanner.pack(intArray2, flags2));

        assertArrayEquals(new float[0], scanner.pack(floatArray1, new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0 }), .01f);
        assertArrayEquals(new float[0], scanner.pack(floatArray1, new byte[] { 1, 1, 1, 1, 1, 1, 1, 1, 1 }, 0, 0), .01f);
        assertArrayEquals(new float[0], scanner.pack(floatArray1, new byte[] { 1, 1, 1, 1, 1, 1, 1, 1, 1 }, 5, 5), .01f);
        assertArrayEquals(new float[] { 4 }, scanner.pack(floatArray1, new byte[] { 1, 1, 1, 1, 1, 1, 1, 1, 1 }, 5, 6), .01f);

        assertArrayEquals(new float[] { 11 }, scanner.pack(floatArray1, new byte[] { 0, 0, 0, 1, 0, 0, 0, 0, 0 }), .01f);
        assertArrayEquals(floatArray1, scanner.pack(floatArray1, new byte[] { 1, 1, 1, 1, 1, 1, 1, 1, 1 }), .01f);
        assertArrayEquals(new float[] { 7, 7 }, scanner.pack(floatArray2, flags2), .01f);
    }

    @Test
    public void testScatter() throws Exception {

        final int[] indices = new int[] { 2, 3, 4, 5, 6, 7, 8, 9, 0 };
        final byte[] flags = new byte[] { 0, 1, 0, 1, 0, 1, 0, 1, 0 };

        // Unflagged scatter methods
        assertArrayEquals(new int[] { 7, 0, 2, 5, 0, 11, 4, 4, 0, 3 }, scanner.scatter(intArray1, indices));
        assertArrayEquals(new float[] { 7, 0, 2, 5, 0, 11, 4, 4, 0, 3 }, scanner.scatter(floatArray1, indices), .01f);
        assertArrayEquals(new short[] { 7, 0, 2, 5, 0, 11, 4, 4, 0, 3 }, scanner.scatter(shortArray1, indices));

        // Flagged scatter methods
        assertArrayEquals(new int[] { 0, 0, 0, 5, 0, 11, 0, 4, 0, 3 }, scanner.scatter(intArray1, indices, flags));
        assertArrayEquals(new float[] { 0, 0, 0, 5, 0, 11, 0, 4, 0, 3 }, scanner.scatter(floatArray1, indices, flags), .01f);
        assertArrayEquals(new short[] { 0, 0, 0, 5, 0, 11, 0, 4, 0, 3 }, scanner.scatter(shortArray1, indices, flags));
    }

    @Test
    public void testParallelArrayInclusiveSegmentedMax() {
        final float[] floatResult = new float[floatArray2.length];
        final short[] shortResult = new short[shortArray2.length];
        scanner.parallelArrayInclusiveSegmentedMax(floatArray2, floatResult, shortArray2, shortResult, flags2);
        assertArrayEquals(new float[] { 2, 5, 5, 11, 11, 11, 11, 11, 11, 2, 5, 5, 11, 11, 11, 11, 11, 11, 2, 5, 5, 11, 11, 11, 11, 11, 11 }, floatResult, 0.01f);
        assertArrayEquals(new short[] { 2, 5, 5, 11, 11, 11, 11, 11, 11, 2, 5, 5, 11, 11, 11, 11, 11, 11, 2, 5, 5, 11, 11, 11, 11, 11, 11 }, shortResult);
    }

    @Test
    public void testFlagEndOfKeySegments() throws Exception {
        final byte[] result = new byte[intArray2.length];
        scanner.flagEndOfKeySegments(intArray2, result);
        assertArrayEquals(new byte[] { 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1 }, result);
    }
}

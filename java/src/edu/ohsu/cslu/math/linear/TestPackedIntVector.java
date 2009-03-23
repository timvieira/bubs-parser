package edu.ohsu.cslu.math.linear;

import static junit.framework.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.tests.FilteredRunner;
import edu.ohsu.cslu.tests.PerformanceTest;

/**
 * Unit tests for {@link PackedIntVector}
 * 
 * @author Aaron Dunlop
 * @since Nov 11, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
@RunWith(FilteredRunner.class)
public class TestPackedIntVector extends IntVectorTestCase
{
    private int[] sampleArray;

    @Override
    public void setUp() throws Exception
    {
        StringBuilder sb = new StringBuilder();
        sb.append("vector type=packed-int length=11 bits=8\n");
        sb.append("11 0 11 22 33 44 56 67 78 89 100\n");
        stringSampleVector = sb.toString();

        sampleArray = new int[] {11, 0, 11, 22, 33, 44, 56, 67, 78, 89, 100};
        sampleVector = new PackedIntVector(sampleArray, 8);

        vectorClass = PackedIntVector.class;
    }

    @Override
    protected Vector create(float[] array)
    {
        int[] intArray = new int[array.length];
        for (int i = 0; i < array.length; i++)
        {
            intArray[i] = Math.round(array[i]);
        }
        return new PackedIntVector(intArray, 8);
    }

    @Override
    public void testInfinity() throws Exception
    {
        assertEquals(255, sampleVector.infinity(), .001f);
    }

    @Override
    public void testNegativeInfinity() throws Exception
    {
        assertEquals(0, sampleVector.negativeInfinity(), .001f);
    }

    /**
     * Tests 'getFloat' method
     * 
     * @throws Exception if something bad happens
     */
    @Override
    @Test
    public void testGetFloat() throws Exception
    {
        assertEquals("Wrong value", 11, sampleVector.getFloat(0), 0.01f);
        assertEquals("Wrong value", 0, sampleVector.getFloat(1), 0.01f);
        assertEquals("Wrong value", 11, sampleVector.getFloat(2), 0.01f);
        assertEquals("Wrong value", 22, sampleVector.getFloat(3), 0.01f);
        assertEquals("Wrong value", 33, sampleVector.getFloat(4), 0.01f);
        assertEquals("Wrong value", 44, sampleVector.getFloat(5), 0.01f);
        assertEquals("Wrong value", 56, sampleVector.getFloat(6), 0.01f);
        assertEquals("Wrong value", 67, sampleVector.getFloat(7), 0.01f);
        assertEquals("Wrong value", 78, sampleVector.getFloat(8), 0.01f);
        assertEquals("Wrong value", 89, sampleVector.getFloat(9), 0.01f);
        assertEquals("Wrong value", 100, sampleVector.getFloat(10), 0.01f);
    }

    @Override
    @Test
    public void testGetInt()
    {
        PackedIntVector packedVector = new PackedIntVector(16, 2);
        assertEquals("Wrong value", 0, packedVector.getInt(1));
        assertEquals("Wrong value", 0, packedVector.getInt(2));
        assertEquals("Wrong value", 0, packedVector.getInt(3));
        assertEquals("Wrong value", 0, packedVector.getInt(4));

        packedVector.set(2, 1);
        packedVector.set(3, 2);
        packedVector.set(4, 0);
        assertEquals("Wrong value", 1, packedVector.getInt(2));
        assertEquals("Wrong value", 2, packedVector.getInt(3));
        assertEquals("Wrong value", 0, packedVector.getInt(4));

        packedVector = new PackedIntVector(137, 1);
        packedVector.set(101, 1);
        packedVector.set(102, 0);
        packedVector.set(103, 0);
        packedVector.set(104, 1);
        packedVector.set(105, 1);
        assertEquals("Wrong value", 1, packedVector.getInt(101));
        assertEquals("Wrong value", 0, packedVector.getInt(102));
        assertEquals("Wrong value", 0, packedVector.getInt(103));
        assertEquals("Wrong value", 1, packedVector.getInt(104));
        assertEquals("Wrong value", 1, packedVector.getInt(105));

        packedVector = new PackedIntVector(137, 2);
        packedVector.set(101, 1);
        packedVector.set(102, 2);
        packedVector.set(103, 3);
        packedVector.set(104, 0);
        packedVector.set(105, 1);
        packedVector.set(106, 2);
        packedVector.set(107, 3);
        assertEquals("Wrong value", 1, packedVector.getInt(101));
        assertEquals("Wrong value", 2, packedVector.getInt(102));
        assertEquals("Wrong value", 3, packedVector.getInt(103));
        assertEquals("Wrong value", 0, packedVector.getInt(104));
        assertEquals("Wrong value", 1, packedVector.getInt(105));
        assertEquals("Wrong value", 2, packedVector.getInt(106));
        assertEquals("Wrong value", 3, packedVector.getInt(107));

        packedVector = new PackedIntVector(137, 4);
        packedVector.set(101, 1);
        packedVector.set(102, 2);
        packedVector.set(103, 3);
        packedVector.set(104, 4);
        packedVector.set(105, 5);
        packedVector.set(106, 6);
        packedVector.set(107, 7);
        assertEquals("Wrong value", 1, packedVector.getInt(101));
        assertEquals("Wrong value", 2, packedVector.getInt(102));
        assertEquals("Wrong value", 3, packedVector.getInt(103));
        assertEquals("Wrong value", 4, packedVector.getInt(104));
        assertEquals("Wrong value", 5, packedVector.getInt(105));
        assertEquals("Wrong value", 6, packedVector.getInt(106));
        assertEquals("Wrong value", 7, packedVector.getInt(107));

        // And a couple randomized stress-tests
        int count = 1024;
        Random random = new Random();
        int[] intArray = new int[count];
        packedVector = new PackedIntVector(count, 2);
        for (int i = 0; i < count; i++)
        {
            int value = random.nextInt(4);
            intArray[i] = value;
            packedVector.set(i, value);
        }

        for (int i = 0; i < count; i++)
        {
            assertEquals(intArray[i], packedVector.getInt(i));
        }

        // And a couple randomized stress-tests
        count = 1024;
        packedVector = new PackedIntVector(count, 4);
        for (int i = 0; i < count; i++)
        {
            int value = random.nextInt(8);
            intArray[i] = value;
            packedVector.set(i, value);
        }

        for (int i = 0; i < count; i++)
        {
            assertEquals(intArray[i], packedVector.getInt(i));
        }
    }

    @Override
    @Test
    public void testSet()
    {
        PackedIntVector packedVector = new PackedIntVector(200, 2);
        packedVector.set(101, 1);
        packedVector.set(102, 2);
        packedVector.set(103, 3);
        packedVector.set(104, 0);
        packedVector.set(105, 1);
        packedVector.set(106, 2);
        packedVector.set(107, 3);
        assertEquals("Wrong value", 1, packedVector.getInt(101));
        assertEquals("Wrong value", 2, packedVector.getInt(102));
        assertEquals("Wrong value", 3, packedVector.getInt(103));
        assertEquals("Wrong value", 0, packedVector.getInt(104));
        assertEquals("Wrong value", 1, packedVector.getInt(105));
        assertEquals("Wrong value", 2, packedVector.getInt(106));
        assertEquals("Wrong value", 3, packedVector.getInt(107));

        packedVector.set(101, 0);
        packedVector.set(102, 0);
        packedVector.set(103, 0);
        packedVector.set(104, 0);
        packedVector.set(105, 0);
        packedVector.set(106, 0);
        packedVector.set(107, 0);
        assertEquals("Wrong value", 0, packedVector.getInt(101));
        assertEquals("Wrong value", 0, packedVector.getInt(102));
        assertEquals("Wrong value", 0, packedVector.getInt(103));
        assertEquals("Wrong value", 0, packedVector.getInt(104));
        assertEquals("Wrong value", 0, packedVector.getInt(105));
        assertEquals("Wrong value", 0, packedVector.getInt(106));
        assertEquals("Wrong value", 0, packedVector.getInt(107));

        packedVector = new PackedIntVector(200, 8);
        packedVector.set(101, 1);
        packedVector.set(102, 2);
        packedVector.set(103, 3);
        packedVector.set(104, 16);
        packedVector.set(105, 25);
        packedVector.set(106, 65);
        packedVector.set(107, 150);
        assertEquals("Wrong value", 1, packedVector.getInt(101));
        assertEquals("Wrong value", 2, packedVector.getInt(102));
        assertEquals("Wrong value", 3, packedVector.getInt(103));
        assertEquals("Wrong value", 16, packedVector.getInt(104));
        assertEquals("Wrong value", 25, packedVector.getInt(105));
        assertEquals("Wrong value", 65, packedVector.getInt(106));
        assertEquals("Wrong value", 150, packedVector.getInt(107));

        packedVector.set(101, 0);
        packedVector.set(102, 0);
        packedVector.set(103, 0);
        packedVector.set(104, 0);
        packedVector.set(105, 0);
        packedVector.set(106, 0);
        packedVector.set(107, 0);
        assertEquals("Wrong value", 0, packedVector.getInt(101));
        assertEquals("Wrong value", 0, packedVector.getInt(102));
        assertEquals("Wrong value", 0, packedVector.getInt(103));
        assertEquals("Wrong value", 0, packedVector.getInt(104));
        assertEquals("Wrong value", 0, packedVector.getInt(105));
        assertEquals("Wrong value", 0, packedVector.getInt(106));
        assertEquals("Wrong value", 0, packedVector.getInt(107));
    }

    /**
     * Tests min(), intMin(), and argMin() methods
     * 
     * @throws Exception if something bad happens
     */
    @Override
    @Test
    public void testMin() throws Exception
    {
        assertEquals(0f, sampleVector.min(), .01f);
        assertEquals(0, sampleVector.intMin());
        assertEquals(1, sampleVector.argMin());
    }

    /**
     * Tests max(), intMax(), and argMax() methods
     * 
     * @throws Exception if something bad happens
     */
    @Override
    @Test
    public void testMax() throws Exception
    {
        assertEquals(100, sampleVector.intMax());
        assertEquals(10, sampleVector.argMax());
    }

    @Override
    public void testScalarAdd() throws Exception
    {
        Vector v = sampleVector.scalarAdd(1);
        assertEquals("Wrong class", IntVector.class, v.getClass());
        assertEquals("Wrong value", 12, v.getInt(0));
        assertEquals("Wrong value", 1, v.getInt(1));
        assertEquals("Wrong value", 12, v.getInt(2));
        assertEquals("Wrong value", 23, v.getInt(3));
        assertEquals("Wrong value", 34, v.getInt(4));
        assertEquals("Wrong value", 45, v.getInt(5));
        assertEquals("Wrong value", 57, v.getInt(6));
        assertEquals("Wrong value", 68, v.getInt(7));
        assertEquals("Wrong value", 79, v.getInt(8));
        assertEquals("Wrong value", 90, v.getInt(9));
        assertEquals("Wrong value", 101, v.getInt(10));

        v = sampleVector.scalarAdd(-2.5f);
        assertEquals("Wrong class", FloatVector.class, v.getClass());
        assertEquals("Wrong value", 8.5f, v.getFloat(0), .001f);
        assertEquals("Wrong value", -2.5f, v.getFloat(1), .001f);
        assertEquals("Wrong value", 8.5f, v.getFloat(2), .001f);
        assertEquals("Wrong value", 19.5f, v.getFloat(3), .001f);
        assertEquals("Wrong value", 30.5f, v.getFloat(4), .001f);
        assertEquals("Wrong value", 41.5f, v.getFloat(5), .001f);
        assertEquals("Wrong value", 53.5f, v.getFloat(6), .001f);
        assertEquals("Wrong value", 64.5f, v.getFloat(7), .001f);
        assertEquals("Wrong value", 75.5f, v.getFloat(8), .001f);
        assertEquals("Wrong value", 86.5f, v.getFloat(9), .001f);
        assertEquals("Wrong value", 97.5f, v.getFloat(10), .001f);
    }

    @Override
    public void testScalarMultiply() throws Exception
    {
        Vector v = sampleVector.scalarMultiply(3);
        assertEquals("Wrong class", IntVector.class, v.getClass());
        assertEquals("Wrong value", 33, v.getInt(0));
        assertEquals("Wrong value", 0, v.getInt(1));
        assertEquals("Wrong value", 33, v.getInt(2));
        assertEquals("Wrong value", 66, v.getInt(3));
        assertEquals("Wrong value", 99, v.getInt(4));
        assertEquals("Wrong value", 132, v.getInt(5));
        assertEquals("Wrong value", 168, v.getInt(6));
        assertEquals("Wrong value", 201, v.getInt(7));
        assertEquals("Wrong value", 234, v.getInt(8));
        assertEquals("Wrong value", 267, v.getInt(9));
        assertEquals("Wrong value", 300, v.getInt(10));

        v = sampleVector.scalarMultiply(-2.5f);
        assertEquals("Wrong class", FloatVector.class, v.getClass());
        assertEquals("Wrong value", -27.5f, v.getFloat(0), .001f);
        assertEquals("Wrong value", 0f, v.getFloat(1), .001f);
        assertEquals("Wrong value", -27.5f, v.getFloat(2), .001f);
        assertEquals("Wrong value", -55f, v.getFloat(3), .001f);
        assertEquals("Wrong value", -82.5f, v.getFloat(4), .001f);
        assertEquals("Wrong value", -110f, v.getFloat(5), .001f);
        assertEquals("Wrong value", -140f, v.getFloat(6), .001f);
        assertEquals("Wrong value", -167.5f, v.getFloat(7), .001f);
        assertEquals("Wrong value", -195f, v.getFloat(8), .001f);
        assertEquals("Wrong value", -222.5f, v.getFloat(9), .001f);
        assertEquals("Wrong value", -250f, v.getFloat(10), .001f);
    }

    @PerformanceTest
    @Test
    public void profilePackedIntVector()
    {
        final int iterations = 100000000;
        final int length = 65536;

        long startTime = System.currentTimeMillis();

        PackedIntVector packedArray = new PackedIntVector(length, 2);
        for (int i = 0; i < iterations; i++)
        {
            packedArray.set(i & 0xffff, i & 0x03);
        }
        for (int i = 0; i < iterations; i++)
        {
            packedArray.getInt(i & 0xffff);
        }

        long totalTime = System.currentTimeMillis() - startTime;
        System.out.format("%d iterations in %d ms (%.1f million per sec)", iterations, totalTime,
            ((float) iterations / totalTime));
    }
}

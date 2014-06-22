/*
 * Copyright 2010-2014, Oregon Health & Science University
 * 
 * This file is part of the BUBS Parser.
 * 
 * The BUBS Parser is free software: you can redistribute it and/or 
 * modify  it under the terms of the GNU Affero General Public License 
 * as published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * The BUBS Parser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Further documentation and contact information is available at
 *   https://code.google.com/p/bubs-parser/ 
 */
package edu.ohsu.cslu.datastructs.vectors;

import static junit.framework.Assert.assertEquals;

import java.util.Random;

import org.cjunit.FilteredRunner;
import org.cjunit.PerformanceTest;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit tests for {@link PackedIntVector}
 * 
 * @author Aaron Dunlop
 * @since Nov 11, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
@RunWith(FilteredRunner.class)
public class TestPackedIntVector extends IntVectorTestCase<PackedIntVector> {

    @Override
    public void setUp() throws Exception {
        final StringBuilder sb = new StringBuilder();
        sb.append("vector type=" + serializedName() + " length=11 bits=8 sparse=false\n");
        sb.append("11 0 11 22 33 44 56 67 78 89 100\n");
        stringSampleVector = sb.toString();

        final int[] sampleArray = new int[] { 11, 0, 11, 22, 33, 44, 56, 67, 78, 89, 100 };
        sampleVector = create(sampleArray);
    }

    @Override
    protected String serializedName() {
        return "packed-int";
    }

    @Override
    public void testInfinity() throws Exception {
        assertEquals(255, sampleVector.infinity(), .001f);
    }

    @Override
    public void testNegativeInfinity() throws Exception {
        assertEquals(0, sampleVector.negativeInfinity(), .001f);
    }

    /**
     * Tests 'getFloat' method
     * 
     * @throws Exception if something bad happens
     */
    @Override
    @Test
    public void testGetFloat() throws Exception {
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
    public void testGetInt() {
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
        final Random random = new Random();
        final int[] intArray = new int[count];
        packedVector = new PackedIntVector(count, 2);
        for (int i = 0; i < count; i++) {
            final int value = random.nextInt(4);
            intArray[i] = value;
            packedVector.set(i, value);
        }

        for (int i = 0; i < count; i++) {
            assertEquals(intArray[i], packedVector.getInt(i));
        }

        // And a couple randomized stress-tests
        count = 1024;
        packedVector = new PackedIntVector(count, 4);
        for (int i = 0; i < count; i++) {
            final int value = random.nextInt(8);
            intArray[i] = value;
            packedVector.set(i, value);
        }

        for (int i = 0; i < count; i++) {
            assertEquals(intArray[i], packedVector.getInt(i));
        }
    }

    @Override
    @Test
    public void testSet() {
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

    @Override
    public void testScalarAdd() throws Exception {
        NumericVector v = ((NumericVector) sampleVector).scalarAdd(1);
        assertEquals("Wrong class", sampleVector.getClass(), v.getClass());
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

        v = ((NumericVector) sampleVector).scalarAdd(-2.5f);
        assertEquals("Wrong class", DenseFloatVector.class, v.getClass());
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
    public void testScalarMultiply() throws Exception {
        NumericVector v = ((NumericVector) sampleVector).scalarMultiply(3);
        assertEquals("Wrong class", vectorClass(), v.getClass());
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

        v = ((NumericVector) sampleVector).scalarMultiply(-2.5f);
        assertEquals("Wrong class", DenseFloatVector.class, v.getClass());
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

    /**
     * Tests min(), intMin(), and argMin() methods
     * 
     * @throws Exception if something bad happens
     */
    @Override
    @Test
    public void testMin() throws Exception {
        assertEquals(0f, ((NumericVector) sampleVector).min(), .01f);
        assertEquals(0, ((NumericVector) sampleVector).intMin());
        assertEquals(1, ((NumericVector) sampleVector).argMin());
    }

    /**
     * Tests max(), intMax(), and argMax() methods
     * 
     * @throws Exception if something bad happens
     */
    @Override
    @Test
    public void testMax() throws Exception {
        assertEquals(100, ((NumericVector) sampleVector).intMax());
        assertEquals(10, ((NumericVector) sampleVector).argMax());
    }

    @PerformanceTest
    @Test
    public void profilePackedIntVector() {
        final int iterations = 100000000;
        final int length = 65536;

        final long startTime = System.currentTimeMillis();

        final PackedIntVector packedArray = new PackedIntVector(length, 2);
        for (int i = 0; i < iterations; i++) {
            packedArray.set(i & 0xffff, i & 0x03);
        }
        for (int i = 0; i < iterations; i++) {
            packedArray.getInt(i & 0xffff);
        }

        final long totalTime = System.currentTimeMillis() - startTime;
        System.out.format("%d iterations in %d ms (%.1f million per sec)", iterations, totalTime,
                ((float) iterations / totalTime));
    }
}

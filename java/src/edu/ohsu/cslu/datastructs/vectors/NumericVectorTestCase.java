/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
 * 
 * This file is part of the cslu-common project.
 * 
 * cslu-common is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * cslu-common is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with cslu-common. If not, see <http://www.gnu.org/licenses/>
 */
package edu.ohsu.cslu.datastructs.vectors;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import it.unimi.dsi.fastutil.longs.LongSet;

import org.junit.Test;

/**
 * Unit tests common to all {@link NumericVector} implementations
 * 
 * @author Aaron Dunlop
 * @since Apr 1, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public abstract class NumericVectorTestCase<V extends NumericVector> extends VectorTestCase<V> {

    /**
     * Tests scalar addition
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public abstract void testScalarAdd() throws Exception;

    /**
     * Tests scalar multiplication
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public abstract void testScalarMultiply() throws Exception;

    /**
     * Tests min(), intMin(), and argMin() methods
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testMin() throws Exception {
        assertEquals(-11f, ((NumericVector) sampleVector).min(), .01f);
        assertEquals(-11, ((NumericVector) sampleVector).intMin());
        assertEquals(0, ((NumericVector) sampleVector).argMin());

        sampleVector.set(1, -22.0f);
        sampleVector.set(2, -44.0f);
        assertEquals(-44f, ((NumericVector) sampleVector).min(), .01f);
        assertEquals(-44, ((NumericVector) sampleVector).intMin());
        assertEquals(2, ((NumericVector) sampleVector).argMin());
    }

    /**
     * Tests {@link NumericVector#max()}, {@link NumericVector#intMax()}, and {@link NumericVector#argMax()} functions
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testMax() throws Exception {
        assertEquals(100, ((NumericVector) sampleVector).intMax());
        assertEquals(10, ((NumericVector) sampleVector).argMax());

        sampleVector.set(1, 125f);
        sampleVector.set(2, 126f);
        assertEquals(126f, ((NumericVector) sampleVector).max(), .01f);
        assertEquals(126, ((NumericVector) sampleVector).intMax());
        assertEquals(2, ((NumericVector) sampleVector).argMax());
    }

    @Test
    public void testPopulatedDimensions() throws Exception {
        assertLongSetEquals(new long[] { 0, 2, 3, 4, 5, 6, 7, 8, 9, 10 },
                ((NumericVector) sampleVector).populatedDimensions());
        sampleVector.set(4, 0);
        sampleVector.set(6, 0);
        assertLongSetEquals(new long[] { 0, 2, 3, 5, 7, 8, 9, 10 },
                ((NumericVector) sampleVector).populatedDimensions());
    }

    private void assertLongSetEquals(final long[] expected, final LongSet actual) {
        assertEquals("Wrong length", expected.length, actual.size());
        for (final long l : expected) {
            assertTrue("Expected " + l, actual.contains(l));
        }
    }
}

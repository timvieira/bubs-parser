/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
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
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>
 */ 
package edu.ohsu.cslu.parser.chart;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.cjunit.FilteredRunner;
import org.cjunit.PerformanceTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit tests for {@link BoundedPriorityQueue}
 * 
 * @author Aaron Dunlop
 * @since Sep 23, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
@RunWith(FilteredRunner.class)
public class TestBoundedPriorityQueue {

    final BoundedPriorityQueue queue = new BoundedPriorityQueue(5);

    @Before
    public void setUp() {
        queue.insert((short) 3, -1f);
        queue.insert((short) 2, -2f);
        queue.insert((short) 1, -3f);
        queue.insert((short) 7, -7f);
        queue.insert((short) 6, -6f);
        queue.insert((short) 4, -5f);
        queue.insert((short) 5, -4f);
    }

    @Test
    public void testSize() {
        final BoundedPriorityQueue q = new BoundedPriorityQueue(3);
        assertEquals(0, q.size());

        q.insert((short) 1, -1f);
        assertEquals(1, q.size());

        q.insert((short) 2, -2f);
        assertEquals(2, q.size());

        q.insert((short) 3, -3f);
        assertEquals(3, q.size());

        q.insert((short) 7, -7f);
        assertEquals(3, q.size());
    }

    @Test
    public void testInsert() {
        // Verify insertions from setUp()
        assertEquals(3, queue.parentIndices[0]);
        assertEquals(-1f, queue.foms[0], .001f);

        assertEquals(2, queue.parentIndices[1]);
        assertEquals(-2f, queue.foms[1], .001f);

        assertEquals(1, queue.parentIndices[2]);
        assertEquals(-3f, queue.foms[2], .001f);

        assertEquals(5, queue.parentIndices[3]);
        assertEquals(-4f, queue.foms[3], .001f);

        assertEquals(4, queue.parentIndices[4]);
        assertEquals(-5f, queue.foms[4], .001f);

        // Verify that the queue properly accepts new insertions and rejects insertions below the lowest entry
        assertFalse(queue.insert((short) 8, -8f));
        assertFalse(queue.insert((short) 6, -5f));
        assertTrue(queue.insert((short) 6, -4.9f));
        assertEquals(6, queue.parentIndices[4]);
        assertEquals(-4.9f, queue.foms[4], .001f);
    }

    @Test
    public void testReplace() {
        assertEquals(3, queue.parentIndices[queue.headIndex()]);
        assertEquals(-1f, queue.foms[queue.headIndex()], 0.001f);

        // Replace the head
        queue.replace((short) 3, -.5f);
        assertEquals(3, queue.parentIndices[queue.headIndex()]);
        assertEquals(-.5f, queue.foms[queue.headIndex()], 0.001f);

        // An entry not present in the queue
        assertFalse(queue.replace((short) 7, -10f));

        // And an entry present with a higher FOM
        assertFalse(queue.replace((short) 5, -10f));

        // Replace another entry lower in the queue
        assertTrue(queue.replace((short) 5, -2f));
        queue.popHead(); // 3
        queue.popHead(); // 2
        queue.popHead(); // 1
        assertEquals(5, queue.parentIndices[queue.headIndex()]);
        assertEquals(-2f, queue.foms[queue.headIndex()], 0.001f);

        // And an entry present with a higher FOM, now that the queue is not full
        assertFalse(queue.replace((short) 5, -10f));

        // Replace the tail entry
        assertTrue(queue.replace((short) 4, -1f));
        assertEquals(4, queue.parentIndices[queue.headIndex() + 1]);
        assertEquals(-1f, queue.foms[queue.headIndex() + 1], 0.001f);

        // Replace an entry that is both head and tail
        queue.popHead();
        assertTrue(queue.replace((short) 4, -0.5f));
        assertEquals(4, queue.parentIndices[queue.headIndex()]);
        assertEquals(-0.5f, queue.foms[queue.headIndex()], 0.001f);

        // Now try to replace in an empty queue
        queue.popHead();
        assertFalse(queue.replace((short) 4, -1f));
    }

    @Test
    public void testHeadIndex() {
        assertEquals(3, queue.parentIndices[queue.headIndex()]);
        assertEquals(-1f, queue.foms[queue.headIndex()], 0.001f);
    }

    @Test
    public void testPopHead() {

        assertTrue(queue.popHead());
        assertEquals(4, queue.size());

        // The second-most-probable entry should now be the head
        assertEquals(2, queue.parentIndices[queue.headIndex()]);
        assertEquals(-2f, queue.foms[queue.headIndex()], 0.001f);

        // Continue popping
        assertTrue(queue.popHead());
        assertEquals(3, queue.size());
        assertEquals(1, queue.parentIndices[queue.headIndex()]);
        assertEquals(-3f, queue.foms[queue.headIndex()], 0.001f);

        assertTrue(queue.popHead());
        assertEquals(2, queue.size());
        assertEquals(5, queue.parentIndices[queue.headIndex()]);
        assertEquals(-4f, queue.foms[queue.headIndex()], 0.001f);

        assertTrue(queue.popHead());
        assertEquals(1, queue.size());
        assertEquals(4, queue.parentIndices[queue.headIndex()]);
        assertEquals(-5f, queue.foms[queue.headIndex()], 0.001f);

        assertTrue(queue.popHead());
        assertEquals(0, queue.size());

        assertFalse(queue.popHead());
        assertEquals(0, queue.size());

        assertFalse(queue.insert((short) 5, -8));
    }

    @Test
    public void testSetMaxSize() {

        queue.setMaxSize(3);
        assertTrue(queue.popHead());
        assertTrue(queue.popHead());
        assertTrue(queue.popHead());
        assertFalse(queue.popHead());

        queue.setMaxSize(4);
        assertTrue(queue.insert((short) 2, -2f));
        assertTrue(queue.insert((short) 3, -2f));
        assertTrue(queue.insert((short) 4, -2f));
        assertTrue(queue.insert((short) 5, -2f));
        assertFalse(queue.insert((short) 2, -2f));
    }

    @Test
    @PerformanceTest({ "d820", "2850" })
    public void profileQueue() {
        // Pseudo-random numbers, but with the same initial seed, so always the same series
        final Random r = new Random(1);

        for (int i = 0; i < 5000; i++) {
            final BoundedPriorityQueue q = new BoundedPriorityQueue(50);
            for (int j = 0; j < 1000; j++) {
                q.insert((short) r.nextInt(500), -100f * r.nextFloat());
            }
            for (int j = 0; j < 30; j++) {
                q.popHead();
                for (int k = 0; k < 100; k++) {
                    q.replace((short) r.nextInt(500), -100f * r.nextFloat());
                }
            }
        }
    }
}

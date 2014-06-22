/*
 * Copyright 2010-2012 Aaron Dunlop and Nathan Bodenstab
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

package edu.ohsu.cslu.util;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

/**
 * Unit tests for {@link Arrays}
 * 
 * @author Aaron Dunlop
 */
public class TestArrays {

    @Test
    public void testSortFloatAndShort() {

        // A few tests with short arrays (these will use insertion sort)
        float[] f = new float[] { 0, 0, 0 };
        short[] s = new short[] { 0, 1, 2 };
        Arrays.sort(f, s);
        assertArrayEquals(new float[] { 0, 0, 0 }, f, .001f);
        assertArrayEquals(new short[] { 0, 1, 2 }, s);

        f = new float[] { 0, 1, 2 };
        Arrays.sort(f, s);
        assertArrayEquals(new float[] { 0, 1, 2 }, f, .001f);
        assertArrayEquals(new short[] { 0, 1, 2 }, s);

        f = new float[] { 0, -1, -2 };
        Arrays.sort(f, s);
        assertArrayEquals(new float[] { -2, -1, 0 }, f, .001f);
        assertArrayEquals(new short[] { 2, 1, 0 }, s);

        // And some of length > 7
        f = new float[] { 0, 0, 0, 0, 0, 0, 0, 0 };
        s = new short[] { 0, 1, 2, 3, 4, 5, 6, 7 };
        Arrays.sort(f, s);
        assertArrayEquals(new float[] { 0, 0, 0, 0, 0, 0, 0, 0 }, f, .001f);
        assertArrayEquals(new short[] { 0, 1, 2, 3, 4, 5, 6, 7 }, s);

        f = new float[] { 0, 1, 2, 3, 4, 5, 6, 7 };
        Arrays.sort(f, s);
        assertArrayEquals(new float[] { 0, 1, 2, 3, 4, 5, 6, 7 }, f, .001f);
        assertArrayEquals(new short[] { 0, 1, 2, 3, 4, 5, 6, 7 }, s);

        f = new float[] { 0, -1, -2, -3, -4, -5, -6, -7 };
        Arrays.sort(f, s);
        assertArrayEquals(new float[] { -7, -6, -5, -4, -3, -2, -1, 0 }, f, .001f);
        assertArrayEquals(new short[] { 7, 6, 5, 4, 3, 2, 1, 0 }, s);
    }

    @Test
    public void testReverseSortFloatAndShort() {

        // A few tests with short arrays (these will use insertion sort)
        float[] f = new float[] { 0, 0, 0 };
        short[] s = new short[] { 0, 1, 2 };
        Arrays.reverseSort(f, s);
        assertArrayEquals(new float[] { 0, 0, 0 }, f, .001f);
        assertArrayEquals(new short[] { 0, 1, 2 }, s);

        f = new float[] { 0, 1, 2 };
        Arrays.reverseSort(f, s);
        assertArrayEquals(new float[] { 2, 1, 0 }, f, .001f);
        assertArrayEquals(new short[] { 2, 1, 0 }, s);

        f = new float[] { 0, -1, -2 };
        s = new short[] { 0, 1, 2 };
        Arrays.reverseSort(f, s);
        assertArrayEquals(new float[] { 0, -1, -2 }, f, .001f);
        assertArrayEquals(new short[] { 0, 1, 2 }, s);

        // And some of length > 7
        f = new float[] { 0, 0, 0, 0, 0, 0, 0, 0 };
        s = new short[] { 0, 1, 2, 3, 4, 5, 6, 7 };
        Arrays.reverseSort(f, s);
        assertArrayEquals(new float[] { 0, 0, 0, 0, 0, 0, 0, 0 }, f, .001f);
        assertArrayEquals(new short[] { 0, 1, 2, 3, 4, 5, 6, 7 }, s);

        f = new float[] { 0, 1, 2, 3, 4, 5, 6, 7 };
        s = new short[] { 0, 1, 2, 3, 4, 5, 6, 7 };
        Arrays.reverseSort(f, s);
        assertArrayEquals(new float[] { 7, 6, 5, 4, 3, 2, 1, 0 }, f, .001f);
        assertArrayEquals(new short[] { 7, 6, 5, 4, 3, 2, 1, 0 }, s);

        f = new float[] { 0, -1, -2, -3, -4, -5, -6, -7 };
        s = new short[] { 0, 1, 2, 3, 4, 5, 6, 7 };
        Arrays.reverseSort(f, s);
        assertArrayEquals(new float[] { 0, -1, -2, -3, -4, -5, -6, -7 }, f, .001f);
        assertArrayEquals(new short[] { 0, 1, 2, 3, 4, 5, 6, 7 }, s);
    }
}

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
package edu.ohsu.cslu.datastructs.narytree;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

import org.cjunit.FilteredRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit tests for the {@link ShiftRegister class}
 * 
 * @author Aaron Dunlop
 * @since Sep 23, 2008
 * 
 *        $Id$
 */
@RunWith(FilteredRunner.class)
public class TestIntShiftRegister {

    @Test
    public void testConstructor() {
        final ShiftRegister<Integer> reg = new ShiftRegister<Integer>(new Integer[] { 1, 2, 3, 4 });
        assertEquals(4, reg.size());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4 }, reg.register());
    }

    @Test
    public void testShift() {
        ShiftRegister<Integer> register = new ShiftRegister<Integer>(5);
        assertEquals(5, register.size());
        assertArrayEquals(new Integer[] { null, null, null, null, null }, register.register());

        register = register.shift(5);
        assertArrayEquals(new Integer[] { null, null, null, null, 5 }, register.register());

        register = register.shift(4);
        assertArrayEquals(new Integer[] { null, null, null, 5, 4 }, register.register());

        register = register.shift(3);
        assertArrayEquals(new Integer[] { null, null, 5, 4, 3 }, register.register());

        register = register.shift(2);
        assertArrayEquals(new Integer[] { null, 5, 4, 3, 2 }, register.register());

        register = register.shift(1);
        assertArrayEquals(new Integer[] { 5, 4, 3, 2, 1 }, register.register());

        register = register.shift(5);
        assertArrayEquals(new Integer[] { 4, 3, 2, 1, 5 }, register.register());
    }

    @Test
    public void testConcat() {
        final ShiftRegister<Integer> reg = new ShiftRegister<Integer>(new Integer[] { 1, 2, 3 });
        final ShiftRegister<Integer> reg2 = new ShiftRegister<Integer>(new Integer[] { 4, 5 });

        final ShiftRegister<Integer> reg3 = reg.concat(reg2);
        assertEquals(5, reg3.size());
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, reg3.register());
    }
}

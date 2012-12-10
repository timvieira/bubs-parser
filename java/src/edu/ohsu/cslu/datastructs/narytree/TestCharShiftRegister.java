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
package edu.ohsu.cslu.datastructs.narytree;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

import org.cjunit.FilteredRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit tests for the {@link ShiftRegister class} with characters
 * 
 * @author Aaron Dunlop
 * @since Sep 23, 2008
 * 
 *        $Id$
 */
@RunWith(FilteredRunner.class)
public class TestCharShiftRegister {

    @Test
    public void testConstructor() {
        final ShiftRegister<Character> reg = new ShiftRegister<Character>(new Character[] { 'a', 'b', 'c', 'd' });
        assertEquals(4, reg.size());
        assertArrayEquals(new Character[] { 'a', 'b', 'c', 'd' }, reg.register());
    }

    @Test
    public void testShift() {
        ShiftRegister<Character> register = new ShiftRegister<Character>(5);
        assertEquals(5, register.size());
        assertArrayEquals(new Character[] { null, null, null, null, null }, register.register());

        register = register.shift('e');
        assertArrayEquals(new Character[] { null, null, null, null, 'e' }, register.register());

        register = register.shift('d');
        assertArrayEquals(new Character[] { null, null, null, 'e', 'd' }, register.register());

        register = register.shift('c');
        assertArrayEquals(new Character[] { null, null, 'e', 'd', 'c' }, register.register());

        register = register.shift('b');
        assertArrayEquals(new Character[] { null, 'e', 'd', 'c', 'b' }, register.register());

        register = register.shift('a');
        assertArrayEquals(new Character[] { 'e', 'd', 'c', 'b', 'a' }, register.register());

        register = register.shift('e');
        assertArrayEquals(new Character[] { 'd', 'c', 'b', 'a', 'e' }, register.register());
    }

    @Test
    public void testConcat() {
        final ShiftRegister<Character> reg = new ShiftRegister<Character>(new Character[] { 'a', 'b', 'c' });
        final ShiftRegister<Character> reg2 = new ShiftRegister<Character>(new Character[] { 'd', 'e' });

        final ShiftRegister<Character> reg3 = reg.concat(reg2);
        assertEquals(5, reg3.size());
        assertArrayEquals(new Character[] { 'a', 'b', 'c', 'd', 'e' }, reg3.register());
    }

}

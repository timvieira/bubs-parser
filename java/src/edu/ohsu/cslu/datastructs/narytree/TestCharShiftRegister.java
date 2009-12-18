package edu.ohsu.cslu.datastructs.narytree;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.tests.FilteredRunner;

/**
 * Unit tests for the {@link CharShiftRegister class}
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
        final CharShiftRegister reg = new CharShiftRegister(new char[] { 'a', 'b', 'c', 'd' });
        assertEquals(4, reg.size());
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, reg.register());
    }

    @Test
    public void testShift() {
        CharShiftRegister register = new CharShiftRegister(5);
        assertEquals(5, register.size());
        assertArrayEquals(new char[] { '*', '*', '*', '*', '*' }, register.register());

        register = register.shift('e');
        assertArrayEquals(new char[] { '*', '*', '*', '*', 'e' }, register.register());

        register = register.shift('d');
        assertArrayEquals(new char[] { '*', '*', '*', 'e', 'd' }, register.register());

        register = register.shift('c');
        assertArrayEquals(new char[] { '*', '*', 'e', 'd', 'c' }, register.register());

        register = register.shift('b');
        assertArrayEquals(new char[] { '*', 'e', 'd', 'c', 'b' }, register.register());

        register = register.shift('a');
        assertArrayEquals(new char[] { 'e', 'd', 'c', 'b', 'a' }, register.register());

        register = register.shift('e');
        assertArrayEquals(new char[] { 'd', 'c', 'b', 'a', 'e' }, register.register());
    }

    @Test
    public void testConcat() {
        final CharShiftRegister reg = new CharShiftRegister(new char[] { 'a', 'b', 'c' });
        final CharShiftRegister reg2 = new CharShiftRegister(new char[] { 'd', 'e' });

        final CharShiftRegister reg3 = reg.concat(reg2);
        assertEquals(5, reg3.size());
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, reg3.register());
    }

}

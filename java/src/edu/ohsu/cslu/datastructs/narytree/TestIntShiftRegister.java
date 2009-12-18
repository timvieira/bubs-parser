package edu.ohsu.cslu.datastructs.narytree;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.tests.FilteredRunner;

/**
 * Unit tests for the {@link IntShiftRegister class}
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
        final IntShiftRegister reg = new IntShiftRegister(new int[] { 1, 2, 3, 4 });
        assertEquals(4, reg.size());
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, reg.register());
    }

    @Test
    public void testShift() {
        IntShiftRegister register = new IntShiftRegister(5);
        assertEquals(5, register.size());
        assertArrayEquals(new int[] { Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE,
                Integer.MIN_VALUE, Integer.MIN_VALUE }, register.register());

        register = register.shift(5);
        assertArrayEquals(new int[] { Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE,
                Integer.MIN_VALUE, 5 }, register.register());

        register = register.shift(4);
        assertArrayEquals(new int[] { Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, 5, 4 },
            register.register());

        register = register.shift(3);
        assertArrayEquals(new int[] { Integer.MIN_VALUE, Integer.MIN_VALUE, 5, 4, 3 }, register.register());

        register = register.shift(2);
        assertArrayEquals(new int[] { Integer.MIN_VALUE, 5, 4, 3, 2 }, register.register());

        register = register.shift(1);
        assertArrayEquals(new int[] { 5, 4, 3, 2, 1 }, register.register());

        register = register.shift(5);
        assertArrayEquals(new int[] { 4, 3, 2, 1, 5 }, register.register());
    }

    @Test
    public void testConcat() {
        final IntShiftRegister reg = new IntShiftRegister(new int[] { 1, 2, 3 });
        final IntShiftRegister reg2 = new IntShiftRegister(new int[] { 4, 5 });

        final IntShiftRegister reg3 = reg.concat(reg2);
        assertEquals(5, reg3.size());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, reg3.register());
    }
}

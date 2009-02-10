package edu.ohsu.cslu.narytree;

import org.junit.Test;

import edu.ohsu.cslu.tests.SharedNlpTests;

import static junit.framework.Assert.assertEquals;

/**
 * Unit tests for the {@link IntShiftRegister class}
 * 
 * @author Aaron Dunlop
 * @since Sep 23, 2008
 * 
 *        $Id$
 */
public class TestIntShiftRegister
{

    @Test
    public void testConstructor()
    {
        IntShiftRegister reg = new IntShiftRegister(new int[] {1, 2, 3, 4});
        assertEquals(4, reg.size());
        SharedNlpTests.assertEquals(new int[] {1, 2, 3, 4}, reg.register());
    }

    @Test
    public void testShift()
    {
        IntShiftRegister register = new IntShiftRegister(5);
        assertEquals(5, register.size());
        SharedNlpTests.assertEquals(new int[] {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE,
                                         Integer.MIN_VALUE}, register.register());

        register = register.shift(5);
        SharedNlpTests.assertEquals(
            new int[] {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, 5}, register
                .register());

        register = register.shift(4);
        SharedNlpTests.assertEquals(new int[] {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, 5, 4}, register
            .register());

        register = register.shift(3);
        SharedNlpTests.assertEquals(new int[] {Integer.MIN_VALUE, Integer.MIN_VALUE, 5, 4, 3}, register.register());

        register = register.shift(2);
        SharedNlpTests.assertEquals(new int[] {Integer.MIN_VALUE, 5, 4, 3, 2}, register.register());

        register = register.shift(1);
        SharedNlpTests.assertEquals(new int[] {5, 4, 3, 2, 1}, register.register());

        register = register.shift(5);
        SharedNlpTests.assertEquals(new int[] {4, 3, 2, 1, 5}, register.register());
    }

    @Test
    public void testConcat()
    {
        IntShiftRegister reg = new IntShiftRegister(new int[] {1, 2, 3});
        IntShiftRegister reg2 = new IntShiftRegister(new int[] {4, 5});

        IntShiftRegister reg3 = reg.concat(reg2);
        assertEquals(5, reg3.size());
        SharedNlpTests.assertEquals(new int[] {1, 2, 3, 4, 5}, reg3.register());
    }
}

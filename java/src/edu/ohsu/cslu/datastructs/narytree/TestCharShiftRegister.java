package edu.ohsu.cslu.datastructs.narytree;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.tests.FilteredRunner;
import edu.ohsu.cslu.tests.SharedNlpTests;

/**
 * Unit tests for the {@link CharShiftRegister class}
 * 
 * @author Aaron Dunlop
 * @since Sep 23, 2008
 * 
 *        $Id$
 */
@RunWith(FilteredRunner.class)
public class TestCharShiftRegister
{

    @Test
    public void testConstructor()
    {
        CharShiftRegister reg = new CharShiftRegister(new char[] {'a', 'b', 'c', 'd'});
        assertEquals(4, reg.size());
        SharedNlpTests.assertEquals(new char[] {'a', 'b', 'c', 'd'}, reg.register());
    }

    @Test
    public void testShift()
    {
        CharShiftRegister register = new CharShiftRegister(5);
        assertEquals(5, register.size());
        SharedNlpTests.assertEquals(new char[] {'*', '*', '*', '*', '*'}, register.register());

        register = register.shift('e');
        SharedNlpTests.assertEquals(new char[] {'*', '*', '*', '*', 'e'}, register.register());

        register = register.shift('d');
        SharedNlpTests.assertEquals(new char[] {'*', '*', '*', 'e', 'd'}, register.register());

        register = register.shift('c');
        SharedNlpTests.assertEquals(new char[] {'*', '*', 'e', 'd', 'c'}, register.register());

        register = register.shift('b');
        SharedNlpTests.assertEquals(new char[] {'*', 'e', 'd', 'c', 'b'}, register.register());

        register = register.shift('a');
        SharedNlpTests.assertEquals(new char[] {'e', 'd', 'c', 'b', 'a'}, register.register());

        register = register.shift('e');
        SharedNlpTests.assertEquals(new char[] {'d', 'c', 'b', 'a', 'e'}, register.register());
    }

    @Test
    public void testConcat()
    {
        CharShiftRegister reg = new CharShiftRegister(new char[] {'a', 'b', 'c'});
        CharShiftRegister reg2 = new CharShiftRegister(new char[] {'d', 'e'});

        CharShiftRegister reg3 = reg.concat(reg2);
        assertEquals(5, reg3.size());
        SharedNlpTests.assertEquals(new char[] {'a', 'b', 'c', 'd', 'e'}, reg3.register());
    }

}

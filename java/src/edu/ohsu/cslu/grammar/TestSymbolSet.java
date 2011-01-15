package edu.ohsu.cslu.grammar;

import static org.junit.Assert.assertEquals;

import java.util.Comparator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.tests.FilteredRunner;

/**
 * Unit tests for {@link SymbolSet}.
 * 
 * @author Aaron Dunlop
 * @since Jan 12, 2011
 * 
 * @version $Revision$ $Date$ $Author$
 */
@RunWith(FilteredRunner.class)
public class TestSymbolSet {

    private final SymbolSet<String> s = new SymbolSet<String>();

    @Before
    public void setUp() {
        s.addSymbol("D");
        s.addSymbol("C");
        s.addSymbol("B");
        s.addSymbol("A");
    }

    /**
     * Tests insertion order and sorting by natural string order
     */
    @Test
    public void testSetComparator() {
        assertEquals(0, s.getIndex("D"));
        assertEquals("D", s.getSymbol(0));
        assertEquals(1, s.getIndex("C"));
        assertEquals("C", s.getSymbol(1));
        assertEquals(2, s.getIndex("B"));
        assertEquals("B", s.getSymbol(2));
        assertEquals(3, s.getIndex("A"));
        assertEquals("A", s.getSymbol(3));

        s.setComparator(new Comparator<String>() {

            @Override
            public int compare(final String o1, final String o2) {
                return o1.compareTo(o2);
            }
        });

        assertEquals(0, s.getIndex("A"));
        assertEquals("A", s.getSymbol(0));
        assertEquals(1, s.getIndex("B"));
        assertEquals("B", s.getSymbol(1));
        assertEquals(2, s.getIndex("C"));
        assertEquals("C", s.getSymbol(2));
        assertEquals(3, s.getIndex("D"));
        assertEquals("D", s.getSymbol(3));
    }

    /**
     * Tests removing mappings by key (index) and by value (symbol).
     */
    @Test
    public void testRemove() {
        s.removeIndex(2);

        assertEquals(0, s.getIndex("D"));
        assertEquals("D", s.getSymbol(0));
        assertEquals(1, s.getIndex("C"));
        assertEquals("C", s.getSymbol(1));
        assertEquals(2, s.getIndex("A"));
        assertEquals("A", s.getSymbol(2));

        s.remove("C");

        assertEquals(0, s.getIndex("D"));
        assertEquals("D", s.getSymbol(0));
        assertEquals(1, s.getIndex("A"));
        assertEquals("A", s.getSymbol(1));
    }

    @Test
    public void testRemoveArray() {
        s.removeIndices(new int[] { 1, 3 });

        assertEquals(2, s.size());

        assertEquals(0, s.getIndex("D"));
        assertEquals("D", s.getSymbol(0));
        assertEquals(1, s.getIndex("B"));
        assertEquals("B", s.getSymbol(1));
    }

}

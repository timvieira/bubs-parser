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
package edu.ohsu.cslu.grammar;

import static org.junit.Assert.assertEquals;

import java.util.Comparator;

import org.cjunit.FilteredRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit tests for {@link SymbolSet}.
 * 
 * @author Aaron Dunlop
 * @since Jan 12, 2011
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

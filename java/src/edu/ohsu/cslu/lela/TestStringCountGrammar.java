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
package edu.ohsu.cslu.lela;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;

import org.cjunit.FilteredRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SymbolSet;

/**
 * Unit tests for {@link StringCountGrammar}.
 * 
 * @author Aaron Dunlop
 * @since Jan 12, 2011
 */
@RunWith(FilteredRunner.class)
public class TestStringCountGrammar extends CountGrammarTestCase {

    private StringCountGrammar sg;

    @Before
    public void setUp() throws IOException {
        sg = new StringCountGrammar(new StringReader(AllLelaTests.STRING_SAMPLE_TREE), null, null);
        g = sg;
    }

    @Test
    public void testInduceVocabulary() {
        final SymbolSet<String> unsortedVocabulary = sg.induceVocabulary(null);
        assertEquals(5, unsortedVocabulary.size());
        assertEquals(0, unsortedVocabulary.getIndex("top"));
        assertEquals(2, unsortedVocabulary.getIndex("a"));
        assertEquals(4, unsortedVocabulary.getIndex("b"));
        assertEquals(1, unsortedVocabulary.getIndex("c"));
        assertEquals(3, unsortedVocabulary.getIndex("d"));

        final SymbolSet<String> sortedVocabulary = sg.induceVocabulary(sg.binaryParentCountComparator());
        assertEquals(5, sortedVocabulary.size());
        assertEquals(0, sortedVocabulary.getIndex("top"));
        assertEquals(1, sortedVocabulary.getIndex("a"));
        assertEquals(2, sortedVocabulary.getIndex("b"));
        assertEquals(3, sortedVocabulary.getIndex("c"));
        assertEquals(4, sortedVocabulary.getIndex("d"));
    }

    @Test
    public void testInduceLexicon() {
        final SymbolSet<String> lexicon = sg.induceLexicon();
        assertEquals(2, lexicon.size());
        assertEquals(0, lexicon.getIndex("e"));
        assertEquals(1, lexicon.getIndex("f"));
    }

    @Test
    public void testBinaryProductions() {
        final ArrayList<Production> binaryProductions = sg.binaryProductions(sg.induceVocabulary(null));
        assertEquals(4, binaryProductions.size());
    }

    @Test
    public void testUnaryProductions() {
        final ArrayList<Production> unaryProductions = sg.unaryProductions(sg.induceVocabulary(null));
        assertEquals(2, unaryProductions.size());
        assertEquals(0, unaryProductions.get(0).parent); // s
        assertEquals(2, unaryProductions.get(0).leftChild); // a
        assertEquals(4, unaryProductions.get(1).parent); // b
        assertEquals(3, unaryProductions.get(1).leftChild); // b
    }

    @Test
    public void testLexicalProductions() {
        final ArrayList<Production> lexicalProductions = sg.lexicalProductions(sg.induceVocabulary(null));
        assertEquals(3, lexicalProductions.size());
    }

    @Test
    public void testBinaryParentCountComparator() {
        final Comparator<String> c = sg.binaryParentCountComparator();

        assertEquals(0, c.compare("a", "a"));
        assertEquals(-1, c.compare("a", "b"));
        assertEquals(1, c.compare("b", "a"));

        // Special-cases for the start symbol
        assertEquals(-1, c.compare("top", "a"));
        assertEquals(-1, c.compare("top", "b"));
        assertEquals(0, c.compare("top", "top"));
    }
}

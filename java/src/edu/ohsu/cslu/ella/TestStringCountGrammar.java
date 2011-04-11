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
package edu.ohsu.cslu.ella;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
 * 
 * @version $Revision$ $Date$ $Author$
 */
@RunWith(FilteredRunner.class)
public class TestStringCountGrammar extends CountGrammarTestCase {

    private StringCountGrammar sg;

    @Before
    public void setUp() throws IOException {
        sg = new StringCountGrammar(new StringReader(AllEllaTests.STRING_SAMPLE_TREE), null, null, 1);
        g = sg;
    }

    @Test
    public void testUnknownWordThreshold() throws IOException {
        // Two trees, differing only in two terminals which occur one time each
        final String corpus = AllEllaTests.STRING_SAMPLE_TREE + '\n'
                + "(s (a (a (a (a e) (a c)) (b d)) (b (b (b f)) (a d))))";

        // With an occurrence threshold of 1, all observed symbols will be in the lexicon
        SymbolSet<String> lexicon = new StringCountGrammar(new StringReader(corpus), null, null, 1)
            .induceLexicon();
        assertTrue("Expected 'e' in lexicon", lexicon.contains("e"));
        assertTrue("Expected 'f' in lexicon", lexicon.contains("f"));

        // Raise the occurrence threshold so 'e' and 'f' will be translated to an UNK- token
        lexicon = new StringCountGrammar(new StringReader(corpus), null, null, 2).induceLexicon();
        assertTrue("Expected 'UNK-LC' in lexicon", lexicon.contains("UNK-LC"));
        assertFalse("Did not expect 'e' in lexicon", lexicon.contains("e"));
        assertFalse("Did not expect 'f' in lexicon", lexicon.contains("f"));
    }

    @Test
    public void testInduceVocabulary() {
        final SymbolSet<String> unsortedVocabulary = sg.induceVocabulary(null);
        assertEquals(3, unsortedVocabulary.size());
        assertEquals(0, unsortedVocabulary.getIndex("top"));
        assertEquals(1, unsortedVocabulary.getIndex("a"));
        assertEquals(2, unsortedVocabulary.getIndex("b"));

        final SymbolSet<String> sortedVocabulary = sg.induceVocabulary(sg.binaryParentCountComparator());
        assertEquals(3, sortedVocabulary.size());
        assertEquals(0, sortedVocabulary.getIndex("top"));
        assertEquals(1, sortedVocabulary.getIndex("a"));
        assertEquals(2, sortedVocabulary.getIndex("b"));
    }

    @Test
    public void testInduceLexicon() {
        final SymbolSet<String> lexicon = sg.induceLexicon();
        assertEquals(2, lexicon.size());
        assertEquals(0, lexicon.getIndex("c"));
        assertEquals(1, lexicon.getIndex("d"));
    }

    @Test
    public void testBinaryProductions() {
        final ArrayList<Production> binaryProductions = sg.binaryProductions(sg.induceVocabulary(null));
        assertEquals(3, binaryProductions.size());
    }

    @Test
    public void testUnaryProductions() {
        final ArrayList<Production> unaryProductions = sg.unaryProductions(sg.induceVocabulary(null));
        assertEquals(2, unaryProductions.size());
        assertEquals(0, unaryProductions.get(0).parent); // s
        assertEquals(1, unaryProductions.get(0).leftChild); // a
        assertEquals(2, unaryProductions.get(1).parent); // b
        assertEquals(2, unaryProductions.get(1).leftChild); // b
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

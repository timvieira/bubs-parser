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

import org.junit.Before;

import edu.ohsu.cslu.grammar.SymbolSet;

/**
 * Unit tests for {@link MappedCountGrammar}.
 * 
 * @author Aaron Dunlop
 * @since Jan 13, 2011
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestMappedCountGrammar extends MappedCountGrammarTestCase {

    @Before
    public void setUp() {
        g = SAMPLE_MAPPED_GRAMMAR();
    }

    static MappedCountGrammar SAMPLE_MAPPED_GRAMMAR() {
        final SplitVocabulary vocabulary = new SplitVocabulary(new String[] { "top", "a", "b" });
        final SymbolSet<String> lexicon = new SymbolSet<String>(new String[] { "c", "d" });

        // Build up the same grammar as that induced from the tree in AllElviTests
        final MappedCountGrammar g = new MappedCountGrammar(vocabulary, lexicon);
        g.incrementUnaryCount("top", "a");
        g.incrementBinaryCount("a", "a", "b");
        g.incrementBinaryCount("a", "a", "b");
        g.incrementBinaryCount("a", "a", "a");
        g.incrementLexicalCount("a", "c");
        g.incrementLexicalCount("a", "c");
        g.incrementLexicalCount("b", "d");
        g.incrementBinaryCount("b", "b", "a");
        g.incrementUnaryCount("b", "b");
        g.incrementLexicalCount("b", "d");
        g.incrementLexicalCount("a", "d");

        return g;
    }
}

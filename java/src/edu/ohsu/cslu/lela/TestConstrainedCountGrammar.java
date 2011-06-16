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
package edu.ohsu.cslu.lela;

import org.junit.Before;

import edu.ohsu.cslu.grammar.SymbolSet;

/**
 * Unit tests for {@link ConstrainedCountGrammar}.
 * 
 * @author Aaron Dunlop
 * @since Jan 13, 2011
 */
public class TestConstrainedCountGrammar extends FractionalCountGrammarTestCase {

    @Before
    public void setUp() {
        g = SAMPLE_MAPPED_GRAMMAR();
    }

    static ConstrainedCountGrammar SAMPLE_MAPPED_GRAMMAR() {
        final SplitVocabulary vocabulary = new SplitVocabulary(new String[] { "top", "a", "b" });
        final SymbolSet<String> lexicon = new SymbolSet<String>(new String[] { "c", "d" });

        // Build up the same grammar as that induced from the tree in AllElviTests
        final ConstrainedCountGrammar g = new ConstrainedCountGrammar(vocabulary, lexicon, null);
        g.incrementUnaryCount("top", "a", 1);
        g.incrementBinaryCount("a", "a", "b", 1);
        g.incrementBinaryCount("a", "a", "b", 1);
        g.incrementBinaryCount("a", "a", "a", 1);
        g.incrementLexicalCount("a", "c", 1);
        g.incrementLexicalCount("a", "c", 1);
        g.incrementLexicalCount("b", "d", 1);
        g.incrementBinaryCount("b", "b", "a", 1);
        g.incrementUnaryCount("b", "b", 1);
        g.incrementLexicalCount("b", "d", 1);
        g.incrementLexicalCount("a", "d", 1);

        return g;
    }

}

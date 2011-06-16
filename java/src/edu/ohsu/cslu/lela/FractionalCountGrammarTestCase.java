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
import org.junit.Test;

import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.tests.JUnit;

public class FractionalCountGrammarTestCase extends CountGrammarTestCase {

    @Before
    public void setUp() {
        g = SAMPLE_GRAMMAR();
    }

    static FractionalCountGrammar SAMPLE_GRAMMAR() {
        final SplitVocabulary vocabulary = new SplitVocabulary(new String[] { "top", "a", "b" });
        final SymbolSet<String> lexicon = new SymbolSet<String>(new String[] { "c", "d" });

        // Build up the same grammar as that induced from the tree in AllElviTests
        final FractionalCountGrammar g = new FractionalCountGrammar(vocabulary, lexicon, null);
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

    private FractionalCountGrammar grammar() {
        final SplitVocabulary vocabulary = new SplitVocabulary(new String[] { "top", "a", "b" });
        final SymbolSet<String> lexicon = new SymbolSet<String>(new String[] { "c", "d" });
        final FractionalCountGrammar fcg = new FractionalCountGrammar(vocabulary, lexicon, null);

        // top -> a 1
        fcg.incrementUnaryCount("top", "a", 1);

        // a -> a b 5/12
        // a -> a a 3/12
        // a -> c 3/12
        // a -> d 1/12
        fcg.incrementBinaryCount("a", "a", "b", 1.5f);
        fcg.incrementBinaryCount("a", "a", "b", 1.0f);
        fcg.incrementBinaryCount("a", "a", "a", 1.5f);
        fcg.incrementLexicalCount("a", "c", .5f);
        fcg.incrementLexicalCount("a", "c", 1.0f);
        fcg.incrementLexicalCount("a", "d", .5f);

        // b -> b a 7/16
        // b -> b 3/16
        // b -> c 5/16
        // b -> d 1/16
        fcg.incrementBinaryCount("b", "b", "a", 3.5f);
        fcg.incrementUnaryCount("b", "b", 1.5f);
        fcg.incrementLexicalCount("b", "c", 2.5f);
        fcg.incrementLexicalCount("b", "d", .5f);
        return fcg;
    }

    @Test
    public void testFractionalCounts() {
        final FractionalCountGrammar fcg = grammar();

        final ProductionListGrammar plg = new ProductionListGrammar(fcg);
        JUnit.assertLogFractionEquals(0, plg.unaryLogProbability("top", "a"), 0.01f);

        JUnit.assertLogFractionEquals(Math.log(5f / 12), plg.binaryLogProbability("a", "a", "b"), 0.01f);
        JUnit.assertLogFractionEquals(Math.log(3f / 12), plg.binaryLogProbability("a", "a", "a"), 0.01f);
        JUnit.assertLogFractionEquals(Math.log(3f / 12), plg.lexicalLogProbability("a", "c"), 0.01f);
        JUnit.assertLogFractionEquals(Math.log(1f / 12), plg.lexicalLogProbability("a", "d"), 0.01f);

        JUnit.assertLogFractionEquals(Math.log(7f / 16), plg.binaryLogProbability("b", "b", "a"), 0.01f);
        JUnit.assertLogFractionEquals(Math.log(3f / 16), plg.unaryLogProbability("b", "b"), 0.01f);
        JUnit.assertLogFractionEquals(Math.log(5f / 16), plg.lexicalLogProbability("b", "c"), 0.01f);
        JUnit.assertLogFractionEquals(Math.log(1f / 16), plg.lexicalLogProbability("b", "d"), 0.01f);
    }

    // @Test
    // public void testSmooth() {
    // final MappedCountGrammar mcg = grammar();
    // mcg.smooth();
    //
    // }
}

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

import org.junit.Test;

import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.tests.JUnit;

public class MappedCountGrammarTestCase extends CountGrammarTestCase {

    private MappedCountGrammar grammar() {
        final SplitVocabulary vocabulary = new SplitVocabulary(new String[] { "top", "a", "b" });
        final SymbolSet<String> lexicon = new SymbolSet<String>(new String[] { "c", "d" });
        final MappedCountGrammar mcg = new MappedCountGrammar(vocabulary, lexicon);

        // top -> a 1
        mcg.incrementUnaryCount("top", "a", 1);

        // a -> a b 5/12
        // a -> a a 3/12
        // a -> c 3/12
        // a -> d 1/12
        mcg.incrementBinaryCount("a", "a", "b", 1.5f);
        mcg.incrementBinaryCount("a", "a", "b", 1.0f);
        mcg.incrementBinaryCount("a", "a", "a", 1.5f);
        mcg.incrementLexicalCount("a", "c", .5f);
        mcg.incrementLexicalCount("a", "c", 1.0f);
        mcg.incrementLexicalCount("a", "d", .5f);

        // b -> b a 7/16
        // b -> b 3/16
        // b -> c 5/16
        // b -> d 1/16
        mcg.incrementBinaryCount("b", "b", "a", 3.5f);
        mcg.incrementUnaryCount("b", "b", 1.5f);
        mcg.incrementLexicalCount("b", "c", 2.5f);
        mcg.incrementLexicalCount("b", "d", .5f);
        return mcg;
    }

    @Test
    public void testFractionalCounts() {
        final MappedCountGrammar mcg = grammar();

        final ProductionListGrammar plg = new ProductionListGrammar(mcg);
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

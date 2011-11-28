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

import static edu.ohsu.cslu.tests.JUnit.assertLogFractionEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;

import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

/**
 * Unit tests for {@link ProductionListGrammar}.
 * 
 * @author Aaron Dunlop
 */
@RunWith(Theories.class)
public class TestProductionListGrammar {

    @DataPoints
    public static ProductionListGrammar[] dataPoints() throws IOException {
        final ProductionListGrammar g1 = new ProductionListGrammar(new StringCountGrammar(new StringReader(
                AllLelaTests.STRING_SAMPLE_TREE), null, null));
        final ProductionListGrammar g2 = TestFractionalCountGrammar.SAMPLE_GRAMMAR().toProductionListGrammar(
                Float.NEGATIVE_INFINITY);
        return new ProductionListGrammar[] { g1, g2 };
    }

    @Theory
    public void testLexicalLogProbability(final ProductionListGrammar g) {
        assertEquals(-.4054, g.lexicalLogProbability("c", "e"), .0001f);
        assertEquals(-1.0986, g.lexicalLogProbability("c", "f"), .0001f);
        assertLogFractionEquals(0, g.lexicalLogProbability("d", "f"), .0001f);
        assertEquals(Float.NEGATIVE_INFINITY, g.lexicalLogProbability("d", "e"), .0001f);

    }

    @Theory
    public void testUnaryLogProbability(final ProductionListGrammar g) {
        assertEquals(-.6931, g.unaryLogProbability("b", "d"), .0001f);
        assertEquals(Float.NEGATIVE_INFINITY, g.unaryLogProbability("a", "b"), .0001f);
    }

    @Theory
    public void testBinaryLogProbability(final ProductionListGrammar g) {
        assertEquals(-1.0986f, g.binaryLogProbability("a", "a", "b"), .0001f);
        assertEquals(-1.0986f, g.binaryLogProbability("a", "c", "c"), .0001f);
        assertEquals(-.6931f, g.binaryLogProbability("b", "b", "c"), .0001f);
        assertEquals(Float.NEGATIVE_INFINITY, g.binaryLogProbability("b", "a", "a"), .0001f);
    }

    @Theory
    public void testPcfgString(final ProductionListGrammar g) {
        final StringBuilder pcfg = new StringBuilder(1024);
        // Start Symbol
        pcfg.append("top\n");
        // Binary Rules
        pcfg.append("a -> a b -1.098612\n");
        pcfg.append("a -> a d -1.098612\n");
        pcfg.append("a -> c c -1.098612\n");
        pcfg.append("b -> b c -0.693147\n");
        // Unary Rules
        pcfg.append("top -> a 0.000000\n");
        pcfg.append("b -> d -0.693147\n");
        assertEquals(pcfg.toString(), g.pcfgString());
    }

    @Theory
    public void testLexiconString(final ProductionListGrammar g) {
        final StringBuilder lexicon = new StringBuilder(512);
        lexicon.append("c -> e -0.405465\n");
        lexicon.append("c -> f -1.098612\n");
        lexicon.append("d -> f 0.000000\n");
        assertEquals(lexicon.toString(), g.lexiconString());
    }
}

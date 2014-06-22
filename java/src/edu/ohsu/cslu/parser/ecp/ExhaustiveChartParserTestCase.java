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
package edu.ohsu.cslu.parser.ecp;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.cjunit.DetailedTest;
import org.cjunit.FilteredRunner;
import org.cjunit.PerformanceTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.chart.Chart;
import edu.ohsu.cslu.parser.ml.SparseMatrixLoopParser;
import edu.ohsu.cslu.parser.spmv.SparseMatrixVectorParser;
import edu.ohsu.cslu.tests.JUnit;

/**
 * Base test case for all exhaustive parsers (or agenda-based parsers run to exhaustion). Tests a couple trivial
 * sentences using very simple grammars and the first 10 sentences of WSJ section 24 using a slightly more reasonable
 * PCFG. Profiles sentences 11-20 to aid in performance tuning and prevent performance regressions.
 * 
 * @author Aaron Dunlop
 * @since Dec 23, 2009
 */
@RunWith(FilteredRunner.class)
public abstract class ExhaustiveChartParserTestCase<P extends ChartParser<? extends Grammar, ? extends Chart>> extends
        ChartParserTestCase<P> {

    /**
     * Reads in the first 20 sentences of WSJ section 24. Run once for the class, prior to execution of the first test
     * method.
     * 
     * @throws Exception if unable to read
     */
    @BeforeClass
    public static void suiteSetUp() throws Exception {
        // Read test sentences
        // TODO Parameterize test sentences (this will require a custom Runner implementation)
        final BufferedReader tokenizedReader = new BufferedReader(new InputStreamReader(
                JUnit.unitTestDataAsStream("parsing/wsj.24.tokens.1-20")));

        // We have two major hierarchies of chart structures, one which stores constituents in a sorted array and one
        // which uses a HashSet. Parsers which use different chart implementations iterate over child constituents in
        // differing orders, which produces variances in tie-breaking behavior. For testing, we store two versions of
        // the 'correct' parse.
        final BufferedReader hscParsedReader = new BufferedReader(new InputStreamReader(
                JUnit.unitTestDataAsStream("parsing/wsj.24.parsed.R2.hsccyk.1-20")));

        // TODO Reconcile differing parse output between sparse-matrix-vector and sparse-matrix-loop parser hierarchies
        final BufferedReader spmvParsedReader = new BufferedReader(new InputStreamReader(
                JUnit.unitTestDataAsStream("parsing/wsj.24.parsed.R2.spmvcyk.1-20")));

        final BufferedReader spmlParsedReader = new BufferedReader(new InputStreamReader(
                JUnit.unitTestDataAsStream("parsing/wsj.24.parsed.R2.spmlcyk.1-20")));

        for (String sentence = tokenizedReader.readLine(); sentence != null; sentence = tokenizedReader.readLine()) {
            sentences.add(new String[] { sentence, hscParsedReader.readLine(), spmvParsedReader.readLine(),
                    spmlParsedReader.readLine() });
        }
    }

    /**
     * Tests parsing with a _very_ simple grammar.
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testSimpleGrammar1() throws Exception {
        parser = createParser(simpleGrammar1, parserOptions(), configProperties());

        String parse = parser.parseSentence("systems analyst arbitration chef").parseBracketString(true);
        assertEquals("(ROOT (NP (NP (NP (NN systems) (NN analyst)) (NN arbitration)) (NN chef)))", parse);

        // Tests with an unknown word.
        parse = parser.parseSentence("systems XXX arbitration chef").parseBracketString(true);
        assertEquals("(ROOT (NP (NP (NP (NN systems) (NN XXX)) (NN arbitration)) (NN chef)))", parse);

        parse = parser.parseSentence("systems analyst arbitration XXX").parseBracketString(true);
        assertEquals("(ROOT (NP (NP (NP (NN systems) (NN analyst)) (NN arbitration)) (NN XXX)))", parse);
    }

    /**
     * Tests parsing with a slightly larger grammar.
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testSimpleGrammar2() throws Exception {
        parser = createParser(simpleGrammar2, parserOptions(), configProperties());

        String parse = parser.parseSentence("The fish market stands last").parseBracketString(true);
        assertEquals("(ROOT (S (NP (DT The) (NP (NN fish) (NN market))) (VP (VB stands) (RB last))))", parse);

        // Tests with an unknown word
        parse = parser.parseSentence("The XXX market stands last").parseBracketString(true);
        assertEquals("(ROOT (S (NP (DT The) (NP (NN XXX) (NN market))) (VP (VB stands) (RB last))))", parse);

        parse = parser.parseSentence("The fish market stands XXX").parseBracketString(true);
        assertEquals("(ROOT (S (NP (DT The) (NN fish)) (VP (VB market) (VP|VB (NP (NN stands) (NN XXX))))))", parse);

        parse = parser.parseSentence("The fish market stands last XXX").parseBracketString(true);
        assertEquals("()", parse);

        //
        // Specific tests with words starting with '@' and containing '|' (to validate unfactoring the tree if
        // lexical items contain special characters)
        //

        parse = parser.parseSentence("The fish market @stands last").parseBracketString(true);
        assertEquals("(ROOT (S (NP (DT The) (NN fish)) (VP (VB market) (VP|VB (NP (NN @stands) (RB last))))))", parse);

        parse = parser.parseSentence("The fish market stands|X last").parseBracketString(true);
        assertEquals("(ROOT (S (NP (DT The) (NN fish)) (VP (VB market) (VP|VB (NP (NN stands|X) (RB last))))))", parse);
    }

    @Test
    @DetailedTest
    public void testSentence1() throws Exception {
        parseTreebankSentence(0);
    }

    @Test
    public void testSentence2() throws Exception {
        parseTreebankSentence(1);
    }

    @Test
    @DetailedTest
    public void testSentence3() throws Exception {
        parseTreebankSentence(2);
    }

    @Test
    @DetailedTest
    public void testSentence4() throws Exception {
        parseTreebankSentence(3);
    }

    @Test
    @DetailedTest
    public void testSentence5() throws Exception {
        parseTreebankSentence(4);
    }

    @Test
    @DetailedTest
    public void testSentence6() throws Exception {
        parseTreebankSentence(5);
    }

    @Test
    public void testSentence7() throws Exception {
        parseTreebankSentence(6);
    }

    @Test
    @DetailedTest
    public void testSentence8() throws Exception {
        parseTreebankSentence(7);
    }

    @Test
    @DetailedTest
    public void testSentence9() throws Exception {
        parseTreebankSentence(8);
    }

    @Test
    @DetailedTest
    public void testSentence10() throws Exception {
        parseTreebankSentence(9);
    }

    /**
     * Profiles parsing sentences 11-20 of WSJ section 24. This method must be overridden (calling
     * {@link #internalProfileSentences11Through20()}) in each subclass, simply to allow re-annotating the
     * {@link PerformanceTest} annotation with the expected performance for that implementation.
     * 
     * @throws Exception
     */
    @Test
    @PerformanceTest({ "mbp", "0", "mbp2012", "0" })
    public abstract void profileSentences11Through20() throws Exception;

    protected void internalProfileSentences11Through20() throws Exception {
        for (int i = 10; i < 20; i++) {
            parseTreebankSentence(i);
        }
    }

    protected void parseTreebankSentence(final int index) throws Exception {
        final String parse = parser.parseSentence(sentences.get(index)[0]).parseBracketString(true);

        String correctParse;
        final Class<P> parserClass = parserClass();
        if (SparseMatrixVectorParser.class.isAssignableFrom(parserClass)) {
            correctParse = sentences.get(index)[2];
        } else if (SparseMatrixLoopParser.class.isAssignableFrom(parserClass)) {
            correctParse = sentences.get(index)[3];
        } else {
            correctParse = sentences.get(index)[1];
        }

        // System.out.println(parse);
        assertEquals(correctParse, parse);
    }

}

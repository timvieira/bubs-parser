/*
 * Copyright 2010-2012 Aaron Dunlop and Nathan Bodenstab
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

package edu.ohsu.cslu.parser.ml;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;

import org.cjunit.FilteredRunner;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import cltool4j.ConfigProperties;
import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.grammar.DecisionTreeTokenClassifier;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.InsideOutsideCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashPackingFunction;
import edu.ohsu.cslu.parser.Parser;
import edu.ohsu.cslu.parser.Parser.DecodeMethod;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.SparseMatrixParser;
import edu.ohsu.cslu.parser.chart.PackedArrayChart;
import edu.ohsu.cslu.parser.fom.InsideProb;
import edu.ohsu.cslu.tests.JUnit;

@RunWith(FilteredRunner.class)
public class TestViterbiInOutCphSpmlParser {

    private InsideOutsideCscSparseMatrixGrammar grammar;
    private SparseMatrixParser<InsideOutsideCscSparseMatrixGrammar, PackedArrayChart> parser;

    /** WSJ section 24 sentences 1-20 */
    protected static ArrayList<String[]> sentences = new ArrayList<String[]>();

    /**
     * Reads in the first 20 sentences of WSJ section 24. Run once for the class, prior to execution of the first test
     * method.
     * 
     * @throws Exception if unable to read
     */
    @BeforeClass
    public static void suiteSetUp() throws Exception {
        // Read test sentences
        final BufferedReader tokenizedReader = new BufferedReader(new InputStreamReader(
                JUnit.unitTestDataAsStream("parsing/wsj.24.tokens.1-20")));

        for (String sentence = tokenizedReader.readLine(); sentence != null; sentence = tokenizedReader.readLine()) {
            sentences.add(new String[] { sentence });
        }
    }

    @Before
    public void setUp() throws Exception {
        grammar = new InsideOutsideCscSparseMatrixGrammar(JUnit.unitTestDataAsReader("grammars/eng.R2.gr.gz"),
                new DecisionTreeTokenClassifier(), PerfectIntPairHashPackingFunction.class);

        final ConfigProperties props = GlobalConfigProperties.singleton();
        props.put(Parser.PROPERTY_MAX_BEAM_WIDTH, "30");
        props.put(Parser.PROPERTY_LEXICAL_ROW_BEAM_WIDTH, "30");
        props.put(Parser.PROPERTY_LEXICAL_ROW_UNARIES, "10");
        props.put(Parser.PROPERTY_MAX_LOCAL_DELTA, "15");
        props.put(Parser.PROPERTY_MAXC_LAMBDA, "0.5");
    }

    @After
    public void tearDown() {
        if (parser != null) {
            parser.shutdown();
        }
    }

    @AfterClass
    public static void suiteTearDown() {
        GlobalConfigProperties.singleton().clear();
    }

    /** Simple grammar for parsing 'The fish market stands last' */
    public static Reader simpleGrammar2() throws Exception {
        final StringBuilder sb = new StringBuilder(256);
        sb.append("format=Berkeley start=ROOT\n");
        sb.append("S => NP VP 0\n"); // 1
        sb.append("ROOT => S 0\n"); // 1
        sb.append("NP => DT NP -1.386294361\n"); // 1/4
        sb.append("NP => DT NN -1.386294361\n"); // 1/4
        sb.append("NP => NN NN -1.791759469\n"); // 1/6
        sb.append("NP => @NP NN -1.791759469\n"); // 1/6
        sb.append("NP => NN RB -1.791759469\n"); // 1/6
        sb.append("@NP => NN NN 0\n"); // 1
        sb.append("VP => VB RB -0.693147181\n"); // 1/2
        sb.append("VP => VB -0.693147181\n"); // 1/2

        sb.append(Grammar.LEXICON_DELIMITER);
        sb.append('\n');

        sb.append("DT => The 0\n");

        sb.append("NN => fish -0.980829253\n"); // 3/8
        sb.append("NN => market -2.079441542\n"); // 1/8
        sb.append("NN => stands -1.386294361\n"); // 1/4
        sb.append("NN => UNK -1.386294361\n"); // 1/4

        sb.append("VB => market -1.098612289\n"); // 1/3
        sb.append("VB => stands -1.098612289\n"); // 1/3
        sb.append("VB => last -1.098612289\n"); // 1/3

        sb.append("RB => last 0\n"); // 1

        return new StringReader(sb.toString());
    }

    @Test
    public void testSimpleGrammar2Goodman() throws Exception {

        final ParserDriver opts = new ParserDriver();
        opts.decodeMethod = DecodeMethod.Goodman;
        opts.fomModel = new InsideProb();
        final String sentence = "The fish market stands last";

        // Max-recall decoding
        GlobalConfigProperties.singleton().setProperty(Parser.PROPERTY_MAXC_LAMBDA, "0");
        parser = new ViterbiInOutCphSpmlParser(opts, new InsideOutsideCscSparseMatrixGrammar(simpleGrammar2(),
                new DecisionTreeTokenClassifier(), PerfectIntPairHashPackingFunction.class));
        assertEquals("(ROOT (S (NP (DT The) (NP (NN fish) (NN market))) (VP (VB stands) (RB last))))", parser
                .parseSentence(sentence).parseBracketString(false));

        // Max-precision decoding
        GlobalConfigProperties.singleton().setProperty(Parser.PROPERTY_MAXC_LAMBDA, "1");
        parser = new ViterbiInOutCphSpmlParser(opts, new InsideOutsideCscSparseMatrixGrammar(simpleGrammar2(),
                new DecisionTreeTokenClassifier(), PerfectIntPairHashPackingFunction.class));
        assertEquals("(ROOT (S (NP (DT The) (NP (NN fish) (NN market))) (VP (VB stands) (RB last))))", parser
                .parseSentence(sentence).parseBracketString(false));
    }

    /**
     * Tests summing over split categories
     */
    @Test
    public void testSimpleGrammar2SplitSum() throws Exception {

        final ParserDriver opts = new ParserDriver();
        opts.decodeMethod = DecodeMethod.SplitSum;
        opts.fomModel = new InsideProb();
        final String sentence = "The fish market stands last";

        // Max-recall decoding
        GlobalConfigProperties.singleton().setProperty(Parser.PROPERTY_MAXC_LAMBDA, "0");
        parser = new ViterbiInOutCphSpmlParser(opts, new InsideOutsideCscSparseMatrixGrammar(simpleGrammar2(),
                new DecisionTreeTokenClassifier(), PerfectIntPairHashPackingFunction.class));
        assertEquals("(ROOT (S (NP (DT The) (NP (NN fish) (NN market))) (VP (VB stands) (RB last))))", parser
                .parseSentence(sentence).parseBracketString(false));

        // Max-precision decoding
        GlobalConfigProperties.singleton().setProperty(Parser.PROPERTY_MAXC_LAMBDA, "1");
        parser = new ViterbiInOutCphSpmlParser(opts, new InsideOutsideCscSparseMatrixGrammar(simpleGrammar2(),
                new DecisionTreeTokenClassifier(), PerfectIntPairHashPackingFunction.class));
        assertEquals("(ROOT (S (DT The) (NN fish) (NN market) (VP (VB stands) (RB last))))",
                parser.parseSentence(sentence).parseBracketString(false));
    }

    @Test
    public void testSimpleGrammar2MaxRuleProd() throws Exception {

        final ParserDriver opts = new ParserDriver();
        opts.decodeMethod = DecodeMethod.MaxRuleProd;
        opts.fomModel = new InsideProb();
        final String sentence = "The fish market stands last";

        parser = new ViterbiInOutCphSpmlParser(opts, new InsideOutsideCscSparseMatrixGrammar(simpleGrammar2(),
                new DecisionTreeTokenClassifier(), PerfectIntPairHashPackingFunction.class));
        assertEquals("(ROOT (S (NP (DT The) (NP (NN fish) (NN market))) (VP (VB stands) (RB last))))", parser
                .parseSentence(sentence).parseBracketString(false));
    }

    @Test
    public void testPartialSentence2Goodman() throws Exception {

        final ParserDriver opts = new ParserDriver();
        opts.decodeMethod = DecodeMethod.Goodman;
        opts.fomModel = new InsideProb();
        parser = new ViterbiInOutCphSpmlParser(opts, grammar);
        assertEquals(
                "(ROOT (S (NP (DT The) (NN report)) (VP (VBZ is) (ADJP (JJ due) (PP (IN out) (NP (NN tomorrow))))) (. .)))",
                parser.parseSentence("The report is due out tomorrow .").parseBracketString(false));
    }

    @Test
    public void testPartialSentence2SplitSum() throws Exception {

        final ParserDriver opts = new ParserDriver();
        opts.decodeMethod = DecodeMethod.SplitSum;
        opts.fomModel = new InsideProb();
        parser = new ViterbiInOutCphSpmlParser(opts, grammar);
        assertEquals(
                "(ROOT (S (NP (DT The) (NN report)) (VP (VBZ is) (ADJP (JJ due) (PP (IN out) (NP (NN tomorrow))))) (. .)))",
                parser.parseSentence("The report is due out tomorrow .").parseBracketString(false));
    }

    @Test
    public void testPartialSentence2MaxRuleProd() throws Exception {

        final ParserDriver opts = new ParserDriver();
        opts.decodeMethod = DecodeMethod.MaxRuleProd;
        opts.fomModel = new InsideProb();
        parser = new ViterbiInOutCphSpmlParser(opts, grammar);
        assertEquals(
                "(ROOT (S (NP (DT The) (NN report)) (VP (VBZ is) (ADJP (JJ due) (PP (IN out) (NP (NN tomorrow))))) (. .)))",
                parser.parseSentence("The report is due out tomorrow .").parseBracketString(false));
    }

    @Test
    public void testSentence2SplitSum() throws Exception {
        final ParserDriver opts = new ParserDriver();
        opts.decodeMethod = DecodeMethod.SplitSum;
        opts.fomModel = new InsideProb();
        parser = new ViterbiInOutCphSpmlParser(opts, grammar);
        assertEquals(
                "(ROOT (S (NP (DT The) (ADJP (RBS most) (JJ troublesome)) (NN report)) (VP (MD may) (VP (VB be) (NP (DT the) (NNP August) (NN merchandise) (NN trade) (NN deficit)) (PP (JJ due) (PP (IN out) (NP (NN tomorrow)))))) (. .)))",
                parser.parseSentence(sentences.get(1)[0]).parseBracketString(false));
    }

    @Test
    public void testSentence2MaxRuleProd() throws Exception {
        final ParserDriver opts = new ParserDriver();
        opts.decodeMethod = DecodeMethod.MaxRuleProd;
        opts.fomModel = new InsideProb();
        parser = new ViterbiInOutCphSpmlParser(opts, grammar);
        assertEquals(
                "(ROOT (S (NP (DT The) (ADJP (RBS most) (JJ troublesome)) (NN report)) (VP (MD may) (VP (VB be) (NP (DT the) (NNP August) (NN merchandise) (NN trade) (NN deficit)) (PP (JJ due) (IN out) (NP (NN tomorrow))))) (. .)))",
                parser.parseSentence(sentences.get(1)[0]).parseBracketString(false));
    }

}

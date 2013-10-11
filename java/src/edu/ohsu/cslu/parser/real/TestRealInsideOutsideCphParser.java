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

package edu.ohsu.cslu.parser.real;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;

import org.cjunit.FilteredRunner;
import org.cjunit.PerformanceTest;
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
import edu.ohsu.cslu.parser.Parser;
import edu.ohsu.cslu.parser.Parser.DecodeMethod;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.fom.InsideProb;
import edu.ohsu.cslu.parser.ml.TestInsideOutsideCphSpmlParser;
import edu.ohsu.cslu.tests.JUnit;

/**
 * Test cases for {@link RealInsideOutsideCphParser}. This is mostly copy-and-paste code from
 * {@link TestInsideOutsideCphSpmlParser}. We could probably share a lot of code between the two, but if the real-valued
 * version works, we probably don't need to keep the log-domain version anyway.
 * 
 * @author Aaron Dunlop
 * @since May 9, 2013
 */
@RunWith(FilteredRunner.class)
public class TestRealInsideOutsideCphParser {

    private RealInsideOutsideCscSparseMatrixGrammar grammar;
    private RealInsideOutsideCphParser parser;

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
        grammar = new RealInsideOutsideCscSparseMatrixGrammar(JUnit.unitTestDataAsReader("grammars/eng.R2.gr.gz"),
                new DecisionTreeTokenClassifier());

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
        parser = new RealInsideOutsideCphParser(opts, new RealInsideOutsideCscSparseMatrixGrammar(simpleGrammar2(),
                new DecisionTreeTokenClassifier()));
        assertEquals("(ROOT (S (NP (DT The) (NP (NN fish) (NN market))) (VP (VB stands) (RB last))))", parser
                .parseSentence(sentence).parseBracketString(false));

        // Max-precision decoding
        GlobalConfigProperties.singleton().setProperty(Parser.PROPERTY_MAXC_LAMBDA, "1");
        parser = new RealInsideOutsideCphParser(opts, new RealInsideOutsideCscSparseMatrixGrammar(simpleGrammar2(),
                new DecisionTreeTokenClassifier()));
        assertEquals("(ROOT (S (NP (DT The) (NN fish) (NN market)) (VP (VB stands) (RB last))))",
                parser.parseSentence(sentence).parseBracketString(false));
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
        parser = new RealInsideOutsideCphParser(opts, new RealInsideOutsideCscSparseMatrixGrammar(simpleGrammar2(),
                new DecisionTreeTokenClassifier()));
        assertEquals("(ROOT (S (NP (DT The) (NP (NN fish) (NN market))) (VP (VB stands) (RB last))))", parser
                .parseSentence(sentence).parseBracketString(false));

        // Max-precision decoding
        GlobalConfigProperties.singleton().setProperty(Parser.PROPERTY_MAXC_LAMBDA, "1");
        parser = new RealInsideOutsideCphParser(opts, new RealInsideOutsideCscSparseMatrixGrammar(simpleGrammar2(),
                new DecisionTreeTokenClassifier()));
        assertEquals("(ROOT (S (DT The) (NN fish) (NN market) (VB stands) (RB last)))", parser.parseSentence(sentence)
                .parseBracketString(false));
    }

    @Test
    public void testSimpleGrammar2MaxRuleProd() throws Exception {

        final ParserDriver opts = new ParserDriver();
        opts.decodeMethod = DecodeMethod.MaxRuleProd;
        opts.fomModel = new InsideProb();
        final String sentence = "The fish market stands last";

        parser = new RealInsideOutsideCphParser(opts, new RealInsideOutsideCscSparseMatrixGrammar(simpleGrammar2(),
                new DecisionTreeTokenClassifier()));
        assertEquals("(ROOT (S (NP (DT The) (NP (NN fish) (NN market))) (VP (VB stands) (RB last))))", parser
                .parseSentence(sentence).parseBracketString(false));
    }

    @Test
    public void testPartialSentence2Goodman() throws Exception {

        final ParserDriver opts = new ParserDriver();
        opts.decodeMethod = DecodeMethod.Goodman;
        opts.fomModel = new InsideProb();
        parser = new RealInsideOutsideCphParser(opts, grammar);
        assertEquals(
                "(ROOT (S (NP (DT The) (NN report)) (VP (VBZ is) (ADJP (JJ due) (PP (IN out) (NP (NN tomorrow))))) (. .)))",
                parser.parseSentence("The report is due out tomorrow .").parseBracketString(false));
    }

    @Test
    public void testPartialSentence2SplitSum() throws Exception {

        final ParserDriver opts = new ParserDriver();
        opts.decodeMethod = DecodeMethod.SplitSum;
        opts.fomModel = new InsideProb();
        parser = new RealInsideOutsideCphParser(opts, grammar);
        assertEquals(
                "(ROOT (S (NP (DT The) (NN report)) (VP (VBZ is) (ADJP (JJ due) (PP (IN out) (NP (NN tomorrow))))) (. .)))",
                parser.parseSentence("The report is due out tomorrow .").parseBracketString(false));
    }

    @Test
    public void testPartialSentence2MaxRuleProd() throws Exception {

        final ParserDriver opts = new ParserDriver();
        opts.decodeMethod = DecodeMethod.MaxRuleProd;
        opts.fomModel = new InsideProb();
        parser = new RealInsideOutsideCphParser(opts, grammar);
        assertEquals(
                "(ROOT (S (NP (DT The) (NN report)) (VP (VBZ is) (ADJP (JJ due) (PP (IN out) (NP (NN tomorrow))))) (. .)))",
                parser.parseSentence("The report is due out tomorrow .").parseBracketString(false));
    }

    @Test
    public void testSentence2SplitSum() throws Exception {
        final ParserDriver opts = new ParserDriver();
        opts.decodeMethod = DecodeMethod.SplitSum;
        opts.fomModel = new InsideProb();
        parser = new RealInsideOutsideCphParser(opts, grammar);
        assertEquals(
                "(ROOT (S (NP (DT The) (ADJP (RBS most) (JJ troublesome)) (NN report)) (VP (MD may) (VP (VB be) (NP (DT the) (NNP August) (NN merchandise) (NN trade) (NN deficit)) (JJ due) (PP (IN out) (NP (NN tomorrow))))) (. .)))",
                parser.parseSentence(sentences.get(1)[0]).parseBracketString(false));
    }

    @Test
    public void testSentence2Goodman() throws Exception {
        final ParserDriver opts = new ParserDriver();
        opts.decodeMethod = DecodeMethod.Goodman;
        opts.fomModel = new InsideProb();
        parser = new RealInsideOutsideCphParser(opts, grammar);
        assertEquals(
                "(ROOT (S (NP (DT The) (ADJP (RBS most) (JJ troublesome)) (NN report)) (VP (MD may) (VP (VB be) (NP (DT the) (NNP August) (NN merchandise) (NN trade) (NN deficit)) (JJ due) (PP (IN out) (NP (NN tomorrow))))) (. .)))",
                parser.parseSentence(sentences.get(1)[0]).parseBracketString(false));
    }

    @Test
    public void testSentence2MaxRuleProd() throws Exception {
        final ParserDriver opts = new ParserDriver();
        opts.decodeMethod = DecodeMethod.MaxRuleProd;
        opts.fomModel = new InsideProb();
        parser = new RealInsideOutsideCphParser(opts, grammar);
        assertEquals(
                "(ROOT (S (NP (DT The) (ADJP (RBS most) (JJ troublesome)) (NN report)) (VP (MD may) (VP (VB be) (NP (DT the) (NNP August) (NN merchandise) (NN trade) (NN deficit)) (PP (JJ due) (IN out) (NP (NN tomorrow))))) (. .)))",
                parser.parseSentence(sentences.get(1)[0]).parseBracketString(false));
    }

    @Test
    @PerformanceTest({ "mbp", "5701", "mbp2012", "2850" })
    public void profileMaxRule() throws Exception {
        final ParserDriver opts = new ParserDriver();
        opts.decodeMethod = DecodeMethod.MaxRuleProd;
        opts.fomModel = new InsideProb();
        parser = new RealInsideOutsideCphParser(opts, grammar);

        for (int i = 10; i < 20; i++) {
            parser.parseSentence(sentences.get(i)[0]);
        }
    }

    @Test
    public void testM0MR() throws Exception {

        final ParserDriver opts = new ParserDriver();
        opts.decodeMethod = DecodeMethod.MaxRuleProd;
        opts.fomModel = new InsideProb();

        StringBuilder sb = new StringBuilder(2048);
        sb.append("(TOP (S (NP (NP (JJ Influential) (NNS members)) (PP (IN of) (NP (DT the) (NNP House) (NNP Ways) (CC and) (NNP Means) (NNP Committee)))) (VP (VBD introduced) (NP (NP (NN legislation)) (SBAR (WHNP (WDT that)) (S (VP (MD would) (VP (VB restrict) (SBAR (WHADVP (WRB how)) (S (NP (DT the) (JJ new) (NN savings-and-loan) (NN bailout) (NN agency)) (VP (MD can) (VP (VB raise) (NP (NN capital)))))) (, ,) (S (VP (VBG creating) (NP (NP (DT another) (JJ potential) (NN obstacle)) (PP (TO to) (NP (NP (NP (DT the) (NN government) (POS 's)) (NN sale)) (PP (IN of) (NP (JJ sick) (NNS thrifts)))))))))))))) (. .)))\n");
        sb.append("(TOP (S (NP (NP (DT The) (NN bill)) (, ,) (SBAR (WHNP (WP$ whose) (NNS backers)) (S (VP (VBP include) (NP (NP (NNP Chairman) (NNP Dan) (NNP Rostenkowski)) (PRN (-LRB- -LRB-) (NP (NNP D.)) (, ,) (NP (NNP Ill.)) (-RRB- -RRB-)))))) (, ,)) (VP (MD would) (VP (VB prevent) (NP (DT the) (NNP Resolution) (NNP Trust) (NNP Corp.)) (PP (IN from) (S (VP (VBG raising) (NP (JJ temporary) (VBG working) (NN capital))))) (PP (IN by) (S (VP (VBG having) (NP (NP (DT an) (JJ RTC-owned) (NN bank) (CC or) (NN thrift) (NN issue) (NN debt)) (SBAR (WHNP (WDT that)) (S (VP (MD would) (RB n't) (VP (VB be) (VP (VBN counted) (PP (IN on) (NP (DT the) (JJ federal) (NN budget)))))))))))))) (. .)))\n");
        sb.append("(TOP (S (NP (DT The) (NN bill)) (VP (VBZ intends) (S (VP (TO to) (VP (VB restrict) (NP (DT the) (NNP RTC)) (PP (TO to) (NP (NNP Treasury) (NNS borrowings) (RB only)))))) (, ,) (SBAR (IN unless) (S (NP (DT the) (NN agency)) (VP (VBZ receives) (NP (JJ specific) (JJ congressional) (NN authorization)))))) (. .)))\n");
        sb.append("(TOP (SINV (`` ``) (S (NP (JJ Such) (NN agency) (`` `) (NN self-help) ('' ') (NN borrowing)) (VP (VBZ is) (ADJP (ADJP (JJ unauthorized) (CC and) (JJ expensive)) (, ,) (ADJP (ADJP (RB far) (RBR more) (JJ expensive)) (PP (IN than) (NP (JJ direct) (NNP Treasury) (NN borrowing))))))) (, ,) ('' '') (VP (VBD said)) (NP (NP (NP (NNP Rep.) (NNP Fortney) (NNP Stark)) (PRN (-LRB- -LRB-) (NP (NNP D.)) (, ,) (NP (NNP Calif.)) (-RRB- -RRB-))) (, ,) (NP (NP (DT the) (NN bill) (POS 's)) (JJ chief) (NN sponsor))) (. .)))\n");
        sb.append("(TOP (S (NP (NP (DT The) (JJ complex) (NN financing) (NN plan)) (PP (IN in) (NP (DT the) (NN S&L) (NN bailout) (NN law)))) (VP (VBZ includes) (S (VP (VBG raising) (NP (QP ($ $) (CD 30) (CD billion))) (PP (IN from) (NP (NP (NN debt)) (VP (VBN issued) (PP (IN by) (NP (DT the) (ADJP (RB newly) (VBN created)) (NNP RTC))))))))) (. .)))\n");
        sb.append("(TOP (S (NP (DT This) (NN financing) (NN system)) (VP (VBD was) (VP (VBN created) (PP (IN in) (NP (DT the) (JJ new) (NN law))) (SBAR (IN in) (NN order) (S (VP (TO to) (VP (VB keep) (NP (DT the) (NN bailout) (NN spending)) (PP (IN from) (S (VP (VBG swelling) (NP (DT the) (NN budget) (NN deficit))))))))))) (. .)))\n");
        sb.append("(TOP (S (NP (QP (DT Another) ($ $) (CD 20) (CD billion))) (VP (MD would) (VP (VB be) (VP (VBN raised) (PP (IN through) (NP (NP (NNP Treasury) (NNS bonds)) (, ,) (SBAR (WHNP (WDT which)) (S (VP (VBP pay) (NP (JJR lower) (NN interest) (NNS rates)))))))))) (. .)))\n");
        sb.append("(TOP (S (CC But) (NP (DT the) (NNP RTC)) (ADVP (RB also)) (VP (VBZ requires) (NP (NP (`` ``) (NN working) ('' '') (NN capital)) (SBAR (S (VP (TO to) (VP (VB maintain) (NP (NP (DT the) (JJ bad) (NNS assets)) (PP (IN of) (NP (NP (NNS thrifts)) (SBAR (WHNP (WDT that)) (S (VP (VBP are) (VP (VBN sold)))))))) (, ,) (SBAR (IN until) (S (NP (DT the) (NNS assets)) (VP (MD can) (VP (VB be) (VP (VBN sold) (ADVP (RB separately))))))))))))) (. .)))\n");
        sb.append("(TOP (S (NP (DT That) (NN debt)) (VP (MD would) (VP (VB be) (VP (VBN paid) (PRT (RP off)) (SBAR (IN as) (S (NP (DT the) (NNS assets)) (VP (VBP are) (VP (VBN sold))))) (, ,) (S (VP (VBG leaving) (NP (NP (DT the) (JJ total) (NN spending)) (PP (IN for) (NP (DT the) (NN bailout)))) (PP (IN at) (NP (NP (QP ($ $) (CD 50) (CD billion))) (, ,) (CC or) (NP (NP (QP ($ $) (CD 166) (CD billion))) (PP (VBG including) (NP (NP (NN interest)) (PP (IN over) (NP (CD 10) (NNS years))))))))))))) (. .)))\n");
        sb.append("(TOP (SINV (`` ``) (S (NP (PRP It)) (VP (VBZ 's) (NP (NP (DT a) (NN problem)) (SBAR (WHNP (WDT that)) (S (ADVP (RB clearly)) (VP (VBZ has) (S (VP (TO to) (VP (VB be) (VP (VBN resolved))))))))))) (, ,) ('' '') (VP (VBD said)) (NP (NP (NNP David) (NNP Cooke)) (, ,) (NP (NP (JJ executive) (NN director)) (PP (IN of) (NP (DT the) (NNP RTC))))) (. .)))\n");
        sb.append("(TOP (S (S (NP (DT The) (NN agency)) (VP (VBZ has) (ADVP (RB already)) (VP (VBN spent) (NP (QP (RB roughly) ($ $) (CD 19) (CD billion))) (S (VP (VBG selling) (NP (CD 34) (JJ insolvent) (NNS S&Ls))))))) (, ,) (CC and) (S (NP (PRP it)) (VP (VBZ is) (ADJP (JJ likely) (S (VP (TO to) (VP (VB sell) (CC or) (VB merge) (NP (CD 600)) (PP (IN by) (NP (NP (DT the) (NN time)) (SBAR (S (NP (DT the) (NN bailout)) (VP (VBZ concludes)))))))))))) (. .)))\n");
        sb.append("(TOP (S (S (ADJP (VB Absent)) (NP (JJ other) (NN working) (NN capital))) (PRN (, ,) (S (NP (PRP he)) (VP (VBD said))) (, ,)) (NP (DT the) (NNP RTC)) (VP (MD would) (VP (VB be) (VP (VBN forced) (S (VP (TO to) (VP (VB delay) (NP (JJ other) (NN thrift) (NNS resolutions)) (SBAR (IN until) (S (NP (NN cash)) (VP (MD could) (VP (VB be) (VP (VBN raised) (PP (IN by) (S (VP (VBG selling) (NP (DT the) (JJ bad) (NNS assets)))))))))))))))) (. .)))\n");
        sb.append("(TOP (S (`` ``) (S (NP (PRP We)) (VP (MD would) (VP (VB have) (S (VP (TO to) (VP (VB wait) (SBAR (IN until) (S (NP (PRP we)) (VP (VBP have) (VP (VBN collected) (PP (IN on) (NP (DT those) (NNS assets))))))) (SBAR (IN before) (S (NP (PRP we)) (VP (MD can) (VP (VB move) (ADVP (RB forward)))))))))))) (, ,) ('' '') (NP (PRP he)) (VP (VBD said)) (. .)))\n");
        sb.append("(TOP (S (NP (NP (DT The) (VBN complicated) (NN language)) (PP (IN in) (NP (DT the) (JJ huge) (JJ new) (NN law)))) (VP (VBZ has) (VP (VBN muddied) (NP (DT the) (NN fight)))) (. .)))\n");
        sb.append("(TOP (S (NP (DT The) (NN law)) (VP (VBZ does) (VP (VB allow) (S (NP (DT the) (NNP RTC)) (VP (TO to) (VP (VB borrow) (PP (IN from) (NP (DT the) (NNP Treasury))) (NP (QP (IN up) (TO to) ($ $) (CD 5) (CD billion))) (PP (IN at) (NP (DT any) (NN time)))))))) (. .)))\n");
        sb.append("(TOP (S (ADVP (RB Moreover)) (, ,) (S (NP (PRP it)) (VP (VBZ says) (SBAR (S (NP (NP (DT the) (NNP RTC) (POS 's)) (JJ total) (NNS obligations)) (VP (MD may) (RB not) (VP (VB exceed) (NP (QP ($ $) (CD 50) (CD billion))))))))) (, ,) (CC but) (S (NP (DT that) (NN figure)) (VP (VBZ is) (VP (VBN derived) (PP (IN after) (S (VP (VP (VBG including) (NP (NP (NNS notes)) (CC and) (NP (JJ other) (NN debt)))) (, ,) (CC and) (VP (VBG subtracting) (PP (IN from) (NP (PRP it))) (NP (NP (DT the) (NN market) (NN value)) (PP (IN of) (NP (NP (DT the) (NNS assets)) (SBAR (S (NP (DT the) (NNP RTC)) (VP (VBZ holds)))))))))))))) (. .)))\n");
        sb.append("(TOP (S (SINV (S (CC But) (NP (NNP Congress)) (VP (VBD did) (RB n't) (VP (VB anticipate) (CC or) (VB intend) (NP (JJR more) (JJ public) (NN debt))))) (, ,) (VP (VBP say)) (NP (NP (NNS opponents)) (PP (IN of) (NP (NP (DT the) (NNP RTC) (POS 's)) (JJ working-capital) (NN plan))))) (, ,) (CC and) (S (NP (NP (NNP Rep.) (NNP Charles) (NNP Schumer)) (PRN (-LRB- -LRB-) (NP (NNP D.)) (, ,) (NP (NNP N.Y) (. .)) (-RRB- -RRB-))) (VP (VBD said) (SBAR (S (NP (DT the) (NNP RTC) (NNP Oversight) (NNP Board)) (VP (VBZ has) (VP (VBN been) (ADJP (JJ remiss) (PP (IN in) (S (VP (RB not) (VBG keeping) (S (NP (NNP Congress)) (ADJP (VBN informed))))))))))))) (. .)))\n");
        sb.append("(TOP (S (`` ``) (S (NP (DT That) (NN secrecy)) (VP (VBZ leads) (PP (TO to) (NP (NP (DT a) (NN proposal)) (PP (IN like) (NP (NP (DT the) (CD one)) (PP (IN from) (NP (NNP Ways) (CC and) (NNP Means))) (, ,) (SBAR (WHNP (WDT which)) (S (VP (VBZ seems) (PP (TO to) (NP (PRP me))) (ADJP (ADVP (NN sort) (IN of)) (JJ draconian))))))))))) (, ,) ('' '') (NP (PRP he)) (VP (VBD said)) (. .)))\n");
        sb.append("(TOP (S (`` ``) (NP (DT The) (NNP RTC)) (VP (VBZ is) (VP (VBG going) (S (VP (TO to) (VP (VB have) (S (VP (TO to) (VP (VB pay) (NP (NP (DT a) (NN price)) (PP (IN of) (NP (NP (JJ prior) (NN consultation)) (PP (IN on) (NP (DT the) (NNP Hill)))))))))))) (SBAR (IN if) (S (NP (PRP they)) (VP (VBP want) (NP (NP (DT that) (NN kind)) (PP (IN of) (NP (NN flexibility))))))))) (. .) ('' '')))\n");
        sb.append("(TOP (S (NP (DT The) (NNP Ways) (CC and) (NNP Means) (NNP Committee)) (VP (MD will) (VP (VB hold) (NP (NP (DT a) (NN hearing)) (PP (IN on) (NP (DT the) (NN bill)))) (NP (IN next) (NNP Tuesday)))) (. .)))\n");
        final String[] input = sb.toString().split("\n");

        sb = new StringBuilder(2048);
        sb.append("(ROOT (S (NP (NP (JJ Influential) (NNS members)) (PP (IN of) (NP (DT the) (NP (NNP House) (NNPS Ways)) (CC and) (NP (NNP Means) (NNP Committee))))) (VP (VBD introduced) (NP (NN legislation)) (SBAR (WHNP (WDT that)) (S (VP (MD would) (VP (VB restrict) (SBAR (WHADVP (WRB how)) (S (NP (DT the) (JJ new) (JJ savings-and-loan) (NN bailout) (NN agency)) (VP (MD can) (VP (VB raise) (NP (NN capital) (, ,) (VP (VBG creating) (NP (NP (DT another) (JJ potential) (NN obstacle)) (PP (TO to) (NP (DT the) (NN government) (POS 's))))) (NN sale)) (PP (IN of) (NP (JJ sick) (NNS thrifts)))))))))))) (. .)))\n");
        sb.append("(ROOT (S (NP (DT The) (NN bill)) (, ,) (SBAR (WHNP (WP$ whose) (NNS backers)) (S (VP (VB include) (NP (NNP Chairman) (NNP Dan) (NNP Rostenkowski))))) (PRN (-LRB- -LRB-) (NP (NP (NNP D.)) (, ,) (NNP Ill.)) (-RRB- -RRB-)) (, ,) (VP (MD would) (VP (VB prevent) (NP (NP (DT the) (NNP Resolution) (NP (NP (NP (NNP Trust) (NNP Corp.)) (PP (IN from) (NP (NP (VBG raising) (JJ temporary) (VBG working) (NN capital)) (PP (IN by) (NP (VBG having) (DT an) (JJ RTC-owned) (NN bank)))))) (CC or) (NN thrift)) (NN issue) (NN debt)) (SBAR (WHNP (WDT that)) (S (VP (MD would) (RB n't) (VP (VB be) (VP (VBN counted) (PP (IN on) (NP (DT the) (JJ federal) (NN budget))))))))))) (. .)))\n");
        sb.append("(ROOT (S (NP (DT The) (NN bill)) (VP (VBZ intends) (VP (TO to) (VP (VB restrict) (S (NP (NP (DT the) (NNP RTC)) (PP (TO to) (NP (NP (NNP Treasury) (NNS borrowings)) (VP (ADVP (RB only)) (, ,) (PP (IN unless) (NP (DT the) (NN agency))))))) (VP (VBZ receives) (NP (JJ specific) (JJ congressional) (NN authorization))))))) (. .)))\n");
        sb.append("(ROOT (SINV (`` ``) (S (NP (NP (JJ Such) (NN agency)) (`` `) (JJ self-help) ('' ') (NN borrowing)) (VP (VBZ is) (ADJP (ADJP (JJ unauthorized) (CC and) (JJ expensive)) (, ,) (RB far) (RBR more) (JJ expensive) (PP (IN than) (NP (JJ direct) (NNP Treasury) (NN borrowing)))))) (, ,) ('' '') (VP (VBD said) (NP (NNP Rep.) (NP (NP (NNP Fortney) (NNP Stark)) (PRN (-LRB- -LRB-) (NP (NP (NNP D.)) (, ,) (NNP Calif.)) (-RRB- -RRB-))) (, ,) (NP (DT the) (NN bill)) (POS 's))) (NP (JJ chief) (NN sponsor)) (. .)))\n");
        sb.append("(ROOT (S (NP (DT The) (NP (JJ complex) (NN financing) (NN plan)) (PP (IN in) (NP (DT the) (NN S&L) (NN bailout))) (NN law)) (VP (VBZ includes) (VP (VBG raising) (NP (NP (QP ($ $) (CD 30) (CD billion))) (PP (IN from) (NP (NP (NN debt)) (VP (VBN issued) (PP (IN by) (NP (DT the) (ADJP (RB newly) (VBN created)) (NNP RTC))))))))) (. .)))\n");
        sb.append("(ROOT (S (NP (DT This) (NN financing) (NN system)) (VP (VBD was) (VP (VBN created) (PP (IN in) (NP (NP (DT the) (JJ new) (NN law)) (PP (IN in) (NP (NP (NN order)) (VP (TO to) (VP (VB keep) (NP (NP (DT the) (NN bailout) (NN spending)) (PP (IN from) (NP (VBG swelling) (DT the) (NN budget) (NN deficit)))))))))))) (. .)))\n");
        sb.append("(ROOT (S (NP (DT Another) (QP ($ $) (CD 20) (CD billion))) (VP (MD would) (VP (VB be) (VP (VBD raised) (PP (IN through) (NP (NNP Treasury) (NNS bonds))) (, ,) (SBAR (WHNP (WDT which)) (S (VP (VB pay) (NP (JJR lower) (NN interest) (NNS rates)))))))) (. .)))\n");
        sb.append("(ROOT (S (CC But) (NP (DT the) (NNP RTC)) (VP (ADVP (RB also)) (VBZ requires) (`` ``) (VP (VBG working) (S ('' '') (NP (NN capital)) (VP (TO to) (VP (VB maintain) (NP (NP (DT the) (JJ bad) (NNS assets)) (PP (IN of) (NP (NP (NNS thrifts)) (SBAR (WHNP (WDT that)) (S (VP (VBP are) (VP (VBN sold) (, ,) (PP (IN until) (NP (DT the) (NNS assets)))))))))))) (VP (MD can) (VP (VB be) (VP (VBN sold) (ADVP (RB separately)))))))) (. .)))\n");
        sb.append("(ROOT (S (NP (DT That) (NN debt)) (VP (MD would) (VP (VB be) (VP (VBN paid) (PRT (RP off)) (SBAR (IN as) (S (NP (DT the) (NNS assets)) (VP (VBP are) (VP (VBN sold) (, ,) (VBG leaving) (NP (NP (DT the) (JJ total) (NN spending)) (PP (IN for) (NP (NP (DT the) (NN bailout)) (PP (IN at) (NP (NP (NP (QP ($ $) (CD 50) (CD billion))) (PP (, ,) (CC or) (NP (QP ($ $) (CD 166) (CD billion))))) (VBG including) (NN interest)))))) (PP (IN over) (NP (CD 10) (NNS years)))))))))) (. .)))\n");
        sb.append("(ROOT (SINV (`` ``) (S (NP (PRP It)) (VP (VBZ 's) (NP (NP (DT a) (NN problem)) (SBAR (WHNP (WDT that)) (S (ADVP (RB clearly)) (VP (VBZ has) (VP (TO to) (VP (VB be) (VP (VBN resolved)))))))))) (, ,) ('' '') (VP (VBD said) (NP (NNP David) (NNP Cooke) (, ,))) (NP (NP (JJ executive) (NN director)) (PP (IN of) (NP (DT the) (NNP RTC)))) (. .)))\n");
        sb.append("(ROOT (S (NP (DT The) (NN agency)) (VP (VBZ has) (RB already) (VP (VBN spent) (S (ADVP (RB roughly)) (S (NP (QP ($ $) (CD 19) (CD billion))) (VP (VBG selling) (NP (CD 34) (JJ insolvent) (NNS S&Ls)))) (, ,) (CC and) (NP (PRP it)) (VP (VBZ is) (RB likely) (VP (TO to) (VP (VB sell) (VP (CC or) (VB merge) (NP (NP (CD 600)) (PP (IN by) (NP (DT the) (NN time)))) (S (NP (DT the) (NN bailout)) (VP (VBZ concludes))))))) (. .))))))\n");
        sb.append("(ROOT (S (PP (IN Absent) (NP (JJ other) (VBG working) (NN capital))) (, ,) (NP (PRP he)) (VP (VBD said) (S (, ,) (NP (DT the) (NNP RTC)) (VP (MD would) (VP (VB be) (VP (VBN forced) (VP (TO to) (VP (VB delay) (NP (JJ other) (NN thrift) (NNS resolutions)) (PP (IN until) (NP (NN cash)))))))) (VP (MD could) (VP (VB be) (VP (VBN raised) (PP (IN by) (NP (NN selling) (DT the) (JJ bad) (NNS assets)))))))) (. .)))\n");
        sb.append("(ROOT (S (`` ``) (S (NP (PRP We)) (VP (MD would) (VP (VBP have) (VP (TO to) (VP (VB wait) (SBAR (IN until) (S (NP (PRP we)) (VP (VBP have) (VP (VBN collected) (PP (IN on) (NP (NP (DT those) (NNS assets)) (PP (IN before) (NP (NP (PRP we)) (VP (MD can) (VP (VB move) (ADVP (RB forward))))))))))))))))) (, ,) ('' '') (NP (PRP he)) (VP (VBD said)) (. .)))\n");
        sb.append("(ROOT (S (NP (NP (DT The) (JJ complicated) (NN language)) (PP (IN in) (NP (DT the) (JJ huge) (JJ new) (NN law)))) (VP (VBZ has) (VP (VBN muddied) (NP (DT the) (NN fight)))) (. .)))\n");
        sb.append("(ROOT (S (NP (DT The) (NN law)) (VP (VBZ does) (VP (VB allow) (NP (DT the) (NNP RTC)) (VP (TO to) (VP (VB borrow) (PP (IN from) (NP (NP (NP (DT the) (NNP Treasury)) (PP (IN up) (NP (QP (TO to) ($ $) (CD 5) (CD billion))))) (PP (IN at) (NP (DT any) (NN time))))))))) (. .)))\n");
        sb.append("(ROOT (S (ADVP (RB Moreover)) (, ,) (S (NP (PRP it)) (VP (VBZ says) (NP (DT the) (NNP RTC) (POS 's)))) (NP (JJ total) (NNS obligations)) (VP (MD may) (RB not) (VP (VB exceed) (NP (NP (QP ($ $) (CD 50) (CD billion))) (SBAR (, ,) (CC but) (SBAR (WHNP (WDT that)) (S (VP (VBP figure) (VP (VBZ is) (VP (VBN derived) (PP (IN after) (NP (NP (NP (NP (NP (NP (NP (VBG including) (NNS notes)) (CC and) (NP (JJ other) (NN debt) (, ,))) (CC and) (NN subtracting)) (PP (IN from) (NP (PRP it)))) (DT the) (NN market) (NN value)) (PP (IN of) (NP (DT the) (NNS assets)))) (DT the) (NNP RTC) (VBZ holds)))))))))))) (. .)))\n");
        sb.append("(ROOT (S (CC But) (NP (NNP Congress)) (VP (VBD did) (RB n't) (VP (VBP anticipate) (VP (CC or) (VB intend) (NP (NP (JJR more) (JJ public) (NN debt)) (PRN (, ,) (SINV (VP (VBP say) (NP (NP (NNS opponents)) (PP (IN of) (NP (NP (DT the) (NP (NNP RTC) (POS 's)) (NP (JJ working-capital) (NN plan) (, ,)) (CC and) (NP (NNP Rep.) (NNP Charles) (NNP Schumer))) (PRN (-LRB- -LRB-) (NP (NNP D.)) (, ,)))))) (NP (NNP N.Y)) (. .)) (-RRB- -RRB-))) (VP (VBD said) (S (NP (DT the) (NNP RTC) (NNP Oversight) (NNP Board)) (VP (VBZ has) (VP (VBN been) (VP (VB remiss) (SBAR (IN in) (S (ADVP (RB not)) (VP (VBG keeping) (S (NP (NNP Congress)) (VP (VBN informed)))))))))))))) (. .)))\n");
        sb.append("(ROOT (S (`` ``) (NP (DT That) (NN secrecy)) (VP (VBZ leads) (VP (TO to) (VP (NP (NP (DT a) (NN proposal)) (PP (IN like) (NP (NP (NP (DT the) (NN one)) (PP (IN from) (NP (NNPS Ways) (CC and) (NNPS Means)))) (, ,) (SBAR (WHNP (WDT which)) (S (VP (VBZ seems) (PP (TO to) (NP (PRP me)))))) (NN sort)))) (PP (IN of) (NP (JJ draconian) (, ,) ('' '') (NP (PRP he)))) (VP (VBD said))))) (. .)))\n");
        sb.append("(ROOT (S (`` ``) (NP (DT The) (NNP RTC)) (VP (VBZ is) (VP (VBG going) (VP (TO to) (VP (VBP have) (VP (TO to) (VP (VB pay) (NP (NP (DT a) (NN price)) (PP (IN of) (NP (NP (JJ prior) (NN consultation)) (PP (IN on) (NP (DT the) (NNP Hill) (SBAR (IN if) (S (NP (PRP they)) (VP (VB want) (NP (DT that) (NN kind)) (PP (IN of) (NP (NN flexibility)))) (. .)))))))))))))) ('' '')))\n");
        sb.append("(ROOT (S (NP (DT The) (NNPS Ways) (CC and) (NP (NNP Means) (NNP Committee))) (VP (MD will) (VP (VB hold) (NP (NP (DT a) (NN hearing)) (PP (IN on) (NP (NP (DT the) (NN bill)) (PP (IN next) (NP (NNP Tuesday)))))))) (. .)))\n");

        final String[] expected = sb.toString().split("\n");

        GlobalConfigProperties.singleton().setProperty(Parser.PROPERTY_MAXC_LAMBDA, "0");
        parser = new RealInsideOutsideCphParser(opts, new RealInsideOutsideCscSparseMatrixGrammar(
                JUnit.unitTestDataAsReader("grammars/eng.R0.gr.gz"), new DecisionTreeTokenClassifier()));
        for (int i = 0; i < input.length; i++) {
            assertEquals("Failed on sentence " + i, expected[i],
                    parser.parseSentence(input[i]).parseBracketString(false));
        }
    }
    // @Test
    // @Ignore
    // public void testAll() throws IOException {
    //
    // final BufferedReader tokenizedReader = new BufferedReader(new InputStreamReader(
    // JUnit.unitTestDataAsStream("parsing/wsj.24.tokens.1-20")));
    //
    // final BufferedReader parsedReader = new BufferedReader(new InputStreamReader(
    // JUnit.unitTestDataAsStream("parsing/wsj.24.parsed.R2.beam.fom.1-20")));
    //
    // int i = 1;
    // for (String sentence = tokenizedReader.readLine(); sentence != null; sentence = tokenizedReader.readLine()) {
    // final String parsedSentence = parsedReader.readLine();
    // System.out.println(parser.parseSentence(sentence).binaryParse.toString());
    // i++;
    // }
    // }
}

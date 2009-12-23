package edu.ohsu.cslu.parser;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.parser.traversal.ChartTraversal.ChartTraversalType;
import edu.ohsu.cslu.parser.util.ParseTree;
import edu.ohsu.cslu.tests.FilteredRunner;
import edu.ohsu.cslu.tests.PerformanceTest;
import edu.ohsu.cslu.tests.SharedNlpTests;

/**
 * Base test case for all exhaustive parsers (or agenda-based parsers run to exhaustion). Tests a trivial
 * sentence using a very simple grammar and the first 10 sentences of WSJ section 24 using a reasonable PCFG.
 * Profiles sentences 11-20 to aid in performance tuning and prevent performance regressions.
 * 
 * TODO More documentation
 * 
 * @author Aaron Dunlop
 * @since Dec 23, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
@RunWith(FilteredRunner.class)
public abstract class ExhaustiveChartParserTestCase {

    private final static String PCFG_FILE = "grammars/f2-21-R2-p1-unk.pcfg";
    private final static String LEX_FILE = "grammars/f2-21-R2-p1-unk.lex";

    protected static Grammar f2_21_grammar;
    private static ArrayList<String[]> sentences = new ArrayList<String[]>();
    private MaximumLikelihoodParser parser;

    protected abstract MaximumLikelihoodParser createParser(Grammar grammar,
            ChartTraversalType chartTraversalType);

    protected abstract Class<? extends Grammar> grammarClass();

    private Grammar createGrammar(final Reader grammarReader, final Reader lexiconReader) throws Exception {
        return grammarClass().getConstructor(new Class[] { Reader.class, Reader.class }).newInstance(
            new Object[] { grammarReader, lexiconReader });
    }

    @BeforeClass
    public static void suiteSetUp() throws Exception {
        // Read test sentences
        // TODO Parameterize test sentences (this will require a custom Runner implementation)
        final BufferedReader tokenizedReader = new BufferedReader(new InputStreamReader(SharedNlpTests
            .unitTestDataAsStream("parsing/wsj_24.mrgEC.tokens.1-20")));

        final BufferedReader parsedReader = new BufferedReader(new InputStreamReader(SharedNlpTests
            .unitTestDataAsStream("parsing/wsj_24.mrgEC.parsed.1-20")));

        for (String sentence = tokenizedReader.readLine(); sentence != null; sentence = tokenizedReader
            .readLine()) {
            final String parsedSentence = parsedReader.readLine();
            sentences.add(new String[] { sentence, parsedSentence });
        }
    }

    @Before
    public void setUp() throws Exception {
        if (f2_21_grammar == null || f2_21_grammar.getClass() != grammarClass()) {
            f2_21_grammar = createGrammar(SharedNlpTests.unitTestDataAsReader(PCFG_FILE), SharedNlpTests
                .unitTestDataAsReader(LEX_FILE));
        }

        // TODO Parameterize ChartTraversalType (this will require a custom Runner implementation)
        parser = createParser(f2_21_grammar, ChartTraversalType.LeftRightBottomTopTraversal);
    }

    @Test
    public void testSimpleGrammar() throws Exception {
        final String sentence = "systems analyst arbitration chef";

        final StringBuilder lexiconSb = new StringBuilder(256);
        lexiconSb.append("NN => systems 0\n");
        lexiconSb.append("NN => analyst 0\n");
        lexiconSb.append("NN => arbitration 0\n");
        lexiconSb.append("NN => chef 0\n");
        lexiconSb.append("NN => UNK 0\n");

        final StringBuilder grammarSb = new StringBuilder(256);
        grammarSb.append("TOP\n");
        grammarSb.append("TOP => NP 0\n");
        grammarSb.append("NP => NN NN -0.693147\n");
        grammarSb.append("NP => NP NN -1.203972\n");
        grammarSb.append("NP => NN NP -2.302585\n");
        grammarSb.append("NP => NP NP -2.302585\n");

        final Grammar simpleGrammar = createGrammar(new StringReader(grammarSb.toString()), new StringReader(
            lexiconSb.toString()));
        final MaximumLikelihoodParser p = createParser(simpleGrammar,
            ChartTraversalType.LeftRightBottomTopTraversal);

        final ParseTree bestParseTree = p.findMLParse(sentence);
        assertEquals("(TOP (NP (NP (NP (NN systems) (NN analyst)) (NN arbitration)) (NN chef)))",
            bestParseTree.toString());
    }

    @Test
    public void testSentence1() throws Exception {
        parseSentence(0);
    }

    @Test
    public void testSentence2() throws Exception {
        parseSentence(1);
    }

    @Test
    public void testSentence3() throws Exception {
        parseSentence(2);
    }

    @Test
    public void testSentence4() throws Exception {
        parseSentence(3);
    }

    @Test
    public void testSentence5() throws Exception {
        parseSentence(4);
    }

    @Test
    public void testSentence6() throws Exception {
        parseSentence(5);
    }

    @Test
    public void testSentence7() throws Exception {
        parseSentence(6);
    }

    @Test
    public void testSentence8() throws Exception {
        parseSentence(7);
    }

    @Test
    public void testSentence9() throws Exception {
        parseSentence(8);
    }

    @Test
    public void testSentence10() throws Exception {
        parseSentence(9);
    }

    @Test
    @PerformanceTest( { "mbp", "9495" })
    public abstract void profileSentences11Through20() throws Exception;

    protected void internalProfileSentences11Through20() throws Exception {
        for (int i = 10; i < 20; i++) {
            parseSentence(i);
        }
    }

    protected void parseSentence(final int index) throws Exception {
        final ParseTree bestParseTree = parser.findMLParse(sentences.get(index)[0]);
        assertEquals(sentences.get(index)[1], bestParseTree.toString());
    }

}

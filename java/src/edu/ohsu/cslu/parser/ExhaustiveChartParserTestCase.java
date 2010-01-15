package edu.ohsu.cslu.parser;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.GrammarTestCase;
import edu.ohsu.cslu.parser.ParserDriver.GrammarFormatType;
import edu.ohsu.cslu.parser.traversal.ChartTraversal.ChartTraversalType;
import edu.ohsu.cslu.parser.util.ParseTree;
import edu.ohsu.cslu.tests.FilteredRunner;
import edu.ohsu.cslu.tests.PerformanceTest;
import edu.ohsu.cslu.tests.SharedNlpTests;

/**
 * Base test case for all exhaustive parsers (or agenda-based parsers run to exhaustion). Tests a trivial sentence using a very simple grammar and the first 10 sentences of WSJ
 * section 24 using a reasonable PCFG. Profiles sentences 11-20 to aid in performance tuning and prevent performance regressions.
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

    // Grammar file paths, relative to unit test data root directory
    private final static String PCFG_FILE = "grammars/f2-21-R2-p1-unk.pcfg";
    private final static String LEX_FILE = "grammars/f2-21-R2-p1-unk.lex";

    /** Grammar induced from WSJ sections 2-21 */
    protected static Grammar f2_21_grammar;

    /** WSJ section 24 sentences 1-20 */
    private static ArrayList<String[]> sentences = new ArrayList<String[]>();

    /** The parser under test */
    protected MaximumLikelihoodParser parser;

    /**
     * Creates the appropriate parser for each test class.
     * 
     * @param grammar The grammar to use when parsing
     * @param chartTraversalType The chart traversal order
     * @return Parser instance
     */
    protected abstract MaximumLikelihoodParser createParser(Grammar grammar, ChartTraversalType chartTraversalType);

    /**
     * @return the grammar class appropriate for the parser under test
     */
    protected abstract Class<? extends Grammar> grammarClass();

    private Grammar createGrammar(final Reader grammarReader, final Reader lexiconReader) throws Exception {
        return grammarClass().getConstructor(new Class[] { Reader.class, Reader.class, GrammarFormatType.class }).newInstance(
                new Object[] { grammarReader, lexiconReader, GrammarFormatType.CSLU });
    }

    /**
     * Reads in the first 20 sentences of WSJ section 24. Run once for the class, prior to execution of the first test method.
     * 
     * @throws Exception if unable to read
     */
    @BeforeClass
    public static void suiteSetUp() throws Exception {
        // Read test sentences
        // TODO Parameterize test sentences (this will require a custom Runner implementation)
        final BufferedReader tokenizedReader = new BufferedReader(new InputStreamReader(SharedNlpTests.unitTestDataAsStream("parsing/wsj_24.mrgEC.tokens.1-20")));

        final BufferedReader parsedReader = new BufferedReader(new InputStreamReader(SharedNlpTests.unitTestDataAsStream("parsing/wsj_24.mrgEC.parsed.1-20")));

        for (String sentence = tokenizedReader.readLine(); sentence != null; sentence = tokenizedReader.readLine()) {
            final String parsedSentence = parsedReader.readLine();
            sentences.add(new String[] { sentence, parsedSentence });
        }
    }

    /**
     * Constructs the grammar (if necessary) and a new parser instance. Run prior to each test method.
     * 
     * @throws Exception if unable to construct grammar or parser.
     */
    @Before
    public void setUp() throws Exception {
        if (f2_21_grammar == null || f2_21_grammar.getClass() != grammarClass()) {
            f2_21_grammar = createGrammar(SharedNlpTests.unitTestDataAsReader(PCFG_FILE), SharedNlpTests.unitTestDataAsReader(LEX_FILE));
        }

        // TODO Parameterize ChartTraversalType (this will require a custom Runner implementation)
        parser = createParser(f2_21_grammar, ChartTraversalType.LeftRightBottomTopTraversal);
    }

    /**
     * Tests parsing with a _very_ simple grammar.
     * 
     * TODO Share grammar creation with GrammarTestCase
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testSimpleGrammar() throws Exception {
        final String sentence = "systems analyst arbitration chef";

        final Grammar simpleGrammar = GrammarTestCase.createSimpleGrammar(grammarClass());
        final MaximumLikelihoodParser p = createParser(simpleGrammar, ChartTraversalType.LeftRightBottomTopTraversal);

        final ParseTree bestParseTree = p.findMLParse(sentence);
        assertEquals("(TOP (NP (NP (NP (NN systems) (NN analyst)) (NN arbitration)) (NN chef)))", bestParseTree.toString());
    }

    @Test
    public void testSentence1() throws Exception {
        parseTreebankSentence(0);
    }

    @Test
    public void testSentence2() throws Exception {
        parseTreebankSentence(1);
    }

    @Test
    public void testSentence3() throws Exception {
        parseTreebankSentence(2);
    }

    @Test
    public void testSentence4() throws Exception {
        parseTreebankSentence(3);
    }

    @Test
    public void testSentence5() throws Exception {
        parseTreebankSentence(4);
    }

    @Test
    public void testSentence6() throws Exception {
        parseTreebankSentence(5);
    }

    @Test
    public void testSentence7() throws Exception {
        parseTreebankSentence(6);
    }

    @Test
    public void testSentence8() throws Exception {
        parseTreebankSentence(7);
    }

    @Test
    public void testSentence9() throws Exception {
        parseTreebankSentence(8);
    }

    @Test
    public void testSentence10() throws Exception {
        parseTreebankSentence(9);
    }

    /**
     * Profiles parsing sentences 11-20 of WSJ section 24. This method must be overridden (calling {@link #internalProfileSentences11Through20()}) in each subclass, simply to allow
     * re-annotating the {@link PerformanceTest} annotation with the expected performance for that implementation.
     * 
     * @throws Exception
     */
    @Test
    @PerformanceTest( { "mbp", "0" })
    public abstract void profileSentences11Through20() throws Exception;

    protected void internalProfileSentences11Through20() throws Exception {
        for (int i = 10; i < 20; i++) {
            parseTreebankSentence(i);
        }
    }

    protected void parseTreebankSentence(final int index) throws Exception {
        final ParseTree bestParseTree = parser.findMLParse(sentences.get(index)[0]);
        assertEquals(sentences.get(index)[1], bestParseTree.toString());
    }

}

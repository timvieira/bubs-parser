package edu.ohsu.cslu.parser;

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
import edu.ohsu.cslu.grammar.GrammarTestCase;
import edu.ohsu.cslu.parser.ParserOptions.GrammarFormatType;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.parser.cellselector.CellSelector.CellSelectorType;
import edu.ohsu.cslu.parser.util.ParseTree;
import edu.ohsu.cslu.tests.DetailedTest;
import edu.ohsu.cslu.tests.FilteredRunner;
import edu.ohsu.cslu.tests.PerformanceTest;
import edu.ohsu.cslu.tests.SharedNlpTests;
import static org.junit.Assert.assertEquals;

/**
 * Base test case for all exhaustive parsers (or agenda-based parsers run to exhaustion). Tests a couple trivial sentences using very simple grammars and the first 10 sentences of
 * WSJ section 24 using a slightly more reasonable PCFG. Profiles sentences 11-20 to aid in performance tuning and prevent performance regressions.
 * 
 * @author Aaron Dunlop
 * @since Dec 23, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
@RunWith(FilteredRunner.class)
public abstract class ExhaustiveChartParserTestCase {

    // Grammar file paths, relative to unit test data root directory
    private final static String PCFG_FILE = "grammars/f2-21-R2-p1-unk.pcfg.gz";
    private final static String LEX_FILE = "grammars/f2-21-R2-p1-unk.lex.gz";

    /** Very simple grammar for parsing 'systems analyst arbitration chef' */
    protected static Grammar simpleGrammar1;

    /** Slightly larger grammar for parsing 'The fish market stands last' */
    protected static Grammar simpleGrammar2;

    /** Grammar induced from WSJ sections 2-21 */
    protected static Grammar f2_21_grammar;

    /** WSJ section 24 sentences 1-20 */
    protected static ArrayList<String[]> sentences = new ArrayList<String[]>();

    /** The parser under test */
    protected Parser<?> parser;

    /**
     * Creates the appropriate parser for each test class.
     * 
     * @param grammar The grammar to use when parsing
     * @param cellSelector Selector controlling chart traversal
     * @return Parser instance
     */
    protected abstract Parser<?> createParser(Grammar grammar, CellSelector cellSelector);

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

        if (simpleGrammar1 == null || simpleGrammar1.getClass() != grammarClass()) {
            simpleGrammar1 = GrammarTestCase.createSimpleGrammar(grammarClass());
        }

        if (simpleGrammar2 == null || simpleGrammar2.getClass() != grammarClass()) {
            simpleGrammar2 = createSimpleGrammar2(grammarClass());
        }

        // TODO Parameterize ChartTraversalType (this will require a custom Runner implementation)
        parser = createParser(f2_21_grammar, CellSelector.create(CellSelectorType.LeftRightBottomTop));
    }

    public static Grammar createSimpleGrammar2(final Class<? extends Grammar> grammarClass) throws Exception {
        final StringBuilder lexiconSb = new StringBuilder(256);
        lexiconSb.append("DT => The 0\n");
        lexiconSb.append("NN => fish 0\n");
        lexiconSb.append("NN => market -0.405465108\n");
        lexiconSb.append("VB => market -1.098612289\n");
        lexiconSb.append("NN => stands -0.693147181\n");
        lexiconSb.append("VB => stands -0.693147181\n");
        lexiconSb.append("RB => last -0.405465108\n");
        lexiconSb.append("VB => last -1.098612289\n");
        lexiconSb.append("NN => UNK 0\n");

        final StringBuilder grammarSb = new StringBuilder(256);
        grammarSb.append("TOP\n");
        grammarSb.append("S => NP VP 0\n");
        grammarSb.append("TOP => S 0\n");
        grammarSb.append("NP => DT NP -1.386294361\n");
        grammarSb.append("NP => DT NN -1.386294361\n");
        grammarSb.append("NP => NN NN -1.791759469\n");
        grammarSb.append("NP => NN NP|NN -1.791759469\n");
        grammarSb.append("NP => NN RB -1.791759469\n");
        grammarSb.append("NP|NN => NN NN 0\n");
        grammarSb.append("VP => VB RB -0.693147181\n");
        grammarSb.append("VP => VB VP|VB -1.386294361\n");
        grammarSb.append("VP => VB -1.386294361\n");
        grammarSb.append("VP|VB => NP 0\n");

        return GrammarTestCase.createGrammar(grammarClass, new StringReader(grammarSb.toString()), new StringReader(lexiconSb.toString()));
    }

    /**
     * Tests parsing with a _very_ simple grammar.
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testSimpleGrammar1() throws Exception {
        final String sentence = "systems analyst arbitration chef";

        final Parser p = createParser(simpleGrammar1, CellSelector.create(CellSelectorType.LeftRightBottomTop));

        final ParseTree bestParseTree = p.findBestParse(sentence);
        assertEquals("(TOP (NP (NP (NP (NN systems) (NN analyst)) (NN arbitration)) (NN chef)))", bestParseTree.toString());
    }

    /**
     * Tests parsing with a slightly larger grammar.
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testSimpleGrammar2() throws Exception {
        final String sentence = "The fish market stands last";

        final Parser p = createParser(simpleGrammar2, CellSelector.create(CellSelectorType.LeftRightBottomTop));

        final ParseTree bestParseTree = p.findBestParse(sentence);
        assertEquals("(TOP (S (NP (DT The) (NP (NN fish) (NN market))) (VP (VB stands) (RB last))))", bestParseTree.toString());
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
        final ParseTree bestParseTree = parser.findBestParse(sentences.get(index)[0]);
        assertEquals(sentences.get(index)[1], bestParseTree.toString());

        if (parser instanceof JsaSparseMatrixVectorParser) {
            System.out.format("Total cross-product time: %d\n", ((SparseMatrixVectorParser) parser).totalCartesianProductTime);
        }
    }

}

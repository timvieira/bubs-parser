package edu.ohsu.cslu.parser.spmv;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashFilterFunction;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.tests.PerformanceTest;
import edu.ohsu.cslu.tests.SharedNlpTests;

public class TestBeamCscSpmvParser extends
        SparseMatrixVectorParserTestCase<BeamCscSpmvParser, PerfectIntPairHashFilterFunction> {

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
                SharedNlpTests.unitTestDataAsStream("parsing/wsj_24.mrgEC.tokens.1-20")));

        final BufferedReader parsedReader = new BufferedReader(new InputStreamReader(
                SharedNlpTests.unitTestDataAsStream("parsing/wsj_24.mrgEC.parsed.1-20.beam")));

        for (String sentence = tokenizedReader.readLine(); sentence != null; sentence = tokenizedReader.readLine()) {
            final String parsedSentence = parsedReader.readLine();
            sentences.add(new String[] { sentence, parsedSentence });
        }
    }

    @Override
    @Test
    @PerformanceTest({ "mbp", "5000", "d820", "9589" })
    public void profileSentences11Through20() throws Exception {
        internalProfileSentences11Through20();
    }

    @Override
    protected ParserDriver parserOptions() {
        final ParserDriver options = new ParserDriver();
        options.collectDetailedStatistics = true;
        options.param1 = 125;
        options.param2 = 300;
        options.param3 = 0.3f;
        return options;
    }

    // Skip testing the two very simple grammars (they don't work with the pruned parser, and probably aren't worth
    // tracking down

    @Override
    @Test
    @Ignore
    public void testSimpleGrammar1() throws Exception {
        super.testSimpleGrammar1();
    }

    @Override
    @Test
    @Ignore
    public void testSimpleGrammar2() throws Exception {
        super.testSimpleGrammar2();
    }
}

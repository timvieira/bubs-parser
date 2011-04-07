package edu.ohsu.cslu.parser.spmv;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import cltool4j.ConfigProperties;
import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashPackingFunction;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.edgeselector.BoundaryInOut;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector.EdgeSelectorType;
import edu.ohsu.cslu.tests.JUnit;

/**
 * Tests FOM-pruned parsing, using row-level threading.
 * 
 * @author Aaron Dunlop
 * @since Mar 9, 2011
 * 
 * @version $Revision$ $Date$ $Author$
 */
public abstract class PrunedSpmvParserTestCase<G extends SparseMatrixGrammar> {

    private PackedArraySpmvParser<G> parser;

    @Before
    public void setUp() throws IOException {
        final G grammar = createGrammar(JUnit.unitTestDataAsReader("grammars/wsj.2-21.unk.R2-p1.gz"),
                PerfectIntPairHashPackingFunction.class);
        final ParserDriver opts = new ParserDriver();
        opts.edgeSelectorFactory = new BoundaryInOut(EdgeSelectorType.BoundaryInOut, grammar, new BufferedReader(
                JUnit.unitTestDataAsReader("fom/R2-p1.boundary.gz")));

        final ConfigProperties props = GlobalConfigProperties.singleton();
        props.put("maxBeamWidth", "20");
        props.put("lexicalRowBeamWidth", "60");
        props.put("lexicalRowUnaries", "20");
        parser = createParser(opts, grammar);
    }

    @After
    public void tearDown() {
        if (parser != null) {
            parser.shutdown();
        }
    }

    @BeforeClass
    public static void configureThreads() throws Exception {
        GlobalConfigProperties.singleton().setProperty(ParserDriver.OPT_ROW_THREAD_COUNT, "2");
    }

    @AfterClass
    public static void suiteTearDown() {
        GlobalConfigProperties.singleton().clear();
    }

    protected abstract G createGrammar(Reader grammarReader, Class<? extends PackingFunction> packingFunctionClass)
            throws IOException;

    protected abstract PackedArraySpmvParser<G> createParser(ParserDriver opts, G grammar);

    /**
     * TODO Make this a PerformanceTest
     * 
     * @throws IOException
     */
    @Test
    public void testPruned() throws IOException {

        final BufferedReader tokenizedReader = new BufferedReader(new InputStreamReader(
                JUnit.unitTestDataAsStream("parsing/wsj_24.mrgEC.tokens.1-20")));

        final BufferedReader parsedReader = new BufferedReader(new InputStreamReader(
                JUnit.unitTestDataAsStream("parsing/wsj_24.mrgEC.parsed.1-20.fom")));

        int i = 1;
        for (String sentence = tokenizedReader.readLine(); sentence != null; sentence = tokenizedReader.readLine()) {
            final String parsedSentence = parsedReader.readLine();
            assertEquals("Failed on sentence " + i, parsedSentence, parser.parseSentence(sentence).parse.toString());
            i++;
        }
    }
}

package edu.ohsu.cslu.parser.spmv;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.junit.Before;
import org.junit.Test;

import cltool4j.ConfigProperties;
import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashPackingFunction;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.edgeselector.BoundaryInOut;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector.EdgeSelectorType;
import edu.ohsu.cslu.tests.SharedNlpTests;

/**
 * Tests FOM-pruned parsing.
 * 
 * TODO Add to AllTests
 * 
 * @author Aaron Dunlop
 * @since Mar 9, 2011
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestPrunedBeamCscSpmvParser {

    private BeamCscSpmvParser parser;

    @Before
    public void setUp() throws IOException {
        final LeftCscSparseMatrixGrammar grammar = new LeftCscSparseMatrixGrammar(
                SharedNlpTests.unitTestDataAsReader("grammars/wsj.2-21.unk.R2-p1.gz"),
                PerfectIntPairHashPackingFunction.class);
        final ParserDriver opts = new ParserDriver();
        opts.edgeSelectorFactory = new BoundaryInOut(EdgeSelectorType.BoundaryInOut, grammar, new BufferedReader(
                SharedNlpTests.unitTestDataAsReader("fom/R2-p1.boundary.gz")));

        final ConfigProperties props = new ConfigProperties();
        props.put("beamcsc.lexicalRowBeamWidth", "60");
        props.put("beamcsc.beamWidth", "20");
        props.put("beamcsc.lexicalRowUnaries", "20");
        parser = new BeamCscSpmvParser(opts, props, grammar);
    }

    /**
     * TODO Make this a PerformanceTest
     * 
     * @throws IOException
     */
    @Test
    public void testPruned() throws IOException {

        final BufferedReader tokenizedReader = new BufferedReader(new InputStreamReader(
                SharedNlpTests.unitTestDataAsStream("parsing/wsj_24.mrgEC.tokens.1-20")));

        final BufferedReader parsedReader = new BufferedReader(new InputStreamReader(
                SharedNlpTests.unitTestDataAsStream("parsing/wsj_24.mrgEC.parsed.1-20.fom")));

        for (String sentence = tokenizedReader.readLine(); sentence != null; sentence = tokenizedReader.readLine()) {
            final String parsedSentence = parsedReader.readLine();
            assertEquals(parsedSentence, parser.parseSentence(sentence).parse.toString());
        }

    }
}

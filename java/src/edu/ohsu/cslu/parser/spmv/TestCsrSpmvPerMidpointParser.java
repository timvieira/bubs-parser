package edu.ohsu.cslu.parser.spmv;

import org.junit.Ignore;
import org.junit.Test;

import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashPackingFunction;
import edu.ohsu.cslu.tests.PerformanceTest;

public class TestCsrSpmvPerMidpointParser extends
        SparseMatrixVectorParserTestCase<CsrSpmvPerMidpointParser, PerfectIntPairHashPackingFunction> {

    @Override
    @Test
    @PerformanceTest({ "mbp", "33306", "d820", "71017" })
    public void profileSentences11Through20() throws Exception {
        internalProfileSentences11Through20();
    }

    @Override
    @Ignore
    public void testCartesianProductVectorExample() {
    }

    @Override
    @Ignore
    public void testUnfilteredCartesianProductVectorSimpleGrammar2() {
    }

    @Override
    @Ignore
    public void testFilteredCartesianProductVectorSimpleGrammar2() {
    }

    @Override
    @Ignore
    public void testBinarySpMVMultiplySimpleGrammar2() {
    }

    @Override
    @Ignore
    public void testSimpleGrammar2() {
    }
}

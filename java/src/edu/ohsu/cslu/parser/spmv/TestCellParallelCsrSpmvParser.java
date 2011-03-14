package edu.ohsu.cslu.parser.spmv;

import org.junit.Ignore;
import org.junit.Test;

import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashPackingFunction;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.tests.PerformanceTest;

/**
 * Tests for {@link CellParallelCsrSpmvParser}.
 * 
 * @author Aaron Dunlop
 * @since Mar 11, 2011
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestCellParallelCsrSpmvParser extends
        SparseMatrixVectorParserTestCase<CellParallelCsrSpmvParser, PerfectIntPairHashPackingFunction> {

    @Override
    @Test
    @PerformanceTest({ "mbp", "13144", "d820", "23892" })
    public void profileSentences11Through20() throws Exception {
        internalProfileSentences11Through20();
    }

    @Override
    public void setUp() throws Exception {
        GlobalConfigProperties.singleton().setProperty(ParserDriver.OPT_REQUESTED_THREAD_COUNT, "4");
        super.setUp();
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

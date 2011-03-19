package edu.ohsu.cslu.parser.spmv;

import org.junit.Test;

import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashPackingFunction;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.tests.PerformanceTest;

/**
 * Unit tests for row-level parallelization of {@link CscSpmvParser}.
 * 
 * @author Aaron Dunlop
 * @since Mar 12, 2011
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestRowParallelCscSpmvParser extends
        SparseMatrixVectorParserTestCase<CscSpmvParser, PerfectIntPairHashPackingFunction> {

    @Override
    @Test
    @PerformanceTest({ "mbp", "6732" })
    public void profileSentences11Through20() throws Exception {
        internalProfileSentences11Through20();
    }

    @Override
    public void setUp() throws Exception {
        GlobalConfigProperties.singleton().setProperty(ParserDriver.OPT_ROW_THREAD_COUNT, "2");
        super.setUp();
    }
}

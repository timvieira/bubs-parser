package edu.ohsu.cslu.parser.spmv;

import org.junit.Ignore;
import org.junit.Test;

import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashPackingFunction;
import edu.ohsu.cslu.tests.PerformanceTest;

/**
 * Tests for {@link CscSpmvParser}.
 * 
 * @author Aaron Dunlop
 * @since Feb 11, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestCscSpmvParser extends
        SparseMatrixVectorParserTestCase<CscSpmvParser, PerfectIntPairHashPackingFunction> {

    @Override
    @Test
    @PerformanceTest({ "mbp", "11934", "d820", "24168" })
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

    // @Override
    // protected ParserDriver parserOptions() {
    // final ParserDriver options = new ParserDriver();
    // options.collectDetailedStatistics = true;
    // options.param1 = 250;
    // return options;
    // }

}

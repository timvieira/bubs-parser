package edu.ohsu.cslu.parser.spmv;

import org.junit.Test;

import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashFilterFunction;
import edu.ohsu.cslu.tests.PerformanceTest;

/**
 * Tests for {@link CsrSpmvParser}.
 * 
 * @author Aaron Dunlop
 * @since Feb 11, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestCsrSpmvParser extends
        SparseMatrixVectorParserTestCase<CsrSpmvParser, PerfectIntPairHashFilterFunction> {

    @Override
    @Test
    @PerformanceTest({ "mbp", "22223", "d820", "45104" })
    public void profileSentences11Through20() throws Exception {
        internalProfileSentences11Through20();
    }
}

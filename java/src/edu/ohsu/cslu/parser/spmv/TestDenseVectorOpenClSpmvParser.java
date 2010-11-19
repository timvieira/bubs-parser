package edu.ohsu.cslu.parser.spmv;

import org.junit.Ignore;
import org.junit.Test;

import edu.ohsu.cslu.grammar.SparseMatrixGrammar.LeftShiftFunction;
import edu.ohsu.cslu.tests.PerformanceTest;

/**
 * Unit tests for {@link DenseVectorOpenClSpmvParser}
 * 
 * @author Aaron Dunlop
 * @since Jun 2, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestDenseVectorOpenClSpmvParser extends
        OpenClSpmvParserTestCase<DenseVectorOpenClSpmvParser, LeftShiftFunction> {

    @Override
    @Test
    @PerformanceTest({ "mbp", "667853" })
    public void profileSentences11Through20() throws Exception {
        internalProfileSentences11Through20();
    }

    @Override
    @Ignore
    public void testFilteredCartesianProductVectorSimpleGrammar2() throws Exception {
    }
}

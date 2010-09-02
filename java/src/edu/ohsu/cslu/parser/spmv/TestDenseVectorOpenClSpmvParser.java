package edu.ohsu.cslu.parser.spmv;

import org.junit.Test;

import edu.ohsu.cslu.tests.PerformanceTest;

/**
 * Unit tests for {@link DenseVectorOpenClSpmvParser}
 * 
 * @author Aaron Dunlop
 * @since Jun 2, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestDenseVectorOpenClSpmvParser extends OpenClSpmvParserTestCase<DenseVectorOpenClSpmvParser> {

    @Override
    @Test
    @PerformanceTest({ "mbp", "667853" })
    public void profileSentences11Through20() throws Exception {
        internalProfileSentences11Through20();
    }
}

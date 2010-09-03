package edu.ohsu.cslu.parser.spmv;

import org.junit.Test;

import edu.ohsu.cslu.tests.PerformanceTest;

/**
 * Tests for {@link CscSpmvParser}.
 * 
 * @author Aaron Dunlop
 * @since Feb 11, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestCscSpmvParser extends SparseMatrixVectorParserTestCase<CscSpmvParser> {

    @Override
    @Test
    @PerformanceTest({ "mbp", "23541", "d820", "48282" })
    public void profileSentences11Through20() throws Exception {
        internalProfileSentences11Through20();
    }

}

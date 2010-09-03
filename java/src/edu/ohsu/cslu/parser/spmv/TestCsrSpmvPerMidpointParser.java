package edu.ohsu.cslu.parser.spmv;

import org.junit.Test;

import edu.ohsu.cslu.tests.PerformanceTest;

public class TestCsrSpmvPerMidpointParser extends SparseMatrixVectorParserTestCase<CsrSpmvPerMidpointParser> {

    @Override
    @Test
    @PerformanceTest({ "mbp", "33306", "d820", "71017" })
    public void profileSentences11Through20() throws Exception {
        internalProfileSentences11Through20();
    }
}

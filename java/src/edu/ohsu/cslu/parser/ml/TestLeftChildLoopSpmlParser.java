package edu.ohsu.cslu.parser.ml;

import org.junit.Test;

import edu.ohsu.cslu.tests.PerformanceTest;

public class TestLeftChildLoopSpmlParser extends SparseMatrixLoopParserTestCase<LeftChildLoopSpmlParser> {

    @Override
    @Test
    @PerformanceTest({ "mbp", "5619", "d820", "10000" })
    public void profileSentences11Through20() throws Exception {
        internalProfileSentences11Through20();
    }

}

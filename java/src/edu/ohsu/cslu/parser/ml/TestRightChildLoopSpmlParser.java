package edu.ohsu.cslu.parser.ml;

import org.junit.Test;

import edu.ohsu.cslu.tests.PerformanceTest;

public class TestRightChildLoopSpmlParser extends SparseMatrixLoopParserTestCase<RightChildLoopSpmlParser> {

    @Override
    @Test
    @PerformanceTest({ "d820", "0", "mbp", "8211" })
    public void profileSentences11Through20() throws Exception {
        internalProfileSentences11Through20();
    }
}

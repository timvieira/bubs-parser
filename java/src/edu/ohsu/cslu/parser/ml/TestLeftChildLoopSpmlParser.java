package edu.ohsu.cslu.parser.ml;

import edu.ohsu.cslu.tests.PerformanceTest;

public class TestLeftChildLoopSpmlParser extends SparseMatrixLoopParserTestCase<LeftChildLoopSpmlParser> {

    @Override
    @PerformanceTest({ "d820", "47" })
    public void profileSentences11Through20() throws Exception {
    }

}

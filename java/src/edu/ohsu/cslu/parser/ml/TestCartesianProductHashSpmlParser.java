package edu.ohsu.cslu.parser.ml;

import org.junit.Test;

import edu.ohsu.cslu.tests.PerformanceTest;

public class TestCartesianProductHashSpmlParser extends SparseMatrixLoopParserTestCase<CartesianProductHashSpmlParser> {

    @Override
    @Test
    @PerformanceTest({ "mbp", "70211", "d820", "100000" })
    public void profileSentences11Through20() throws Exception {
        internalProfileSentences11Through20();
    }
}

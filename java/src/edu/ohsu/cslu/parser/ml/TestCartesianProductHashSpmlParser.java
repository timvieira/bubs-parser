package edu.ohsu.cslu.parser.ml;

import org.junit.Test;

import edu.ohsu.cslu.tests.PerformanceTest;

public class TestCartesianProductHashSpmlParser extends SparseMatrixLoopParserTestCase<CartesianProductHashSpmlParser> {

    @Override
    @Test
    @PerformanceTest({ "d820", "0", "mbp", "105927" })
    public void profileSentences11Through20() throws Exception {
        internalProfileSentences11Through20();
    }
}

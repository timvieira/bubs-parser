package edu.ohsu.cslu.parser.ml;

import org.junit.Test;

import edu.ohsu.cslu.tests.PerformanceTest;

public class TestCartesianProductBinarySearchLeftChildSpmlParser extends
        SparseMatrixLoopParserTestCase<CartesianProductBinarySearchLeftChildSpmlParser> {

    @Override
    @Test
    @PerformanceTest({ "mbp", "58744", "d820", "100000" })
    public void profileSentences11Through20() throws Exception {
        internalProfileSentences11Through20();
    }
}

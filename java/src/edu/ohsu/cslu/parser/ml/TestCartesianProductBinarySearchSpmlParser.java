package edu.ohsu.cslu.parser.ml;

import org.junit.Test;

import edu.ohsu.cslu.tests.PerformanceTest;

public class TestCartesianProductBinarySearchSpmlParser extends
        SparseMatrixLoopParserTestCase<CartesianProductBinarySearchSpmlParser> {

    @Override
    @Test
    @PerformanceTest({ "mbp", "272087", "d820", "300000" })
    public void profileSentences11Through20() throws Exception {
        internalProfileSentences11Through20();
    }
}

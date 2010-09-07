package edu.ohsu.cslu.parser.ml;

import org.junit.Test;

import edu.ohsu.cslu.tests.PerformanceTest;

public class TestLeftChildLoopSpmlParser extends SparseMatrixLoopParserTestCase<LeftChildLoopSpmlParser> {

    @Override
    @Test
    @PerformanceTest({ "d820", "0", "mbp", "7814" })
    public void profileSentences11Through20() throws Exception {
        internalProfileSentences11Through20();
    }

}

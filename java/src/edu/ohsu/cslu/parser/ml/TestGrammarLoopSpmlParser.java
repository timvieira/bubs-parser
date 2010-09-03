package edu.ohsu.cslu.parser.ml;

import edu.ohsu.cslu.tests.PerformanceTest;

public class TestGrammarLoopSpmlParser extends SparseMatrixLoopParserTestCase<GrammarLoopSpmlParser> {

    @Override
    @PerformanceTest({ "d820", "1407" })
    public void profileSentences11Through20() throws Exception {
    }
}

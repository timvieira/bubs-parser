package edu.ohsu.cslu.parser.ml;

import static org.junit.Assert.fail;

import org.junit.Test;

import edu.ohsu.cslu.tests.PerformanceTest;

public class TestGrammarLoopSpmlParser extends SparseMatrixLoopParserTestCase<GrammarLoopSpmlParser> {

    @Override
    @Test
    @PerformanceTest({ "d820", "0", "mbp", "1" })
    public void profileSentences11Through20() throws Exception {
        fail("Too inefficient to profile");
    }
}

package edu.ohsu.cslu.parser.ml;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.tests.PerformanceTest;

public class TestLeftChildLoopSpmlParser extends SparseMatrixLoopParserTestCase<LeftChildLoopSpmlParser> {

    @Override
    protected Class<? extends Grammar> grammarClass() {
        return LeftCscSparseMatrixGrammar.class;
    }

    @Override
    @PerformanceTest({ "d820", "47" })
    public void profileSentences11Through20() throws Exception {
    }

}

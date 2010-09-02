package edu.ohsu.cslu.parser.ml;

import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.tests.PerformanceTest;

public class TestGrammarLoopSpmlParser extends SparseMatrixLoopParserTestCase<GrammarLoopSpmlParser> {

    @Override
    protected Class<? extends Grammar> grammarClass() {
        return CsrSparseMatrixGrammar.class;
    }

    @Override
    @PerformanceTest({ "d820", "1407" })
    public void profileSentences11Through20() throws Exception {
    }
}

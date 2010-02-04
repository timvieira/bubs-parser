package edu.ohsu.cslu.parser;

import org.junit.Test;

import edu.ohsu.cslu.grammar.BaseGrammar;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.JsaSparseMatrixGrammar;
import edu.ohsu.cslu.parser.traversal.ChartTraversal.ChartTraversalType;
import edu.ohsu.cslu.tests.PerformanceTest;

public class TestJsaSparseMatrixVectorParser extends SparseMatrixVectorParserTestCase {

    @Override
    protected Class<? extends BaseGrammar> grammarClass() {
        return JsaSparseMatrixGrammar.class;
    }

    @Override
    protected MaximumLikelihoodParser createParser(final Grammar grammar, final ChartTraversalType chartTraversalType) {
        return new JsaSparseMatrixVectorParser((JsaSparseMatrixGrammar) grammar, chartTraversalType);
    }

    @Override
    @Test
    @PerformanceTest( { "mbp", "500000", "d820", "500000" })
    public void profileSentences11Through20() throws Exception {
        internalProfileSentences11Through20();
    }

}

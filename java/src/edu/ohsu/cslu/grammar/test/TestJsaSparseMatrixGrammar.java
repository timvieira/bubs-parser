package edu.ohsu.cslu.grammar.test;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.JsaSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SortedGrammarTestCase;

public class TestJsaSparseMatrixGrammar extends SortedGrammarTestCase {

    @Override
    protected Class<? extends Grammar> grammarClass() {
        return JsaSparseMatrixGrammar.class;
    }
}

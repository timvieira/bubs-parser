package edu.ohsu.cslu.grammar;

public class TestCscSparseMatrixGrammar extends SortedGrammarTestCase {

    @Override
    protected Class<? extends Grammar> grammarClass() {
        return LeftCscSparseMatrixGrammar.class;
    }

}

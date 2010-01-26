package edu.ohsu.cslu.grammar;

public class TestPackedSparseMatrixGrammar extends SortedGrammarTestCase {

    @Override
    protected Class<? extends Grammar> grammarClass() {
        return PackedSparseMatrixGrammar.class;
    }

}

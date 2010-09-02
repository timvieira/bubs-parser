package edu.ohsu.cslu.parser.ml;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;

public class TestCartesianProductBinarySearchLeftChildSpmlParser extends
        SparseMatrixLoopParserTestCase<CartesianProductBinarySearchLeftChildSpmlParser> {

    @Override
    protected Class<? extends Grammar> grammarClass() {
        return LeftCscSparseMatrixGrammar.class;
    }

}

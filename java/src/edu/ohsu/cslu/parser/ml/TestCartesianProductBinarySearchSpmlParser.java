package edu.ohsu.cslu.parser.ml;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;

public class TestCartesianProductBinarySearchSpmlParser extends
        SparseMatrixLoopParserTestCase<CartesianProductBinarySearchSpmlParser> {

    @Override
    protected Class<? extends Grammar> grammarClass() {
        return LeftCscSparseMatrixGrammar.class;
    }
}

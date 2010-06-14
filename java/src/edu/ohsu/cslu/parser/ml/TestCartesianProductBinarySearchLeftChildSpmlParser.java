package edu.ohsu.cslu.parser.ml;

import edu.ohsu.cslu.grammar.CscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.parser.Parser;
import edu.ohsu.cslu.parser.cellselector.CellSelector;

public class TestCartesianProductBinarySearchLeftChildSpmlParser extends SparseMatrixLoopParserTestCase {

    @Override
    protected Parser<?> createParser(final Grammar grammar, final CellSelector cellSelector) {
        return new CartesianProductBinarySearchLeftChildSpmlParser((CscSparseMatrixGrammar) grammar);
    }

    @Override
    protected Class<? extends Grammar> grammarClass() {
        return CscSparseMatrixGrammar.class;
    }

}

package edu.ohsu.cslu.parser.ml;

import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.parser.Parser;
import edu.ohsu.cslu.parser.cellselector.CellSelector;

public class TestCartesianProductBinarySearchLeftChildSpmlParser extends SparseMatrixLoopParserTestCase {

    @Override
    protected Parser<?> createParser(final Grammar grammar, final CellSelector cellSelector) {
        return new CartesianProductBinarySearchLeftChildSpmlParser((LeftCscSparseMatrixGrammar) grammar);
    }

    @Override
    protected Class<? extends Grammar> grammarClass() {
        return LeftCscSparseMatrixGrammar.class;
    }

}

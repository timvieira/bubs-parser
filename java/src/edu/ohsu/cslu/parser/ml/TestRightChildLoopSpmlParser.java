package edu.ohsu.cslu.parser.ml;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.RightCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.RightShiftFunction;
import edu.ohsu.cslu.parser.Parser;
import edu.ohsu.cslu.parser.cellselector.CellSelector;

public class TestRightChildLoopSpmlParser extends SparseMatrixLoopParserTestCase {

    @Override
    protected Parser<?> createParser(final Grammar grammar, final CellSelector cellSelector) {
        return new RightChildLoopSpmlParser((RightCscSparseMatrixGrammar) grammar);
    }

    @Override
    protected Class<? extends Grammar> grammarClass() {
        return RightCscSparseMatrixGrammar.class;
    }

    @Override
    protected Class<? extends SparseMatrixGrammar.CartesianProductFunction> cartesianProductFunctionClass() {
        return RightShiftFunction.class;
    }
}

package edu.ohsu.cslu.parser.ml;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.RightCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.RightShiftFunction;

public class TestRightChildLoopSpmlParser extends SparseMatrixLoopParserTestCase<RightChildLoopSpmlParser> {

    @Override
    protected Class<? extends Grammar> grammarClass() {
        return RightCscSparseMatrixGrammar.class;
    }

    @Override
    protected Class<? extends SparseMatrixGrammar.CartesianProductFunction> cartesianProductFunctionClass() {
        return RightShiftFunction.class;
    }
}

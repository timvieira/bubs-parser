package edu.ohsu.cslu.parser.ml;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;

public class TestCartesianProductHashSpmlParser extends SparseMatrixLoopParserTestCase<CartesianProductHashSpmlParser> {

    @Override
    protected Class<? extends Grammar> grammarClass() {
        return LeftCscSparseMatrixGrammar.class;
    }

}

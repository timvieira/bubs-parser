package edu.ohsu.cslu.parser.ml;

import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.parser.ExhaustiveChartParserTestCase;

public abstract class SparseMatrixLoopParserTestCase extends ExhaustiveChartParserTestCase {

    @Override
    public void profileSentences11Through20() throws Exception {
    }

    protected Class<? extends SparseMatrixGrammar.CartesianProductFunction> cartesianProductFunctionClass() {
        return null;
    }
}

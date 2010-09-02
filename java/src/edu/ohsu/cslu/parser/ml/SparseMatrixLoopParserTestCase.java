package edu.ohsu.cslu.parser.ml;

import edu.ohsu.cslu.grammar.GrammarByChild;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.parser.ExhaustiveChartParser;
import edu.ohsu.cslu.parser.ExhaustiveChartParserTestCase;
import edu.ohsu.cslu.parser.chart.Chart;
import edu.ohsu.cslu.tests.PerformanceTest;

public abstract class SparseMatrixLoopParserTestCase<P extends ExhaustiveChartParser<? extends GrammarByChild, ? extends Chart>>
        extends ExhaustiveChartParserTestCase<P> {

    protected Class<? extends SparseMatrixGrammar.CartesianProductFunction> cartesianProductFunctionClass() {
        return null;
    }

    @Override
    @PerformanceTest({ "d820", "0" })
    public void profileSentences11Through20() throws Exception {
    }
}

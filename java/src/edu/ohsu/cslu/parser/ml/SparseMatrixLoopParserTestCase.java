package edu.ohsu.cslu.parser.ml;

import edu.ohsu.cslu.grammar.GrammarByChild;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.ExhaustiveChartParserTestCase;
import edu.ohsu.cslu.parser.chart.Chart;

public abstract class SparseMatrixLoopParserTestCase<P extends ChartParser<? extends GrammarByChild, ? extends Chart>>
        extends ExhaustiveChartParserTestCase<P> {

    protected Class<? extends SparseMatrixGrammar.CartesianProductFunction> cartesianProductFunctionClass() {
        return null;
    }
}

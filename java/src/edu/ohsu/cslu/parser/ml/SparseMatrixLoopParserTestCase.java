package edu.ohsu.cslu.parser.ml;

import java.io.Reader;

import org.junit.Before;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.LeftShiftFunction;
import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.ExhaustiveChartParserTestCase;
import edu.ohsu.cslu.parser.chart.Chart;

public abstract class SparseMatrixLoopParserTestCase<P extends ChartParser<? extends Grammar, ? extends Chart>> extends
        ExhaustiveChartParserTestCase<P> {

    @Override
    public Grammar createGrammar(final Reader grammarReader) throws Exception {
        return grammarClass().getConstructor(new Class[] { Reader.class, Class.class }).newInstance(
                new Object[] { grammarReader, LeftShiftFunction.class });
    }

    /**
     * Ensure the grammar is constructed with the Constructs the grammar (if necessary) and a new parser instance. Run
     * prior to each test method.
     * 
     * @throws Exception if unable to construct grammar or parser.
     */
    @Override
    @Before
    public void setUp() throws Exception {
        if (f2_21_grammar != null
                && (f2_21_grammar.getClass() != grammarClass() || ((SparseMatrixGrammar) f2_21_grammar)
                        .cartesianProductFunction().getClass() != LeftShiftFunction.class)) {
            f2_21_grammar = null;
        }

        if (simpleGrammar1 != null
                && (simpleGrammar1.getClass() != grammarClass() || ((SparseMatrixGrammar) simpleGrammar1)
                        .cartesianProductFunction().getClass() != LeftShiftFunction.class)) {
            simpleGrammar1 = null;
        }

        if (simpleGrammar2 != null
                && (simpleGrammar2.getClass() != grammarClass() || ((SparseMatrixGrammar) simpleGrammar2)
                        .cartesianProductFunction().getClass() != LeftShiftFunction.class)) {
            simpleGrammar2 = null;
        }

        super.setUp();
    }
}

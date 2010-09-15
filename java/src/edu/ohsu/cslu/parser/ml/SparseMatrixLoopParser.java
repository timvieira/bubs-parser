package edu.ohsu.cslu.parser.ml;

import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.SparseMatrixParser;
import edu.ohsu.cslu.parser.chart.ParallelArrayChart;

public abstract class SparseMatrixLoopParser<G extends SparseMatrixGrammar, C extends ParallelArrayChart> extends
        SparseMatrixParser<G, C> {

    public long startTime = 0;

    public SparseMatrixLoopParser(final ParserDriver opts, final G grammar) {
        super(opts, grammar);
    }

    @Override
    protected void initParser(final int[] tokens) {
        startTime = System.currentTimeMillis();
    }

}

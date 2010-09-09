package edu.ohsu.cslu.parser.ml;

import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.SparseMatrixParser;
import edu.ohsu.cslu.parser.chart.ParallelArrayChart;

public abstract class SparseMatrixLoopParser<G extends SparseMatrixGrammar, C extends ParallelArrayChart> extends
        SparseMatrixParser<G, C> {

    public long startTime = 0;

    /**
     * True if we're collecting detailed counts of cell populations, cartesian-product sizes, etc. Set from
     * {@link ParserDriver}, but duplicated here as a final variable, so that the JIT can eliminate
     * potentially-expensive counting code when we don't need it
     * 
     * TODO Move up to {@link ExhaustiveChartParser} (or even higher) and share with {@link SparseMatrixLoopParser}
     */
    protected final boolean collectDetailedStatistics;

    public SparseMatrixLoopParser(final ParserDriver opts, final G grammar) {
        super(opts, grammar);
        this.collectDetailedStatistics = opts.collectDetailedStatistics;
    }

    @Override
    protected void initParser(final int sentLength) {
        startTime = System.currentTimeMillis();
    }

}

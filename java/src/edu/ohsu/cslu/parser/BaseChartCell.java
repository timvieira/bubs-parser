package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.BaseGrammar;
import edu.ohsu.cslu.grammar.BaseGrammar.Production;

public abstract class BaseChartCell implements ChartCell {
    public final int start;
    public final int end;
    protected int numEdgesConsidered;
    protected int numEdgesAdded;
    protected final BaseGrammar grammar;

    protected BaseChartCell(final int start, final int end, final BaseGrammar grammar) {
        this.start = start;
        this.end = end;
        this.grammar = grammar;
    }

    public abstract boolean addEdge(final ChartEdge edge);

    // alternate addEdge function so we aren't required to create a new ChartEdge object
    // in the CYK inner loop for every potential new edge entry
    public abstract boolean addEdge(final Production p, final float insideProb, final ChartCell leftCell, final ChartCell rightCell);

    /**
     * Returns the edge with the highest probability for the specified non-terminal
     * 
     * @param nonTermIndex
     * @return the best edge present in this chart cell for the specified non-terminal
     */
    public abstract ChartEdge getBestEdge(final int nonTermIndex);

    public final int start() {
        return start;
    }

    public final int end() {
        return end;
    }

    public final int getNumEdgesConsidered() {
        return numEdgesConsidered;
    }

    public final int getNumEdgesAdded() {
        return numEdgesAdded;
    }

    public abstract int getNumEdgeEntries();

    @Override
    public boolean equals(final Object o) {
        return this == o;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[" + start() + "][" + end() + "] with " + getNumEdgeEntries() + " (of " + grammar.numNonTerms() + ") edges";
    }

}

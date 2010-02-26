package edu.ohsu.cslu.parser;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import edu.ohsu.cslu.grammar.Grammar.Production;

public abstract class ChartCell implements Comparable<ChartCell> {
    private final int start, end;
    protected Chart chart; // reverse pointer back to the chart this ChartCell is in
    public float figureOfMerit;
    public int numEdgesConsidered, numEdgesAdded, numSpanVisits;

    public ChartCell(final int start, final int end, final Chart chart) {
        this.start = start;
        this.end = end;
        // this.grammar = grammar;
        this.chart = chart;
        this.figureOfMerit = 0;
        this.numSpanVisits = 0;
        this.numEdgesAdded = 0;
        this.numEdgesConsidered = 0;
    }

    public abstract boolean addEdge(final ChartEdge edge);

    // alternate addEdge function so we aren't required to create a new ChartEdge object
    // in the CYK inner loop for every potential new edge entry
    public abstract boolean addEdge(final Production p, final ChartCell leftCell, final ChartCell rightCell, final float insideProb);

    public boolean addEdgeForceOverwrite(final ChartEdge edge) {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the edge with the highest probability for the specified non-terminal
     * 
     * @param nonTermIndex
     * @return the best edge present in this chart cell for the specified non-terminal
     */
    public abstract ChartEdge getBestEdge(final int nonTermIndex);

    public abstract Collection<ChartEdge> getEdges();

    public abstract boolean hasEdge(ChartEdge edge) throws Exception;

    public abstract boolean hasEdge(int nonTermIndex);

    public abstract LinkedList<Integer> getPosEntries();

    public List<ChartEdge> getBestLeftEdges() {
        throw new UnsupportedOperationException();
    }

    public List<ChartEdge> getBestRightEdges() {
        throw new UnsupportedOperationException();
    }

    public final int start() {
        return start;
    }

    public final int end() {
        return end;
    }

    public int width() {
        return end() - start();
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
        return getClass().getName() + "[" + start() + "][" + end() + "] with " + getNumEdgeEntries() + " (of " + chart.grammar.numNonTerms() + ") edges";
    }

    @Override
    public int compareTo(final ChartCell otherCell) {
        if (this.figureOfMerit == otherCell.figureOfMerit) {
            return 0;
        } else if (figureOfMerit > otherCell.figureOfMerit) {
            return -1;
        } else {
            return 1;
        }
    }

    // TODO: each ChartCell needs to have a pointer back to it's parent chart data structure
    public boolean canBuild(final ChartEdge edge) throws Exception {
        if (edge.prod.isBinaryProd()) {
            final int midpt = edge.midpt();
            if (chart.getCell(start, midpt).hasEdge(edge.prod.leftChild) && chart.getCell(midpt, end).hasEdge(edge.prod.rightChild)) {
                return true;
            }
        } else {
            if (hasEdge(edge.prod.child())) {
                return true;
            }
        }
        return false;
    }
}

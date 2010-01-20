package edu.ohsu.cslu.parser;

import java.util.LinkedList;

import edu.ohsu.cslu.grammar.ArrayGrammar;
import edu.ohsu.cslu.grammar.BaseGrammar.Production;

public final class ArrayChartCell extends BaseChartCell {

    public ChartEdge[] bestEdge;
    private LinkedList<ChartEdge> bestLeftEdges, bestRightEdges;
    boolean bestEdgesHaveChanged = true;
    private final ArrayGrammar arrayGrammar;

    public ArrayChartCell(final int start, final int end, final ArrayGrammar grammar) {
        super(start, end, grammar);
        this.arrayGrammar = grammar;

        bestEdge = new ChartEdge[grammar.numNonTerms()];
    }

    @Override
    public ChartEdge getBestEdge(final int nonTermIndex) {
        return bestEdge[nonTermIndex];
    }

    public LinkedList<ChartEdge> getBestLeftEdges() {
        if (bestEdgesHaveChanged) {
            buildLeftRightEdgeLists();
        }
        return bestLeftEdges;
    }

    public LinkedList<ChartEdge> getBestRightEdges() {
        if (bestEdgesHaveChanged) {
            buildLeftRightEdgeLists();
        }
        return bestRightEdges;
    }

    private void buildLeftRightEdgeLists() {
        bestLeftEdges = new LinkedList<ChartEdge>();
        bestRightEdges = new LinkedList<ChartEdge>();
        for (int i = 0; i < bestEdge.length; i++) {
            final ChartEdge tmpEdge = bestEdge[i];
            if (tmpEdge != null) {
                final int parent = tmpEdge.p.parent;
                if (arrayGrammar.isLeftChild(parent))
                    bestLeftEdges.add(tmpEdge);
                if (arrayGrammar.isRightChild(parent))
                    bestRightEdges.add(tmpEdge);
            }
        }
        bestEdgesHaveChanged = false;
    }

    @Override
    public boolean addEdge(final ChartEdge edge) {
        final int parent = edge.p.parent;
        numEdgesConsidered += 1;
        // System.out.println("Considering: "+edge);
        final ChartEdge prevBestEdge = bestEdge[parent];
        if (prevBestEdge == null || edge.insideProb > prevBestEdge.insideProb) {
            bestEdge[parent] = edge;
            bestEdgesHaveChanged = true;
            numEdgesAdded++;
            return true;
        }

        return false;
    }

    /**
     * Alternate addEdge() function so we aren't required to create a new ChartEdge object in the CYK inner loop for every potential new edge entry. Adds an edge to the cell if the
     * edge's probability is greater than an existing edge with the same non-terminal. Optional operation (some {@link ChartCell} implementations may be immutable).
     * 
     * @param p The production to add
     * @param insideProb The production probability
     * @param leftCell The left child of this production
     * @param rightCell The right child of this production
     * @return True if the edge was added, false if another edge with greater probability was already present.
     */
    @Override
    public boolean addEdge(final Production p, final float insideProb, final ChartCell leftCell, final ChartCell rightCell) {
        numEdgesConsidered += 1;
        // System.out.println("Considering: " + new ChartEdge(p, leftCell, rightCell, insideProb));

        final ChartEdge prevBestEdge = bestEdge[p.parent];
        if (prevBestEdge == null) {
            bestEdge[p.parent] = new ChartEdge(p, leftCell, rightCell, insideProb);
            bestEdgesHaveChanged = true;
            numEdgesAdded += 1;
            return true;
        } else if (prevBestEdge.insideProb < insideProb) {
            prevBestEdge.p = p;
            prevBestEdge.insideProb = insideProb;
            prevBestEdge.leftCell = leftCell;
            prevBestEdge.rightCell = rightCell;
            // bestLeftEdgesHasChanged = true; // pointer to old edge will still be correct
            // numEdgesAdded += 1; // we are replacing an edge, so the same number are in the chart
            return true;
        }

        return false;
    }

    @Override
    public int getNumEdgeEntries() {
        int numEntries = 0;
        for (int i = 0; i < bestEdge.length; i++) {
            if (bestEdge[i] != null)
                numEntries++;
        }
        return numEntries;
    }

    @Override
    public String toString() {
        return "ChartCell[" + start + "][" + end + "] with " + getNumEdgeEntries() + " (of " + grammar.numNonTerms() + ") edges";
    }

}

package edu.ohsu.cslu.parser;

import java.util.Collection;
import java.util.LinkedList;

import edu.ohsu.cslu.grammar.Grammar.Production;

public final class ArrayChartCell extends ChartCell {

    private ChartEdge[] bestEdge;
    private LinkedList<ChartEdge> bestLeftEdges, bestRightEdges;
    private boolean bestEdgesHaveChanged = true;
    private LinkedList<Integer> posEntries;
    private boolean isLexCell;

    public ArrayChartCell(final int start, final int end, final Chart<ArrayChartCell> chart) {
        super(start, end, chart);

        bestEdge = new ChartEdge[chart.grammar.numNonTerms()];
        posEntries = new LinkedList<Integer>();

        if (end - start == 1) {
            isLexCell = true;
        } else {
            isLexCell = false;
        }
    }

    @Override
    public ChartEdge getBestEdge(final int nonTermIndex) {
        return bestEdge[nonTermIndex];
    }

    @Override
    public Collection<ChartEdge> getEdges() {
        final LinkedList<ChartEdge> edgeList = new LinkedList<ChartEdge>();
        for (final ChartEdge edge : bestEdge) {
            if (edge != null) {
                edgeList.add(edge);
            }
        }
        return edgeList;
    }

    @Override
    public boolean hasEdge(final ChartEdge edge) throws Exception {
        final ChartEdge curEdge = bestEdge[edge.prod.parent];
        if (curEdge == null)
            return false;
        if (!curEdge.prod.equals(edge.prod))
            return false;
        // make sure midpoints are the same if it's a binary production/edge
        if (edge.prod.isBinaryProd() && (edge.midpt() != curEdge.midpt()))
            return false;

        return true;
    }

    @Override
    public boolean hasEdge(final int nonTermIndex) {
        return bestEdge[nonTermIndex] != null;
    }

    @Override
    public LinkedList<ChartEdge> getBestLeftEdges() {
        if (bestEdgesHaveChanged) {
            buildLeftRightEdgeLists();
        }
        return bestLeftEdges;
    }

    @Override
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
                final int parent = tmpEdge.prod.parent;
                if (chart.grammar.isLeftChild(parent))
                    bestLeftEdges.add(tmpEdge);
                if (chart.grammar.isRightChild(parent))
                    bestRightEdges.add(tmpEdge);
            }
        }
        bestEdgesHaveChanged = false;
    }

    @Override
    public LinkedList<Integer> getPosEntries() {
        return posEntries;
    }

    private boolean insertNewEdge(final ChartEdge edge) {
        // assuming bestEdge[parent] == null
        final int parent = edge.prod.parent;
        numEdgesAdded++;
        bestEdgesHaveChanged = true;
        bestEdge[parent] = edge;
        if (isLexCell && chart.grammar.isPOS(parent)) {
            posEntries.addLast(parent);
        }
        return true;
    }

    @Override
    public boolean addEdge(final ChartEdge edge) {
        final int parent = edge.prod.parent;
        final ChartEdge prevBestEdge = bestEdge[parent];
        numEdgesConsidered++;

        if (prevBestEdge == null) {
            return insertNewEdge(edge);
        } else if (edge.inside > prevBestEdge.inside) {
            bestEdge[parent] = edge;
            return true;
        }

        return false;
    }

    @Override
    public boolean addEdgeForceOverwrite(final ChartEdge edge) {
        final int parent = edge.prod.parent;
        final ChartEdge prevBestEdge = bestEdge[parent];
        numEdgesConsidered++;

        if (prevBestEdge == null) {
            return insertNewEdge(edge);
        }
        bestEdge[parent] = edge;
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
    public boolean addEdge(final Production p, final ChartCell leftCell, final ChartCell rightCell, final float insideProb) {
        final int parent = p.parent;
        final ChartEdge prevBestEdge = bestEdge[parent];
        numEdgesConsidered++;

        // System.out.println("Considering: " + new ChartEdge(p, leftCell, rightCell, insideProb));
        if (prevBestEdge == null) {
            return insertNewEdge(new ChartEdge(p, leftCell, rightCell, insideProb));
        } else if (prevBestEdge.inside < insideProb) {
            prevBestEdge.prod = p;
            prevBestEdge.inside = insideProb;
            prevBestEdge.leftCell = leftCell;
            prevBestEdge.rightCell = rightCell;
            // bestLeftEdgesHasChanged = true; // pointer to old edge will still be correct
            // numEdgesAdded++; // we are replacing an edge, so the same number are in the chart
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
        return "ChartCell[" + start() + "][" + end() + "] with " + getNumEdgeEntries() + " (of " + chart.grammar.numNonTerms() + ") edges";
    }
}

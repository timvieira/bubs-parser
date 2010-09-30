package edu.ohsu.cslu.parser.chart;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.grammar.NonTerminal;
import edu.ohsu.cslu.parser.Parser;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector;
import edu.ohsu.cslu.parser.util.Log;
import edu.ohsu.cslu.parser.util.ParseTree;
import edu.ohsu.cslu.parser.util.ParserUtil;

public class CellChart extends Chart {

    protected Parser<?> parser;
    protected HashSetChartCell chart[][];

    protected CellChart() {

    }

    public CellChart(final int[] tokens, final boolean viterbiMax, final Parser<?> parser) {
        super(tokens, viterbiMax);
        this.parser = parser;
        allocateChart(tokens.length);
    }

    public CellChart(final ParseTree tree, final boolean viterbiMax, final Parser<?> parser) {
        this.viterbiMax = viterbiMax;
        this.tokens = extractTokensFromParseTree(tree, parser.grammar);
        this.size = tokens.length;
        this.parser = parser;
        allocateChart(size);
        addParseTreeToChart(tree);
    }

    private void allocateChart(final int n) {
        chart = new HashSetChartCell[n][n + 1];
        for (int start = 0; start < n; start++) {
            for (int end = start + 1; end < n + 1; end++) {
                chart[start][end] = new HashSetChartCell(start, end);
            }
        }
    }

    private int[] extractTokensFromParseTree(final ParseTree tree, final Grammar grammar) {
        String sentence = "";
        for (final ParseTree node : tree.getLeafNodes()) {
            sentence += node.contents + " ";
        }
        return grammar.tokenizer.tokenizeToIndex(sentence.trim());
    }

    @Override
    public HashSetChartCell getCell(final int start, final int end) {
        return chart[start][end];
    }

    private void addParseTreeToChart(final ParseTree tree) {

        // NOTE: the purpose of this function is that I need to be able
        // to reference the constituents of a gold tree by reference to
        // a <start,end> position. I was hoping to reuse the chart class
        // since this is exactly what it does, but am running into problems
        // of just instantiating a "basic" version (edgeSelector, inside prob,
        // etc). Maybe it would be easier just to create an 2-dim array of
        // lists of ChartEdges (list because there can be gold unary AND binary
        // edges in each cell)

        final List<ParseTree> leafNodes = tree.getLeafNodes();
        int start, end, midpt, numChildren;
        Production prod = null;
        ChartEdge edge;
        String A, B, C;

        assert tree.isBinaryTree() == true;
        assert tree.parent == null; // must be root so start/end indicies make sense
        assert leafNodes.size() == this.size; // tree width/span must be same as chart

        // bad hack so that the FOM isn't computed for the new ChartEdges created below.
        final EdgeSelector saveEdgeSelector = parser.edgeSelector;
        parser.edgeSelector = null;

        for (final ParseTree node : tree.preOrderTraversal()) {
            // TODO: could make this O(1) instead of O(n) ...
            start = leafNodes.indexOf(node.leftMostLeaf());
            end = leafNodes.indexOf(node.rightMostLeaf()) + 1;
            numChildren = node.children.size();
            // System.out.println("convertToChart: node=" + node.contents + " start=" + start + " end=" + end
            // + " numChildren=" + numChildren);

            if (numChildren > 0) {
                A = node.contents;
                if (numChildren == 2) {
                    B = node.children.get(0).contents;
                    C = node.children.get(1).contents;
                    prod = parser.grammar.getBinaryProduction(A, B, C);
                    midpt = leafNodes.indexOf(node.children.get(0).rightMostLeaf()) + 1;
                    edge = new ChartEdge(prod, getCell(start, midpt), getCell(midpt, end));
                } else if (numChildren == 1) {
                    B = node.children.get(0).contents;
                    if (node.isPOS()) {
                        prod = parser.grammar.getLexicalProduction(A, B);
                    } else {
                        prod = parser.grammar.getUnaryProduction(A, B);
                    }
                    edge = new ChartEdge(prod, getCell(start, end));
                } else {
                    throw new RuntimeException("ERROR: Number of node children is " + node.children.size()
                            + ".  Expecting <= 2.");
                }

                if (prod == null) {
                    Log.info(
                            0,
                            "WARNING: production does not exist in grammar for node: " + A + " -> "
                                    + node.childrenToString());
                } else {
                    // chart[start][end].updateInside(edge);
                    chart[start][end].bestEdge[edge.prod.parent] = edge;
                    // System.out.println("updateInside: " + edge);
                }
            }
        }

        parser.edgeSelector = saveEdgeSelector;
    }

    @Override
    public HashSetChartCell getRootCell() {
        return getCell(0, size);
    }

    @Override
    public float getInside(final int start, final int end, final int nt) {
        return getCell(start, end).getInside(nt);

    }

    @Override
    public void updateInside(final int start, final int end, final int nt, final float insideProb) {
        getCell(start, end).updateInside(nt, insideProb);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(10240);
        for (int span = 1; span <= size; span++) {
            for (int start = 0; start <= size - span; start++) {
                final int end = start + span;
                sb.append(getCell(start, end).toString());
                sb.append("\n\n");
            }
        }
        return sb.toString();
    }

    // TODO: why is this not its own class in its own file? Do we actually need
    // the abstraction of a ChartCell? Can we put this all into Chart?
    public class HashSetChartCell extends ChartCell implements Comparable<HashSetChartCell> {
        public float fom = Float.NEGATIVE_INFINITY;
        protected boolean isLexCell;

        public ChartEdge[] bestEdge;
        public float[] inside;
        protected HashSet<Integer> childNTs = new HashSet<Integer>();
        protected HashSet<Integer> leftChildNTs = new HashSet<Integer>();
        protected HashSet<Integer> rightChildNTs = new HashSet<Integer>();
        protected HashSet<Integer> posNTs = new HashSet<Integer>();

        public HashSetChartCell(final int start, final int end) {
            super(start, end);

            if (end - start == 1) {
                isLexCell = true;
            } else {
                isLexCell = false;
            }

            bestEdge = new ChartEdge[parser.grammar.numNonTerms()];
            inside = new float[parser.grammar.numNonTerms()];
            Arrays.fill(inside, Float.NEGATIVE_INFINITY);
        }

        @Override
        public float getInside(final int nt) {
            return inside[nt];
        }

        public void updateInside(final int nt, final float insideProb) {
            if (viterbiMax) {
                if (insideProb > inside[nt]) {
                    inside[nt] = insideProb;
                    addToHashSets(nt);
                }
            } else {
                inside[nt] = (float) ParserUtil.logSum(inside[nt], insideProb);
                addToHashSets(nt);
            }
        }

        @Override
        public void updateInside(final Chart.ChartEdge edge) {
            final int nt = edge.prod.parent;
            final float insideProb = edge.inside();
            if (viterbiMax && insideProb > getInside(nt)) {
                bestEdge[nt] = (ChartEdge) edge;
            }
            updateInside(nt, insideProb);
        }

        // unary edges
        public void updateInside(final Production p, final float insideProb) {
            final int nt = p.parent;
            if (viterbiMax && insideProb > getInside(nt)) {
                bestEdge[nt] = new ChartEdge(p, this);
            }
            updateInside(nt, insideProb);
        }

        // binary edges
        @Override
        public void updateInside(final Production p, final ChartCell leftCell, final ChartCell rightCell,
                final float insideProb) {
            final int nt = p.parent;
            if (viterbiMax && insideProb > getInside(nt)) {
                if (bestEdge[nt] == null) {
                    bestEdge[nt] = new ChartEdge(p, leftCell, rightCell);
                } else {
                    bestEdge[nt].prod = p;
                    bestEdge[nt].leftCell = leftCell;
                    bestEdge[nt].rightCell = rightCell;
                    // TODO: bestEdge[nt].fom ??
                }
            }
            updateInside(nt, insideProb);
        }

        @Override
        public ChartEdge getBestEdge(final int nt) {
            return bestEdge[nt];
        }

        public List<ChartEdge> getBestEdgeList() {
            final List<ChartEdge> bestEdges = new LinkedList<ChartEdge>();
            for (int i = 0; i < bestEdge.length; i++) {
                if (bestEdge[i] != null) {
                    bestEdges.add(bestEdge[i]);
                }
            }
            return bestEdges;
        }

        public boolean hasNT(final int nt) {
            return inside[nt] > Float.NEGATIVE_INFINITY;
        }

        protected void addToHashSets(final int ntIndex) {
            childNTs.add(ntIndex);
            final NonTerminal nt = parser.grammar.getNonterminal(ntIndex);
            if (nt.isLeftChild) {
                leftChildNTs.add(ntIndex);
            }
            if (nt.isRightChild) {
                rightChildNTs.add(ntIndex);
            }
            if (nt.isPOS) {
                posNTs.add(ntIndex);
            }
        }

        public HashSet<Integer> getNTs() {
            return childNTs;
        }

        // TODO: this is called a lot but it is creating a new array for each call!
        // the whole point was NOT to do this. We need to use getNTs() where ever we can.
        public int[] getNtArray() {
            final int[] array = new int[childNTs.size()];
            int i = 0;
            for (final int nt : childNTs) {
                array[i++] = nt;
            }
            return array;
        }

        public HashSet<Integer> getPosNTs() {
            return posNTs;
        }

        public HashSet<Integer> getLeftChildNTs() {
            return leftChildNTs;
        }

        public HashSet<Integer> getRightChildNTs() {
            return rightChildNTs;
        }

        @Override
        public int width() {
            return end() - start();
        }

        @Override
        public int getNumNTs() {
            return childNTs.size();
        }

        @Override
        public boolean equals(final Object o) {
            return this == o;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder(1024);
            sb.append(getClass().getName() + "[" + start() + "][" + end() + "] with " + getNumNTs() + " (of "
                    + parser.grammar.numNonTerms() + ") edges");
            sb.append('\n');

            for (int i = 0; i < bestEdge.length; i++) {
                if (bestEdge[i] != null) {
                    sb.append(bestEdge[i].toString());
                    sb.append('\n');
                }
            }
            return sb.toString();
        }

        @Override
        public int compareTo(final HashSetChartCell otherCell) {
            if (this.fom == otherCell.fom) {
                return 0;
            } else if (fom > otherCell.fom) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    public class ChartEdge extends Chart.ChartEdge implements Comparable<ChartEdge> {
        public float fom = 0; // figure of merit

        // binary production
        public ChartEdge(final Production prod, final ChartCell leftCell, final ChartCell rightCell) {
            super(prod, leftCell, rightCell);

            if (parser.edgeSelector != null) {
                this.fom = parser.edgeSelector.calcFOM(this);
            }
        }

        // unary production
        public ChartEdge(final Production prod, final HashSetChartCell childCell) {
            super(prod, childCell);

            if (parser.edgeSelector != null) {
                this.fom = parser.edgeSelector.calcFOM(this);
            }
        }

        @Override
        public int compareTo(final ChartEdge otherEdge) {
            if (this.equals(otherEdge)) {
                return 0;
            } else if (fom > otherEdge.fom) {
                return -1;
            } else {
                return 1;
            }
        }

        @Override
        public String toString() {
            return super.toString() + String.format(" fom=%.3f", fom);
        }
    }
}

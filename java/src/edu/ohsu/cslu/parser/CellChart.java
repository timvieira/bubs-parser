package edu.ohsu.cslu.parser;

import java.util.Arrays;
import java.util.HashSet;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.NonTerminal;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector;
import edu.ohsu.cslu.parser.util.ParserUtil;

public class CellChart extends Chart {

    protected ChartCell chart[][];
    protected EdgeSelector edgeSelector;
    protected CellSelector cellSelector;
    protected boolean viterbiMax;

    protected CellChart() {

    }

    public CellChart(final int size, final Grammar grammar, final CellSelector cellSelector, final EdgeSelector edgeSelector) {
        super(size, grammar);
        this.cellSelector = cellSelector;
        this.edgeSelector = edgeSelector;
        this.viterbiMax = true;

        chart = new ChartCell[size][size + 1];
        for (int start = 0; start < size; start++) {
            for (int end = start + 1; end < size + 1; end++) {
                chart[start][end] = new ChartCell(start, end);
            }
        }
    }

    public CellChart(final int size, final Grammar grammar) {
        this(size, grammar, null, null);
    }

    public ChartCell getCell(final int start, final int end) {
        return chart[start][end];
    }

    public ChartCell getRootCell() {
        return chart[0][size];
    }

    @Override
    public float getInside(final int start, final int end, final int nt) {
        return getCell(start, end).getInside(nt);

    }

    @Override
    public void updateInside(final int start, final int end, final int nt, final float insideProb) {
        getCell(start, end).updateInside(nt, insideProb);
    }

    public class ChartCell implements Comparable<ChartCell> {
        protected final int start, end;
        public float fom = Float.NEGATIVE_INFINITY;
        public int numEdgesConsidered = 0, numEdgesAdded = 0, numSpanVisits = 0;
        protected boolean isLexCell;

        protected ChartEdge[] bestEdge;
        protected float[] inside;
        protected HashSet<Integer> childNTs = new HashSet<Integer>();
        protected HashSet<Integer> leftChildNTs = new HashSet<Integer>();
        protected HashSet<Integer> rightChildNTs = new HashSet<Integer>();
        protected HashSet<Integer> posNTs = new HashSet<Integer>();
        protected boolean bestEdgesHaveChanged = true;

        public ChartCell(final int start, final int end) {
            this.start = start;
            this.end = end;

            if (end - start == 1) {
                isLexCell = true;
            } else {
                isLexCell = false;
            }

            bestEdge = new ChartEdge[grammar.numNonTerms()];
            inside = new float[grammar.numNonTerms()];
            Arrays.fill(inside, Float.NEGATIVE_INFINITY);
        }

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

        public void updateInside(final ChartEdge edge) {
            final int nt = edge.prod.parent;
            final float insideProb = edge.inside();
            if (viterbiMax && insideProb > getInside(nt)) {
                bestEdge[nt] = edge;
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
        public void updateInside(final Production p, final ChartCell leftCell, final ChartCell rightCell, final float insideProb) {
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

        public float getOutside(final int nt) {
            throw new RuntimeException("ChartCell.getOutside(int) not implemented yet.");
        }

        public void updateOutside(final int nt, final float outsideProb) {
            throw new RuntimeException("ChartCell.getOutside(int) not implemented yet.");
        }

        public ChartEdge getBestEdge(final int nt) {
            return bestEdge[nt];
        }

        public boolean hasNT(final int nt) {
            // return bestEdge[nt] != null;
            return inside[nt] > Float.NEGATIVE_INFINITY;
        }

        protected void addToHashSets(final int ntIndex) {
            childNTs.add(ntIndex);
            final NonTerminal nt = grammar.getNonterminal(ntIndex);
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
            // if (bestEdgesHaveChanged) {
            // buildNTLists();
            // }
            return childNTs;
        }

        public HashSet<Integer> getPosNTs() {
            return posNTs;
        }

        public HashSet<Integer> getLeftChildNTs() {
            // if (bestEdgesHaveChanged) {
            // buildNTLists();
            // }
            return leftChildNTs;
        }

        public HashSet<Integer> getRightChildNTs() {
            // if (bestEdgesHaveChanged) {
            // buildNTLists();
            // }
            return rightChildNTs;
        }

        // private void buildNTLists() {
        // childNTs = new LinkedList<Integer>();
        // leftChildNTs = new LinkedList<Integer>();
        // rightChildNTs = new LinkedList<Integer>();
        // for (int ntIndex = 0; ntIndex < bestEdge.length; ntIndex++) {
        // if (bestEdge[ntIndex] != null) {
        // childNTs.add(ntIndex);
        // final NonTerminal nt = grammar.getNonterminal(ntIndex);
        // if (nt.isLeftChild()) {
        // leftChildNTs.add(ntIndex);
        // }
        // if (nt.isRightChild()) {
        // rightChildNTs.add(ntIndex);
        // }
        // }
        // }
        // bestEdgesHaveChanged = false;
        // }

        public final int start() {
            return start;
        }

        public final int end() {
            return end;
        }

        public int width() {
            return end() - start();
        }

        public int getNumNTs() {
            int numEntries = 0;
            for (int i = 0; i < bestEdge.length; i++) {
                if (bestEdge[i] != null)
                    numEntries++;
            }
            return numEntries;
        }

        @Override
        public boolean equals(final Object o) {
            return this == o;
        }

        @Override
        public String toString() {
            return getClass().getName() + "[" + start() + "][" + end() + "] with " + getNumNTs() + " (of " + grammar.numNonTerms() + ") edges";
        }

        @Override
        public int compareTo(final ChartCell otherCell) {
            if (this.fom == otherCell.fom) {
                return 0;
            } else if (fom > otherCell.fom) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    public class ChartEdge implements Comparable<ChartEdge> {
        public Production prod;
        public float fom = 0; // figure of merit
        // TODO: should we create a getLeftCell() and getRightCell(). We'd need to keep a chart pointer...
        public ChartCell leftCell, rightCell;

        // binary production
        public ChartEdge(final Production prod, final ChartCell leftCell, final ChartCell rightCell) {
            assert leftCell.end() == rightCell.start();
            assert leftCell.start() < rightCell.end();

            this.prod = prod;
            // this.inside = inside;
            this.leftCell = leftCell;
            this.rightCell = rightCell;
            if (edgeSelector != null) {
                this.fom = edgeSelector.calcFOM(this);
            }
        }

        // unary production
        public ChartEdge(final Production prod, final ChartCell childCell) {
            this.prod = prod;
            // this.inside = inside;
            this.leftCell = childCell;
            this.rightCell = null;
            if (edgeSelector != null) {
                this.fom = edgeSelector.calcFOM(this);
            }

        }

        public final int start() {
            return leftCell.start();
        }

        public final int end() {
            if (rightCell == null) {
                return leftCell.end();
            }
            return rightCell.end();
        }

        public final int midpt() {
            if (rightCell == null) {
                if (leftCell == null) {
                    throw new RuntimeException("right/leftCell must be set to use start(), end(), and midpt()");
                }
                throw new RuntimeException("Do not use midpt() with unary productions.  They do not have midpoints.");
            }
            return leftCell.end();
        }

        public float inside() {
            if (prod.isBinaryProd()) {
                return prod.prob + leftCell.getInside(prod.leftChild) + rightCell.getInside(prod.rightChild);
            } else if (prod.isUnaryProd()) {
                return prod.prob + leftCell.getInside(prod.child());
            }
            return prod.prob;
        }

        public ChartEdge copy() {
            return new ChartEdge(this.prod, this.leftCell, this.rightCell);
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

        public int spanLength() {
            return end() - start();
        }

        @Override
        public String toString() {
            String start = "-", midpt = "-", end = "-", prodStr = "null";

            if (leftCell != null) {
                start = "" + leftCell.start();
                if (rightCell != null) {
                    midpt = "" + leftCell.end();
                } else {
                    end = "" + leftCell.end();
                }
            }
            if (rightCell != null) {
                end = "" + rightCell.end();
                assert leftCell.end() == rightCell.start();
            }
            if (prod != null) {
                prodStr = prod.toString();
            }

            return String.format("[%s,%s,%s] %s inside=%f fom=%f", start, midpt, end, prodStr, inside(), fom);
        }

        @Override
        public boolean equals(final Object other) {
            try {
                if (this == other) {
                    return true;
                }

                if (other == null) {
                    return false;
                }

                final ChartEdge otherEdge = (ChartEdge) other;
                if (prod == null && otherEdge.prod != null) {
                    return false;
                }

                if (!prod.equals(otherEdge.prod)) {
                    return false;
                }

                // not comparing left/right cell object pointers because I want to be able to compare
                // cells from different charts
                if (start() != otherEdge.start()) {
                    return false;
                }

                if (end() != otherEdge.end()) {
                    return false;
                }

                if (prod.isBinaryProd() && (midpt() != otherEdge.midpt())) {
                    return false;
                }

                return true;
            } catch (final Exception e) {
                return false;
            }
        }
    }
}

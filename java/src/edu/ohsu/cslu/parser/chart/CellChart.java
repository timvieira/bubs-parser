package edu.ohsu.cslu.parser.chart;

import java.util.Arrays;
import java.util.HashSet;

import edu.ohsu.cslu.grammar.NonTerminal;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.Parser;
import edu.ohsu.cslu.parser.util.ParserUtil;

public class CellChart extends Chart {

    protected HashSetChartCell chart[][];

    protected CellChart() {

    }

    public CellChart(final int size, final boolean viterbiMax, final Parser<?> parser) {
        super(size, viterbiMax, parser);

        chart = new HashSetChartCell[size][size + 1];
        for (int start = 0; start < size; start++) {
            for (int end = start + 1; end < size + 1; end++) {
                chart[start][end] = new HashSetChartCell(start, end);
            }
        }
    }

    @Override
    public HashSetChartCell getCell(final int start, final int end) {
        return chart[start][end];
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
            return getClass().getName() + "[" + start() + "][" + end() + "] with " + getNumNTs() + " (of "
                    + parser.grammar.numNonTerms() + ") edges";
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
                    throw new RuntimeException(
                        "right/leftCell must be set to use start(), end(), and midpt()");
                }
                throw new RuntimeException(
                    "Do not use midpt() with unary productions.  They do not have midpoints.");
            }
            return leftCell.end();
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

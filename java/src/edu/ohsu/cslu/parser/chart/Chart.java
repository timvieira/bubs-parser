package edu.ohsu.cslu.parser.chart;

import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.util.ParseTree;

public abstract class Chart {

    protected int size;
    protected boolean viterbiMax;
    public int[] tokens;

    protected Chart() {
    }

    public Chart(final int[] tokens, final boolean viterbiMax) {
        this.tokens = tokens;
        this.size = tokens.length;
        this.viterbiMax = viterbiMax;
    }

    // public Chart(final int size, final boolean viterbiMax) {
    // this.size = size;
    // this.viterbiMax = viterbiMax;
    // }

    public final int size() {
        return size;
    }

    /**
     * Returns the specified cell.
     * 
     * @param start
     * @param end
     * @return the specified cell.
     */
    public abstract ChartCell getCell(final int start, final int end);

    /**
     * Returns the root cell
     * 
     * @return the root cell
     */
    public ChartCell getRootCell() {
        return getCell(0, size);
    }

    /**
     * Updates the inside probability of the specified non-terminal in the a cell.
     * 
     * @param start
     * @param end
     * @param nonTerminal Non-terminal index
     * @param insideProbability New inside probability
     */
    public abstract void updateInside(int start, int end, int nonTerminal, float insideProbability);

    /**
     * Returns the inside probability of the specified non-terminal in the a cell.
     * 
     * @param start
     * @param end
     * @param nonTerminal Non-terminal index
     * @return the inside probability of the specified non-terminal in the a cell.
     */
    public abstract float getInside(int start, int end, int nonTerminal);

    public boolean hasCompleteParse(final int startSymbol) {
        return getRootCell().getBestEdge(startSymbol) != null;
    }

    public ParseTree extractBestParse(final int startSymbol) {
        return extractBestParse(getRootCell(), startSymbol);
    }

    public ParseTree extractBestParse(final ChartCell cell, final int nonTermIndex) {
        ChartEdge bestEdge;
        ParseTree curNode = null;

        if (cell != null) {
            bestEdge = cell.getBestEdge(nonTermIndex);
            if (bestEdge != null) {
                curNode = new ParseTree(bestEdge.prod.parentToString());
                if (bestEdge.prod.isUnaryProd()) {
                    curNode.children.add(extractBestParse(bestEdge.leftCell, bestEdge.prod.leftChild));
                } else if (bestEdge.prod.isLexProd()) {
                    curNode.addChild(new ParseTree(bestEdge.prod.childrenToString()));
                } else { // binary production
                    curNode.children.add(extractBestParse(bestEdge.leftCell, bestEdge.prod.leftChild));
                    curNode.children.add(extractBestParse(bestEdge.rightCell, bestEdge.prod.rightChild));
                }
            }
        }

        return curNode;
    }

    public String getStats() {
        String result = "";
        int con = 0, add = 0;

        for (int start = 0; start < size(); start++) {
            for (int end = start + 1; end < size() + 1; end++) {
                con += getCell(start, end).numEdgesConsidered;
                add += getCell(start, end).numEdgesAdded;
            }
        }

        result += " chartEdges=" + add + " processedEdges=" + con;
        return result;
    }

    public static abstract class ChartCell {
        protected final int start, end;
        public int numEdgesConsidered = 0, numEdgesAdded = 0, numSpanVisits = 0;

        public ChartCell(final int start, final int end) {
            this.start = start;
            this.end = end;
        }

        /**
         * Returns the log probability of the specified non-terminal
         * 
         * @param nonTerminal
         * @return log probability of the specified non-terminal
         */
        public abstract float getInside(final int nonTerminal);

        /**
         * Returns the most probable edge producing the specified non-terminal. Most {@link ChartCell} implementations
         * will only maintain one edge per non-terminal, but some implementations may maintain multiple edges (e.g., for
         * k-best parsing).
         * 
         * @param nonTerminal
         * @return the most probable populated edge producing the specified non-terminal
         */
        public abstract ChartEdge getBestEdge(final int nonTerminal);

        public abstract void updateInside(final ChartEdge edge);

        public abstract void updateInside(final Production p, final ChartCell leftCell, final ChartCell rightCell,
                final float insideProb);

        /**
         * @return the word index of the first word covered by this cell
         */
        public final int start() {
            return start;
        }

        /**
         * @return the word index of the last word covered by this cell
         */
        public final int end() {
            return end;
        }

        /**
         * @return the width of the span covered by this cell
         */
        public int width() {
            return end() - start();
        }

        /**
         * @return the number of populated non-terminals in this cell
         */
        public abstract int getNumNTs();

        @Override
        public boolean equals(final Object o) {
            return this == o;
        }

        @Override
        public String toString() {
            return getClass().getName() + "[" + start() + "][" + end() + "] with " + getNumNTs() + " edges";
        }

        public void finalizeCell() {
        }
    }

    public static class ChartEdge {
        public Production prod;
        public ChartCell leftCell, rightCell;

        // TODO: other options to keeping leftCell and rightCell:
        // 1) keep int start,midpt,end
        // 2) keep ChartCell ptr and midpt

        // binary production
        public ChartEdge(final Production prod, final ChartCell leftCell, final ChartCell rightCell) {
            assert leftCell.end() == rightCell.start();
            assert leftCell.start() < rightCell.end();

            this.prod = prod;
            this.leftCell = leftCell;
            this.rightCell = rightCell;
        }

        // unary production
        public ChartEdge(final Production prod, final ChartCell childCell) {
            this.prod = prod;
            this.leftCell = childCell;
            this.rightCell = null;
        }

        public float inside() {
            if (prod.isBinaryProd()) {
                return prod.prob + leftCell.getInside(prod.leftChild) + rightCell.getInside(prod.rightChild);
            } else if (prod.isUnaryProd()) {
                return prod.prob + leftCell.getInside(prod.child());
            }
            return prod.prob;
        }
    }
}

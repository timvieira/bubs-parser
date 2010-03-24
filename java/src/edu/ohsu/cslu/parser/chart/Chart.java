package edu.ohsu.cslu.parser.chart;

import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.Parser;

public abstract class Chart {

    protected int size;
    protected boolean viterbiMax;
    protected Parser<?> parser;

    protected Chart() {
    }

    public Chart(final int size, final boolean viterbiMax, final Parser<?> parser) {
        this.size = size;
        this.viterbiMax = viterbiMax;
        this.parser = parser;
    }

    public int size() {
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
         * Returns the most probable edge producing the specified non-terminal. Most {@link ChartCell} implementations will only maintain one edge per non-terminal, but some
         * implementations may maintain multiple edges (e.g., for k-best parsing).
         * 
         * @param nonTerminal
         * @return the most probable populated edge producing the specified non-terminal
         */
        public abstract ChartEdge getBestEdge(final int nonTerminal);

        public abstract void updateInside(final ChartEdge edge);

        public abstract void updateInside(final Production p, final ChartCell leftCell, final ChartCell rightCell, final float insideProb);

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

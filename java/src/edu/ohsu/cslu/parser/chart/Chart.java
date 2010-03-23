package edu.ohsu.cslu.parser.chart;

import edu.ohsu.cslu.parser.Parser;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;

public abstract class Chart<T extends Chart.ChartCell> {

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

    public abstract void updateInside(int start, int end, int nt, float insideProb);

    public abstract float getInside(int start, int end, int nt);

    // public abstract void updateOutside(int start, int end, int nt, float insideProb);
    // public abstract float getOutside(int start, int end, int nt);

    public static abstract class ChartCell {
        protected final int start, end;
        public int numEdgesConsidered = 0, numEdgesAdded = 0, numSpanVisits = 0;

        public ChartCell(final int start, final int end) {
            this.start = start;
            this.end = end;
        }

        public abstract float getInside(final int nt);

        public abstract ChartEdge getBestEdge(final int nt);

        // public abstract boolean hasNT(final int nt);

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

        public abstract int getNumNTs();

        @Override
        public boolean equals(final Object o) {
            return this == o;
        }

        @Override
        public String toString() {
            return getClass().getName() + "[" + start() + "][" + end() + "] with " + getNumNTs() + " edges";
        }
    }
}

package edu.ohsu.cslu.parser.chart;

import edu.ohsu.cslu.grammar.SparseMatrixGrammar;

public class InsideOutsideChart extends PackedArrayChart {

    public final float[] outsideProbabilities;

    public InsideOutsideChart(final int[] tokens, final SparseMatrixGrammar sparseMatrixGrammar) {
        super(tokens, sparseMatrixGrammar);
        this.outsideProbabilities = new float[chartArraySize];
    }

    public void finalizeOutside(final float[] tmpOutsideProbabilities, final int offset) {

        // Copy all populated entries from temporary storage
        int nonTerminalOffset = offset;
        for (short nonTerminal = 0; nonTerminal < tmpOutsideProbabilities.length; nonTerminal++) {

            if (tmpOutsideProbabilities[nonTerminal] != Float.NEGATIVE_INFINITY) {
                outsideProbabilities[nonTerminalOffset++] = tmpOutsideProbabilities[nonTerminal];
            }
        }
    }
}

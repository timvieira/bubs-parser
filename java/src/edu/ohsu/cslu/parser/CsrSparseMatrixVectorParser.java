package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.parser.chart.DenseVectorChart;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.DenseVectorChart.DenseVectorChartCell;

/**
 * {@link SparseMatrixVectorParser} which uses a sparse grammar stored in CSR format ({@link CsrSparseMatrixGrammar}) and implements cross-product and SpMV multiplication in Java.
 * 
 * @see OpenClSparseMatrixVectorParser
 * 
 * @author Aaron Dunlop
 * @since Feb 11, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class CsrSparseMatrixVectorParser extends SparseMatrixVectorParser<CsrSparseMatrixGrammar, DenseVectorChart> {

    // private final CsrSparseMatrixGrammar csrSparseMatrixGrammar;

    public CsrSparseMatrixVectorParser(final CsrSparseMatrixGrammar grammar) {
        super(grammar);
    }

    @Override
    protected void initParser(final int sentLength) {
        chart = new DenseVectorChart(sentLength, opts.viterbiMax, this);
        super.initParser(sentLength);
    }

    @Override
    protected void visitCell(final short start, final short end) {

        final DenseVectorChartCell spvChartCell = (DenseVectorChartCell) chart.getCell(start, end);

        final long t0 = System.currentTimeMillis();
        long t1 = t0;
        long crossProductTime = 0;

        // Skip binary grammar intersection for span-1 cells
        if (end - start > 1) {
            final CrossProductVector crossProductVector = crossProductUnion(start, end);

            t1 = System.currentTimeMillis();
            crossProductTime = t1 - t0;

            // Multiply the unioned vector with the grammar matrix and populate the current cell with the
            // vector resulting from the matrix-vector multiplication
            binarySpmvMultiply(crossProductVector, spvChartCell);
        }
        final long t2 = System.currentTimeMillis();
        final long binarySpmvTime = t2 - t1;

        // Handle unary productions
        // TODO: This only goes through unary rules one time, so it can't create unary chains unless such chains are encoded in the grammar. Iterating a few times would probably
        // work, although it's a big-time hack.
        unarySpmvMultiply(spvChartCell);

        final long t3 = System.currentTimeMillis();
        final long unarySpmvTime = t3 - t2;

        // TODO We won't need to do this once we're storing directly into the packed array
        spvChartCell.finalizeCell();

        // System.out.format("Visited cell: %2d,%2d (%5d ms). Cross-product: %6d/%6d combinations (%5.0f ms, %4.2f/ms), Multiply: %5d edges (%5.0f ms, %4.2f /ms)\n", start, end, t3
        // - t0, crossProductSize, totalProducts, crossProductTime, crossProductSize / crossProductTime, edges, spmvTime, edges / spmvTime);
        totalCartesianProductTime += crossProductTime;
        totalSpMVTime += binarySpmvTime + unarySpmvTime;
    }

    @Override
    public void binarySpmvMultiply(final CrossProductVector crossProductVector, final ChartCell chartCell) {

        final DenseVectorChartCell denseVectorCell = (DenseVectorChartCell) chartCell;

        final int[] binaryRuleMatrixRowIndices = grammar.binaryRuleMatrixRowIndices();
        final int[] binaryRuleMatrixColumnIndices = grammar.binaryRuleMatrixColumnIndices();
        final float[] binaryRuleMatrixProbabilities = grammar.binaryRuleMatrixProbabilities();

        final float[] crossProductProbabilities = crossProductVector.probabilities;
        final short[] crossProductMidpoints = crossProductVector.midpoints;

        final int[] chartCellChildren = denseVectorCell.children;
        final float[] chartCellProbabilities = denseVectorCell.inside;
        final short[] chartCellMidpoints = denseVectorCell.midpoints;

        int numValidLeftChildren = 0, numValidRightChildren = 0;

        // Iterate over possible parents (matrix rows)
        for (int parent = 0; parent < grammar.numNonTerms(); parent++) {

            // Production winningProduction = null;
            float winningProbability = Float.NEGATIVE_INFINITY;
            int winningChildren = Integer.MIN_VALUE;
            short winningMidpoint = 0;

            // Iterate over possible children of the parent (columns with non-zero entries)
            for (int i = binaryRuleMatrixRowIndices[parent]; i < binaryRuleMatrixRowIndices[parent + 1]; i++) {
                final int grammarChildren = binaryRuleMatrixColumnIndices[i];
                final float grammarProbability = binaryRuleMatrixProbabilities[i];

                final float crossProductProbability = crossProductProbabilities[grammarChildren];
                final float jointProbability = grammarProbability + crossProductProbability;

                if (jointProbability > winningProbability) {
                    winningProbability = jointProbability;
                    winningChildren = grammarChildren;
                    winningMidpoint = crossProductMidpoints[grammarChildren];
                }
            }

            if (winningProbability != Float.NEGATIVE_INFINITY) {
                chartCellChildren[parent] = winningChildren;
                chartCellProbabilities[parent] = winningProbability;
                chartCellMidpoints[parent] = winningMidpoint;

                if (grammar.isValidLeftChild(parent)) {
                    numValidLeftChildren++;
                }
                if (grammar.isValidRightChild(parent)) {
                    numValidRightChildren++;
                }
            }
        }
        denseVectorCell.numValidLeftChildren = numValidLeftChildren;
        denseVectorCell.numValidRightChildren = numValidRightChildren;
    }

    @Override
    public void unarySpmvMultiply(final ChartCell chartCell) {

        final DenseVectorChartCell denseVectorCell = (DenseVectorChartCell) chartCell;

        final int[] unaryRuleMatrixRowIndices = grammar.unaryRuleMatrixRowIndices();
        final int[] unaryRuleMatrixColumnIndices = grammar.unaryRuleMatrixColumnIndices();
        final float[] unaryRuleMatrixProbabilities = grammar.unaryRuleMatrixProbabilities();

        final int[] chartCellChildren = denseVectorCell.children;
        final float[] chartCellProbabilities = denseVectorCell.inside;
        final short[] chartCellMidpoints = denseVectorCell.midpoints;
        final short chartCellEnd = (short) chartCell.end();

        // Iterate over possible parents (matrix rows)
        for (int parent = 0; parent < grammar.numNonTerms(); parent++) {

            final float currentProbability = chartCellProbabilities[parent];
            float winningProbability = currentProbability;
            int winningChildren = Integer.MIN_VALUE;
            short winningMidpoint = 0;

            // Iterate over possible children of the parent (columns with non-zero entries)
            for (int i = unaryRuleMatrixRowIndices[parent]; i < unaryRuleMatrixRowIndices[parent + 1]; i++) {

                final int grammarChildren = unaryRuleMatrixColumnIndices[i];
                final int child = grammar.unpackLeftChild(grammarChildren);
                final float grammarProbability = unaryRuleMatrixProbabilities[i];

                final float jointProbability = grammarProbability + chartCellProbabilities[child];

                if (jointProbability > winningProbability) {
                    winningProbability = jointProbability;
                    winningChildren = grammarChildren;
                    winningMidpoint = chartCellEnd;
                }
            }

            if (winningChildren != Integer.MIN_VALUE) {
                chartCellChildren[parent] = winningChildren;
                chartCellProbabilities[parent] = winningProbability;
                chartCellMidpoints[parent] = winningMidpoint;

                if (currentProbability == Float.NEGATIVE_INFINITY) {
                    if (grammar.isValidLeftChild(parent)) {
                        denseVectorCell.numValidLeftChildren++;
                    }
                    if (grammar.isValidRightChild(parent)) {
                        denseVectorCell.numValidRightChildren++;
                    }
                }
            }
        }
    }
}

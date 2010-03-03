package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.JsaSparseMatrixGrammar;
import edu.ohsu.cslu.parser.cellselector.CellSelector;

/**
 * SparseMatrixVectorParser implementation which uses a grammar stored in Java Sparse Array (JSA) format. Stores cell populations and cross-product densely, for efficient array
 * access and to avoid hashing (even though it's not quite as memory-efficient).
 * 
 * @author Aaron Dunlop
 * @since Jan 28, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class JsaSparseMatrixVectorParser extends SparseMatrixVectorParser {

    private final JsaSparseMatrixGrammar jsaSparseMatrixGrammar;

    public JsaSparseMatrixVectorParser(final JsaSparseMatrixGrammar grammar, final CellSelector cellSelector) {
        super(grammar, cellSelector);
        this.jsaSparseMatrixGrammar = grammar;
    }

    @Override
    protected void visitCell(final ChartCell cell) {

        final DenseVectorChartCell spvChartCell = (DenseVectorChartCell) cell;
        // TODO Change ChartCell.start() and end() to return shorts (since we shouldn't have to handle sentences longer than 32767)
        final short start = (short) cell.start();
        final short end = (short) cell.end();

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
    public void binarySpmvMultiply(final CrossProductVector crossProductVector, final DenseVectorChartCell chartCell) {

        final int[][] grammarRuleMatrix = jsaSparseMatrixGrammar.binaryRuleMatrix();
        final float[][] grammarProbabilities = jsaSparseMatrixGrammar.binaryProbabilities();

        final float[] crossProductProbabilities = crossProductVector.probabilities;
        final short[] crossProductMidpoints = crossProductVector.midpoints;

        final int[] chartCellChildren = chartCell.children;
        final float[] chartCellProbabilities = chartCell.probabilities;
        final short[] chartCellMidpoints = chartCell.midpoints;

        int numValidLeftChildren = 0, numValidRightChildren = 0;

        // Iterate over possible parents
        for (int parent = 0; parent < jsaSparseMatrixGrammar.numNonTerms(); parent++) {

            final int[] grammarChildrenForParent = grammarRuleMatrix[parent];
            final float[] grammarProbabilitiesForParent = grammarProbabilities[parent];

            // Production winningProduction = null;
            float winningProbability = Float.NEGATIVE_INFINITY;
            int winningChildren = Integer.MIN_VALUE;
            short winningMidpoint = 0;

            for (int i = 0; i < grammarChildrenForParent.length; i++) {
                final int grammarChildren = grammarChildrenForParent[i];

                final float grammarProbability = grammarProbabilitiesForParent[i];
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

                if (jsaSparseMatrixGrammar.isValidLeftChild(parent)) {
                    numValidLeftChildren++;
                }
                if (jsaSparseMatrixGrammar.isValidRightChild(parent)) {
                    numValidRightChildren++;
                }
            }
        }
        chartCell.numValidLeftChildren = numValidLeftChildren;
        chartCell.numValidRightChildren = numValidRightChildren;
    }

    @Override
    public void unarySpmvMultiply(final DenseVectorChartCell chartCell) {

        final int[][] grammarRuleMatrix = jsaSparseMatrixGrammar.unaryRuleMatrix();
        final float[][] grammarProbabilities = jsaSparseMatrixGrammar.unaryProbabilities();

        final int[] chartCellChildren = chartCell.children;
        final float[] chartCellProbabilities = chartCell.probabilities;
        final short[] chartCellMidpoints = chartCell.midpoints;
        final short chartCellEnd = (short) chartCell.end();

        // Iterate over possible parents
        for (int parent = 0; parent < jsaSparseMatrixGrammar.numNonTerms(); parent++) {
            final int[] grammarChildrenForParent = grammarRuleMatrix[parent];
            final float[] grammarProbabilitiesForParent = grammarProbabilities[parent];

            final float currentProbability = chartCellProbabilities[parent];
            float winningProbability = currentProbability;
            int winningChildren = Integer.MIN_VALUE;
            short winningMidpoint = 0;

            for (int i = 0; i < grammarChildrenForParent.length; i++) {
                final int packedChildren = grammarChildrenForParent[i];
                final int child = jsaSparseMatrixGrammar.unpackLeftChild(packedChildren);

                final float grammarProbability = grammarProbabilitiesForParent[i];
                final float crossProductProbability = chartCellProbabilities[child];
                final float jointProbability = grammarProbability + crossProductProbability;

                if (jointProbability > winningProbability) {
                    winningProbability = jointProbability;
                    winningChildren = packedChildren;
                    winningMidpoint = chartCellEnd;
                }
            }

            if (winningChildren != Integer.MIN_VALUE) {
                chartCellChildren[parent] = winningChildren;
                chartCellProbabilities[parent] = winningProbability;
                chartCellMidpoints[parent] = winningMidpoint;

                if (currentProbability == Float.NEGATIVE_INFINITY) {
                    if (jsaSparseMatrixGrammar.isValidLeftChild(parent)) {
                        chartCell.numValidLeftChildren++;
                    }
                    if (jsaSparseMatrixGrammar.isValidRightChild(parent)) {
                        chartCell.numValidRightChildren++;
                    }
                }
            }
        }
    }
}

package edu.ohsu.cslu.ella;

import java.util.Arrays;

import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashFilterFunction;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;
import edu.ohsu.cslu.parser.spmv.SparseMatrixVectorParser;

/**
 * SpMV parser which constrains the chart population according to the contents of another chart. The constraining chart
 * is generally populated with a related, but simpler, grammar (e.g. coarse-to-fine).
 * 
 * 
 * Implementation notes:
 * 
 * --The target chart need only contain C * S entries per cell, where C is the largest number of non-terminals populated
 * in the constraining chart and S is the largest number of fine non-terminals split from a single coarse non-terminal.
 * e.g., when doing Berkeley-style split-merge learning, we will always split each coarse non-terminal into 2 fine
 * non-terminals, so S will always be 2.
 * 
 * --We need the CellSelector to return for each cell an array of valid non-terminals (computed from the constraining
 * chart)
 * 
 * --The Cartesian-product should only be taken over the known child cells.
 * 
 * TODO Should it be further limited to only the splits of the constraining child in each cell? e.g., on the second
 * iteration, when child A has been split into A_1 and A_2, and then to A_1a, A_1b, A_2a, and A_2b, and child B
 * similarly to B_1a, B_1b, B_2a, and B_2b, should we allow A_1a and A_1b to combine with B_2a and B_2b?
 * 
 * --We only need to maintain a single midpoint for each cell
 * 
 * --We do need to maintain space for a few unary productions; assume the first entry in each chart cell is for the top
 * node in the unary chain; any others (if populated) are unary children.
 * 
 * --Binary SpMV need only consider rules whose parent is in the set of known parent NTs. We iterate over those parent
 * rows in a CSR grammar.
 * 
 * @author Aaron Dunlop
 * @since Dec 23, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class ConstrainedCsrSpmvParser extends SparseMatrixVectorParser<CsrSparseMatrixGrammar, ConstrainedChart> {

    ConstrainedChart constrainingChart;

    public ConstrainedCsrSpmvParser(final ParserDriver opts, final CsrSparseMatrixGrammar grammar) {
        super(opts, grammar);
    }

    /**
     * Takes the cartesian-product of all potential child-cell combinations. Unions those cartesian-products together,
     * saving the maximum probability child combinations.
     * 
     * In a constrained chart, we only have a single (known) midpoint to iterate over
     * 
     * @param start
     * @param end
     * @return Unioned Cartesian-product
     */
    protected final CartesianProductVector cartesianProductUnion(final int start, final int end, final short midpoint) {

        Arrays.fill(cartesianProductMidpoints, (short) 0);
        final int size = 0;

        final PerfectIntPairHashFilterFunction cpf = (PerfectIntPairHashFilterFunction) grammar
                .cartesianProductFunction();
        final short[] nonTerminalIndices = chart.nonTerminalIndices;
        final float[] insideProbabilities = chart.insideProbabilities;

        // final int leftCellIndex = chart.cellIndex(start, midpoint);
        // final int rightCellIndex = chart.cellIndex(midpoint, end);
        //
        // final int leftStart = chart.minLeftChildIndex(leftCellIndex);
        // final int leftEnd = chart.maxLeftChildIndex(leftCellIndex);
        //
        // final int rightStart = chart.minRightChildIndex(rightCellIndex);
        // final int rightEnd = chart.maxRightChildIndex(rightCellIndex);
        //
        // for (int i = leftStart; i <= leftEnd; i++) {
        // final short leftChild = nonTerminalIndices[i];
        // final float leftProbability = insideProbabilities[i];
        // final int mask = cpf.mask(leftChild);
        // final int shift = cpf.shift(leftChild);
        // final int offset = cpf.offset(leftChild);
        //
        // final short minRightSibling = grammar.minRightSiblingIndices[leftChild];
        // final short maxRightSibling = grammar.maxRightSiblingIndices[leftChild];
        //
        // for (int j = rightStart; j <= rightEnd; j++) {
        // // Skip any right children which cannot combine with left child
        // if (nonTerminalIndices[j] < minRightSibling) {
        // continue;
        // } else if (nonTerminalIndices[j] > maxRightSibling) {
        // break;
        // }
        //
        // if (collectDetailedStatistics) {
        // totalCartesianProductEntriesExamined++;
        // }
        //
        // final int childPair = cpf.pack(nonTerminalIndices[j], shift, mask, offset);
        // if (childPair == Integer.MIN_VALUE) {
        // continue;
        // }
        //
        // final float jointProbability = leftProbability + insideProbabilities[j];
        //
        // if (collectDetailedStatistics) {
        // totalValidCartesianProductEntries++;
        // }
        //
        // cartesianProductProbabilities[childPair] = jointProbability;
        // cartesianProductMidpoints[childPair] = midpoint;
        //
        // if (collectDetailedStatistics) {
        // size++;
        // }
        // }
        // }

        return new CartesianProductVector(grammar, cartesianProductProbabilities, cartesianProductMidpoints, size);
    }

    @Override
    public void binarySpmv(final CartesianProductVector cartesianProductVector, final ChartCell chartCell) {

        final PackedArrayChartCell packedArrayCell = (PackedArrayChartCell) chartCell;
        packedArrayCell.allocateTemporaryStorage();

        final int[] chartCellChildren = packedArrayCell.tmpPackedChildren;
        final float[] chartCellProbabilities = packedArrayCell.tmpInsideProbabilities;
        final short[] chartCellMidpoints = packedArrayCell.tmpMidpoints;

        binarySpmvMultiply(cartesianProductVector, chartCellChildren, chartCellProbabilities, chartCellMidpoints);
    }

    protected final void binarySpmvMultiply(final CartesianProductVector cartesianProductVector,
            final int[] productChildren, final float[] productProbabilities, final short[] productMidpoints) {

        // Iterate over possible parents (matrix rows)
        final int v = grammar.numNonTerms();
        for (int parent = 0; parent < v; parent++) {

            float winningProbability = Float.NEGATIVE_INFINITY;
            int winningChildren = Integer.MIN_VALUE;
            short winningMidpoint = 0;

            // Iterate over possible children of the parent (columns with non-zero entries)
            for (int i = grammar.csrBinaryRowIndices[parent]; i < grammar.csrBinaryRowIndices[parent + 1]; i++) {
                final int grammarChildren = grammar.csrBinaryColumnIndices[i];

                if (cartesianProductVector.midpoints[grammarChildren] == 0) {
                    continue;
                }

                final float jointProbability = grammar.csrBinaryProbabilities[i]
                        + cartesianProductVector.probabilities[grammarChildren];

                if (jointProbability > winningProbability) {
                    winningProbability = jointProbability;
                    winningChildren = grammarChildren;
                    winningMidpoint = cartesianProductVector.midpoints[grammarChildren];
                }
            }

            if (winningProbability != Float.NEGATIVE_INFINITY) {
                productChildren[parent] = winningChildren;
                productProbabilities[parent] = winningProbability;
                productMidpoints[parent] = winningMidpoint;
            }
        }
    }

    @Override
    protected edu.ohsu.cslu.parser.spmv.SparseMatrixVectorParser.CartesianProductVector cartesianProductUnion(
            final int start, final int end) {
        // TODO Auto-generated method stub
        return null;
    }

    // @Override
    // public void binarySpmv(final CartesianProductVector cartesianProductVector, final ChartCell chartCell) {
    //
    // final PackedArrayChartCell targetCell = (PackedArrayChartCell) chartCell;
    // targetCell.allocateTemporaryStorage();
    //
    // if (cellSelector.factoredParentsOnly(chartCell.start(), chartCell.end())) {
    // binarySpmvMultiply(cartesianProductVector, grammar.factoredCscBinaryPopulatedColumns,
    // grammar.factoredCscBinaryPopulatedColumnOffsets, grammar.factoredCscBinaryRowIndices,
    // grammar.factoredCscBinaryProbabilities, targetCell.tmpPackedChildren,
    // targetCell.tmpInsideProbabilities, targetCell.tmpMidpoints);
    // } else {
    // binarySpmvMultiply(cartesianProductVector, grammar.csrBinaryPopulatedColumns,
    // grammar.cscBinaryPopulatedColumnOffsets, grammar.csrBinaryRowIndices,
    // grammar.cscBinaryProbabilities, targetCell.tmpPackedChildren, targetCell.tmpInsideProbabilities,
    // targetCell.tmpMidpoints);
    // }
    // }
    //
    // private void binarySpmvMultiply(final CartesianProductVector cartesianProductVector,
    // final int[] grammarCscBinaryPopulatedColumns, final int[] grammarCscBinaryPopulatedColumnOffsets,
    // final short[] grammarCscBinaryRowIndices, final float[] grammarCscBinaryProbabilities,
    // final int[] targetCellChildren, final float[] targetCellProbabilities, final short[] targetCellMidpoints) {
    //
    // // Iterate over possible populated child pairs (matrix columns)
    // for (int i = 0; i < grammarCscBinaryPopulatedColumns.length; i++) {
    //
    // // TODO Try iterating through the midpoints array first and only look up the childPair for populated
    // // columns. Even though some entries will be impossible, the cache-efficiency of in-order iteration might be
    // // a win?
    // final int childPair = grammarCscBinaryPopulatedColumns[i];
    // final short cartesianProductMidpoint = cartesianProductVector.midpoints[childPair];
    //
    // // Skip grammar matrix columns for unpopulated cartesian-product entries
    // if (cartesianProductMidpoint == 0) {
    // continue;
    // }
    // final float cartesianProductProbability = cartesianProductVector.probabilities[childPair];
    //
    // // Iterate over possible parents of the child pair (rows with non-zero entries)
    // for (int j = grammarCscBinaryPopulatedColumnOffsets[i]; j < grammarCscBinaryPopulatedColumnOffsets[i + 1]; j++) {
    //
    // final float jointProbability = grammarCscBinaryProbabilities[j] + cartesianProductProbability;
    // final int parent = grammarCscBinaryRowIndices[j];
    //
    // if (jointProbability > targetCellProbabilities[parent]) {
    // targetCellChildren[parent] = childPair;
    // targetCellProbabilities[parent] = jointProbability;
    // targetCellMidpoints[parent] = cartesianProductMidpoint;
    // }
    // }
    // }
    // }
}

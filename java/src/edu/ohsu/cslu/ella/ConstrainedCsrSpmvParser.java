package edu.ohsu.cslu.ella;

import java.util.Arrays;

import edu.ohsu.cslu.ella.ConstrainedChart.ConstrainedChartCell;
import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashFilterFunction;
import edu.ohsu.cslu.parser.ParseTree;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.spmv.SparseMatrixVectorParser;

/**
 * SpMV parser which constrains the chart population according to the contents of a chart populated using the grammar
 * unsplit parent grammar.
 * 
 * 
 * Implementation notes:
 * 
 * --The target chart need only contain 2C entries per cell, where C is the largest number of non-terminals populated in
 * the constraining chart. i.e., when doing Berkeley-style split-merge learning, we will always split each coarse
 * non-terminal into 2 fine non-terminals.
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
    private final SplitVocabulary splitVocabulary;

    public ConstrainedCsrSpmvParser(final ParserDriver opts, final CsrSparseMatrixGrammar grammar) {
        super(opts, grammar);
        this.splitVocabulary = (SplitVocabulary) grammar.nonTermSet;
    }

    public ParseTree findBestParse(final ConstrainedChart constrainingChart) {
        this.constrainingChart = constrainingChart;

        final int sentLength = constrainingChart.size();

        // Initialize the chart
        if (chart != null && chart.size() >= sentLength
                && chart.maxUnaryChainLength >= constrainingChart.maxUnaryChainLength) {
            chart.clear(constrainingChart);
        } else {
            // Don't set the chart's edge selector for the basic inside-probability version.
            chart = new ConstrainedChart(constrainingChart, grammar);
        }
        super.initSentence(constrainingChart.tokens);
        addLexicalProductions();

        cellSelector.initSentence(this);

        while (cellSelector.hasNext()) {
            final short[] startAndEnd = cellSelector.next();
            visitCell(startAndEnd[0], startAndEnd[1]);
        }

        if (collectDetailedStatistics) {
            final long t2 = System.currentTimeMillis();
            final ParseTree parseTree = chart.extractBestParse(grammar.startSymbol);
            extractTime = System.currentTimeMillis() - t2;
            return parseTree;
        }

        return chart.extractBestParse(grammar.startSymbol);
    }

    /**
     * Adds lexical productions from the constraining chart to the current chart
     */
    private void addLexicalProductions() {
        final int constrainingChartMaxSplits = splitVocabulary.maxSplits / 2;

        for (int start = 0; start < chart.size(); start++) {

            final int cellIndex = chart.cellIndex(start, start + 1);
            final int offset = chart.offset(cellIndex);
            chart.midpoints[cellIndex] = 0;

            int populatedParentEntries = 0;

            for (int i = 0; i < constrainingChartMaxSplits; i++) {

                int constrainingChartIndex = constrainingChart.cellOffsets[cellIndex] + i;

                // TODO Extract this into a private method
                // Find the lexical production in the constraining chart
                for (int j = 0; j < chart.maxUnaryChainLength - 1
                        && constrainingChart.nonTerminalIndices[constrainingChartIndex + constrainingChartMaxSplits] >= 0; j++) {
                    constrainingChartIndex += constrainingChartMaxSplits;
                }

                final short constrainingChartParent = constrainingChart.nonTerminalIndices[constrainingChartIndex];
                final int lexicalProduction = constrainingChart.sparseMatrixGrammar.cartesianProductFunction
                        .unpackLeftChild(constrainingChart.packedChildren[constrainingChartIndex]);

                // TODO Map lexical productions by both child and constraining parent, so we only have to iterate
                // through the 2 productions of interest. Alternatively, map by child and sort by parent, in which case
                // we can assume that each production matches the constraining parent and don't even have to compare.
                // Note - the second approach will fail if we prune unlikely productions from grammar storage.
                for (final Production lexProd : grammar.getLexicalProductionsWithChild(lexicalProduction)) {
                    final short unsplitParent = (short) ((lexProd.parent + 1) / 2);

                    if (unsplitParent == constrainingChartParent) {
                        final int entryIndex = offset + populatedParentEntries;
                        chart.nonTerminalIndices[entryIndex] = (short) lexProd.parent;
                        chart.packedChildren[entryIndex] = grammar.cartesianProductFunction
                                .packLexical(lexProd.leftChild);
                        chart.insideProbabilities[entryIndex] = lexProd.prob;
                        populatedParentEntries++;
                    }
                }

            }
        }
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
    @Override
    protected final CartesianProductVector cartesianProductUnion(final int start, final int end) {

        final short midpoint = ((ConstrainedCellSelector) cellSelector).currentCellMidpoint();

        final PerfectIntPairHashFilterFunction cpf = (PerfectIntPairHashFilterFunction) grammar
                .cartesianProductFunction();

        Arrays.fill(cartesianProductMidpoints, (short) 0);
        final short[] nonTerminalIndices = chart.nonTerminalIndices;
        final float[] insideProbabilities = chart.insideProbabilities;

        final int leftStart = chart.cellOffsets[chart.cellIndex(start, midpoint)];
        final int leftEnd = leftStart + splitVocabulary.maxSplits - 1;

        final int rightStart = chart.cellOffsets[chart.cellIndex(midpoint, end)];
        final int rightEnd = rightStart + splitVocabulary.maxSplits - 1;

        for (int i = leftStart; i <= leftEnd; i++) {
            final short leftChild = nonTerminalIndices[i];
            final float leftProbability = insideProbabilities[i];
            final int mask = cpf.mask(leftChild);
            final int shift = cpf.shift(leftChild);
            final int offset = cpf.offset(leftChild);

            for (int j = rightStart; j <= rightEnd; j++) {

                final int childPair = cpf.pack(nonTerminalIndices[j], shift, mask, offset);
                if (childPair == Integer.MIN_VALUE) {
                    continue;
                }

                final float jointProbability = leftProbability + insideProbabilities[j];
                cartesianProductProbabilities[childPair] = jointProbability;
                cartesianProductMidpoints[childPair] = midpoint;
            }
        }

        return new CartesianProductVector(grammar, cartesianProductProbabilities, cartesianProductMidpoints, (leftEnd
                - leftStart + 1)
                * (rightEnd - rightStart + 1));
    }

    @Override
    public void binarySpmv(final CartesianProductVector cartesianProductVector, final ChartCell chartCell) {

        final ConstrainedChartCell constrainedCell = (ConstrainedChartCell) chartCell;
        final ConstrainedCellSelector constrainedCellSelector = (ConstrainedCellSelector) cellSelector;

        final short[] unsplitEntries = constrainedCellSelector.constrainingChartNonTerminalIndices();
        // TODO Extract this into a private method
        // Find the lexical production in the constraining chart
        int constrainingChartIndex = constrainingChart.cellOffsets[constrainedCell.cellIndex];
        final int increment = splitVocabulary.maxSplits / 2;
        for (int i = 0; i < chart.maxUnaryChainLength - 1
                && constrainingChart.nonTerminalIndices[constrainingChartIndex + increment] >= 0; i++) {
            constrainingChartIndex += increment;
        }
        final int firstUnsplitParent = unsplitEntries[constrainingChartIndex];

        // Iterate over possible parents (matrix rows)
        final short startParent = (short) (firstUnsplitParent == 0 ? 0 : (firstUnsplitParent * 2 - 1));
        final short endParent = (short) (firstUnsplitParent == 0 ? 0 : (startParent + splitVocabulary.maxSplits - 1));
        for (short splitParent = startParent; splitParent <= endParent; splitParent++) {

            final int entryIndex = constrainedCell.offset() + splitParent - startParent;

            // TODO Use log-sum instead of viterbi
            float winningProbability = Float.NEGATIVE_INFINITY;
            int winningChildren = Integer.MIN_VALUE;

            // TODO Store start indices of left-children and only iterate over possible left children

            // Iterate over possible children of the parent (columns with non-zero entries)
            for (int j = grammar.csrBinaryRowIndices[splitParent]; j < grammar.csrBinaryRowIndices[splitParent + 1]; j++) {
                final int grammarChildren = grammar.csrBinaryColumnIndices[j];

                // final short leftChild = (short) grammar.cartesianProductFunction.unpackLeftChild(grammarChildren);
                // final String sLeftChild = grammar.nonTermSet.getSymbol(leftChild);
                // final short rightChild = grammar.cartesianProductFunction.unpackRightChild(grammarChildren);
                // final String sRightChild = grammar.nonTermSet.getSymbol(rightChild);

                if (cartesianProductVector.midpoints[grammarChildren] == 0) {
                    continue;
                }

                final float jointProbability = grammar.csrBinaryProbabilities[j]
                        + cartesianProductVector.probabilities[grammarChildren];

                if (jointProbability > winningProbability) {
                    winningProbability = jointProbability;
                    winningChildren = grammarChildren;
                }
            }

            if (winningProbability != Float.NEGATIVE_INFINITY) {
                chart.nonTerminalIndices[entryIndex] = splitParent;
                chart.packedChildren[entryIndex] = winningChildren;
                chart.insideProbabilities[entryIndex] = winningProbability;
            }
        }
        chart.midpoints[constrainedCell.cellIndex] = constrainedCellSelector.currentCellMidpoint();
    }

    @Override
    public void unarySpmv(final ChartCell chartCell) {
        final ConstrainedChartCell constrainedCell = (ConstrainedChartCell) chartCell;
        final ConstrainedCellSelector constrainedCellSelector = (ConstrainedCellSelector) cellSelector;

        final short[] unsplitEntries = constrainedCellSelector.constrainingChartNonTerminalIndices();
        final int constrainingCellOffset = constrainedCellSelector.constrainingCellOffset();
        final int constrainingGrammarMaxSplits = splitVocabulary.maxSplits / 2;

        final int constrainingCellUnaryDepth = constrainedCellSelector.currentCellUnaryChainDepth();

        for (int unaryDepth = 1; unaryDepth < constrainingCellUnaryDepth; unaryDepth++) {

            final short unsplitParent = unsplitEntries[constrainingCellOffset
                    + (constrainingCellUnaryDepth - 1 - unaryDepth) * constrainingGrammarMaxSplits];

            // Iterate over possible parents (matrix rows)
            final short startParent = (short) (unsplitParent == 0 ? 0 : (unsplitParent * 2 - 1));
            final short endParent = (short) (unsplitParent == 0 ? 0 : (startParent + splitVocabulary.maxSplits - 1));

            for (short splitParent = startParent; splitParent <= endParent; splitParent++) {

                final int entryIndex = constrainedCell.offset() + splitParent - startParent;
                float winningProbability = Float.NEGATIVE_INFINITY;
                short winningChild = Short.MIN_VALUE;

                // Iterate over possible children of the parent (columns with non-zero entries)
                for (int i = grammar.csrUnaryRowStartIndices[splitParent]; i < grammar.csrUnaryRowStartIndices[splitParent + 1]; i++) {

                    final short child = grammar.csrUnaryColumnIndices[i];
                    final float grammarProbability = grammar.csrUnaryProbabilities[i];

                    final float jointProbability = grammarProbability + chart.insideProbabilities[entryIndex];

                    if (jointProbability > winningProbability) {
                        winningProbability = jointProbability;
                        winningChild = child;
                    }
                }

                if (winningChild != Short.MIN_VALUE) {
                    // We found a unary parent; shift existing entries downward
                    chart.shiftCellEntriesDownward(entryIndex);

                    chart.nonTerminalIndices[entryIndex] = splitParent;
                    chart.packedChildren[entryIndex] = grammar.cartesianProductFunction.packUnary(winningChild);
                    chart.insideProbabilities[entryIndex] = winningProbability;
                }
            }
        }
    }
}

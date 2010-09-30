package edu.ohsu.cslu.parser.spmv;

import java.util.Arrays;

import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.CartesianProductFunction;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.BoundedPriorityQueue;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.PackedArrayChart;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;

/**
 * {@link SparseMatrixVectorParser} which uses a sparse grammar stored in CSC format (
 * {@link LeftCscSparseMatrixGrammar}) and implements cross-product and SpMV multiplication in Java.
 * 
 * @see CsrSpmvParser
 * @see OpenClSpmvParser
 * 
 *      TODO Share code copied from {@link CsrSpmvParser}
 * 
 * @author Aaron Dunlop
 * @since Feb 11, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class CscSpmvParser extends SparseMatrixVectorParser<LeftCscSparseMatrixGrammar, PackedArrayChart> {

    private final int beamWidth;
    private final int lexicalRowBeamWidth;
    private final int lexicalRowUnarySlots;

    protected int totalCartesianProductSize;
    protected long totalCartesianProductEntriesExamined;
    protected long totalValidCartesianProductEntries;
    protected long totalCellPopulation;
    protected long totalLeftChildPopulation;
    protected long totalRightChildPopulation;

    public CscSpmvParser(final ParserDriver opts, final LeftCscSparseMatrixGrammar grammar) {
        super(opts, grammar);
        beamWidth = opts.param1 > 0 ? (int) opts.param1 : grammar.numNonTerms();
        if (opts.param2 > 0) {
            lexicalRowBeamWidth = (int) opts.param2;
            lexicalRowUnarySlots = (int) (lexicalRowBeamWidth * (opts.param3 > 0 ? opts.param3 : 0.3f));
        } else {
            lexicalRowBeamWidth = grammar.numNonTerms();
            lexicalRowUnarySlots = 0;
        }
    }

    @Override
    protected void initParser(final int[] tokens) {
        final int sentLength = tokens.length;
        if (chart != null && chart.size() >= sentLength) {
            chart.clear(sentLength);
        } else {
            // Don't set the chart's edge selector for the basic inside-probability version.
            chart = new PackedArrayChart(tokens, grammar, beamWidth, lexicalRowBeamWidth);
        }

        if (collectDetailedStatistics) {
            totalCartesianProductSize = 0;
            totalCartesianProductEntriesExamined = 0;
            totalValidCartesianProductEntries = 0;
        }

        super.initParser(tokens);
    }

    @Override
    protected void visitCell(final short start, final short end) {

        final PackedArrayChartCell spvChartCell = chart.getCell(start, end);

        final long t0 = System.currentTimeMillis();
        long t1 = t0;
        long t2 = t0;

        // Skip binary grammar intersection for span-1 cells
        if (end - start > 1) {
            final CartesianProductVector cartesianProductVector = cartesianProductUnion(start, end);

            if (collectDetailedStatistics) {
                totalCartesianProductSize += cartesianProductVector.size();
                t1 = System.currentTimeMillis();
                totalCartesianProductTime += (t1 - t0);
            }

            // Multiply the unioned vector with the grammar matrix and populate the current cell with the
            // vector resulting from the matrix-vector multiplication
            binarySpmv(cartesianProductVector, spvChartCell);
        }

        if (collectDetailedStatistics) {
            t2 = System.currentTimeMillis();
            totalBinarySpMVTime += (t2 - t1);
        }

        if (beamWidth == grammar.numNonTerms()) {
            // Handle unary productions
            // TODO: This only goes through unary rules one time, so it can't create unary chains unless such
            // chains are encoded in the grammar. Iterating a few times would probably
            // work, although it's a big-time hack.
            unarySpmv(spvChartCell);

            if (collectDetailedStatistics) {
                totalUnaryTime += (System.currentTimeMillis() - t2);

                totalCellPopulation += spvChartCell.getNumNTs();
                totalLeftChildPopulation += spvChartCell.leftChildren();
                totalRightChildPopulation += spvChartCell.rightChildren();
            }

            // Pack the temporary cell storage into the main chart array
            if (collectDetailedStatistics) {
                final long t3 = System.currentTimeMillis();
                spvChartCell.finalizeCell();
                totalFinalizeTime += (System.currentTimeMillis() - t3);
            } else {
                spvChartCell.finalizeCell();
            }
        } else {
            /*
             * Populate the chart cell with the most probable n edges (n = beamWidth).
             * 
             * This operation depends on 3 data structures:
             * 
             * A) The temporary edge storage already populated with binary inside probabilities and (viterbi)
             * backpointers
             * 
             * B) A bounded priority queue of non-terminal indices, prioritized by their figure-of-merit scores
             * 
             * C) A parallel array of edges. We will pop a limited number of edges off the priority queue into this
             * array, so this storage represents the actual cell population.
             * 
             * First, we push all binary edges onto the priority queue (if we're pruning significantly, most will not
             * make the queue). We then begin popping edges off the queue. With each edge popped, we 1) Add the edge to
             * the array of cell edges (C); and 2) Iterate through unary grammar rules with the edge parent as a child,
             * inserting any resulting unary edges to the queue. This insertion replaces the existing queue entry for
             * the parent non-terminal, if greater, and updates the inside probability and backpointer in (A).
             */

            final long t3 = collectDetailedStatistics ? System.currentTimeMillis() : 0;

            // Push all binary or lexical edges onto a bounded priority queue
            final int cellBeamWidth = (end - start == 1 ? lexicalRowBeamWidth : beamWidth);
            final BoundedPriorityQueue q = new BoundedPriorityQueue(cellBeamWidth, grammar);

            final float[] tmpFoms = new float[grammar.numNonTerms()];
            Arrays.fill(tmpFoms, Float.NEGATIVE_INFINITY);

            if (end - start == 1) {
                for (short nt = 0; nt < grammar.numNonTerms(); nt++) {
                    if (spvChartCell.tmpInsideProbabilities[nt] != Float.NEGATIVE_INFINITY) {
                        final float fom = edgeSelector.calcLexicalFOM(start, end, nt,
                                spvChartCell.tmpInsideProbabilities[nt]);
                        q.insert(nt, fom);
                        tmpFoms[nt] = fom;
                    }
                }
                // Truncate the tail and reserve a few entries for unary productions
                q.setMaxSize(lexicalRowBeamWidth - lexicalRowUnarySlots);
                q.setMaxSize(lexicalRowBeamWidth);

            } else {
                for (short nt = 0; nt < grammar.numNonTerms(); nt++) {
                    if (spvChartCell.tmpInsideProbabilities[nt] != Float.NEGATIVE_INFINITY) {
                        final float fom = edgeSelector.calcFOM(start, end, nt, spvChartCell.tmpInsideProbabilities[nt]);
                        q.insert(nt, fom);
                        tmpFoms[nt] = fom;
                    }
                }
            }

            final int[] cellPackedChildren = new int[grammar.numNonTerms()];
            final float[] cellInsideProbabilities = new float[grammar.numNonTerms()];
            Arrays.fill(cellInsideProbabilities, Float.NEGATIVE_INFINITY);
            final float[] cellFoms = new float[grammar.numNonTerms()];
            Arrays.fill(cellFoms, Float.NEGATIVE_INFINITY);
            final short[] cellMidpoints = new short[grammar.numNonTerms()];

            // Pop edges off the queue until we fill the beam width. With each non-terminal popped off the queue, push
            // unary edges for each unary grammar rule with the non-terminal as a child
            for (int edgesPopulated = 0; edgesPopulated < cellBeamWidth && q.size() > 0;) {

                final int headIndex = q.headIndex();
                final short nt = q.parentIndices[headIndex];
                final float fom = q.foms[headIndex];
                q.popHead();

                if (cellFoms[nt] == Float.NEGATIVE_INFINITY) {
                    cellPackedChildren[nt] = spvChartCell.tmpPackedChildren[nt];
                    cellInsideProbabilities[nt] = spvChartCell.tmpInsideProbabilities[nt];
                    cellFoms[nt] = fom;
                    cellMidpoints[nt] = spvChartCell.tmpMidpoints[nt];

                    // Insert all unary edges with the current parent as child into the queue
                    final int child = nt;

                    // Iterate over possible parents of the child (rows with non-zero entries)
                    for (int i = grammar.cscUnaryColumnOffsets[child]; i < grammar.cscUnaryColumnOffsets[child + 1]; i++) {

                        final short parent = grammar.cscUnaryRowIndices[i];
                        final float jointProbability = grammar.cscUnaryProbabilities[i]
                                + spvChartCell.tmpInsideProbabilities[child];
                        final float parentFom = edgeSelector.calcFOM(start, end, parent, jointProbability);

                        if (parentFom > tmpFoms[parent] && parentFom > cellFoms[parent] && q.replace(parent, parentFom)) {
                            // The FOM was high enough that the edge was added to the queue; update temporary storage
                            // (A) to reflect the new unary child and probability
                            spvChartCell.tmpPackedChildren[parent] = grammar.cartesianProductFunction()
                                    .packUnary(child);
                            spvChartCell.tmpInsideProbabilities[parent] = jointProbability;
                        }
                    }

                    edgesPopulated++;

                } else if (fom > cellFoms[nt]) {
                    // We just re-popped a non-terminal we've already seen (meaning a unary which was added to the
                    // queue). Replace the existing edge with the new unary edge.
                    cellPackedChildren[nt] = spvChartCell.tmpPackedChildren[nt];
                    cellInsideProbabilities[nt] = spvChartCell.tmpInsideProbabilities[nt];
                    cellMidpoints[nt] = end;
                    cellFoms[nt] = fom;
                }
            }

            if (collectDetailedStatistics) {
                totalUnaryTime += (System.currentTimeMillis() - t3);
                final long t4 = System.currentTimeMillis();
                spvChartCell.finalizeCell(cellPackedChildren, cellInsideProbabilities, cellMidpoints);
                totalFinalizeTime += (System.currentTimeMillis() - t4);
            } else {
                spvChartCell.finalizeCell(cellPackedChildren, cellInsideProbabilities, cellMidpoints);
            }
        }
    }

    /**
     * Takes the cartesian-product of all potential child-cell combinations. Unions those cartesian-products together,
     * saving the maximum probability child combinations.
     * 
     * TODO Share with {@link CsrSpmvParser}
     * 
     * @param start
     * @param end
     * @return Unioned cartesian-product
     */
    @Override
    protected CartesianProductVector cartesianProductUnion(final int start, final int end) {

        Arrays.fill(cartesianProductMidpoints, (short) 0);
        int size = 0;

        final CartesianProductFunction cpf = grammar.cartesianProductFunction();
        final short[] nonTerminalIndices = chart.nonTerminalIndices;
        final float[] insideProbabilities = chart.insideProbabilities;

        // Iterate over all possible midpoints, unioning together the cross-product of discovered
        // non-terminals in each left/right child pair
        for (short midpoint = (short) (start + 1); midpoint <= end - 1; midpoint++) {
            final int leftCellIndex = chart.cellIndex(start, midpoint);
            final int rightCellIndex = chart.cellIndex(midpoint, end);

            final int leftStart = chart.minLeftChildIndex(leftCellIndex);
            final int leftEnd = chart.maxLeftChildIndex(leftCellIndex);

            final int rightStart = chart.minRightChildIndex(rightCellIndex);
            final int rightEnd = chart.maxRightChildIndex(rightCellIndex);

            for (int i = leftStart; i <= leftEnd; i++) {
                final short leftChild = nonTerminalIndices[i];
                final float leftProbability = insideProbabilities[i];

                for (int j = rightStart; j <= rightEnd; j++) {

                    if (collectDetailedStatistics) {
                        totalCartesianProductEntriesExamined++;
                    }

                    final int childPair = cpf.pack(leftChild, nonTerminalIndices[j]);
                    if (childPair == Integer.MIN_VALUE) {
                        continue;
                    }

                    final float jointProbability = leftProbability + insideProbabilities[j];

                    if (collectDetailedStatistics) {
                        totalValidCartesianProductEntries++;
                    }

                    // If this cartesian-product entry is not populated, we can populate it without comparing
                    // to a current probability.
                    if (cartesianProductMidpoints[childPair] == 0) {
                        cartesianProductProbabilities[childPair] = jointProbability;
                        cartesianProductMidpoints[childPair] = midpoint;

                        if (collectDetailedStatistics) {
                            size++;
                        }

                    } else {
                        if (jointProbability > cartesianProductProbabilities[childPair]) {
                            cartesianProductProbabilities[childPair] = jointProbability;
                            cartesianProductMidpoints[childPair] = midpoint;
                        }
                    }
                }
            }
        }

        return new CartesianProductVector(grammar, cartesianProductProbabilities, cartesianProductMidpoints, size);
    }

    @Override
    public void binarySpmv(final CartesianProductVector cartesianProductVector, final ChartCell chartCell) {

        final PackedArrayChartCell targetCell = (PackedArrayChartCell) chartCell;
        targetCell.allocateTemporaryStorage();

        binarySpmvMultiply(cartesianProductVector, targetCell.tmpPackedChildren, targetCell.tmpInsideProbabilities,
                targetCell.tmpMidpoints);
    }

    protected final void binarySpmvMultiply(final CartesianProductVector cartesianProductVector,
            final int[] targetCellChildren, final float[] targetCellProbabilities, final short[] targetCellMidpoints) {

        // Iterate over possible populated child pairs (matrix columns)
        for (int i = 0; i < grammar.cscBinaryPopulatedColumns.length; i++) {

            final int childPair = grammar.cscBinaryPopulatedColumns[i];
            final short cartesianProductMidpoint = cartesianProductVector.midpoints[childPair];

            // Skip grammar matrix columns for unpopulated cartesian-product entries
            if (cartesianProductMidpoint == 0) {
                continue;
            }
            final float cartesianProductProbability = cartesianProductVector.probabilities[childPair];

            // Iterate over possible parents of the child pair (rows with non-zero entries)
            for (int j = grammar.cscBinaryPopulatedColumnOffsets[i]; j < grammar.cscBinaryPopulatedColumnOffsets[i + 1]; j++) {

                final float jointProbability = grammar.cscBinaryProbabilities[j] + cartesianProductProbability;
                final int parent = grammar.cscBinaryRowIndices[j];

                if (jointProbability > targetCellProbabilities[parent]) {
                    targetCellChildren[parent] = childPair;
                    targetCellProbabilities[parent] = jointProbability;
                    targetCellMidpoints[parent] = cartesianProductMidpoint;
                }
            }
        }
    }

    @Override
    protected void unarySpmv(final int[] chartCellChildren, final float[] chartCellProbabilities,
            final short[] chartCellMidpoints, final int offset, final short chartCellEnd) {

        final CartesianProductFunction cpf = grammar.cartesianProductFunction();

        // Iterate over populated children (matrix columns)
        for (int child = 0; child < grammar.numNonTerms(); child++) {

            final int childOffset = offset + child;
            if (chartCellProbabilities[childOffset] == Float.NEGATIVE_INFINITY) {
                continue;
            }

            // Iterate over possible parents of the child (rows with non-zero entries)
            for (int i = grammar.cscUnaryColumnOffsets[child]; i < grammar.cscUnaryColumnOffsets[child + 1]; i++) {

                final short parent = grammar.cscUnaryRowIndices[i];
                final int parentOffset = offset + parent;
                final float grammarProbability = grammar.cscUnaryProbabilities[i];

                final float jointProbability = grammarProbability + chartCellProbabilities[childOffset];
                if (jointProbability > chartCellProbabilities[parentOffset]) {
                    chartCellProbabilities[parentOffset] = jointProbability;
                    chartCellChildren[parentOffset] = cpf.packUnary(child);
                    chartCellMidpoints[parentOffset] = chartCellEnd;
                }
            }
        }
    }

    @Override
    public String getStatHeader() {
        return super.getStatHeader()
                + ", Avg X-prod size, X-prod Entries Examined, Total X-prod Entries, Cells,   Total C, Total C_l, Total C_r";
    }

    @Override
    public String getStats() {
        return super.getStats()
                + String.format(", %15.1f, %23d, %20d, %6d, %10d, %10d, %10d", totalCartesianProductSize * 1.0f
                        / chart.cells, totalCartesianProductEntriesExamined, totalValidCartesianProductEntries,
                        chart.cells, totalCellPopulation, totalLeftChildPopulation, totalRightChildPopulation);
    }

}

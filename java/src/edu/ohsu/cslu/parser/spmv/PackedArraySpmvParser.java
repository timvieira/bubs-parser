package edu.ohsu.cslu.parser.spmv;

import java.util.Arrays;

import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashFilterFunction;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.PackedArrayChart;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;

/**
 * Base class for CSC and CSR SpMV parsers.
 * 
 * @author Aaron Dunlop
 * @since Dec 23, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public abstract class PackedArraySpmvParser<G extends SparseMatrixGrammar> extends
        SparseMatrixVectorParser<G, PackedArrayChart> {

    protected int totalCartesianProductSize;
    protected long totalCartesianProductEntriesExamined;
    protected long totalValidCartesianProductEntries;
    protected long totalCellPopulation;
    protected long totalLeftChildPopulation;
    protected long totalRightChildPopulation;

    public PackedArraySpmvParser(final ParserDriver opts, final G grammar) {
        super(opts, grammar);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected void initParser(final int[] tokens) {
        initParser(tokens, grammar.numNonTerms(), grammar.numNonTerms());
    }

    protected void initParser(final int[] tokens, final int beamWidth, final int lexicalRowBeamWidth) {
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

        // Handle unary productions
        // TODO: This only goes through unary rules one time, so it can't create unary chains unless such
        // chains are encoded in the grammar. Iterating a few times would probably
        // work, although it's a big-time hack.
        if (!cellSelector.factoredParentsOnly(start, end)) {
            unarySpmv(spvChartCell);
        }

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
    protected final CartesianProductVector cartesianProductUnion(final int start, final int end) {
    
        Arrays.fill(cartesianProductMidpoints, (short) 0);
        int size = 0;
    
        final PerfectIntPairHashFilterFunction cpf = (PerfectIntPairHashFilterFunction) grammar
                .cartesianProductFunction();
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
                final int mask = cpf.mask(leftChild);
                final int shift = cpf.shift(leftChild);
                final int offset = cpf.offset(leftChild);
    
                final short minRightSibling = grammar.minRightSiblingIndices[leftChild];
                final short maxRightSibling = grammar.maxRightSiblingIndices[leftChild];
    
                for (int j = rightStart; j <= rightEnd; j++) {
                    // Skip any right children which cannot combine with left child
                    if (nonTerminalIndices[j] < minRightSibling) {
                        continue;
                    } else if (nonTerminalIndices[j] > maxRightSibling) {
                        break;
                    }
    
                    if (collectDetailedStatistics) {
                        totalCartesianProductEntriesExamined++;
                    }
    
                    final int childPair = cpf.pack(nonTerminalIndices[j], shift, mask, offset);
                    if (childPair == Integer.MIN_VALUE) {
                        continue;
                    }
    
                    final float jointProbability = leftProbability + insideProbabilities[j];
    
                    if (collectDetailedStatistics) {
                        totalValidCartesianProductEntries++;
                    }
    
                    // If this cartesian-product entry is not populated, we can populate it without comparing
                    // to a current probability. The memory write is faster if we don't first have to read.
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

}

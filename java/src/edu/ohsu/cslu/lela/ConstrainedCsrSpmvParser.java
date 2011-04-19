/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
 * 
 * This file is part of the BUBS Parser.
 * 
 * The BUBS Parser is free software: you can redistribute it and/or 
 * modify  it under the terms of the GNU Affero General Public License 
 * as published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * The BUBS Parser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>
 */
package edu.ohsu.cslu.lela;

import java.util.Arrays;

import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashPackingFunction;
import edu.ohsu.cslu.lela.ConstrainedChart.ConstrainedChartCell;
import edu.ohsu.cslu.parser.ParseTree;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.spmv.SparseMatrixVectorParser;
import edu.ohsu.cslu.util.Math;

/**
 * SpMV parser which constrains the chart population according to the contents of a chart populated using the unsplit
 * (Markov Order 0) parent grammar.
 * 
 * 
 * Implementation notes:
 * 
 * --The target chart need only contain S entries per cell, where S is the largest number of splits of a single element
 * of the unsplit vocabulary V_0.
 * 
 * --The Cartesian-product should only be taken over the known child cells.
 * 
 * Note that it is <em>not</em>b further limited to only the splits of the constraining child in each cell? e.g., on the
 * second iteration, when child A has been split into A_1 and A_2, and then to A_1a, A_1b, A_2a, and A_2b, and child B
 * similarly to B_1a, B_1b, B_2a, and B_2b, we allow A_1a and A_1b to combine with B_2a and B_2b.
 * 
 * --We only need to maintain a single midpoint for each cell
 * 
 * --We do need to maintain space for a few unary productions; the first entry in each chart cell is for the top node in
 * the unary chain; any others (if populated) are unary children.
 * 
 * --Binary SpMV need only consider rules whose parent is in the set of known parent NTs. We iterate over those parent
 * rows in a CSR grammar.
 * 
 * @author Aaron Dunlop
 * @since Dec 23, 2010
 */
public class ConstrainedCsrSpmvParser extends
        SparseMatrixVectorParser<ConstrainedCsrSparseMatrixGrammar, ConstrainedChart> {

    ConstrainedChart constrainingChart;
    private final SplitVocabulary splitVocabulary;

    private final float[] cartesianProductProbabilities;
    private final short[] cartesianProductMidpoints;
    private final boolean collectDetailedTimings;

    protected long totalInitializationTime = 0;
    protected long totalLexProdTime = 0;
    protected long totalConstrainedXproductTime = 0;
    // protected long totalXproductFillTime = 0;
    protected long totalVisitTime = 0;
    protected long totalConstrainedBinaryTime = 0;
    protected long totalConstrainedUnaryTime = 0;
    protected long totalConstrainedOutsideTime = 0;
    protected long totalConstrainedOutsideXproductTime = 0;
    protected long totalConstrainedOutsideUnaryTime = 0;
    protected long totalRuleCountTime = 0;
    protected long totalExtractionTime = 0;

    public ConstrainedCsrSpmvParser(final ParserDriver opts, final ConstrainedCsrSparseMatrixGrammar grammar,
            final boolean collectDetailedTimings) {
        super(opts, grammar);
        this.splitVocabulary = (SplitVocabulary) grammar.nonTermSet;
        this.collectDetailedTimings = collectDetailedTimings;
        this.cartesianProductProbabilities = new float[grammar.packingFunction.packedArraySize()];
        this.cartesianProductMidpoints = new short[grammar.packingFunction.packedArraySize()];
    }

    public ConstrainedCsrSpmvParser(final ParserDriver opts, final ConstrainedCsrSparseMatrixGrammar grammar) {
        this(opts, grammar, false);
    }

    public ParseTree findBestParse(final ConstrainedChart unsplitConstrainingChart) {
        this.constrainingChart = unsplitConstrainingChart;

        final long t0 = System.nanoTime();

        // Initialize the chart
        if (chart != null
                && chart.nonTerminalIndices.length >= ConstrainedChart.chartArraySize(constrainingChart.size(),
                        constrainingChart.maxUnaryChainLength, splitVocabulary.maxSplits)
                && chart.cellOffsets.length >= constrainingChart.cellOffsets.length) {
            chart.clear(constrainingChart);
        } else {
            chart = new ConstrainedChart(constrainingChart, grammar);
        }
        super.initSentence(constrainingChart.tokens);
        cellSelector.initSentence(this);

        long t1 = 0;
        if (collectDetailedTimings) {
            t1 = System.nanoTime();
            totalInitializationTime += (t1 - t0);
        }
        addLexicalProductions();

        long t2 = 0;
        if (collectDetailedTimings) {
            t2 = System.nanoTime();
            totalLexProdTime += (t2 - t1);
        }

        while (cellSelector.hasNext()) {
            final short[] startAndEnd = cellSelector.next();
            visitCell(startAndEnd[0], startAndEnd[1]);
        }

        long t3 = 0;
        if (collectDetailedTimings) {
            t3 = System.nanoTime();
            totalVisitTime += (t3 - t2);
        }

        computeOutsideProbabilities();

        if (collectDetailedTimings) {
            final long t4 = System.nanoTime();
            final ParseTree parseTree = chart.extractBestParse(grammar.startSymbol);
            totalExtractionTime += (System.nanoTime() - t4);
            return parseTree;
        }

        return chart.extractBestParse(grammar.startSymbol);
    }

    /**
     * Adds lexical productions from the constraining chart to the current chart
     */
    private void addLexicalProductions() {

        for (int start = 0; start < chart.size(); start++) {

            final int cellIndex = chart.cellIndex(start, start + 1);

            final int constrainingCellOffset = constrainingChart.cellOffsets[cellIndex];

            // Find the lexical production in the constraining chart
            // TODO Use the unary chain length stores in the chart now
            int unaryChainLength = chart.maxUnaryChainLength - 1;
            while (unaryChainLength > 0
                    && constrainingChart.nonTerminalIndices[constrainingCellOffset + unaryChainLength] < 0) {
                unaryChainLength--;
            }

            // Beginning of cell + offset for populated unary parents
            // final int firstPosOffset = chart.offset(cellIndex) + unaryChainLength *
            // splitVocabulary.maxSplits;
            final int constrainingEntryIndex = constrainingChart.offset(cellIndex) + unaryChainLength;
            chart.midpoints[cellIndex] = 0;

            final int lexicalProduction = constrainingChart.sparseMatrixGrammar.packingFunction
                    .unpackLeftChild(constrainingChart.packedChildren[constrainingEntryIndex]);

            // TODO Map lexical productions by both child and unsplit (M-0) parent, so we only have to iterate
            // through the productions of interest.
            for (final Production lexProd : grammar.getLexicalProductionsWithChild(lexicalProduction)) {

                if (splitVocabulary.baseCategoryIndices[lexProd.parent] == constrainingChart.nonTerminalIndices[constrainingEntryIndex]) {
                    // Put the lexical entry in the top position, even if we'll move it in subsequent unary
                    // processing
                    final int entryIndex = chart.offset(cellIndex) + splitVocabulary.subcategoryIndices[lexProd.parent];

                    chart.nonTerminalIndices[entryIndex] = (short) lexProd.parent;
                    chart.packedChildren[entryIndex] = grammar.packingFunction.packLexical(lexProd.leftChild);
                    chart.insideProbabilities[entryIndex] = lexProd.prob;
                }
            }
        }
    }

    /**
     * Takes the cartesian-product of all potential child-cell combinations.
     * 
     * Note: in a constrained chart, we only have a single (known) midpoint to iterate over
     * 
     * @param start
     * @param end
     * @return Cartesian-product
     */
    @Override
    protected final CartesianProductVector cartesianProductUnion(final int start, final int end) {

        long t0 = 0;
        if (collectDetailedTimings) {
            t0 = System.nanoTime();
        }

        final short midpoint = ((ConstrainedCellSelector) cellSelector).currentCellMidpoint();

        final PerfectIntPairHashPackingFunction cpf = (PerfectIntPairHashPackingFunction) grammar
                .cartesianProductFunction();

        final short[] nonTerminalIndices = chart.nonTerminalIndices;
        final float[] insideProbabilities = chart.insideProbabilities;

        final int leftStart = chart.cellOffsets[chart.cellIndex(start, midpoint)];
        final short firstLeftChild = nonTerminalIndices[leftStart];
        final int leftEnd = leftStart + splitVocabulary.splitCount[firstLeftChild];

        final int rightStart = chart.cellOffsets[chart.cellIndex(midpoint, end)];
        final short firstRightChild = nonTerminalIndices[rightStart];
        final int rightEnd = rightStart + splitVocabulary.splitCount[firstRightChild];

        for (int i = leftStart; i < leftEnd; i++) {
            final short leftChild = nonTerminalIndices[i];
            final int fillStart = ((PerfectIntPairHashPackingFunction) grammar.packingFunction)
                    .leftChildStart(leftChild);
            final int fillEnd = ((PerfectIntPairHashPackingFunction) grammar.packingFunction)
                    .leftChildStart((short) (leftChild + 1));

            Arrays.fill(cartesianProductProbabilities, fillStart, fillEnd, Float.NEGATIVE_INFINITY);

            final float leftProbability = insideProbabilities[i];
            final int mask = cpf.mask(leftChild);
            final int shift = cpf.shift(leftChild);
            final int offset = cpf.offset(leftChild);

            for (int j = rightStart; j < rightEnd; j++) {

                final int childPair = cpf.pack(nonTerminalIndices[j], shift, mask, offset);
                if (childPair == Integer.MIN_VALUE) {
                    continue;
                }

                final float jointProbability = leftProbability + insideProbabilities[j];
                cartesianProductProbabilities[childPair] = jointProbability;
                // cartesianProductMidpoints[childPair] = midpoint;
            }
        }

        final CartesianProductVector v = new CartesianProductVector(grammar, cartesianProductProbabilities,
                cartesianProductMidpoints, (leftEnd - leftStart + 1) * (rightEnd - rightStart + 1));

        if (collectDetailedTimings) {
            totalConstrainedXproductTime += System.nanoTime() - t0;
        }

        return v;
    }

    @Override
    public void binarySpmv(final CartesianProductVector cartesianProductVector, final ChartCell chartCell) {

        long t0 = 0;
        if (collectDetailedTimings) {
            t0 = System.nanoTime();
        }

        final ConstrainedChartCell constrainedCell = (ConstrainedChartCell) chartCell;
        final ConstrainedCellSelector constrainedCellSelector = (ConstrainedCellSelector) cellSelector;

        final short[] constrainingChartEntries = constrainedCellSelector.constrainingChartNonTerminalIndices();

        // Find the bottom production in the constraining chart cell
        // TODO Store unary chain length for each cell?
        int constrainingEntryIndex = constrainingChart.cellOffsets[constrainedCell.cellIndex];
        for (int i = 0; i < chart.maxUnaryChainLength - 1
                && constrainingChart.nonTerminalIndices[constrainingEntryIndex + 1] >= 0; i++) {
            constrainingEntryIndex++;
        }
        final short constrainingParent = constrainingChartEntries[constrainingEntryIndex];
        final short constrainingLeftChild = constrainingChartEntries[constrainedCellSelector
                .constrainingLeftChildCellOffset()];

        // Iterate over possible parents (matrix rows)
        final short startParent = splitVocabulary.firstSubcategoryIndices[constrainingParent];

        for (short parent = startParent; parent < startParent + splitVocabulary.splitCount[startParent]; parent++) {

            final int entryIndex = constrainedCell.offset() + parent - startParent;
            chart.nonTerminalIndices[entryIndex] = parent;

            // Iterate over split left children matching the constraining left child
            for (int j = grammar.csrBinaryBaseStartIndices[parent][constrainingLeftChild]; j < grammar.csrBinaryBaseStartIndices[parent][constrainingLeftChild + 1]; j++) {

                final int grammarChildren = grammar.csrBinaryColumnIndices[j];

                if (cartesianProductVector.probabilities[grammarChildren] == Float.NEGATIVE_INFINITY) {
                    continue;
                }

                final float jointProbability = grammar.csrBinaryProbabilities[j]
                        + cartesianProductVector.probabilities[grammarChildren];

                chart.insideProbabilities[entryIndex] = edu.ohsu.cslu.util.Math.logSum(jointProbability,
                        chart.insideProbabilities[entryIndex]);
            }
        }
        chart.midpoints[constrainedCell.cellIndex] = constrainedCellSelector.currentCellMidpoint();

        if (collectDetailedTimings) {
            totalConstrainedBinaryTime += System.nanoTime() - t0;
        }
    }

    @Override
    public void unarySpmv(final ChartCell chartCell) {
        long t0 = 0;
        if (collectDetailedTimings) {
            t0 = System.nanoTime();
        }

        final ConstrainedChartCell constrainedCell = (ConstrainedChartCell) chartCell;
        final ConstrainedCellSelector constrainedCellSelector = (ConstrainedCellSelector) cellSelector;

        final short[] constrainingChartEntries = constrainedCellSelector.constrainingChartNonTerminalIndices();
        final int constrainingCellOffset = constrainedCellSelector.constrainingCellOffset();

        final int constrainingCellUnaryDepth = constrainedCellSelector.currentCellUnaryChainDepth();

        // foreach unary chain depth (starting from 2nd from bottom in chart storage; bottom is binary or
        // lexical
        // parent)
        // - Each unsplit parent has a known unsplit child
        // - All split children are populated (although some may have 0 probability)
        // - foreach split parent
        // - Iterate through grammar looking for winning split child

        // Find the same number of unaries as in the constraining cell
        // foreach unary chain depth (starting from 2nd from bottom in chain; bottom is binary parent)
        for (int unaryDepth = 1; unaryDepth < constrainingCellUnaryDepth; unaryDepth++) {

            // Unsplit child and unsplit parent are fixed
            final short constrainingParent = constrainingChartEntries[constrainingCellOffset
                    + (constrainingCellUnaryDepth - 1 - unaryDepth)];
            final short constrainingChild = constrainingChartEntries[constrainingCellOffset
                    + (constrainingCellUnaryDepth - unaryDepth)];

            final short startParent = splitVocabulary.firstSubcategoryIndices[constrainingParent];
            final short endParent = (short) (startParent + splitVocabulary.splitCount[startParent] - 1);

            for (int i = 0; i < splitVocabulary.maxSplits; i++) {
                // Shift all existing entries downward
                chart.shiftCellEntriesDownward(constrainedCell.offset() + i);
            }

            final int childOffset = constrainedCell.offset() + (unaryDepth * splitVocabulary.maxSplits);

            // foreach split parent
            for (short splitParent = startParent; splitParent <= endParent; splitParent++) {

                final int parentSubcategoryIndex = splitVocabulary.subcategoryIndices[splitParent];
                final int parentEntryIndex = constrainedCell.offset() + parentSubcategoryIndex;

                float probability = Float.NEGATIVE_INFINITY;

                // Iterate over possible children of the parent (columns with non-zero entries)
                // Iterate through grammar looking for winning split of unsplit child
                for (int j = grammar.csrUnaryBaseStartIndices[splitParent][constrainingChild]; j < grammar.csrUnaryBaseStartIndices[splitParent][constrainingChild + 1]; j++) {

                    final int childEntryIndex = childOffset
                            + splitVocabulary.subcategoryIndices[grammar.csrUnaryColumnIndices[j]];

                    if (chart.insideProbabilities[childEntryIndex] == Float.NEGATIVE_INFINITY) {
                        continue;
                    }

                    probability = edu.ohsu.cslu.util.Math.logSum(grammar.csrUnaryProbabilities[j]
                            + chart.insideProbabilities[childEntryIndex], probability);
                }

                chart.nonTerminalIndices[parentEntryIndex] = splitParent;
                chart.insideProbabilities[parentEntryIndex] = probability;
            }
        }

        if (collectDetailedTimings) {
            totalConstrainedUnaryTime += System.nanoTime() - t0;
        }
    }

    /**
     * Takes the cartesian-product of all potential child-cell combinations.
     * 
     * Note: in a constrained chart, we only have a single (known) midpoint to iterate over
     * 
     * @param parentStart
     * @param parentEnd
     * @param childStart
     * @param childEnd
     * @return Cartesian-product
     */
    protected final CartesianProductVector outsideCartesianProductVector(final int parentStart, final int parentEnd,
            final int childStart, final int childEnd, final PerfectIntPairHashPackingFunction packingFunction) {

        long t0 = 0;
        if (collectDetailedTimings) {
            t0 = System.nanoTime();
        }

        final short[] nonTerminalIndices = chart.nonTerminalIndices;
        final float[] outsideProbabilities = chart.outsideProbabilities;
        final float[] insideProbabilities = chart.insideProbabilities;

        final int parentOffset = chart.cellOffsets[chart.cellIndex(parentStart, parentEnd)];
        final int parentStartIndex = parentOffset + (chart.unaryChainDepth(parentOffset) - 1)
                * splitVocabulary.maxSplits;
        final int parentEndIndex = parentStartIndex + splitVocabulary.splitCount[nonTerminalIndices[parentStartIndex]];

        final int childOffset = chart.cellOffsets[chart.cellIndex(childStart, childEnd)];
        final int childStartIndex = childOffset;
        final int childEndIndex = childStartIndex + splitVocabulary.splitCount[nonTerminalIndices[childStartIndex]];

        for (int i = parentStartIndex; i < parentEndIndex; i++) {

            final short parent = nonTerminalIndices[i];
            Arrays.fill(cartesianProductProbabilities, packingFunction.leftChildStart(parent),
                    packingFunction.leftChildStart((short) (parent + 1)), Float.NEGATIVE_INFINITY);

            final float parentOutsideProbability = outsideProbabilities[i];
            final int mask = packingFunction.mask(parent);
            final int shift = packingFunction.shift(parent);
            final int offset = packingFunction.offset(parent);

            for (int j = childStartIndex; j < childEndIndex; j++) {

                final int childPair = packingFunction.pack(nonTerminalIndices[j], shift, mask, offset);
                if (childPair == Integer.MIN_VALUE) {
                    continue;
                }

                // Parent outside x child inside
                cartesianProductProbabilities[childPair] = parentOutsideProbability + insideProbabilities[j];
                if (cartesianProductProbabilities[childPair] == Float.POSITIVE_INFINITY) {
                    System.out.println("Infinity");
                }
            }
        }

        final CartesianProductVector v = new CartesianProductVector(grammar, cartesianProductProbabilities,
                cartesianProductMidpoints, (parentEndIndex - parentStartIndex) * (childEndIndex - childStartIndex));

        if (collectDetailedTimings) {
            totalConstrainedOutsideXproductTime += System.nanoTime() - t0;
        }

        return v;
    }

    /**
     * Computes and populates outside probabilities
     */
    private void computeOutsideProbabilities() {
        long t0 = 0;
        if (collectDetailedTimings) {
            t0 = System.nanoTime();
        }

        final int cellIndex = chart.cellIndex(0, chart.size());
        int offset = chart.offset(cellIndex);

        // Outside probability of the start symbol is 1
        // Arrays.fill(chart.outsideProbabilities, offset, offset + splitVocabulary.maxSplits, 0);
        chart.outsideProbabilities[offset] = 0;

        for (int unaryDepth = 1; unaryDepth < chart.unaryChainDepth(offset); unaryDepth++) {
            offset += splitVocabulary.maxSplits;
            computeUnaryOutsideProbabilities(offset);
        }

        // Strangely, there are 1-word sentences in the WSJ training data
        if (chart.size() > 1) {
            // Recursively compute and populate outside probabilities of binary children
            computeOutsideProbabilities((short) 0, chart.midpoints[cellIndex], (short) 0, (short) chart.size(),
                    BranchDirection.LEFT);
            computeOutsideProbabilities(chart.midpoints[cellIndex], (short) chart.size(), (short) 0,
                    (short) chart.size(), BranchDirection.RIGHT);
        }

        if (collectDetailedTimings) {
            totalConstrainedOutsideTime += System.nanoTime() - t0;
        }
    }

    /**
     * TODO Document
     * 
     * To compute the outside probability of a non-terminal in a cell, we need the outside probability of the cell's
     * parent, so we process recursively from the top of the chart.
     * 
     * @param start
     * @param end
     * @param parentStart
     * @param parentEnd
     * @param branchDirection
     */
    private void computeOutsideProbabilities(final short start, final short end, final short parentStart,
            final short parentEnd, final BranchDirection branchDirection) {

        final int cellIndex = chart.cellIndex(start, end);
        int offset = chart.offset(cellIndex);

        final int constrainingParentCellOffset = constrainingChart.offset(constrainingChart.cellIndex(parentStart,
                parentEnd));
        final short constrainingParent = constrainingChart.nonTerminalIndices[constrainingParentCellOffset
                + constrainingChart.unaryChainDepth(constrainingParentCellOffset) - 1];

        // Top level (generally a binary child)

        if (branchDirection == BranchDirection.LEFT) {

            final CartesianProductVector cartesianProductVector = outsideCartesianProductVector(parentStart, parentEnd,
                    end, parentEnd, grammar.leftChildPackingFunction);

            final short childStartSplit = chart.nonTerminalIndices[offset];

            // foreach child in target (left) cell
            for (short child = childStartSplit; child < childStartSplit + splitVocabulary.splitCount[childStartSplit]; child++) {

                final int childIndex = offset + splitVocabulary.subcategoryIndices[child];

                // Iterate over grammar rules and update outside probability of child
                for (int j = grammar.leftChildCsrBaseStartIndices[child][constrainingParent]; j < grammar.leftChildCsrBaseStartIndices[child][constrainingParent + 1]; j++) {

                    final int grammarChildren = grammar.leftChildCsrBinaryColumnIndices[j];

                    if (cartesianProductVector.probabilities[grammarChildren] == Float.NEGATIVE_INFINITY) {
                        continue;
                    }

                    // Outside probability = sum(production probability x parent outside x sibling inside)
                    // X-product contains parent outside x sibling inside
                    final float jointProbability = grammar.leftChildCsrBinaryProbabilities[j]
                            + cartesianProductVector.probabilities[grammarChildren];
                    chart.outsideProbabilities[childIndex] = Math.logSum(jointProbability,
                            chart.outsideProbabilities[childIndex]);
                }
            }

        } else {

            // Right branch
            final CartesianProductVector cartesianProductVector = outsideCartesianProductVector(parentStart, parentEnd,
                    parentStart, start, grammar.rightChildPackingFunction);

            final short childStartSplit = chart.nonTerminalIndices[offset];

            // foreach child in target (right) cell
            for (short child = childStartSplit; child < childStartSplit + splitVocabulary.splitCount[childStartSplit]; child++) {

                final int childEntryIndex = offset + splitVocabulary.subcategoryIndices[child];

                // Iterate over grammar rules and update outside probability of child
                for (int j = grammar.rightChildCsrBaseStartIndices[child][constrainingParent]; j < grammar.rightChildCsrBaseStartIndices[child][constrainingParent + 1]; j++) {

                    final int grammarChildren = grammar.rightChildCsrBinaryColumnIndices[j];

                    if (cartesianProductVector.probabilities[grammarChildren] == Float.NEGATIVE_INFINITY) {
                        continue;
                    }

                    // Outside probability = sum(production probability x parent outside x sibling inside)
                    // X-product contains parent outside x sibling inside
                    final float jointProbability = grammar.rightChildCsrBinaryProbabilities[j]
                            + cartesianProductVector.probabilities[grammarChildren];
                    chart.outsideProbabilities[childEntryIndex] = Math.logSum(jointProbability,
                            chart.outsideProbabilities[childEntryIndex]);
                }
            }
        }

        // Compute unary outside probabilities at each unary child level
        final int unaryChainDepth = chart.unaryChainDepth(offset);
        for (int unaryDepth = 1; unaryDepth < unaryChainDepth; unaryDepth++) {
            offset += splitVocabulary.maxSplits;
            computeUnaryOutsideProbabilities(offset);
        }

        // If the entry we just computed is a lexical entry, we're done
        if (end - start == 1) {
            return;
        }

        // Recursively compute outside probability of binary children
        final short midpoint = chart.midpoints[cellIndex];

        computeOutsideProbabilities(start, midpoint, start, end, BranchDirection.LEFT); // Left child
        computeOutsideProbabilities(midpoint, end, start, end, BranchDirection.RIGHT); // Right child
    }

    private void computeUnaryOutsideProbabilities(final int offset) {

        long t0 = 0;
        if (collectDetailedTimings) {
            t0 = System.nanoTime();
        }

        final short parentStartSplit = chart.nonTerminalIndices[offset - splitVocabulary.maxSplits];
        final short parentEndSplit = (short) (parentStartSplit + splitVocabulary.splitCount[parentStartSplit]);
        final short childStartSplit = chart.nonTerminalIndices[offset];
        final short childEndSplit = (short) (childStartSplit + splitVocabulary.splitCount[childStartSplit]);

        // foreach split parent
        final int parentStartIndex = offset - splitVocabulary.maxSplits;

        for (int parentIndex = parentStartIndex; parentIndex < parentStartIndex + (parentEndSplit - parentStartSplit); parentIndex++) {

            final float parentOutsideProbability = chart.outsideProbabilities[parentIndex];

            // Iterate over grammar rows headed by the parent and compute unary outside probability
            for (int j = grammar.csrUnaryRowStartIndices[parentStartSplit]; j < grammar.csrUnaryRowStartIndices[parentEndSplit]; j++) {

                final short splitChild = grammar.csrUnaryColumnIndices[j];

                // Skip grammar rules which don't match the populated children
                if (splitChild < childStartSplit || splitChild >= childEndSplit) {
                    continue;
                }

                final int entryIndex = offset + splitVocabulary.subcategoryIndices[splitChild];

                // Outside probability = sum(production probability x parent outside)
                chart.outsideProbabilities[entryIndex] = Math.logSum(grammar.csrUnaryProbabilities[j]
                        + parentOutsideProbability, chart.outsideProbabilities[entryIndex]);
            }
        }

        if (collectDetailedTimings) {
            totalConstrainedOutsideUnaryTime += System.nanoTime() - t0;
        }
    }

    ConstrainedCountGrammar countRuleOccurrences() {
        final ConstrainedCountGrammar countGrammar = new ConstrainedCountGrammar(grammar);
        countRuleOccurrences(countGrammar);
        return countGrammar;
    }

    public void countRuleOccurrences(final ConstrainedCountGrammar countGrammar) {
        long t0 = 0;
        if (collectDetailedTimings) {
            t0 = System.nanoTime();
        }

        cellSelector.reset();
        while (cellSelector.hasNext()) {
            final short[] startAndEnd = cellSelector.next();
            countUnaryRuleOccurrences(countGrammar, startAndEnd[0], startAndEnd[1]);
            if (startAndEnd[1] - startAndEnd[0] == 1) {
                countLexicalRuleOccurrences(countGrammar, startAndEnd[0], startAndEnd[1]);
            } else {
                countBinaryRuleOccurrences(countGrammar, startAndEnd[0], startAndEnd[1]);
            }
        }

        if (collectDetailedTimings) {
            totalRuleCountTime += System.nanoTime() - t0;
        }
    }

    private void countBinaryRuleOccurrences(final ConstrainedCountGrammar countGrammar, final short start,
            final short end) {

        final CartesianProductVector cartesianProductVector = cartesianProductUnion(start, end);
        final int cellIndex = chart.cellIndex(start, end);
        final int cellOffset = chart.offset(cellIndex);
        final int offset = cellOffset + (chart.unaryChainDepth(cellOffset) - 1) * splitVocabulary.maxSplits;

        final ConstrainedCellSelector constrainedCellSelector = (ConstrainedCellSelector) cellSelector;

        final short[] constrainingChartEntries = constrainedCellSelector.constrainingChartNonTerminalIndices();

        // Find the bottom production in the constraining chart cell
        // TODO Store unary chain length for each cell?
        int constrainingEntryIndex = constrainingChart.offset(cellIndex);
        for (int i = 0; i < chart.maxUnaryChainLength - 1
                && constrainingChart.nonTerminalIndices[constrainingEntryIndex + 1] >= 0; i++) {
            constrainingEntryIndex++;
        }
        final short constrainingParent = constrainingChartEntries[constrainingEntryIndex];
        final short constrainingLeftChild = constrainingChartEntries[constrainedCellSelector
                .constrainingLeftChildCellOffset()];

        // Iterate over possible parents (matrix rows)
        final short startParent = splitVocabulary.firstSubcategoryIndices[constrainingParent];
        final short endParent = (short) (startParent + splitVocabulary.splitCount[startParent]);

        for (short parent = startParent; parent < endParent; parent++) {

            final int parentIndex = offset + parent - startParent;
            final float parentOutside = chart.outsideProbabilities[parentIndex];

            // Iterate over split left children matching the constraining left child
            for (int j = grammar.csrBinaryBaseStartIndices[parent][constrainingLeftChild]; j < grammar.csrBinaryBaseStartIndices[parent][constrainingLeftChild + 1]; j++) {

                final int grammarChildren = grammar.csrBinaryColumnIndices[j];

                if (cartesianProductVector.probabilities[grammarChildren] == Float.NEGATIVE_INFINITY) {
                    continue;
                }

                // Parent outside x left child inside x right child inside x production probability
                final float jointProbability = parentOutside + cartesianProductVector.probabilities[grammarChildren]
                        + grammar.csrBinaryProbabilities[j];

                // final short leftChild = (short) grammar.packingFunction.unpackLeftChild(grammarChildren);
                // final short rightChild = grammar.packingFunction.unpackRightChild(grammarChildren);

                countGrammar.incrementBinaryLogCount(parent, grammarChildren, jointProbability);
            }
        }
    }

    private void countUnaryRuleOccurrences(final ConstrainedCountGrammar countGrammar, final short start,
            final short end) {

        // System.out.println("=== " + start + "," + end + " ===");
        final int cellIndex = chart.cellIndex(start, end);
        final int offset = chart.offset(cellIndex);
        final int constrainingCellUnaryDepth = ((ConstrainedCellSelector) cellSelector).currentCellUnaryChainDepth();

        // foreach unary chain depth (starting from 2nd from bottom in chart storage; bottom is binary or
        // lexical parent)
        for (int childUnaryDepth = 1; childUnaryDepth < constrainingCellUnaryDepth; childUnaryDepth++) {

            final int parentStartIndex = offset + (childUnaryDepth - 1) * splitVocabulary.maxSplits;
            final short parentStartSplit = chart.nonTerminalIndices[parentStartIndex];
            final int parentEndIndex = parentStartIndex + splitVocabulary.splitCount[parentStartSplit];
            final short parentEndSplit = (short) (parentStartSplit + splitVocabulary.splitCount[parentStartSplit]);

            final short childStartSplit = chart.nonTerminalIndices[offset + childUnaryDepth * splitVocabulary.maxSplits];
            final short childEndSplit = (short) (childStartSplit + splitVocabulary.splitCount[childStartSplit]);

            // foreach parent
            for (int parentIndex = parentStartIndex; parentIndex < parentEndIndex; parentIndex++) {
                final short parent = chart.nonTerminalIndices[parentIndex];
                final float parentOutside = chart.outsideProbabilities[parentIndex];

                // Iterate over grammar rows headed by the parent and compute unary outside probability
                for (int j = grammar.csrUnaryRowStartIndices[parentStartSplit]; j < grammar.csrUnaryRowStartIndices[parentEndSplit]; j++) {

                    // Skip grammar rules which don't match the populated children
                    final short child = grammar.csrUnaryColumnIndices[j];
                    if (child < childStartSplit || child >= childEndSplit) {
                        continue;
                    }

                    // Parent outside x child inside x production probability
                    final float jointProbability = parentOutside
                            + chart.insideProbabilities[offset + childUnaryDepth * splitVocabulary.maxSplits
                                    + splitVocabulary.subcategoryIndices[child]] + grammar.csrUnaryProbabilities[j];
                    // System.out.format("%s -> %s %s\n", splitVocabulary.getSymbol(parent),
                    // splitVocabulary.getSymbol(child), Assert.fraction(jointProbability));
                    countGrammar.incrementUnaryLogCount(parent, child, jointProbability);
                }
            }
        }
    }

    private void countLexicalRuleOccurrences(final ConstrainedCountGrammar countGrammar, final short start,
            final short end) {

        // System.out.println("=== " + start + "," + end + " ===");

        final int cellIndex = chart.cellIndex(start, end);
        final int cellOffset = chart.offset(cellIndex);

        final int constrainingCellOffset = constrainingChart.cellOffsets[cellIndex];
        final int constrainingNonTerminal = constrainingChart.nonTerminalIndices[constrainingCellOffset
                + constrainingChart.unaryChainDepth(constrainingCellOffset) - 1];

        final int offset = cellOffset + (chart.unaryChainDepth(cellOffset) - 1) * splitVocabulary.maxSplits;
        final int lexicalChild = grammar.packingFunction.unpackLeftChild(chart.packedChildren[offset]);

        // TODO Map lexical productions by both child and unsplit (M-0) parent, so we only have to iterate
        // through the productions of interest.
        for (final Production lexProd : grammar.getLexicalProductionsWithChild(lexicalChild)) {

            if (splitVocabulary.baseCategoryIndices[lexProd.parent] == constrainingNonTerminal) {

                // Parent outside x production probability (child inside = 1 for lexical entries)
                final float jointProbability = chart.outsideProbabilities[offset
                        + splitVocabulary.subcategoryIndices[lexProd.parent]]
                        + lexProd.prob;

                // System.out.format("%s -> %s %s\n", splitVocabulary.getSymbol(lexProd.parent),
                // grammar.lexSet.getSymbol(lexicalChild), Assert.fraction(jointProbability));
                countGrammar.incrementLexicalLogCount((short) lexProd.parent, lexicalChild, jointProbability);
            }
        }
    }

    private enum BranchDirection {
        LEFT, RIGHT;
    }
}

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

package edu.ohsu.cslu.parser.ml;

import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.lela.ConstrainingChart;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.PackedArrayChart;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.TemporaryChartCell;

/**
 * Produces a parse tree constrained by the gold input tree. The resulting parse will be identical to the gold input
 * tree, but split categories will be populated. E.g., NP_12 might be populated in place of NP.
 * 
 * Implementation notes:
 * 
 * This parser cannot recover unary self-chains (e.g. NP -> NP -> ...), because {@link PackedArrayChart} can only store
 * a single instance of a non-terminal in a cell. Such self-chains are fairly rare in the corpora of interest and will
 * <em>not</em> be returned from a constrained parse. If desired, they can be pruned through a pre-processing step.
 * 
 * This implementation is quite simple and does not optimize efficiency, but constrained parsing should be quite fast
 * even so, and we can revisit efficiency if needed.
 * 
 * @author Aaron Dunlop
 */
public class ConstrainedCphSpmlParser extends SparseMatrixLoopParser<LeftCscSparseMatrixGrammar, PackedArrayChart>
        implements ConstrainedChartParser {

    private ConstrainingChart constrainingChart;

    final LeftCscSparseMatrixGrammar baseGrammar;

    final Short2ObjectOpenHashMap<ShortList> baseToSplitMap = new Short2ObjectOpenHashMap<ShortList>();

    /**
     * @param opts
     * @param grammar
     */
    public ConstrainedCphSpmlParser(final ParserDriver opts, final LeftCscSparseMatrixGrammar grammar) {
        super(opts, grammar);
        this.beamWidth = lexicalRowBeamWidth = grammar.numNonTerms();
        baseGrammar = (LeftCscSparseMatrixGrammar) grammar.toUnsplitGrammar();

        // Map from all base NTs to their splits
        for (short nt = 0; nt < grammar.nonTermSet.size(); nt++) {
            final short baseNt = grammar.nonTermSet.getBaseIndex(nt);
            ShortList list = baseToSplitMap.get(baseNt);
            if (list == null) {
                list = new ShortArrayList();
                baseToSplitMap.put(baseNt, list);
            }
            list.add(nt);
        }
    }

    @Override
    protected void initSentence(final ParseTask parseTask) {
        super.initSentence(parseTask);
        constrainingChart = new ConstrainingChart(parseTask.inputTree.binarize(grammar.grammarFormat,
                grammar.binarization()), baseGrammar);
    }

    @Override
    protected void addLexicalProductions(final ChartCell cell) {
        // Add lexical productions to the a base cells of the chart, constrained by the productions in the constraining
        // chart

        final short constrainingParent = bottomConstrainingParent(cell.start(), cell.end());
        boolean foundMatchingProd = false;

        for (final Production lexProd : grammar.getLexicalProductionsWithChild(chart.parseTask.tokens[cell.start()])) {
            if (grammar.nonTermSet.getBaseIndex((short) lexProd.parent) == constrainingParent) {
                cell.updateInside(lexProd, cell, null, lexProd.prob);
                foundMatchingProd = true;
            }
        }

        // We need to ensure that one or more splits of the constraining unary parent are populated in the target
        // cell. If not, we must choose one or more such splits; without a principled way to choose between those
        // splits, we populate _all_ splits matching the constraining edge, assign probability 1 to the sum of those
        // splits, and divide the probability mass between them. This operation is in effect hallucinating
        // additional grammar rules. The proper children for those rules is also unknown, so we simply choose the
        // first populated child in the child cells.
        if (!foundMatchingProd) {

            final ShortList splits = baseToSplitMap.get(constrainingParent);
            final float probability = (float) Math.log(1.0 / splits.size());

            for (final short parent : splits) {
                cell.updateInside(new Production(parent, chart.parseTask.tokens[cell.start()], probability, true,
                        grammar), cell, null, probability);
            }
        }
    }

    @Override
    protected void computeInsideProbabilities(final ChartCell cell) {

        final long t0 = collectDetailedStatistics ? System.nanoTime() : 0;

        final PackingFunction packingFunction = grammar.packingFunction();

        final PackedArrayChartCell targetCell = (PackedArrayChartCell) cell;
        final short start = cell.start();
        final short end = cell.end();
        targetCell.allocateTemporaryStorage();
        final TemporaryChartCell tmpCell = targetCell.tmpCell;

        final PackedArrayChartCell constrainingCell = constrainingChart.getCell(start, end);
        final int constrainingCellIndex = constrainingChart.cellIndex(start, end);
        final int constrainingEntryIndex = constrainingChart.offset(constrainingCellIndex)
                + constrainingChart.unaryChainLength[constrainingCellIndex] - 1;
        final short constrainingParent = bottomConstrainingParent(start, end);

        final short constrainingLeftChild = (short) baseGrammar.packingFunction
                .unpackLeftChild(constrainingChart.packedChildren[constrainingEntryIndex]);
        final short constrainingRightChild = baseGrammar.packingFunction
                .unpackRightChild(constrainingChart.packedChildren[constrainingEntryIndex]);

        if (end - start > 1) {
            final int[] binaryColumnOffsets = grammar.cscBinaryColumnOffsets;
            final float[] binaryProbabilities = grammar.cscBinaryProbabilities;
            final short[] binaryRowIndices = grammar.cscBinaryRowIndices;

            final short midpoint = constrainingChart.midpoints[constrainingCellIndex];
            final int leftCellIndex = chart.cellIndex(start, midpoint);
            final int rightCellIndex = chart.cellIndex(midpoint, end);

            // Iterate over children in the left child cell
            final int leftStart = chart.minLeftChildIndex(leftCellIndex);
            final int leftEnd = chart.maxLeftChildIndex(leftCellIndex);

            final int rightStart = chart.minRightChildIndex(rightCellIndex);
            final int rightEnd = chart.maxRightChildIndex(rightCellIndex);

            boolean foundMatchingProd = false;

            for (int i = leftStart; i <= leftEnd; i++) {
                final short leftChild = chart.nonTerminalIndices[i];
                final short baseLeftChild = grammar.nonTermSet.getBaseIndex(leftChild);
                final float leftProbability = chart.insideProbabilities[i];

                // And over children in the right child cell
                for (int j = rightStart; j <= rightEnd; j++) {
                    final short rightChild = chart.nonTerminalIndices[j];
                    final short baseRightChild = grammar.nonTermSet.getBaseIndex(rightChild);
                    final int column = packingFunction.pack(leftChild, rightChild);

                    if (column == Integer.MIN_VALUE) {
                        continue;
                    }

                    final float childProbability = leftProbability + chart.insideProbabilities[j];

                    for (int k = binaryColumnOffsets[column]; k < binaryColumnOffsets[column + 1]; k++) {

                        final float jointProbability = binaryProbabilities[k] + childProbability;
                        final short parent = binaryRowIndices[k];
                        final short baseParent = grammar.nonTermSet.getBaseIndex(parent);

                        // Skip this edge if it doesn't match the constraining edge
                        if (baseParent != constrainingParent || baseLeftChild != constrainingLeftChild
                                || baseRightChild != constrainingRightChild) {
                            continue;
                        }

                        if (jointProbability > tmpCell.insideProbabilities[parent]) {
                            tmpCell.packedChildren[parent] = column;
                            tmpCell.insideProbabilities[parent] = jointProbability;
                            tmpCell.midpoints[parent] = midpoint;
                            foundMatchingProd = true;
                        }
                    }
                }
            }

            // We need to ensure that one or more splits of the constraining binary parent are populated in the target
            // cell. If not, we must choose one or more such splits; without a principled way to choose between those
            // splits, we populate _all_ splits matching the constraining edge, assign probability 1 to the sum of those
            // splits, and divide the probability mass between them. This operation is in effect hallucinating
            // additional grammar rules. The proper children for those rules is also unknown, so we simply choose the
            // first populated child in the child cells.
            if (!foundMatchingProd) {
                final short leftChild = chart.nonTerminalIndices[chart.offset(chart.cellIndex(start, midpoint))];
                final short rightChild = chart.nonTerminalIndices[chart.offset(chart.cellIndex(midpoint, end))];
                final ShortList splits = baseToSplitMap.get(constrainingParent);
                final float probability = (float) Math.log(1.0 / splits.size());

                for (final short nt : splits) {
                    tmpCell.insideProbabilities[nt] = probability;
                    tmpCell.packedChildren[nt] = packingFunction.pack(leftChild, rightChild);
                    tmpCell.midpoints[nt] = midpoint;
                }
            }

            if (collectDetailedStatistics) {
                chart.parseTask.insideBinaryNs += System.nanoTime() - t0;
            }
        }

        // Apply unary rules
        unaryAndPruning(targetCell, constrainingCell, start, end);

        targetCell.finalizeCell();
    }

    /**
     * Multiplies the unary grammar matrix (stored sparsely) by the contents of this cell (stored densely), and
     * populates this chart cell. Used to populate unary rules.
     * 
     * @param targetCell
     */
    protected void unaryAndPruning(final PackedArrayChartCell targetCell, final PackedArrayChartCell constrainingCell,
            final short start, final short end) {

        targetCell.allocateTemporaryStorage();
        final TemporaryChartCell tmpCell = targetCell.tmpCell;

        final PackingFunction packingFunction = grammar.packingFunction();
        final int constrainingCellIndex = constrainingChart.cellIndex(start, end);

        for (int unaryHeight = 1; unaryHeight < constrainingChart.unaryChainLength(constrainingCellIndex); unaryHeight++) {
            final int constrainingEntryIndex = constrainingChart.offset(constrainingCellIndex)
                    + constrainingChart.unaryChainLength[constrainingCellIndex] - unaryHeight - 1;

            final short constrainingParent = constrainingChart.nonTerminalIndices[constrainingEntryIndex];
            final short constrainingChild = (short) baseGrammar.packingFunction
                    .unpackLeftChild(constrainingChart.packedChildren[constrainingEntryIndex]);

            boolean foundMatchingProd = false;

            // Iterate over populated children (matrix columns)
            for (short child = 0; child < grammar.numNonTerms(); child++) {

                if (tmpCell.insideProbabilities[child] == Float.NEGATIVE_INFINITY) {
                    continue;
                }
                final short baseChild = grammar.nonTermSet.getBaseIndex(child);

                // Iterate over possible parents of the child (rows with non-zero entries)
                for (int i = grammar.cscUnaryColumnOffsets[child]; i < grammar.cscUnaryColumnOffsets[child + 1]; i++) {

                    final short parent = grammar.cscUnaryRowIndices[i];
                    final float grammarProbability = grammar.cscUnaryProbabilities[i];
                    final short baseParent = grammar.nonTermSet.getBaseIndex(parent);

                    // Skip this edge if it doesn't match the constraining edge
                    if (baseParent != constrainingParent || baseChild != constrainingChild) {
                        continue;
                    }

                    final float jointProbability = grammarProbability + tmpCell.insideProbabilities[child];
                    // Our chart structure can't handle unary self-loops, so don't reinsert a parent that is already a
                    // unary for this cell
                    if (jointProbability > tmpCell.insideProbabilities[parent]
                            && (foundMatchingProd || tmpCell.midpoints[parent] != end)) {
                        tmpCell.insideProbabilities[parent] = jointProbability;
                        tmpCell.packedChildren[parent] = packingFunction.packUnary(child);
                        tmpCell.midpoints[parent] = end;
                        foundMatchingProd = true;
                    }
                }
            }

            // We want to ensure that one or more splits of the constraining unary parent are populated in the target
            // cell. If the required productions aren't present in the grammar, we'll hallucinate them. Except for the
            // self-loop case (below) with only a single split of the non-terminal.
            if (!foundMatchingProd) {

                final ShortList allSplits = baseToSplitMap.get(constrainingParent);
                if (allSplits.size() == 1) {
                    // There isn't much we can do in this case - we only have one split of the non-terminal, and we
                    // can't populate it twice in the unary chain. We'll have to give up and drop entries from the
                    // chain.
                    break;
                }

                // We need to ensure that one or more splits of the constraining unary parent are populated in the
                // target cell. If not, we must choose one or more such splits; we do so in one of 2 ways
                //
                // 1) If all splits of the constraining parent are observed (lower in the unary chain), we remove the
                // least-probable of those splits from its position in the chain and place it at the current position.
                //
                // 2) If one or more splits of the constraining parent are not already present, (lacking a principled
                // way to choose between those splits), we populate _all_ of them, assign probability 1 to the sum of
                // those splits, and divide the probability mass between them. This operation is in effect hallucinating
                // additional grammar rules. The proper children for those rules is also unknown, so we simply choose
                // the first populated child in the child cells.

                // Find the most probable populated entry matching the constraining child. We'll use that as the child
                // of the (hallucinated) rule
                short maxChild = -1;
                final float maxChildInsideProbability = Float.NEGATIVE_INFINITY;

                for (short nt = 0; nt < tmpCell.insideProbabilities.length; nt++) {

                    if (tmpCell.insideProbabilities[nt] > maxChildInsideProbability
                            && grammar.nonTermSet.getBaseIndex(nt) == constrainingChild) {
                        maxChild = nt;
                    }
                }

                final ShortList observedParentSplits = new ShortArrayList();
                final float minParentInsideProbability = Float.POSITIVE_INFINITY;
                short minParentSplit = -1;
                for (short nt = 0; nt < tmpCell.insideProbabilities.length; nt++) {

                    if (tmpCell.insideProbabilities[nt] > Float.NEGATIVE_INFINITY
                            && grammar.nonTermSet.getBaseIndex(nt) == constrainingParent) {
                        observedParentSplits.add(nt);

                        if (tmpCell.insideProbabilities[nt] < minParentInsideProbability) {
                            minParentSplit = nt;
                        }
                    }
                }

                ShortList chosenParentSplits = null;
                float parentProbability = Float.NEGATIVE_INFINITY;

                if (observedParentSplits.size() == allSplits.size()) {
                    // All splits of the constraining parent are already populated. Choose the least-probable, and use
                    // it as the parent
                    chosenParentSplits = new ShortArrayList(new short[] { minParentSplit });
                    parentProbability = maxChildInsideProbability;

                } else {
                    // Divide up the probability mass between the unobserved parent splits
                    chosenParentSplits = new ShortArrayList();
                    for (final short parent : allSplits) {
                        if (!observedParentSplits.contains(parent)) {
                            chosenParentSplits.add(parent);
                        }
                    }
                    parentProbability = maxChildInsideProbability
                            + (float) Math.log(1.0 / (allSplits.size() - observedParentSplits.size()));
                }

                for (final short parent : chosenParentSplits) {
                    tmpCell.insideProbabilities[parent] = parentProbability;
                    tmpCell.packedChildren[parent] = packingFunction.packUnary(maxChild);
                    tmpCell.midpoints[parent] = end;
                }
            }
        }
    }

    private short bottomConstrainingParent(final short start, final short end) {

        final int constrainingCellIndex = constrainingChart.cellIndex(start, end);
        final int constrainingEntryIndex = constrainingChart.offset(constrainingCellIndex)
                + constrainingChart.unaryChainLength[constrainingCellIndex] - 1;
        return constrainingChart.nonTerminalIndices[constrainingEntryIndex];
    }

    @Override
    protected boolean implicitPruning() {
        return true;
    }

    @Override
    public ConstrainingChart constrainingChart() {
        return constrainingChart;
    }
}

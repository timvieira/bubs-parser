/*
 * Copyright 2010-2012 Aaron Dunlop and Nathan Bodenstab
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

import java.util.Arrays;
import java.util.Iterator;

import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.grammar.InsideOutsideCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.BoundedPriorityQueue;
import edu.ohsu.cslu.parser.chart.PackedArrayChart;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;
import edu.ohsu.cslu.util.Math;

/**
 * Base class for parsers implementing inside-sum or inside-outside inference rather than simple Viterbi decoding. With
 * a chart containing inside or inside-outside (posterior) probabilities, we have a choice of decoding methods (see
 * {@link DecodeMethod} and {@link PackedArrayChart}).
 * 
 * @author Aaron Dunlop
 */
public abstract class BaseIoCphSpmlParser extends
        SparseMatrixLoopParser<InsideOutsideCscSparseMatrixGrammar, PackedArrayChart> {

    public BaseIoCphSpmlParser(final ParserDriver opts, final InsideOutsideCscSparseMatrixGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    public BinaryTree<String> findBestParse(final ParseTask parseTask) {
        initChart(parseTask);
        insidePass();

        // Outside pass
        final long t0 = collectDetailedStatistics ? System.currentTimeMillis() : 0;

        final Iterator<short[]> reverseIterator = cellSelector.reverseIterator();

        while (reverseIterator.hasNext()) {
            final short[] startAndEnd = reverseIterator.next();
            final PackedArrayChartCell cell = chart.getCell(startAndEnd[0], startAndEnd[1]);
            computeOutsideProbabilities(cell);
        }
        if (collectDetailedStatistics) {
            parseTask.outsidePassMs = System.currentTimeMillis() - t0;
        }

        if (collectDetailedStatistics) {
            final long t3 = System.currentTimeMillis();
            final BinaryTree<String> parseTree = chart.decode();
            parseTask.extractTimeMs = System.currentTimeMillis() - t3;
            return parseTree;
        }

        return chart.decode();
    }

    protected abstract void computeSiblingOutsideProbabilities(PackedArrayChartCell cell, final PackingFunction pf,
            final float[] cscBinaryProbabilities, final short[] cscBinaryRowIndices, final int[] cscColumnOffsets,
            final int parentStartIndex, final int parentEndIndex, final int siblingStartIndex, final int siblingEndIndex);

    @Override
    protected final void unaryAndPruning(final PackedArrayChartCell spvChartCell, final short start, final short end) {

        final long t0 = collectDetailedStatistics ? System.nanoTime() : 0;

        // For the moment, at least, we ignore factored-only cell constraints in span-1 cells
        final boolean factoredOnly = cellSelector.hasCellConstraints()
                && cellSelector.getCellConstraints().isCellOnlyFactored(start, end) && (end - start > 1);
        final boolean allowUnaries = !cellSelector.hasCellConstraints()
                || cellSelector.getCellConstraints().isUnaryOpen(start, end);
        final float minInsideProbability = edu.ohsu.cslu.util.Math.floatMax(spvChartCell.tmpCell.insideProbabilities)
                - maxLocalDelta;

        // We will push all binary or lexical edges onto a bounded priority queue, and then (if unaries are allowed),
        // add those edges as well.
        final int cellBeamWidth = (end - start == 1 ? lexicalRowBeamWidth : java.lang.Math.min(
                cellSelector.getBeamWidth(start, end), beamWidth));
        final BoundedPriorityQueue q = threadLocalBoundedPriorityQueue.get();
        q.clear(cellBeamWidth);

        final float[] maxInsideProbabilities = new float[grammar.numNonTerms()];
        System.arraycopy(spvChartCell.tmpCell.insideProbabilities, 0, maxInsideProbabilities, 0,
                maxInsideProbabilities.length);

        // If unaries are allowed in this cell, compute unary probabilities for all possible parents
        if (!factoredOnly && allowUnaries) {
            final float[] unaryInsideProbabilities = new float[grammar.numNonTerms()];
            Arrays.fill(unaryInsideProbabilities, Float.NEGATIVE_INFINITY);
            final float[] viterbiUnaryInsideProbabilities = new float[grammar.numNonTerms()];
            Arrays.fill(viterbiUnaryInsideProbabilities, Float.NEGATIVE_INFINITY);
            final int[] viterbiUnaryPackedChildren = new int[grammar.numNonTerms()];

            for (short child = 0; child < grammar.numNonTerms(); child++) {
                final float insideProbability = spvChartCell.tmpCell.insideProbabilities[child];
                if (insideProbability == Float.NEGATIVE_INFINITY) {
                    continue;
                }

                // Iterate over possible parents of the child (rows with non-zero entries)
                for (int i = grammar.cscUnaryColumnOffsets[child]; i < grammar.cscUnaryColumnOffsets[child + 1]; i++) {

                    final float unaryProbability = grammar.cscUnaryProbabilities[i] + insideProbability;
                    final short parent = grammar.cscUnaryRowIndices[i];

                    unaryInsideProbabilities[parent] = Math.logSum(unaryInsideProbabilities[parent], unaryProbability);

                    if (unaryProbability > viterbiUnaryInsideProbabilities[parent]) {
                        viterbiUnaryInsideProbabilities[parent] = unaryProbability;
                        viterbiUnaryPackedChildren[parent] = grammar.packingFunction.packUnary(child);
                    }
                }
            }

            // Retain the greater of the binary and unary inside probabilities and the appropriate backpointer (biasing
            // toward recovering unaries in the case of a tie)
            for (short nt = 0; nt < grammar.numNonTerms(); nt++) {
                if (unaryInsideProbabilities[nt] != Float.NEGATIVE_INFINITY
                        && unaryInsideProbabilities[nt] >= maxInsideProbabilities[nt]) {
                    maxInsideProbabilities[nt] = unaryInsideProbabilities[nt];
                    spvChartCell.tmpCell.packedChildren[nt] = viterbiUnaryPackedChildren[nt];
                }
            }
        }

        // Push all observed edges (binary, unary, or lexical) onto a bounded priority queue
        if (end - start == 1) { // Lexical Row (span = 1)

            // Limit the queue to the number of non-unary productions allowed
            q.setMaxSize(lexicalRowBeamWidth - lexicalRowUnaries);

            for (short nt = 0; nt < grammar.numNonTerms(); nt++) {
                if (maxInsideProbabilities[nt] > minInsideProbability) {
                    final float fom = fomModel.calcLexicalFOM(start, end, nt, maxInsideProbabilities[nt]);
                    q.insert(nt, fom);
                }
            }
            // Now that all lexical productions are on the queue, expand it a bit to allow space for unary productions
            q.setMaxSize(lexicalRowBeamWidth);

        } else { // Span >= 2
            for (short nt = 0; nt < grammar.numNonTerms(); nt++) {
                if (maxInsideProbabilities[nt] > minInsideProbability) {
                    final float fom = fomModel.calcFOM(start, end, nt, maxInsideProbabilities[nt]);
                    q.insert(nt, fom);
                }
            }
        }

        Arrays.fill(spvChartCell.tmpCell.insideProbabilities, Float.NEGATIVE_INFINITY);

        // Pop n edges off the queue into the temporary cell storage.
        for (final int edgesPopulated = 0; edgesPopulated < cellBeamWidth && q.size() > 0;) {

            final int headIndex = q.headIndex();
            final short nt = q.nts[headIndex];
            spvChartCell.tmpCell.insideProbabilities[nt] = maxInsideProbabilities[nt];
            q.popHead();
        }

        if (collectDetailedStatistics) {
            chart.parseTask.unaryAndPruningNs += System.nanoTime() - t0;
        }
    }

    /**
     * We retain only 1-best unary probabilities, and only if the probability of a unary child exceeds the sum of all
     * probabilities for that non-terminal as a binary child of parent cells)
     */
    protected final void computeUnaryOutsideProbabilities(final float[] tmpOutsideProbabilities) {

        // Iterate over populated parents (matrix rows)
        for (short parent = 0; parent < grammar.numNonTerms(); parent++) {

            if (tmpOutsideProbabilities[parent] == Float.NEGATIVE_INFINITY) {
                continue;
            }

            // Iterate over possible children (columns with non-zero entries)
            for (int i = grammar.csrUnaryRowStartIndices[parent]; i < grammar.csrUnaryRowStartIndices[parent + 1]; i++) {

                final short child = grammar.csrUnaryColumnIndices[i];
                final float jointProbability = grammar.csrUnaryProbabilities[i] + tmpOutsideProbabilities[parent];
                if (jointProbability > tmpOutsideProbabilities[child]) {
                    tmpOutsideProbabilities[child] = jointProbability;
                }
            }
        }
    }

    /**
     * To compute the outside probability of a non-terminal in a cell, we need the outside probability of the cell's
     * parent, so we process downward from the top of the chart.
     * 
     * @param start
     * @param end
     */
    protected final void computeOutsideProbabilities(final PackedArrayChartCell targetCell) {

        final long t0 = collectDetailedStatistics ? System.currentTimeMillis() : 0;

        final short start = targetCell.start();
        final short end = targetCell.end();

        // Allocate temporary storage and populate start-symbol probability in the top cell
        targetCell.allocateTemporaryStorage(true);
        if (start == 0 && end == chart.size()) {
            targetCell.tmpCell.outsideProbabilities[grammar.startSymbol] = 0;
        }

        // Left-side siblings first

        // foreach parent-start in {0..start - 1}
        for (int parentStart = 0; parentStart < start; parentStart++) {
            final int parentCellIndex = chart.cellIndex(parentStart, end);
            final int parentStartIndex = chart.offset(parentCellIndex);
            final int parentEndIndex = parentStartIndex + chart.numNonTerminals[parentCellIndex] - 1;

            // Sibling (left) cell
            final int siblingCellIndex = chart.cellIndex(parentStart, start);
            final int siblingStartIndex = chart.minLeftChildIndex(siblingCellIndex);
            final int siblingEndIndex = chart.maxLeftChildIndex(siblingCellIndex);

            computeSiblingOutsideProbabilities(targetCell, grammar.rightChildPackingFunction,
                    grammar.rightChildCscBinaryProbabilities, grammar.rightChildCscBinaryRowIndices,
                    grammar.rightChildCscBinaryColumnOffsets, parentStartIndex, parentEndIndex, siblingStartIndex,
                    siblingEndIndex);
        }

        // Right-side siblings

        // foreach parent-end in {end + 1..n}
        for (int parentEnd = end + 1; parentEnd <= chart.size(); parentEnd++) {
            final int parentCellIndex = chart.cellIndex(start, parentEnd);
            final int parentStartIndex = chart.offset(parentCellIndex);
            final int parentEndIndex = parentStartIndex + chart.numNonTerminals[parentCellIndex] - 1;

            // Sibling (right) cell
            final int siblingCellIndex = chart.cellIndex(end, parentEnd);
            final int siblingStartIndex = chart.minRightChildIndex(siblingCellIndex);
            final int siblingEndIndex = chart.maxRightChildIndex(siblingCellIndex);

            computeSiblingOutsideProbabilities(targetCell, grammar.leftChildPackingFunction,
                    grammar.leftChildCscBinaryProbabilities, grammar.leftChildCscBinaryRowIndices,
                    grammar.leftChildCscBinaryColumnOffsets, parentStartIndex, parentEndIndex, siblingStartIndex,
                    siblingEndIndex);
        }

        // Unary outside probabilities
        computeUnaryOutsideProbabilities(targetCell.tmpCell.outsideProbabilities);
        targetCell.finalizeCell();

        if (collectDetailedStatistics) {
            chart.parseTask.outsidePassMs += System.currentTimeMillis() - t0;
        }
    }

}

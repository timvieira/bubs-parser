package edu.ohsu.cslu.parser.ml;

import java.util.Arrays;
import java.util.Iterator;

import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.grammar.InsideOutsideCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.InsideOutsideChart;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;
import edu.ohsu.cslu.util.Math;

public class InsideOutsideCphSpmlParser extends
        SparseMatrixLoopParser<InsideOutsideCscSparseMatrixGrammar, InsideOutsideChart> {

    public InsideOutsideCphSpmlParser(final ParserDriver opts, final InsideOutsideCscSparseMatrixGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    public BinaryTree<String> findBestParse(final int[] tokens) {
        init(tokens);
        insidePass();

        // Outside pass
        final long t0 = collectDetailedStatistics ? System.currentTimeMillis() : 0;

        final Iterator<short[]> reverseIterator = cellSelector.reverseIterator();

        // Populate start-symbol unaries in the top cell. Assume that the top cell will be first; this might not be
        // exactly be the reverse of the original iteration order (e.g., for an agenda parser), but there isn't another
        // sensible order in which to compute outside probabilities.
        final float[] tmpOutsideProbabilities = new float[grammar.numNonTerms()];
        Arrays.fill(tmpOutsideProbabilities, Float.NEGATIVE_INFINITY);
        tmpOutsideProbabilities[grammar.startSymbol] = 0;

        while (reverseIterator.hasNext()) {
            final short[] startAndEnd = reverseIterator.next();
            computeOutsideProbabilities(startAndEnd[0], startAndEnd[1], tmpOutsideProbabilities);
            Arrays.fill(tmpOutsideProbabilities, Float.NEGATIVE_INFINITY);
        }
        if (collectDetailedStatistics) {
            parseTask.outsidePassMs = System.currentTimeMillis() - t0;
        }

        if (collectDetailedStatistics) {
            final long t3 = System.currentTimeMillis();
            chart.decode(opts.decodeMethod);
            final BinaryTree<String> parseTree = chart.extract(0, chart.size(), opts.decodeMethod);
            parseTask.extractTimeMs = System.currentTimeMillis() - t3;
            return parseTree;
        }

        chart.decode(opts.decodeMethod);
        return chart.extract(0, chart.size(), opts.decodeMethod);
    }

    /**
     * Identical to {@link CartesianProductHashSpmlParser}, but computes sum instead of viterbi max.
     */
    @Override
    protected void computeInsideProbabilities(final short start, final short end) {

        final PackingFunction cpf = grammar.cartesianProductFunction();
        final PackedArrayChartCell targetCell = chart.getCell(start, end);
        targetCell.allocateTemporaryStorage();

        final int[] targetCellChildren = targetCell.tmpPackedChildren;
        final float[] targetCellProbabilities = targetCell.tmpInsideProbabilities;
        final short[] targetCellMidpoints = targetCell.tmpMidpoints;

        final float[] maxInsideProbabilities = new float[targetCellProbabilities.length];
        Arrays.fill(maxInsideProbabilities, Float.NEGATIVE_INFINITY);

        // Iterate over all possible midpoints
        for (short midpoint = (short) (start + 1); midpoint <= end - 1; midpoint++) {
            final int leftCellIndex = chart.cellIndex(start, midpoint);
            final int rightCellIndex = chart.cellIndex(midpoint, end);

            // Iterate over children in the left child cell
            final int leftStart = chart.minLeftChildIndex(leftCellIndex);
            final int leftEnd = chart.maxLeftChildIndex(leftCellIndex);

            final int rightStart = chart.minRightChildIndex(rightCellIndex);
            final int rightEnd = chart.maxRightChildIndex(rightCellIndex);

            for (int i = leftStart; i <= leftEnd; i++) {
                final short leftChild = chart.nonTerminalIndices[i];
                final float leftProbability = chart.insideProbabilities[i];

                // And over children in the right child cell
                for (int j = rightStart; j <= rightEnd; j++) {
                    final int column = cpf.pack(leftChild, chart.nonTerminalIndices[j]);
                    if (column == Integer.MIN_VALUE) {
                        continue;
                    }

                    final float childProbability = leftProbability + chart.insideProbabilities[j];

                    for (int k = grammar.cscBinaryColumnOffsets[column]; k < grammar.cscBinaryColumnOffsets[column + 1]; k++) {

                        final float jointProbability = grammar.cscBinaryProbabilities[k] + childProbability;
                        final int parent = grammar.cscBinaryRowIndices[k];
                        targetCellProbabilities[parent] = Math
                                .logSum(targetCellProbabilities[parent], jointProbability);

                        // Keep track of viterbi best-path backpointers, even though we don't really have to.
                        if (jointProbability > maxInsideProbabilities[parent]) {
                            targetCellChildren[parent] = column;
                            maxInsideProbabilities[parent] = jointProbability;
                            targetCellMidpoints[parent] = midpoint;
                        }
                    }
                }
            }
        }

        // Apply unary rules (retaining only 1-best probabilities for unary parents, and only if that probability is
        // greater than the sum of all probabilities for that non-terminal as a binary parent)
        if (exhaustiveSearch) {
            unarySpmv(targetCell);
            targetCell.finalizeCell();
        } else {
            final int[] cellPackedChildren = new int[grammar.numNonTerms()];
            final float[] cellInsideProbabilities = new float[grammar.numNonTerms()];
            final short[] cellMidpoints = new short[grammar.numNonTerms()];
            unaryAndPruning(targetCell, start, end, cellPackedChildren, cellInsideProbabilities, cellMidpoints);

            targetCell.finalizeCell(cellPackedChildren, cellInsideProbabilities, cellMidpoints);
        }
        // targetCell.allocateTemporaryStorage();
        // unaryAndPruning(targetCell, start, end, targetCell.tmpPackedChildren, targetCell.tmpInsideProbabilities,
        // targetCell.tmpMidpoints);
        // targetCell.finalizeCell();
    }

    // @Override
    // protected void unaryAndPruning(final PackedArrayChartCell spvChartCell, final short start, final short end,
    // final int[] cellPackedChildren, final float[] cellInsideProbabilities, final short[] cellMidpoints) {
    //
    // final long t0 = collectDetailedStatistics ? System.currentTimeMillis() : 0;
    //
    // // For the moment, at least, we ignore factored-only cell constraints in span-1 cells
    // final boolean factoredOnly = cellSelector.hasCellConstraints()
    // && cellSelector.getCellConstraints().isCellOnlyFactored(start, end) && (end - start > 1);
    // final boolean allowUnaries = !cellSelector.hasCellConstraints()
    // || cellSelector.getCellConstraints().isUnaryOpen(start, end);
    //
    // // Prune by FOM
    // pruneByFom(spvChartCell, start, end);
    //
    // // Store a copy of the inside probabilities computed for binary parents
    // final float[] binaryParentInsideProbabilities = new float[grammar.numNonTerms()];
    // System.arraycopy(spvChartCell.tmpInsideProbabilities, 0, binaryParentInsideProbabilities, 0,
    // binaryParentInsideProbabilities.length);
    //
    // // Maintain the maximum single-path probabilities for each non-terminal when acting as a unary parent. If (and
    // // only if) the maximum unary probability exceeds the sum of all computed binary probabilities, will we mark the
    // // non-terminal as a unary parent.
    // final float[] unaryParentMaxProbabilities = new float[grammar.numNonTerms()];
    // Arrays.fill(unaryParentMaxProbabilities, Float.NEGATIVE_INFINITY);
    //
    // // Process unary edges for cells which are open to non-factored parents
    // if (!factoredOnly && allowUnaries) {
    // for (short child = 0; child < spvChartCell.tmpInsideProbabilities.length; child++) {
    // final float insideProbability = binaryParentInsideProbabilities[child];
    // final String sChild = grammar.nonTermSet.getSymbol(child);
    // if (insideProbability > Float.NEGATIVE_INFINITY) {
    //
    // // Iterate over possible parents of the child (rows with non-zero entries)
    // for (int i = grammar.cscUnaryColumnOffsets[child]; i < grammar.cscUnaryColumnOffsets[child + 1]; i++) {
    //
    // final short parent = grammar.cscUnaryRowIndices[i];
    // final String sParent = grammar.nonTermSet.getSymbol(parent);
    // final float jointProbability = grammar.cscUnaryProbabilities[i] + insideProbability;
    //
    // // If the unary probability exceeds the sum of all computed binary probabilities for the parent
    // // and also the highest observed unary probability of the child, record the child as a unary
    // // child of the parent
    // if (jointProbability > binaryParentInsideProbabilities[parent]
    // && jointProbability > unaryParentMaxProbabilities[parent]) {
    // unaryParentMaxProbabilities[parent] = jointProbability;
    // spvChartCell.tmpPackedChildren[parent] = grammar.cartesianProductFunction()
    // .packUnary(child);
    // }
    //
    // // Add the unary probability to the total computed probability of the non-terminal
    // spvChartCell.tmpInsideProbabilities[parent] = Math.logSum(
    // spvChartCell.tmpInsideProbabilities[parent], jointProbability);
    // }
    // }
    // }
    // }
    //
    // pruneByFom(spvChartCell, start, end);
    //
    // if (collectDetailedStatistics) {
    // parseTask.unaryAndPruningMs += System.currentTimeMillis() - t0;
    // }
    // }
    //
    // private void pruneByFom(final PackedArrayChartCell spvChartCell, final short start, final short end) {
    //
    // // Ignore observed non-terminals with an inside probability below the local cutoff (i.e., probability more than
    // // maxLocalDelta below that of the most probable non-terminal).
    // final float minInsideProbability = edu.ohsu.cslu.util.Math.max(spvChartCell.tmpInsideProbabilities)
    // - maxLocalDelta;
    // // Push all binary or lexical edges onto a bounded priority queue
    // final int cellBeamWidth = (end - start == 1 ? lexicalRowBeamWidth : java.lang.Math.min(
    // cellSelector.getBeamWidth(start, end), beamWidth));
    // final BoundedPriorityQueue q = threadLocalBoundedPriorityQueue.get();
    // q.clear(cellBeamWidth);
    //
    // if (end - start == 1) { // Lexical Row (span = 1)
    //
    // // Limit the queue to the number of non-unary productions allowed
    // q.setMaxSize(lexicalRowBeamWidth - lexicalRowUnaries);
    //
    // for (short nt = 0; nt < grammar.numNonTerms(); nt++) {
    // if (spvChartCell.tmpInsideProbabilities[nt] > minInsideProbability) {
    // final float fom = fomModel.calcLexicalFOM(start, end, nt, spvChartCell.tmpInsideProbabilities[nt]);
    // q.insert(nt, fom);
    // }
    // }
    // // Now that all lexical productions are on the queue, expand it a bit to allow space for unary
    // // productions
    // q.setMaxSize(lexicalRowBeamWidth);
    //
    // } else { // Span >= 2
    // for (short nt = 0; nt < grammar.numNonTerms(); nt++) {
    // if (spvChartCell.tmpInsideProbabilities[nt] > minInsideProbability) {
    // final float fom = fomModel.calcFOM(start, end, nt, spvChartCell.tmpInsideProbabilities[nt]);
    // q.insert(nt, fom);
    // }
    // }
    // }
    //
    // final float[] tmpInsideProbabilities = new float[grammar.numNonTerms()];
    // System.arraycopy(spvChartCell.tmpInsideProbabilities, 0, tmpInsideProbabilities, 0,
    // tmpInsideProbabilities.length);
    // Arrays.fill(spvChartCell.tmpInsideProbabilities, Float.NEGATIVE_INFINITY);
    // final float[] tmpFoms = threadLocalTmpFoms.get();
    // Arrays.fill(tmpFoms, Float.NEGATIVE_INFINITY);
    //
    // // Pop edges off the queue until we fill the beam width. With each non-terminal popped off the queue,
    // // push unary edges for each unary grammar rule with the non-terminal as a child
    // for (final int edgesPopulated = 0; edgesPopulated < cellBeamWidth && q.size() > 0;) {
    // final int headIndex = q.headIndex();
    // final short nt = q.parentIndices[headIndex];
    // spvChartCell.tmpInsideProbabilities[nt] = tmpInsideProbabilities[nt];
    // tmpFoms[nt] = q.foms[headIndex];
    // q.popHead();
    // }
    // }

    /**
     * To compute the outside probability of a non-terminal in a cell, we need the outside probability of the cell's
     * parent, so we process downward from the top of the chart.
     * 
     * @param start
     * @param end
     */
    private void computeOutsideProbabilities(final short start, final short end, final float[] tmpOutsideProbabilities) {

        // Left-side siblings first

        // foreach parent-start in {0..start - 1}
        for (int parentStart = 0; parentStart < start; parentStart++) {
            final int parentCellIndex = chart.cellIndex(parentStart, end);
            final int parentStartIndex = chart.offset(parentCellIndex);
            final int parentEndIndex = parentStartIndex + chart.numNonTerminals()[parentCellIndex] - 1;

            // Sibling (left) cell
            final int siblingCellIndex = chart.cellIndex(parentStart, start);
            final int siblingStartIndex = chart.minLeftChildIndex(siblingCellIndex);
            final int siblingEndIndex = chart.maxLeftChildIndex(siblingCellIndex);

            computeSiblingOutsideProbabilities(tmpOutsideProbabilities, grammar.rightChildPackingFunction,
                    grammar.rightChildCscBinaryProbabilities, grammar.rightChildCscBinaryRowIndices,
                    grammar.rightChildCscBinaryColumnOffsets, parentStartIndex, parentEndIndex, siblingStartIndex,
                    siblingEndIndex);
        }

        // Right-side siblings

        // foreach parent-end in {end + 1..n}
        for (int parentEnd = end + 1; parentEnd <= chart.size(); parentEnd++) {
            final int parentCellIndex = chart.cellIndex(start, parentEnd);
            final int parentStartIndex = chart.offset(parentCellIndex);
            final int parentEndIndex = parentStartIndex + chart.numNonTerminals()[parentCellIndex] - 1;

            // Sibling (right) cell
            final int siblingCellIndex = chart.cellIndex(end, parentEnd);
            final int siblingStartIndex = chart.minRightChildIndex(siblingCellIndex);
            final int siblingEndIndex = chart.maxRightChildIndex(siblingCellIndex);

            computeSiblingOutsideProbabilities(tmpOutsideProbabilities, grammar.leftChildPackingFunction,
                    grammar.leftChildCscBinaryProbabilities, grammar.leftChildCscBinaryRowIndices,
                    grammar.leftChildCscBinaryColumnOffsets, parentStartIndex, parentEndIndex, siblingStartIndex,
                    siblingEndIndex);
        }

        // Unary outside probabilities
        computeUnaryOutsideProbabilities(tmpOutsideProbabilities);

        chart.finalizeOutside(tmpOutsideProbabilities, chart.cellIndex(start, end));
    }

    private void computeSiblingOutsideProbabilities(final float[] tmpOutsideProbabilities, final PackingFunction cpf,
            final float[] cscBinaryProbabilities, final short[] cscBinaryRowIndices, final int[] cscColumnOffsets,
            final int parentStartIndex, final int parentEndIndex, final int siblingStartIndex, final int siblingEndIndex) {

        // foreach entry in the sibling cell
        for (int i = siblingStartIndex; i <= siblingEndIndex; i++) {
            final short siblingEntry = chart.nonTerminalIndices[i];
            final float siblingInsideProbability = chart.insideProbabilities[i];

            // foreach entry in the parent cell
            for (int j = parentStartIndex; j <= parentEndIndex; j++) {

                final int column = cpf.pack(chart.nonTerminalIndices[j], siblingEntry);
                if (column == Integer.MIN_VALUE) {
                    continue;
                }

                final float jointProbability = siblingInsideProbability + chart.outsideProbabilities[j];

                // foreach grammar rule matching sibling/parent pair (i.e., those which can produce entries in
                // the target cell).
                // TODO Constrain this iteration to entries with non-0 inside probability (e.g. with a merge
                // with insideProbability array)?
                for (int k = cscColumnOffsets[column]; k < cscColumnOffsets[column + 1]; k++) {

                    // Outside probability = sum(production probability x parent outside x sibling inside)
                    final float outsideProbability = cscBinaryProbabilities[k] + jointProbability;
                    final int target = cscBinaryRowIndices[k];
                    final float outsideSum = Math.logSum(outsideProbability, tmpOutsideProbabilities[target]);
                    tmpOutsideProbabilities[target] = outsideSum;
                }
            }
        }
    }

    /**
     * We retain only 1-best unary probabilities, and only if the probability of a unary child exceeds the sum of all
     * probabilities for that non-terminal as a binary child of parent cells)
     */
    private void computeUnaryOutsideProbabilities(final float[] tmpOutsideProbabilities) {

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
}

package edu.ohsu.cslu.lela;

import java.util.Iterator;

import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.ParallelArrayChart;

/**
 * Matrix-loop parser which constrains the chart population according to the contents of a chart populated using the
 * parent grammar (i.e., when training a split grammar, the constraining chart is populated with 1-best parses using the
 * previous grammar)
 * 
 * Analagous to {@link ConstrainedInsideOutsideParser}, but performs Viterbi 1-best inference instead of summing over
 * matching children and rules. Used to populate a {@link ConstrainingChart} to constrain the next split-merge cycle.
 * 
 * If we perform this parse in the real semiring, we occasionally choose a combination of node labels for which no rule
 * productions exist. This choice may produce a more accurate set of node labels (as described by Goodman), but for
 * post-split parsing, it's crucial that we be able to parse the labels represented in the new {@link ConstrainingChart}
 * .
 * 
 * @author Aaron Dunlop
 */
public class ConstrainedViterbiParser extends ConstrainedInsideOutsideParser {

    public ConstrainedViterbiParser(final ParserDriver opts, final ConstrainedInsideOutsideGrammar grammar) {
        super(opts, grammar);
    }

    public ConstrainedChart parse(final ConstrainingChart c) {
        this.constrainingChart = c;

        // Initialize the chart
        chart = new ConstrainedChart(c, grammar);
        chart.parseTask = new ParseTask(c.tokens, grammar);
        cellSelector.initSentence(this);

        // Compute inside probabilities
        insidePass();
        // outsidePass();

        // chart.decode(opts.decodeMethod);
        return chart;
    }

    /**
     * Executes the inside parsing pass (populating {@link ParallelArrayChart#insideProbabilities})
     */
    @Override
    protected void insidePass() {
        while (cellSelector.hasNext()) {
            final short[] startAndEnd = cellSelector.next();
            if (startAndEnd[1] - startAndEnd[0] == 1) {
                // Add lexical productions to the chart from the constraining chart
                addLexicalProductions(startAndEnd[0], startAndEnd[1]);
            }
            computeInsideProbabilities(startAndEnd[0], startAndEnd[1]);
        }
    }

    /**
     * Computes constrained inside viterbi probabilities
     */
    @Override
    protected void computeInsideProbabilities(final short start, final short end) {

        final PackingFunction cpf = grammar.cartesianProductFunction();

        final int cellIndex = chart.cellIndex(start, end);
        final int offset = chart.offset(cellIndex);
        // 0 <= unaryLevels < maxUnaryChainLength
        final int unaryLevels = constrainingChart.unaryChainLength(cellIndex) - 1;

        // Binary productions
        if (end - start > 1) {
            final int parent0Offset = chart.offset(cellIndex) + (unaryLevels << 1);
            final int parent1Offset = parent0Offset + 1;
            final short parent0 = (short) (constrainingChart.nonTerminalIndices[parent0Offset >> 1] << 1);
            final short midpoint = chart.midpoints[cellIndex];

            final int leftCellOffset = chart.offset(chart.cellIndex(start, midpoint));
            final int rightCellOffset = chart.offset(chart.cellIndex(midpoint, end));

            float maxRule = Float.NEGATIVE_INFINITY;

            // Iterate over all possible child pairs
            for (int i = leftCellOffset; i <= leftCellOffset + 1; i++) {
                final short leftChild = chart.nonTerminalIndices[i];
                if (leftChild < 0) {
                    continue;
                }
                final float leftProbability = chart.insideProbabilities[i];

                // And over children in the right child cell
                for (int j = rightCellOffset; j <= rightCellOffset + 1; j++) {
                    final int column = cpf.pack(leftChild, chart.nonTerminalIndices[j]);
                    if (column == Integer.MIN_VALUE) {
                        continue;
                    }

                    final float childInsideProbability = leftProbability + chart.insideProbabilities[j];

                    for (int k = grammar.cscBinaryColumnOffsets[column]; k < grammar.cscBinaryColumnOffsets[column + 1]; k++) {
                        final short parent = grammar.cscBinaryRowIndices[k];

                        // We're only looking for two parents
                        if (parent < parent0) {
                            continue;

                        } else if (parent == parent0) {
                            final float jointProbability = childInsideProbability + grammar.cscBinaryProbabilities[k];
                            if (jointProbability > chart.insideProbabilities[parent0Offset]) {
                                chart.nonTerminalIndices[parent0Offset] = parent;
                                maxRule = Math.max(maxRule, grammar.cscBinaryProbabilities[k]);
                                chart.insideProbabilities[parent0Offset] = jointProbability;
                                chart.packedChildren[parent0Offset] = column;
                            }

                        } else if (parent == parent0 + 1) {
                            final float jointProbability = childInsideProbability + grammar.cscBinaryProbabilities[k];
                            if (jointProbability > chart.insideProbabilities[parent1Offset]) {
                                chart.nonTerminalIndices[parent1Offset] = parent;
                                maxRule = Math.max(maxRule, grammar.cscBinaryProbabilities[k]);
                                chart.insideProbabilities[parent1Offset] = jointProbability;
                                chart.packedChildren[parent1Offset] = column;
                            }

                        } else {
                            // We've passed both target parents. No need to search more grammar rules
                            break;
                        }
                    }
                }
            }
            assert maxRule > -20f;
        }

        // Unary productions
        // foreach unary chain depth (starting from 2nd from bottom in chain; the bottom entry is the binary or lexical
        // parent)
        final int initialChildIndex = offset + (unaryLevels << 1);
        for (int child0Offset = initialChildIndex; child0Offset > offset; child0Offset -= 2) {

            final int parent0Offset = child0Offset - 2;
            final short parent0 = (short) (constrainingChart.nonTerminalIndices[parent0Offset >> 1] << 1);
            final int parent1Offset = parent0Offset + 1;
            float maxRule = Float.NEGATIVE_INFINITY;

            // Iterate over both child slots
            final short child0 = (short) (constrainingChart.nonTerminalIndices[child0Offset >> 1] << 1);
            for (short i = 0; i <= 1; i++) {

                final short child = (short) (child0 + i);
                final float childInsideProbability = chart.insideProbabilities[child0Offset + i];
                if (childInsideProbability == Float.NEGATIVE_INFINITY) {
                    continue;
                }

                // Iterate over possible parents of the child (rows with non-zero entries)
                for (int j = grammar.cscUnaryColumnOffsets[child]; j < grammar.cscUnaryColumnOffsets[child + 1]; j++) {

                    final short parent = grammar.cscUnaryRowIndices[j];

                    // We're only looking for two parents
                    if (parent < parent0) {
                        continue;

                    } else if (parent == parent0) {
                        final float unaryProbability = grammar.cscUnaryProbabilities[j] + childInsideProbability;
                        if (unaryProbability > chart.insideProbabilities[parent0Offset]) {
                            chart.nonTerminalIndices[parent0Offset] = parent;
                            maxRule = Math.max(maxRule, grammar.cscUnaryProbabilities[j]);
                            chart.insideProbabilities[parent0Offset] = unaryProbability;
                            chart.packedChildren[parent0Offset] = grammar.packingFunction.packUnary(child);
                        }

                    } else if (parent == parent0 + 1) {
                        final float unaryProbability = grammar.cscUnaryProbabilities[j] + childInsideProbability;
                        if (unaryProbability > chart.insideProbabilities[parent1Offset]) {
                            chart.nonTerminalIndices[parent1Offset] = parent;
                            maxRule = Math.max(maxRule, grammar.cscUnaryProbabilities[j]);
                            chart.insideProbabilities[parent1Offset] = unaryProbability;
                            chart.packedChildren[parent1Offset] = grammar.packingFunction.packUnary(child);
                        }

                    } else {
                        // We've passed both target parents. No need to search more grammar rules
                        break;
                    }
                }
            }
            assert maxRule > -20f;
        }
    }

    /**
     * Executes the outside parsing pass (populating {@link ConstrainedChart#outsideProbabilities})
     */
    private void outsidePass() {

        final Iterator<short[]> reverseIterator = cellSelector.reverseIterator();

        // Populate start-symbol outside probability in the top cell.
        chart.outsideProbabilities[chart.offset(chart.cellIndex(0, chart.size()))] = 0;

        while (reverseIterator.hasNext()) {
            final short[] startAndEnd = reverseIterator.next();
            computeOutsideProbabilities(startAndEnd[0], startAndEnd[1]);
        }
    }

    private void computeOutsideProbabilities(final short start, final short end) {

        final int cellIndex = chart.cellIndex(start, end);

        // The entry we're computing is the top entry in the target cell
        final int entry0Offset = chart.offset(cellIndex);
        final int entry1Offset = entry0Offset + 1;

        // The parent is the bottom entry in the parent cell
        final int parentCellIndex = chart.parentCellIndices[cellIndex];
        // The top cell won't have a parent, but we still want to compute unary outside probabilities
        if (parentCellIndex >= 0) {
            final int parent0Offset = chart.offset(parentCellIndex)
                    + ((chart.unaryChainLength(parentCellIndex) - 1) << 1);

            // And the sibling is the top entry in the sibling cell
            final short siblingCellIndex = chart.siblingCellIndices[cellIndex];
            final int sibling0Offset = chart.offset(siblingCellIndex);

            if (siblingCellIndex > cellIndex) {
                // Process a left-side sibling
                computeSiblingOutsideProbabilities(entry0Offset, entry1Offset, parent0Offset, sibling0Offset,
                        grammar.leftChildCscBinaryColumnOffsets, grammar.leftChildCscBinaryRowIndices,
                        grammar.leftChildCscBinaryProbabilities, grammar.leftChildPackingFunction);
            } else {
                // Process a right-side sibling
                computeSiblingOutsideProbabilities(entry0Offset, entry1Offset, parent0Offset, sibling0Offset,
                        grammar.rightChildCscBinaryColumnOffsets, grammar.rightChildCscBinaryRowIndices,
                        grammar.rightChildCscBinaryProbabilities, grammar.rightChildPackingFunction);
            }
        }

        // Unary outside probabilities
        computeUnaryOutsideProbabilities(cellIndex);

    }

    private void computeSiblingOutsideProbabilities(final int entry0Offset, final int entry1Offset,
            final int parent0Offset, final int sibling0Offset, final int[] cscBinaryColumnOffsets,
            final short[] cscBinaryRowIndices, final float[] cscBinaryProbabilities, final PackingFunction cpf) {

        final short entry0 = (short) (constrainingChart.nonTerminalIndices[entry0Offset >> 1] << 1);
        final short entry1 = (short) (entry0 + 1);
        float maxRule = Float.NEGATIVE_INFINITY;

        // Iterate over possible siblings
        for (int i = sibling0Offset; i <= sibling0Offset + 1; i++) {
            final short siblingEntry = chart.nonTerminalIndices[i];
            if (siblingEntry < 0) {
                continue;
            }
            final float siblingInsideProbability = chart.insideProbabilities[i];

            // And over possible parents
            for (int j = parent0Offset; j <= parent0Offset + 1; j++) {

                final short parent = chart.nonTerminalIndices[j];
                if (parent < 0) {
                    continue;
                }
                final int column = cpf.pack(parent, siblingEntry);
                if (column == Integer.MIN_VALUE) {
                    continue;
                }

                final float jointProbability = siblingInsideProbability + chart.outsideProbabilities[j];

                // And finally over grammar rules matching the parent and sibling
                for (int k = cscBinaryColumnOffsets[column]; k < cscBinaryColumnOffsets[column + 1]; k++) {
                    final short entry = cscBinaryRowIndices[k];

                    // We're only looking for two entries
                    if (entry < entry0) {
                        continue;

                    } else if (entry == entry0) {
                        final float totalProbability = cscBinaryProbabilities[k] + jointProbability;
                        if (totalProbability > chart.outsideProbabilities[entry0Offset]) {
                            maxRule = Math.max(maxRule, grammar.cscBinaryProbabilities[k]);
                            chart.outsideProbabilities[entry0Offset] = totalProbability;
                        }

                    } else if (entry == entry1) {
                        final float totalProbability = cscBinaryProbabilities[k] + jointProbability;
                        if (totalProbability > chart.outsideProbabilities[entry1Offset]) {
                            maxRule = Math.max(maxRule, grammar.cscBinaryProbabilities[k]);
                            chart.outsideProbabilities[entry1Offset] = totalProbability;
                        }

                    } else {
                        // We've passed both target parents. No need to search more grammar rules
                        break;
                    }
                }
            }
        }
        assert maxRule > -20f;
    }

    private void computeUnaryOutsideProbabilities(final int cellIndex) {

        final int offset = chart.offset(cellIndex);

        // foreach unary chain depth (starting from 2nd from top in chain; the top entry is the binary child)
        final int bottomChildOffset = offset + ((chart.unaryChainLength(cellIndex) - 1) << 1);
        for (int parent0Offset = offset; parent0Offset < bottomChildOffset; parent0Offset += 2) {

            final int child0Offset = parent0Offset + 2;
            final short parent0 = (short) (constrainingChart.nonTerminalIndices[parent0Offset >> 1] << 1);
            final short parent1 = (short) (parent0 + 1);
            float maxRule = Float.NEGATIVE_INFINITY;

            // Iterate over both child slots
            for (int childOffset = child0Offset; childOffset <= child0Offset + 1; childOffset++) {

                final short child = chart.nonTerminalIndices[childOffset];
                if (child < 0) {
                    continue;
                }

                // Iterate over possible parents of the child (rows with non-zero entries)
                for (int j = grammar.cscUnaryColumnOffsets[child]; j < grammar.cscUnaryColumnOffsets[child + 1]; j++) {

                    final short parent = grammar.cscUnaryRowIndices[j];

                    // We're only looking for two parents
                    if (parent < parent0) {
                        continue;

                    } else if (parent == parent0) {
                        final float jointProbability = chart.outsideProbabilities[parent0Offset]
                                + grammar.cscUnaryProbabilities[j];
                        if (jointProbability > chart.outsideProbabilities[childOffset]) {
                            maxRule = Math.max(maxRule, grammar.cscUnaryProbabilities[j]);
                            chart.outsideProbabilities[childOffset] = jointProbability;
                        }

                    } else if (parent == parent1) {
                        final float jointProbability = chart.outsideProbabilities[parent0Offset + 1]
                                + grammar.cscUnaryProbabilities[j];
                        if (jointProbability > chart.outsideProbabilities[childOffset]) {
                            maxRule = Math.max(maxRule, grammar.cscUnaryProbabilities[j]);
                            chart.outsideProbabilities[childOffset] = jointProbability;
                        }

                    } else {
                        // We've passed both target parents. No need to search more grammar rules
                        break;
                    }
                }
            }
            assert maxRule > -20f;
        }
    }

    @Override
    protected void computeInsideProbabilities(final ChartCell cell) {
        throw new UnsupportedOperationException();
    }
}

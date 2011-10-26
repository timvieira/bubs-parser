package edu.ohsu.cslu.lela;

import java.util.Iterator;

import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.ParallelArrayChart;
import edu.ohsu.cslu.parser.ml.SparseMatrixLoopParser;
import edu.ohsu.cslu.util.Math;

/**
 * Matrix-loop parser which constrains the chart population according to the contents of a chart populated using the
 * parent grammar (i.e., when training a split grammar, the constraining chart is populated with 1-best parses using the
 * previous grammar)
 * 
 * 
 * Implementation notes:
 * 
 * --The target chart need only contain S entries per cell, where S is the largest number of splits of a single element
 * of the unsplit vocabulary V_0.
 * 
 * --Given known midpoints (child cells) from the constraining chart, we need only consider a single midpoint for each
 * cell.
 * 
 * --We do need to maintain space for a few unary productions; the first entry in each chart cell is for the top node in
 * the unary chain; any others (if populated) are unary children.
 * 
 * --The grammar intersection need only consider rules whose parent is in the set of known parent NTs. We iterate over
 * the rules for all child pairs, but apply only those rules which match constraining parents.
 * 
 * @author Aaron Dunlop
 */
public class ConstrainedInsideOutsideParser extends
        SparseMatrixLoopParser<ConstrainedInsideOutsideGrammar, ConstrainedChart> {

    ConstrainingChart constrainingChart;
    private final SplitVocabulary splitVocabulary;

    public ConstrainedInsideOutsideParser(final ParserDriver opts, final ConstrainedInsideOutsideGrammar grammar) {
        super(opts, grammar);
        this.splitVocabulary = (SplitVocabulary) grammar.nonTermSet;
    }

    public BinaryTree<String> findBestParse(final ConstrainingChart c) {
        this.constrainingChart = c;

        // Initialize the chart
        if (chart != null
                && chart.midpoints.length >= c.midpoints.length
                && chart.nonTerminalIndices.length >= ConstrainedChart
                        .chartArraySize(c.size(), c.maxUnaryChainLength())
                && chart.cellOffsets.length >= c.cellOffsets.length) {
            chart.clear(c);
        } else {
            chart = new ConstrainedChart(c, grammar);
        }

        chart.parseTask = new ParseTask(c.tokens, grammar);
        cellSelector.initSentence(this);

        // Compute inside and outside probabilities
        insidePass();
        outsidePass();

        // chart.decode(opts.decodeMethod);
        return chart.extractBestParse(0, chart.size(), grammar.startSymbol);
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
     * Adds lexical productions from the constraining chart to the current chart
     */
    private void addLexicalProductions(final short start, final short end) {

        final int cellIndex = chart.cellIndex(start, start + 1);

        final int constrainingOffset = constrainingChart.offset(cellIndex);

        // Find the lexical production in the constraining chart
        int unaryChainLength = chart.maxUnaryChainLength - 1;
        while (unaryChainLength > 0 && constrainingChart.nonTerminalIndices[constrainingOffset + unaryChainLength] < 0) {
            unaryChainLength--;
        }

        final int parent0Offset = chart.offset(cellIndex) + (unaryChainLength << 1);
        final int parent1Offset = parent0Offset + 1;

        // Beginning of cell + offset for populated unary parents
        // final int firstPosOffset = chart.offset(cellIndex) + unaryChainLength *
        // splitVocabulary.maxSplits;
        final int constrainingEntryIndex = constrainingChart.offset(cellIndex) + unaryChainLength;
        chart.midpoints[cellIndex] = end;

        final int lexicalProduction = constrainingChart.sparseMatrixGrammar.packingFunction
                .unpackLeftChild(constrainingChart.packedChildren[constrainingEntryIndex]);

        final short parent0 = (short) (constrainingChart.nonTerminalIndices[constrainingEntryIndex] << 1);
        final float[] lexicalLogProbabilities = grammar.lexicalLogProbabilities(lexicalProduction);
        final short[] lexicalParents = grammar.lexicalParents(lexicalProduction);

        // Iterate through grammar lexicon rules matching this word.
        for (int i = 0; i < lexicalLogProbabilities.length; i++) {
            final short parent = lexicalParents[i];
            // We're only looking for two parents
            if (parent < parent0) {
                continue;

            } else if (parent == parent0) {
                chart.nonTerminalIndices[parent0Offset] = parent;
                chart.insideProbabilities[parent0Offset] = lexicalLogProbabilities[i];
                chart.packedChildren[parent0Offset] = chart.sparseMatrixGrammar.packingFunction
                        .packLexical(lexicalProduction);

            } else if (parent == parent0 + 1) {
                chart.nonTerminalIndices[parent1Offset] = parent;
                chart.insideProbabilities[parent1Offset] = lexicalLogProbabilities[i];
                chart.packedChildren[parent1Offset] = chart.sparseMatrixGrammar.packingFunction
                        .packLexical(lexicalProduction);

            } else {
                // We've passed both target parents. No need to search more grammar rules
                break;
            }
        }
    }

    /**
     * Computes constrained inside sum probabilities
     */
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
                            chart.nonTerminalIndices[parent0Offset] = parent;
                            chart.insideProbabilities[parent0Offset] = Math.logSum(
                                    chart.insideProbabilities[parent0Offset], grammar.cscBinaryProbabilities[k]
                                            + childInsideProbability);

                        } else if (parent == parent0 + 1) {
                            chart.nonTerminalIndices[parent1Offset] = parent;
                            chart.insideProbabilities[parent1Offset] = Math.logSum(
                                    chart.insideProbabilities[parent1Offset], grammar.cscBinaryProbabilities[k]
                                            + childInsideProbability);

                        } else {
                            // We've passed both target parents. No need to search more grammar rules
                            break;
                        }
                    }
                }
            }
        }

        // Unary productions
        // foreach unary chain depth (starting from 2nd from bottom in chain; the bottom entry is the binary or lexical
        // parent)
        final int initialChildIndex = offset + (unaryLevels << 1);
        for (int child0Offset = initialChildIndex; child0Offset > offset; child0Offset -= 2) {

            final int parent0Offset = child0Offset - 2;
            final short parent0 = (short) (constrainingChart.nonTerminalIndices[parent0Offset >> 1] << 1);
            final int parent1Offset = parent0Offset + 1;

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
                        chart.nonTerminalIndices[parent0Offset] = parent;
                        chart.insideProbabilities[parent0Offset] = Math.logSum(
                                chart.insideProbabilities[parent0Offset], unaryProbability);

                    } else if (parent == parent0 + 1) {
                        final float unaryProbability = grammar.cscUnaryProbabilities[j] + childInsideProbability;
                        chart.nonTerminalIndices[parent1Offset] = parent;
                        chart.insideProbabilities[parent1Offset] = Math.logSum(
                                chart.insideProbabilities[parent1Offset], unaryProbability);
                    } else {
                        // We've passed both target parents. No need to search more grammar rules
                        break;
                    }
                }
            }
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

        final short entry0 = chart.nonTerminalIndices[entry0Offset];
        final short entry1 = chart.nonTerminalIndices[entry1Offset];

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
                        chart.outsideProbabilities[entry0Offset] = Math.logSum(
                                chart.outsideProbabilities[entry0Offset], cscBinaryProbabilities[k] + jointProbability);

                    } else if (entry == entry1) {
                        chart.outsideProbabilities[entry1Offset] = Math.logSum(
                                chart.outsideProbabilities[entry1Offset], cscBinaryProbabilities[k] + jointProbability);

                    } else {
                        // We've passed both target parents. No need to search more grammar rules
                        break;
                    }

                }
            }
        }
    }

    private void computeUnaryOutsideProbabilities(final int cellIndex) {

        final int offset = chart.offset(cellIndex);

        // foreach unary chain depth (starting from 2nd from top in chain; the top entry is the binary child)
        final int bottomChildOffset = offset + ((chart.unaryChainLength(cellIndex) - 1) << 1);
        for (int parent0Offset = offset; parent0Offset < bottomChildOffset; parent0Offset += 2) {

            final int child0Offset = parent0Offset + 2;
            final short parent0 = chart.nonTerminalIndices[parent0Offset];
            final short parent1 = chart.nonTerminalIndices[parent0Offset + 1];

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
                        final float parentOutsideProbability = chart.outsideProbabilities[parent0Offset];
                        chart.outsideProbabilities[childOffset] = Math.logSum(chart.outsideProbabilities[childOffset],
                                grammar.cscUnaryProbabilities[j] + parentOutsideProbability);

                    } else if (parent == parent1) {
                        final float parentOutsideProbability = chart.outsideProbabilities[parent0Offset + 1];
                        chart.outsideProbabilities[childOffset] = Math.logSum(chart.outsideProbabilities[childOffset],
                                grammar.cscUnaryProbabilities[j] + parentOutsideProbability);
                    } else {
                        // We've passed both target parents. No need to search more grammar rules
                        break;
                    }
                }
            }
        }
    }

    FractionalCountGrammar countRuleOccurrences() {
        final FractionalCountGrammar countGrammar = new FractionalCountGrammar(grammar);
        countRuleOccurrences(countGrammar);
        return countGrammar;
    }

    /**
     * Counts rule occurrences in the current chart.
     * 
     * @param countGrammar The grammar to populate with rule counts
     * @return countGrammar
     */
    FractionalCountGrammar countRuleOccurrences(final FractionalCountGrammar countGrammar) {
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

        return countGrammar;
    }

    // TODO Could these counts be computed during the outside pass?
    private void countBinaryRuleOccurrences(final FractionalCountGrammar countGrammar, final short start,
            final short end) {

        final PackingFunction cpf = grammar.cartesianProductFunction();

        final int cellIndex = chart.cellIndex(start, end);
        // 0 <= unaryLevels < maxUnaryChainLength
        final int unaryLevels = constrainingChart.unaryChainLength(cellIndex) - 1;

        // Binary productions
        final int parent0Offset = chart.offset(cellIndex) + (unaryLevels << 1);
        final int parent1Offset = parent0Offset + 1;
        final short parent0 = (short) (constrainingChart.nonTerminalIndices[parent0Offset >> 1] << 1);
        final short midpoint = chart.midpoints[cellIndex];

        final int leftCellOffset = chart.offset(chart.cellIndex(start, midpoint));
        final int rightCellOffset = chart.offset(chart.cellIndex(midpoint, end));

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
                        // Parent outside x left child inside x right child inside x production probability.
                        // Equation 1 of Petrov et al., 2006.
                        final float jointProbability = chart.outsideProbabilities[parent0Offset]
                                + childInsideProbability + grammar.cscBinaryProbabilities[k];
                        countGrammar.incrementBinaryLogCount(parent, column, jointProbability);

                    } else if (parent == parent0 + 1) {
                        final float jointProbability = chart.outsideProbabilities[parent1Offset]
                                + childInsideProbability + grammar.cscBinaryProbabilities[k];
                        countGrammar.incrementBinaryLogCount(parent, column, jointProbability);

                    } else {
                        // We've passed both target parents. No need to search more grammar rules
                        break;
                    }
                }
            }
        }
    }

    private void countUnaryRuleOccurrences(final FractionalCountGrammar countGrammar, final short start, final short end) {

        final int cellIndex = chart.cellIndex(start, end);
        // 0 <= unaryLevels < maxUnaryChainLength
        final int unaryLevels = constrainingChart.unaryChainLength(cellIndex) - 1;

        final int offset = chart.offset(cellIndex);
        final int initialChildIndex = offset + (unaryLevels << 1);

        // foreach unary chain depth (starting from 2nd from bottom in chain; the bottom entry is the binary or lexical
        // parent)
        for (int child0Offset = initialChildIndex; child0Offset > offset; child0Offset -= 2) {

            final int parent0Offset = child0Offset - 2;
            final short parent0 = (short) (constrainingChart.nonTerminalIndices[parent0Offset >> 1] << 1);
            final int parent1Offset = parent0Offset + 1;

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
                        // Parent outside x child inside x production probability. From equation 1 of Petrov et al.,
                        // 2006
                        final float jointProbability = chart.outsideProbabilities[parent0Offset]
                                + childInsideProbability + grammar.cscUnaryProbabilities[j];
                        countGrammar.incrementUnaryLogCount(parent, child, jointProbability);

                    } else if (parent == parent0 + 1) {
                        final float jointProbability = chart.outsideProbabilities[parent1Offset]
                                + childInsideProbability + grammar.cscUnaryProbabilities[j];
                        countGrammar.incrementUnaryLogCount(parent, child, jointProbability);

                    } else {
                        // We've passed both target parents. No need to search more grammar rules
                        break;
                    }
                }
            }
        }
    }

    private void countLexicalRuleOccurrences(final FractionalCountGrammar countGrammar, final short start,
            final short end) {

        final int cellIndex = chart.cellIndex(start, start + 1);

        final int constrainingOffset = constrainingChart.offset(cellIndex);

        // Find the lexical production in the constraining chart
        int unaryChainLength = chart.maxUnaryChainLength - 1;
        while (unaryChainLength > 0 && constrainingChart.nonTerminalIndices[constrainingOffset + unaryChainLength] < 0) {
            unaryChainLength--;
        }

        final int parent0Offset = chart.offset(cellIndex) + (unaryChainLength << 1);
        final int parent1Offset = parent0Offset + 1;

        // Beginning of cell + offset for populated unary parents
        // final int firstPosOffset = chart.offset(cellIndex) + unaryChainLength *
        // splitVocabulary.maxSplits;
        final int constrainingEntryIndex = constrainingChart.offset(cellIndex) + unaryChainLength;
        chart.midpoints[cellIndex] = end;

        final int lexicalProduction = constrainingChart.sparseMatrixGrammar.packingFunction
                .unpackLeftChild(constrainingChart.packedChildren[constrainingEntryIndex]);

        final short parent0 = (short) (constrainingChart.nonTerminalIndices[constrainingEntryIndex] << 1);
        final float[] lexicalLogProbabilities = grammar.lexicalLogProbabilities(lexicalProduction);
        final short[] lexicalParents = grammar.lexicalParents(lexicalProduction);

        // Iterate through grammar lexicon rules matching this word.
        for (int i = 0; i < lexicalLogProbabilities.length; i++) {
            final short parent = lexicalParents[i];

            // We're only looking for two parents
            if (parent < parent0) {
                continue;

            } else if (parent == parent0) {
                // Parent outside x production probability (child inside = 1 for lexical entries)
                // From Equation 1 of Petrov et al., 2006
                final float jointProbability = chart.outsideProbabilities[parent0Offset] + lexicalLogProbabilities[i];
                countGrammar.incrementLexicalLogCount(parent, lexicalProduction, jointProbability);

            } else if (parent == parent0 + 1) {
                final float jointProbability = chart.outsideProbabilities[parent1Offset] + lexicalLogProbabilities[i];
                countGrammar.incrementLexicalLogCount(parent, lexicalProduction, jointProbability);

            } else {
                // We've passed both target parents. No need to search more grammar rules
                break;
            }
        }
    }

    @Override
    protected void computeInsideProbabilities(final ChartCell cell) {
        throw new UnsupportedOperationException();
    }
}

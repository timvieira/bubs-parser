package edu.ohsu.cslu.lela;

import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
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

    public BinaryTree<String> findBestParse(final ConstrainingChart constrainingChart) {
        this.constrainingChart = constrainingChart;

        // Initialize the chart
        if (chart != null
                && chart.nonTerminalIndices.length >= ConstrainedChart.chartArraySize(constrainingChart.size(),
                        constrainingChart.maxUnaryChainLength())
                && chart.cellOffsets.length >= constrainingChart.cellOffsets.length) {
            chart.clear(constrainingChart);
        } else {
            chart = new ConstrainedChart(constrainingChart, grammar);
        }

        chart.parseTask = new ParseTask(constrainingChart.tokens, grammar);
        cellSelector.initSentence(this);

        // Add lexical productions to the chart from the constraining chart
        for (short start = 0; start < chart.size(); start++) {
            addLexicalProductions(start, (short) (start + 1));
        }

        insidePass();

        // Outside pass
        final long t0 = collectDetailedStatistics ? System.currentTimeMillis() : 0;

        // final Iterator<short[]> reverseIterator = cellSelector.reverseIterator();
        //
        // // Populate start-symbol unaries in the top cell. Assume that the top cell will be first; this might not be
        // // exactly be the reverse of the original iteration order (e.g., for an agenda parser), but there isn't
        // another
        // // sensible order in which to compute outside probabilities.
        // final float[] tmpOutsideProbabilities = new float[grammar.numNonTerms()];
        // Arrays.fill(tmpOutsideProbabilities, Float.NEGATIVE_INFINITY);
        // tmpOutsideProbabilities[grammar.startSymbol] = 0;
        //
        // while (reverseIterator.hasNext()) {
        // final short[] startAndEnd = reverseIterator.next();
        // computeOutsideProbabilities(startAndEnd[0], startAndEnd[1], tmpOutsideProbabilities);
        // Arrays.fill(tmpOutsideProbabilities, Float.NEGATIVE_INFINITY);
        // }

        // chart.decode(opts.decodeMethod);
        return chart.extractBestParse(0, chart.size(), grammar.startSymbol);
    }

    /**
     * Executes the inside / viterbi parsing pass
     */
    @Override
    protected void insidePass() {
        while (cellSelector.hasNext()) {
            final short[] startAndEnd = cellSelector.next();
            if (startAndEnd[1] - startAndEnd[0] == 1) {
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

        final int index0 = chart.offset(cellIndex) + (unaryChainLength << 1);
        final int index1 = index0 + 1;

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
                chart.nonTerminalIndices[index0] = parent;
                chart.insideProbabilities[index0] = lexicalLogProbabilities[i];
                chart.packedChildren[index0] = chart.sparseMatrixGrammar.packingFunction.packLexical(lexicalProduction);

            } else if (parent == parent0 + 1) {
                chart.nonTerminalIndices[index1] = parent;
                chart.insideProbabilities[index1] = lexicalLogProbabilities[i];
                chart.packedChildren[index1] = chart.sparseMatrixGrammar.packingFunction.packLexical(lexicalProduction);

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
            final int parentIndex0 = chart.offset(cellIndex) + (unaryLevels << 1);
            final int parentIndex1 = parentIndex0 + 1;
            final short parent0 = (short) (constrainingChart.nonTerminalIndices[parentIndex0 >> 1] << 1);
            final short midpoint = chart.midpoints[cellIndex];

            final int leftCellOffset = chart.offset(chart.cellIndex(start, midpoint));
            final int rightCellOffset = chart.offset(chart.cellIndex(midpoint, end));

            // Iterate over all possible child pairs
            for (int i = leftCellOffset; i <= leftCellOffset + 1; i++) {
                final short leftChild = chart.nonTerminalIndices[i];
                final float leftProbability = chart.insideProbabilities[i];

                // And over children in the right child cell
                for (int j = rightCellOffset; j <= rightCellOffset + 1; j++) {
                    final int column = cpf.pack(leftChild, chart.nonTerminalIndices[j]);
                    if (column == Integer.MIN_VALUE) {
                        continue;
                    }

                    final float childProbability = leftProbability + chart.insideProbabilities[j];

                    for (int k = grammar.cscBinaryColumnOffsets[column]; k < grammar.cscBinaryColumnOffsets[column + 1]; k++) {
                        final short parent = grammar.cscBinaryRowIndices[k];

                        // We're only looking for two parents
                        if (parent < parent0) {
                            continue;

                        } else if (parent == parent0) {
                            final float jointProbability = grammar.cscBinaryProbabilities[k] + childProbability;
                            chart.nonTerminalIndices[parentIndex0] = parent;
                            chart.insideProbabilities[parentIndex0] = Math.logSum(
                                    chart.insideProbabilities[parentIndex0], jointProbability);

                        } else if (parent == parent0 + 1) {
                            final float jointProbability = grammar.cscBinaryProbabilities[k] + childProbability;
                            chart.nonTerminalIndices[parentIndex1] = parent;
                            chart.insideProbabilities[parentIndex1] = Math.logSum(
                                    chart.insideProbabilities[parentIndex1], jointProbability);

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
        final int initialParentIndex = offset + ((unaryLevels - 1) << 1);
        for (int parentIndex0 = initialParentIndex; parentIndex0 >= offset; parentIndex0 -= 2) {

            final int childIndex0 = parentIndex0 + 2;
            final short parent0 = (short) (constrainingChart.nonTerminalIndices[parentIndex0 >> 1] << 1);
            final int parentIndex1 = parentIndex0 + 1;

            // Iterate over both child slots
            for (int i = childIndex0; i <= childIndex0 + 1; i++) {

                final short child = chart.nonTerminalIndices[i];
                final float childProbability = chart.insideProbabilities[i];

                // Iterate over possible parents of the child (rows with non-zero entries)
                for (int j = grammar.cscUnaryColumnOffsets[child]; j < grammar.cscUnaryColumnOffsets[child + 1]; j++) {

                    final short parent = grammar.cscUnaryRowIndices[j];

                    // We're only looking for two parents
                    if (parent < parent0) {
                        continue;

                    } else if (parent == parent0) {
                        final float unaryProbability = grammar.cscUnaryProbabilities[j] + childProbability;
                        chart.nonTerminalIndices[parentIndex0] = parent;
                        chart.insideProbabilities[parentIndex0] = Math.logSum(chart.insideProbabilities[parentIndex0],
                                unaryProbability);

                    } else if (parent == parent0 + 1) {
                        final float unaryProbability = grammar.cscUnaryProbabilities[j] + childProbability;
                        chart.nonTerminalIndices[parentIndex1] = parent;
                        chart.insideProbabilities[parentIndex1] = Math.logSum(chart.insideProbabilities[parentIndex1],
                                unaryProbability);
                    } else {
                        // We've passed both target parents. No need to search more grammar rules
                        break;
                    }
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
    private void computeOutsideProbabilities(final short start, final short end, final float[] tmpOutsideProbabilities) {

        final long t0 = collectDetailedStatistics ? System.currentTimeMillis() : 0;

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

        if (collectDetailedStatistics) {
            chart.parseTask.outsidePassMs += System.currentTimeMillis() - t0;
        }
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

    private void countBinaryRuleOccurrences(final FractionalCountGrammar countGrammar, final short start,
            final short end) {

        // final CartesianProductVector cartesianProductVector = cartesianProductUnion(start, end);
        // final int cellIndex = chart.cellIndex(start, end);
        // final int cellOffset = chart.offset(cellIndex);
        // final int offset = cellOffset + (chart.unaryChainDepth(cellOffset) - 1) * splitVocabulary.maxSplits;
        //
        // final ConstrainedCellSelector constrainedCellSelector = (ConstrainedCellSelector) cellSelector;
        //
        // final short[] constrainingChartEntries = constrainedCellSelector.constrainingChartNonTerminalIndices();
        //
        // // Find the bottom production in the constraining chart cell
        // // TODO Store unary chain length for each cell?
        // int constrainingEntryIndex = constrainingChart.offset(cellIndex);
        // for (int i = 0; i < chart.maxUnaryChainLength - 1
        // && constrainingChart.nonTerminalIndices[constrainingEntryIndex + 1] >= 0; i++) {
        // constrainingEntryIndex++;
        // }
        // final short constrainingParent = constrainingChartEntries[constrainingEntryIndex];
        // final short constrainingLeftChild = constrainingChartEntries[constrainedCellSelector
        // .constrainingLeftChildCellOffset()];
        //
        // // Iterate over possible parents (matrix rows)
        // final short startParent = splitVocabulary.firstSubcategoryIndices[constrainingParent];
        // final short endParent = (short) (startParent + splitVocabulary.splitCount[startParent]);
        //
        // for (short parent = startParent; parent < endParent; parent++) {
        //
        // final int parentIndex = offset + parent - startParent;
        // final float parentOutside = chart.outsideProbabilities[parentIndex];
        //
        // // Iterate over split left children matching the constraining left child
        // for (int j = grammar.csrBinaryBaseStartIndices[parent][constrainingLeftChild]; j <
        // grammar.csrBinaryBaseStartIndices[parent][constrainingLeftChild + 1]; j++) {
        //
        // final int grammarChildren = grammar.csrBinaryColumnIndices[j];
        //
        // if (cartesianProductVector.probabilities[grammarChildren] == Float.NEGATIVE_INFINITY) {
        // continue;
        // }
        //
        // // Parent outside x left child inside x right child inside x production probability
        // final float jointProbability = parentOutside + cartesianProductVector.probabilities[grammarChildren]
        // + grammar.csrBinaryProbabilities[j];
        //
        // // final short leftChild = (short) grammar.packingFunction.unpackLeftChild(grammarChildren);
        // // final short rightChild = grammar.packingFunction.unpackRightChild(grammarChildren);
        //
        // countGrammar.incrementBinaryLogCount(parent, grammarChildren, jointProbability);
        // }
        // }
    }

    private void countUnaryRuleOccurrences(final FractionalCountGrammar countGrammar, final short start, final short end) {

        // // System.out.println("=== " + start + "," + end + " ===");
        // final int cellIndex = chart.cellIndex(start, end);
        // final int offset = chart.offset(cellIndex);
        // final int constrainingCellUnaryDepth = ((ConstrainedCellSelector) cellSelector).currentCellUnaryChainDepth();
        //
        // // foreach unary chain depth (starting from 2nd from bottom in chart storage; bottom is binary or
        // // lexical parent)
        // for (int childUnaryDepth = 1; childUnaryDepth < constrainingCellUnaryDepth; childUnaryDepth++) {
        //
        // final int parentStartIndex = offset + (childUnaryDepth - 1) * splitVocabulary.maxSplits;
        // final int parentEndIndex = parentStartIndex
        // + splitVocabulary.splitCount[chart.nonTerminalIndices[parentStartIndex]];
        // // final short parentEndSplit = (short) (parentStartSplit + splitVocabulary.splitCount[parentStartSplit]);
        //
        // final short childStartSplit = chart.nonTerminalIndices[offset + childUnaryDepth * splitVocabulary.maxSplits];
        // final short childEndSplit = (short) (childStartSplit + splitVocabulary.splitCount[childStartSplit]);
        //
        // // foreach parent
        // for (int parentIndex = parentStartIndex; parentIndex < parentEndIndex; parentIndex++) {
        // final short parent = chart.nonTerminalIndices[parentIndex];
        // final float parentOutside = chart.outsideProbabilities[parentIndex];
        //
        // // Iterate over grammar rows headed by the parent and compute unary outside probability
        // for (int j = grammar.csrUnaryRowStartIndices[parent]; j < grammar.csrUnaryRowStartIndices[parent + 1]; j++) {
        //
        // // Skip grammar rules which don't match the populated children
        // final short child = grammar.csrUnaryColumnIndices[j];
        // if (child < childStartSplit || child >= childEndSplit) {
        // continue;
        // }
        //
        // // Parent outside x child inside x production probability
        // final float jointProbability = parentOutside
        // + chart.insideProbabilities[offset + childUnaryDepth * splitVocabulary.maxSplits
        // + splitVocabulary.subcategoryIndices[child]] + grammar.csrUnaryProbabilities[j];
        // // System.out.format("%s -> %s %s\n", splitVocabulary.getSymbol(parent),
        // // splitVocabulary.getSymbol(child), Assert.fraction(jointProbability));
        // countGrammar.incrementUnaryLogCount(parent, child, jointProbability);
        // }
        // }
        // }
    }

    private void countLexicalRuleOccurrences(final FractionalCountGrammar countGrammar, final short start,
            final short end) {

        // // System.out.println("=== " + start + "," + end + " ===");
        //
        // final int cellIndex = chart.cellIndex(start, end);
        // final int cellOffset = chart.offset(cellIndex);
        //
        // final int constrainingCellOffset = constrainingChart.cellOffsets[cellIndex];
        // final int constrainingNonTerminal = constrainingChart.nonTerminalIndices[constrainingCellOffset
        // + constrainingChart.unaryChainDepth(constrainingCellOffset) - 1];
        //
        // final int offset = cellOffset + (chart.unaryChainDepth(cellOffset) - 1) * splitVocabulary.maxSplits;
        // final int lexicalChild = grammar.packingFunction.unpackLeftChild(chart.packedChildren[offset]);
        // // final String sLexicalChild = grammar.lexSet.getSymbol(lexicalChild);
        //
        // // TODO Map lexical productions by both child and unsplit (M-0) parent, so we only have to iterate
        // // through the productions of interest.
        // for (final Production lexProd : grammar.getLexicalProductionsWithChild(lexicalChild)) {
        //
        // if (splitVocabulary.baseCategoryIndices[lexProd.parent] == constrainingNonTerminal) {
        //
        // // Parent outside x production probability (child inside = 1 for lexical entries)
        // final float jointProbability = chart.outsideProbabilities[offset
        // + splitVocabulary.subcategoryIndices[lexProd.parent]]
        // + lexProd.prob;
        //
        // countGrammar.incrementLexicalLogCount((short) lexProd.parent, lexicalChild, jointProbability);
        // }
        // }
    }

    @Override
    protected void computeInsideProbabilities(final ChartCell cell) {
        throw new UnsupportedOperationException();
    }
}

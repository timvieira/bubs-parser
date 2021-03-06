/*
 * Copyright 2010-2014, Oregon Health & Science University
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
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Further documentation and contact information is available at
 *   https://code.google.com/p/bubs-parser/ 
 */

package edu.ohsu.cslu.parser.real;

import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Level;

import cltool4j.BaseLogger;
import cltool4j.ConfigProperties;
import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.Parser;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.SparseMatrixParser;
import edu.ohsu.cslu.parser.chart.BoundedPriorityQueue;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.ml.CartesianProductHashSpmlParser;
import edu.ohsu.cslu.parser.ml.ConstrainedCphSpmlParser;
import edu.ohsu.cslu.parser.ml.SparseMatrixLoopParser;
import edu.ohsu.cslu.parser.real.RealInsideOutsideCscSparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.parser.real.RealPackedArrayChart.RealPackedArrayChartCell;
import edu.ohsu.cslu.parser.real.RealPackedArrayChart.TemporaryChartCell;
import edu.ohsu.cslu.util.IEEEDoubleScaling;
import edu.ohsu.cslu.util.Math;

/**
 * Stores all grammar probabilities in the real domain (as 64-bit doubles) instead of the log domain. Used for
 * inside-outside parsing, with chart probability storage scaled by scaling tools to avoid numeric underflow. The
 * approach is adapted from the Berkeley parser. Individual probability calculations are more expensive (requiring
 * double-precision floating-point multiplies instead of simple 32-bit adds), but it avoids the expense and
 * precision-loss of repeated <code>logsumexp</code> operations.
 * 
 * This alternate representation means a lot of copy-and-paste code, mostly from {@link SparseMatrixParser},
 * {@link SparseMatrixLoopParser}, and {@link CartesianProductHashSpmlParser}.
 * 
 * @see RealInsideOutsideCscSparseMatrixGrammar
 * @see RealPackedArrayChart
 * 
 * @author Aaron Dunlop
 * @since May 3, 2013
 */
public class RealInsideOutsideCphParser extends
        ChartParser<RealInsideOutsideCscSparseMatrixGrammar, RealPackedArrayChart> {

    /**
     * Configures the number of iterations through unary grammar rules during exhaustive parsing. The default is 1, but
     * (depending on the ordering of unary rules), additional iterations may recover longer unary chains.
     */
    public final static String PROPERTY_UNARY_ITERATIONS = "unaryIterations";

    private final static int UNARY_ITERATIONS = GlobalConfigProperties.singleton().getIntProperty(
            PROPERTY_UNARY_ITERATIONS, 1);

    /** The amount to increase {@link #maxLocalDelta} at each reparsing stage */
    public final static float MAX_LOCAL_DELTA_MULTIPLIER = 1.5f;

    protected int beamWidth;
    protected int lexicalRowBeamWidth;
    protected int lexicalRowUnaries;
    protected float maxLocalDelta;
    protected boolean exhaustiveSearch;

    protected final ThreadLocal<BoundedPriorityQueue> threadLocalBoundedPriorityQueue;
    protected final ThreadLocal<float[]> threadLocalTmpFoms;
    protected final ThreadLocal<TemporaryChartCell> threadLocalQueueEdges;

    public RealInsideOutsideCphParser(final ParserDriver opts, final RealInsideOutsideCscSparseMatrixGrammar grammar) {
        super(opts, grammar);

        initDefaultPruningParams();

        // If we're pruning, initialize the thread-local pruning storage
        if (beamWidth > 0 || implicitPruning()) {

            this.threadLocalBoundedPriorityQueue = new ThreadLocal<BoundedPriorityQueue>() {
                @Override
                protected BoundedPriorityQueue initialValue() {
                    return new BoundedPriorityQueue(Math.max(beamWidth, lexicalRowBeamWidth), grammar);
                }
            };
            this.threadLocalTmpFoms = new ThreadLocal<float[]>() {
                @Override
                protected float[] initialValue() {
                    return new float[grammar.numNonTerms()];
                }
            };
            this.threadLocalQueueEdges = new ThreadLocal<TemporaryChartCell>() {
                @Override
                protected TemporaryChartCell initialValue() {
                    return new TemporaryChartCell(grammar, false);
                }
            };

        } else {
            this.threadLocalBoundedPriorityQueue = null;
            this.threadLocalTmpFoms = null;
            this.threadLocalQueueEdges = null;
        }
    }

    private void initDefaultPruningParams() {

        final ConfigProperties props = GlobalConfigProperties.singleton();

        // Pruning Parameters
        if (props.containsKey(PROPERTY_MAX_BEAM_WIDTH) && props.getIntProperty(PROPERTY_MAX_BEAM_WIDTH) > 0) {
            this.beamWidth = props.getIntProperty(Parser.PROPERTY_MAX_BEAM_WIDTH);
            this.lexicalRowBeamWidth = props.getIntProperty(PROPERTY_LEXICAL_ROW_BEAM_WIDTH, beamWidth);
            this.lexicalRowUnaries = props.getIntProperty(PROPERTY_LEXICAL_ROW_UNARIES, lexicalRowBeamWidth / 3);
            this.maxLocalDelta = props.getFloatProperty(PROPERTY_MAX_LOCAL_DELTA, 15f);
            this.exhaustiveSearch = false;
        } else {
            this.beamWidth = grammar.numNonTerms();
            this.lexicalRowBeamWidth = grammar.numNonTerms();
            this.lexicalRowUnaries = grammar.numNonTerms();
            this.maxLocalDelta = 0f;
            this.exhaustiveSearch = true;
        }
    }

    @Override
    public BinaryTree<String> findBestParse(final ParseTask parseTask) {

        // return super.findBestParse(parseTask);
        initChart(parseTask);
        parseTask.reparseStages = -1;

        for (final Parser.ReparseStrategy.Stage stage : opts.reparseStrategy.stages()) {

            final long stageStartTime = System.currentTimeMillis();
            parseTask.reparseStages++;

            switch (stage) {
            case NORMAL:
                initDefaultPruningParams();
                break;

            case FIXED_BEAM:
                initSentence(parseTask, beamWidth, lexicalRowBeamWidth, lexicalRowUnaries, maxLocalDelta, 0);
                cellSelector.reset(false);
                break;

            case DOUBLE:
                // Skip this doubling if it results in exhaustive parsing. We'll get to EXHAUSTIVE later if it's
                // included in the hierarchy.
                if ((beamWidth << 1) >= grammar.nonTermSet.size()) {
                    continue;
                }
                initSentence(parseTask, beamWidth << 1, lexicalRowBeamWidth << 1, lexicalRowUnaries << 1, maxLocalDelta
                        * MAX_LOCAL_DELTA_MULTIPLIER, 0);
                cellSelector.reset(false);
                break;

            case EXHAUSTIVE:
                initSentence(parseTask, grammar.nonTermSet.size(), grammar.nonTermSet.size(),
                        grammar.nonTermSet.size(), Float.MAX_VALUE, 0);
                cellSelector.reset(false);
                break;
            }

            insidePass();

            // To compute the outside probability of a non-terminal in a cell, we need the outside probability of the
            // cell's parent, so we process downward from the top of the chart.

            // Outside pass
            final Iterator<short[]> reverseIterator = cellSelector.reverseIterator();

            while (reverseIterator.hasNext()) {
                final short[] startAndEnd = reverseIterator.next();
                final RealPackedArrayChartCell cell = chart.getCell(startAndEnd[0], startAndEnd[1]);
                computeOutsideProbabilities(cell);
            }

            if (BaseLogger.singleton().isLoggable(Level.ALL)) {
                BaseLogger.singleton().finest(chart.toString());
            }

            if (chart.hasCompleteParse(grammar.startSymbol)) {
                BaseLogger.singleton().finer(
                        String.format("INFO: stage=%s time=%d success=true", stage.toString(),
                                System.currentTimeMillis() - stageStartTime));
                return chart.decode();
            }
            BaseLogger.singleton().finer(
                    String.format("INFO: stage=%s time=%d success=false", stage.toString(), System.currentTimeMillis()
                            - stageStartTime));
        }

        return extract(parseTask.recoveryStrategy);
    }

    @Override
    protected void initSentence(final ParseTask parseTask) {
        initSentence(parseTask, beamWidth, lexicalRowBeamWidth, lexicalRowUnaries, maxLocalDelta, 0);
    }

    protected void initSentence(final ParseTask parseTask, final int newBeamWidth, final int newLexicalRowBeamWidth,
            final int newLexicalRowUnaries, final float newMaxLocalDelta, final int leftChildSegments) {

        // Set beam width parameters
        if (newBeamWidth >= grammar.nonTermSet.size()) {
            this.beamWidth = this.lexicalRowBeamWidth = this.lexicalRowUnaries = grammar.nonTermSet.size();
            this.exhaustiveSearch = true;
        } else {
            this.beamWidth = newBeamWidth;
        }
        this.lexicalRowBeamWidth = Math.min(newLexicalRowBeamWidth, grammar.nonTermSet.size());
        this.lexicalRowUnaries = Math.min(newLexicalRowUnaries, grammar.nonTermSet.size());
        this.maxLocalDelta = newMaxLocalDelta;

        // Replace the pruning priority queue if the beam width has increased beyond its capacity
        if (threadLocalBoundedPriorityQueue != null
                && Math.max(newBeamWidth, newLexicalRowBeamWidth) > threadLocalBoundedPriorityQueue.get().size()) {
            threadLocalBoundedPriorityQueue.set(new BoundedPriorityQueue(
                    Math.max(newBeamWidth, newLexicalRowBeamWidth), grammar));
        }

        if (chart != null
                && chart.size() >= parseTask.sentenceLength()
                && chart.chartArraySize() >= chart.chartArraySize(parseTask.sentenceLength(), this.beamWidth,
                        this.lexicalRowBeamWidth)) {
            chart.reset(parseTask, this.beamWidth, this.lexicalRowBeamWidth);
        } else {
            chart = new RealPackedArrayChart(parseTask, grammar, beamWidth, lexicalRowBeamWidth);
        }
    }

    @Override
    protected void addLexicalProductions(final ChartCell cell) {
        if (ParserDriver.parseFromInputTags) {
            super.addLexicalProductions(cell);
        } else {
            final short start = cell.start();
            final int child = chart.parseTask.tokens[start];
            final short[] parents = grammar.lexicalParents(child);
            final double[] lexicalProbabilities = grammar.lexicalProbabilities(child);
            ((RealPackedArrayChartCell) cell).storeLexicalProductions(child, parents, lexicalProbabilities);
        }
    }

    @Override
    protected void computeInsideProbabilities(final ChartCell cell) {

        final long t0 = collectDetailedStatistics ? System.nanoTime() : 0;

        final RealPackedArrayChartCell targetCell = (RealPackedArrayChartCell) cell;
        final short start = cell.start();
        final short end = cell.end();
        targetCell.allocateTemporaryStorage();
        final TemporaryChartCell tmpCell = targetCell.tmpCell;

        final boolean factoredOnly = cellSelector.hasCellConstraints() && cellSelector.isCellOnlyFactored(start, end);

        final int[] binaryColumnOffsets = factoredOnly ? grammar.factoredCscBinaryColumnOffsets
                : grammar.cscBinaryColumnOffsets;
        final double[] binaryProbabilities = factoredOnly ? grammar.factoredCscBinaryProbabilities
                : grammar.cscBinaryProbabilities;
        final short[] binaryRowIndices = factoredOnly ? grammar.factoredCscBinaryRowIndices
                : grammar.cscBinaryRowIndices;

        final PackingFunction pf = grammar.packingFunction();

        // Search possible midpoints for the largest scaling step (minimum scaling factor). Scaling steps are large
        // enough that we can reasonably assume that the probability mass of midpoints scaled much more will be
        // inconsequential.
        final int maxScalingStep = chart.maxInsideScalingStep(start, end);

        // Iterate over all possible midpoints
        for (short midpoint = (short) (start + 1); midpoint <= end - 1; midpoint++) {

            final int leftCellIndex = chart.cellIndex(start, midpoint);
            final int rightCellIndex = chart.cellIndex(midpoint, end);
            final int scalingStep = chart.insideScalingSteps[leftCellIndex] + chart.insideScalingSteps[rightCellIndex];

            // Skip midpoints with scaling steps that won't contribute meaningfully to the total probability mass
            if (scalingStep < maxScalingStep - 1) {
                continue;
            }

            // Iterate over children in the left child cell
            final int leftStart = chart.minLeftChildIndex(leftCellIndex);
            final int leftEnd = chart.maxLeftChildIndex(leftCellIndex);

            final int rightStart = chart.minRightChildIndex(rightCellIndex);
            final int rightEnd = chart.maxRightChildIndex(rightCellIndex);

            for (int i = leftStart; i <= leftEnd; i++) {
                final short leftChild = chart.nonTerminalIndices[i];
                final double leftProbability = chart.insideProbabilities[i];

                // And over children in the right child cell
                for (int j = rightStart; j <= rightEnd; j++) {
                    final int column = pf.pack(leftChild, chart.nonTerminalIndices[j]);
                    if (column == Integer.MIN_VALUE) {
                        continue;
                    }

                    final double childProbability = IEEEDoubleScaling.scalingMultiplier(scalingStep - maxScalingStep)
                            * leftProbability * chart.insideProbabilities[j];

                    for (int k = binaryColumnOffsets[column]; k < binaryColumnOffsets[column + 1]; k++) {

                        final double jointProbability = binaryProbabilities[k] * childProbability;
                        final short parent = binaryRowIndices[k];
                        tmpCell.insideProbabilities[parent] += jointProbability;
                    }
                }
            }

            if (collectDetailedStatistics) {
                chart.parseTask.nBinaryConsidered += (leftEnd - leftStart + 1) * (rightEnd - rightStart + 1);
            }
        }

        if (collectDetailedStatistics) {
            chart.parseTask.insideBinaryNs += System.nanoTime() - t0;
        }

        // Apply unary rules
        if (exhaustiveSearch) {
            unarySpmv(targetCell);
        } else {
            unaryAndPruning(targetCell, start, end);
        }

        tmpCell.insideScalingStep = IEEEDoubleScaling.scaleArray(tmpCell.insideProbabilities, maxScalingStep);
        targetCell.finalizeCell();

        if (collectDetailedStatistics) {
            chart.parseTask.totalPopulatedEdges += targetCell.getNumNTs();
        }
    }

    protected final void computeOutsideProbabilities(final RealPackedArrayChartCell cell) {

        final long t0 = collectDetailedStatistics ? System.nanoTime() : 0;

        final short start = cell.start();
        final short end = cell.end();

        // Allocate temporary storage and populate start-symbol probability in the top cell
        cell.allocateTemporaryStorage(true, false);
        if (start == 0 && end == chart.size()) {
            cell.tmpCell.outsideProbabilities[grammar.startSymbol] = 1;
        }

        // Search possible sibling/parent-cell combinations for the largest scaling step (minimum scaling factor).
        // Scaling steps are large enough that we can reasonably assume that the probability mass of siblings with
        // smaller scaling steps will be inconsequential.
        final int maxScalingStep = chart.maxOutsideScalingStep(start, end);

        // Left-side siblings first

        // foreach parent-start in {0..start - 1}
        for (int parentStart = 0; parentStart < start; parentStart++) {

            final RealPackedArrayChartCell parentCell = chart.getCell(parentStart, end);

            // Sibling (left) cell
            final int siblingCellIndex = chart.cellIndex(parentStart, start);
            final int scalingStep = chart.outsideScalingSteps[parentCell.cellIndex]
                    + chart.insideScalingSteps[siblingCellIndex];

            // Skip midpoints with scaling steps that won't contribute meaningfully to the total probability mass
            if (scalingStep < maxScalingStep - 1) {
                continue;
            }

            parentCell.allocateTemporaryStorage(true, true);
            final double[] parentOutsideProbabilities = parentCell.tmpCell.outsideProbabilities;
            final int siblingStartIndex = chart.minLeftChildIndex(siblingCellIndex);
            final int siblingEndIndex = chart.maxLeftChildIndex(siblingCellIndex);

            computeLeftSiblingOutsideProbabilities(cell.tmpCell.outsideProbabilities, cell.minRightChildIndex(),
                    cell.maxRightChildIndex(), siblingStartIndex, siblingEndIndex, parentOutsideProbabilities,
                    IEEEDoubleScaling.scalingMultiplier(scalingStep - maxScalingStep));
        }

        // Right-side siblings

        // foreach parent-end in {end + 1..n}
        for (int parentEnd = end + 1; parentEnd <= chart.size(); parentEnd++) {

            final RealPackedArrayChartCell parentCell = chart.getCell(start, parentEnd);

            // Sibling (right) cell
            final int siblingCellIndex = chart.cellIndex(end, parentEnd);
            final int scalingStep = chart.outsideScalingSteps[parentCell.cellIndex]
                    + chart.insideScalingSteps[siblingCellIndex];

            // Skip midpoints with scaling steps that won't contribute meaningfully to the total probability mass
            if (scalingStep < maxScalingStep - 1) {
                continue;
            }

            parentCell.allocateTemporaryStorage(true, true);
            final double[] parentOutsideProbabilities = parentCell.tmpCell.outsideProbabilities;

            final int siblingStartIndex = chart.minRightChildIndex(siblingCellIndex);
            final int siblingEndIndex = chart.maxRightChildIndex(siblingCellIndex);

            computeRightSiblingOutsideProbabilities(cell.tmpCell.outsideProbabilities, cell.minLeftChildIndex(),
                    cell.maxLeftChildIndex(), siblingStartIndex, siblingEndIndex, parentOutsideProbabilities,
                    IEEEDoubleScaling.scalingMultiplier(scalingStep - maxScalingStep));
        }

        // Unary outside probabilities
        if (collectDetailedStatistics) {
            final long t1 = System.nanoTime();
            chart.parseTask.outsideBinaryNs += t1 - t0;
            computeUnaryOutsideProbabilities(cell.tmpCell.outsideProbabilities);
            chart.parseTask.outsideUnaryNs += System.nanoTime() - t1;
        } else {
            computeUnaryOutsideProbabilities(cell.tmpCell.outsideProbabilities);
        }

        cell.tmpCell.outsideScalingStep = IEEEDoubleScaling.scaleArray(cell.tmpCell.outsideProbabilities,
                maxScalingStep);
        cell.finalizeCell();
    }

    private void computeLeftSiblingOutsideProbabilities(final double[] outsideProbabilities, final int targetStart,
            final int targetEnd, final int siblingStart, final int siblingEnd,
            final double[] parentOutsideProbabilities, final double scalingMultiplier) {

        final PackingFunction pf = grammar.packingFunction();

        // Iterate over entries in the left sibling cell
        for (int i = siblingStart; i <= siblingEnd; i++) {
            final short leftSibling = chart.nonTerminalIndices[i];
            final double scaledSiblingInsideProbability = chart.insideProbabilities[i] * scalingMultiplier;

            // And over entries in the target cell
            for (int j = targetStart; j <= targetEnd; j++) {
                final short entry = chart.nonTerminalIndices[j];
                final int column = pf.pack(leftSibling, entry);
                if (column == Integer.MIN_VALUE) {
                    continue;
                }

                for (int k = grammar.cscBinaryColumnOffsets[column]; k < grammar.cscBinaryColumnOffsets[column + 1]; k++) {

                    final int parent = grammar.cscBinaryRowIndices[k];
                    // Skip parents with 0 outside probability
                    if (parentOutsideProbabilities[parent] == 0.0) {
                        continue;
                    }

                    // Outside probability = sum(production probability x parent outside x sibling inside)
                    final double outsideProbability = grammar.cscBinaryProbabilities[k]
                            * parentOutsideProbabilities[parent] * scaledSiblingInsideProbability;
                    outsideProbabilities[entry] += outsideProbability;
                }
            }
        }
    }

    private void computeRightSiblingOutsideProbabilities(final double[] outsideProbabilities, final int targetStart,
            final int targetEnd, final int siblingStart, final int siblingEnd,
            final double[] parentOutsideProbabilities, final double scalingMultiplier) {

        final PackingFunction pf = grammar.packingFunction();

        // Iterate over entries in the left sibling cell
        for (int i = siblingStart; i <= siblingEnd; i++) {
            final short rightSibling = chart.nonTerminalIndices[i];
            final double scaledSiblingInsideProbability = chart.insideProbabilities[i] * scalingMultiplier;

            // And over entries in the target cell
            for (int j = targetStart; j <= targetEnd; j++) {
                final short entry = chart.nonTerminalIndices[j];
                final int column = pf.pack(entry, rightSibling);
                if (column == Integer.MIN_VALUE) {
                    continue;
                }

                for (int k = grammar.cscBinaryColumnOffsets[column]; k < grammar.cscBinaryColumnOffsets[column + 1]; k++) {

                    final int parent = grammar.cscBinaryRowIndices[k];
                    // Skip parents with 0 outside probability
                    if (parentOutsideProbabilities[parent] == 0.0) {
                        continue;
                    }

                    // Outside probability = sum(production probability x parent outside x sibling inside)
                    final double outsideProbability = grammar.cscBinaryProbabilities[k]
                            * parentOutsideProbabilities[parent] * scaledSiblingInsideProbability;
                    outsideProbabilities[entry] += outsideProbability;
                }
            }
        }
    }

    /**
     * We retain only 1-best unary probabilities, and only if the probability of a unary child exceeds the sum of all
     * probabilities for that non-terminal as a binary child of parent cells)
     */
    protected final void computeUnaryOutsideProbabilities(final double[] tmpOutsideProbabilities) {

        // Iterate over populated parents (matrix rows)
        for (short parent = 0; parent < grammar.numNonTerms(); parent++) {

            if (tmpOutsideProbabilities[parent] == 0.0) {
                continue;
            }

            // Iterate over possible children (columns with non-zero entries)
            for (int i = grammar.csrUnaryRowStartIndices[parent]; i < grammar.csrUnaryRowStartIndices[parent + 1]; i++) {

                final short child = grammar.csrUnaryColumnIndices[i];
                final double jointProbability = grammar.csrUnaryProbabilities[i] * tmpOutsideProbabilities[parent];
                if (jointProbability > tmpOutsideProbabilities[child]) {
                    tmpOutsideProbabilities[child] = jointProbability;
                }
            }
        }
    }

    /**
     * Multiplies the unary grammar matrix (stored sparsely) by the contents of this cell (stored densely), and
     * populates this chart cell. Used to populate unary rules.
     * 
     * @param chartCell
     */
    public void unarySpmv(final ChartCell chartCell) {

        final RealPackedArrayChartCell packedArrayCell = (RealPackedArrayChartCell) chartCell;
        packedArrayCell.allocateTemporaryStorage();

        unarySpmv(packedArrayCell.tmpCell.packedChildren, packedArrayCell.tmpCell.insideProbabilities,
                packedArrayCell.tmpCell.midpoints, 0, chartCell.end());
    }

    protected void unarySpmv(final int[] chartCellChildren, final double[] chartCellProbabilities,
            final short[] chartCellMidpoints, final int offset, final short chartCellEnd) {

        final PackingFunction cpf = grammar.packingFunction();

        for (int iteration = 0; iteration < UNARY_ITERATIONS; iteration++) {
            // Iterate over populated children (matrix columns)
            for (short child = 0; child < grammar.numNonTerms(); child++) {

                final int childOffset = offset + child;
                if (chartCellProbabilities[childOffset] == 0.0) {
                    continue;
                }

                // Iterate over possible parents of the child (rows with non-zero entries)
                for (int i = grammar.cscUnaryColumnOffsets[child]; i < grammar.cscUnaryColumnOffsets[child + 1]; i++) {

                    final short parent = grammar.cscUnaryRowIndices[i];
                    final int parentOffset = offset + parent;
                    final double grammarProbability = grammar.cscUnaryProbabilities[i];

                    final double jointProbability = grammarProbability * chartCellProbabilities[childOffset];
                    if (jointProbability > chartCellProbabilities[parentOffset]) {
                        chartCellProbabilities[parentOffset] = jointProbability;
                        chartCellChildren[parentOffset] = cpf.packUnary(child);
                        chartCellMidpoints[parentOffset] = chartCellEnd;
                    }
                }

                if (collectDetailedStatistics) {
                    chart.parseTask.nUnaryConsidered += (grammar.cscUnaryColumnOffsets[child + 1] - grammar.cscUnaryColumnOffsets[child]);
                }
            }
        }
    }

    protected final void unaryAndPruning(final RealPackedArrayChart.RealPackedArrayChartCell spvChartCell,
            final short start, final short end) {

        final long t0 = collectDetailedStatistics ? System.nanoTime() : 0;

        // For the moment, at least, we ignore factored-only cell constraints in span-1 cells
        final boolean factoredOnly = cellSelector.hasCellConstraints() && cellSelector.isCellOnlyFactored(start, end)
                && (end - start > 1);
        final boolean allowUnaries = !cellSelector.hasCellConstraints() || cellSelector.isUnaryOpen(start, end);
        final double minInsideProbability = edu.ohsu.cslu.util.Math.doubleMax(spvChartCell.tmpCell.insideProbabilities)
                * java.lang.Math.exp(-maxLocalDelta);

        // We will push all binary or lexical edges onto a bounded priority queue, and then (if unaries are allowed),
        // add those edges as well.
        final int cellBeamWidth = (end - start == 1 ? lexicalRowBeamWidth : java.lang.Math.min(
                cellSelector.getBeamWidth(start, end), beamWidth));
        final BoundedPriorityQueue q = threadLocalBoundedPriorityQueue.get();
        q.clear(cellBeamWidth);

        final double[] maxInsideProbabilities = new double[grammar.numNonTerms()];
        System.arraycopy(spvChartCell.tmpCell.insideProbabilities, 0, maxInsideProbabilities, 0,
                maxInsideProbabilities.length);

        // If unaries are allowed in this cell, compute unary probabilities for all possible parents
        if (!factoredOnly && allowUnaries) {
            final double[] unaryInsideProbabilities = new double[grammar.numNonTerms()];
            Arrays.fill(unaryInsideProbabilities, 0);
            final double[] viterbiUnaryInsideProbabilities = new double[grammar.numNonTerms()];
            Arrays.fill(viterbiUnaryInsideProbabilities, 0);
            final int[] viterbiUnaryPackedChildren = new int[grammar.numNonTerms()];

            for (short child = 0; child < grammar.numNonTerms(); child++) {
                final double insideProbability = spvChartCell.tmpCell.insideProbabilities[child];
                if (insideProbability == 0) {
                    continue;
                }

                // Iterate over possible parents of the child (rows with non-zero entries)
                for (int i = grammar.cscUnaryColumnOffsets[child]; i < grammar.cscUnaryColumnOffsets[child + 1]; i++) {

                    final double unaryProbability = grammar.cscUnaryProbabilities[i] * insideProbability;
                    final short parent = grammar.cscUnaryRowIndices[i];

                    unaryInsideProbabilities[parent] += unaryProbability;

                    if (unaryProbability > viterbiUnaryInsideProbabilities[parent]) {
                        viterbiUnaryInsideProbabilities[parent] = unaryProbability;
                        viterbiUnaryPackedChildren[parent] = grammar.packingFunction.packUnary(child);
                    }
                }
            }

            // Retain the greater of the binary and unary inside probabilities and the appropriate backpointer (biasing
            // toward recovering unaries in the case of a tie)
            for (short nt = 0; nt < grammar.numNonTerms(); nt++) {
                if (unaryInsideProbabilities[nt] >= maxInsideProbabilities[nt]) {
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
                    final float fom = figureOfMerit.calcLexicalFOM(start, end, nt,
                            (float) java.lang.Math.log(maxInsideProbabilities[nt]));
                    q.insert(nt, fom);
                }
            }
            // Now that all lexical productions are on the queue, expand it a bit to allow space for unary productions
            q.setMaxSize(lexicalRowBeamWidth);

        } else { // Span >= 2
            for (short nt = 0; nt < grammar.numNonTerms(); nt++) {
                if (maxInsideProbabilities[nt] > minInsideProbability) {
                    final float fom = figureOfMerit.calcFOM(start, end, nt,
                            (float) java.lang.Math.log(maxInsideProbabilities[nt]));
                    q.insert(nt, fom);
                }
            }
        }

        Arrays.fill(spvChartCell.tmpCell.insideProbabilities, 0.0);

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
     * @return True if this {@link Parser} implementation does implicit pruning (regardless of configuration
     *         properties); e.g. {@link ConstrainedCphSpmlParser}.
     */
    protected boolean implicitPruning() {
        return false;
    }
}

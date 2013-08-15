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

package edu.ohsu.cslu.perceptron;

import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.Arrays;

import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.datastructs.vectors.BitVector;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.parser.chart.Chart;
import edu.ohsu.cslu.parser.chart.PackedArrayChart;

/**
 * Represents binary classification for open chart cells (i.e., those with a beam width > 0, as classified by
 * {@link AdaptiveBeamClassifier}).
 * 
 * @author Aaron Dunlop
 * @since Jul 19, 2013
 */
public abstract class BinaryConstituentBoundarySequence extends ConstituentBoundarySequence implements BinarySequence {

    /**
     * Gold classifications for open cells (those that participate in the 1-best constrained parse); parallel array to
     * {@link #goldCellIndices}. All other cells are considered to be classified as negative, and are omitted from the
     * data structure to conserve heap memory during training. In most cases, closed cells are excluded from training
     * and evaluation, as they will also be omitted during inference.
     */
    protected final boolean[] goldClasses;

    /**
     * Indices for open cells (those that participate in the 1-best constrained parse); parallel array to
     * {@link #goldClasses}.
     */
    protected final int[] goldCellIndices;

    /**
     * Predicted classes, one for each cell in the chart (note that this is many more than are stored in
     * {@link #goldCellIndices}, so we must special-case comparisons when evaluating classification accuracy.)
     * 
     * TODO We could replace this with a {@link BitVector} to save a little storage.
     */
    protected boolean[] predictedClasses;

    public BinaryConstituentBoundarySequence(final PackedArrayChart chart, final BinaryTree<String> parseTree,
            final AdaptiveBeamClassifier classifier) {

        super(parseTree, classifier.lexicon, classifier.decisionTreeUnkClassSet, classifier.vocabulary);

        this.length = sentenceLength * (sentenceLength + 1) / 2;

        //
        // Populate classes for each open cell
        //
        final boolean[] tmpClasses = new boolean[sentenceLength * (sentenceLength + 1) / 2];
        recoverClasses(chart, (short) 0, (short) chart.size(), chart.sparseMatrixGrammar.startSymbol, tmpClasses);

        // Copy from temporary storage (one entry per chart cell) to a compact structure with one entry for each cell in
        // the 1-best parse output
        final BooleanArrayList tmpGoldClasses = new BooleanArrayList();
        final IntArrayList tmpGoldCellIndices = new IntArrayList();
        for (int cellIndex = 0; cellIndex < tmpClasses.length; cellIndex++) {
            final short[] startAndEnd = Chart.startAndEnd(cellIndex, sentenceLength);
            if (chart.numNonTerminals[cellIndex] > 0 && includeCell(startAndEnd[0], startAndEnd[1])) {
                tmpGoldClasses.add(tmpClasses[cellIndex]);
                tmpGoldCellIndices.add(cellIndex);
            }
        }

        this.goldClasses = tmpGoldClasses.toBooleanArray();
        this.goldCellIndices = tmpGoldCellIndices.toIntArray();
    }

    /**
     * Recurses down through the chart, recovering the classification of each populated cell (unpopulated cells are by
     * definition classified as false). Populates <code>tmpClasses</code>, indexed by cellIndex. Subsequent processing
     * in the constructor will omit the empty cells to compact that storage.
     * 
     * @param chart
     * @param start
     * @param end
     * @param parent
     * @param tmpClasses
     */
    protected void recoverClasses(final PackedArrayChart chart, final short start, final short end, final short parent,
            final boolean[] tmpClasses) {

        final int cellIndex = chart.cellIndex(start, end);
        final int offset = chart.offset(cellIndex);

        int bottomEntryIndex = Arrays.binarySearch(chart.nonTerminalIndices, offset, offset
                + chart.numNonTerminals[cellIndex], parent);

        if (includeCell(start, end) && classifyCell(chart, start, end, bottomEntryIndex)) {
            tmpClasses[cellIndex] = true;
        }

        // Iterate through the unary chain (if any) to the bottom entry
        short bottomEntry = parent;

        while (chart.sparseMatrixGrammar.packingFunction.unpackRightChild(chart.packedChildren[bottomEntryIndex]) == Production.UNARY_PRODUCTION) {
            bottomEntry = (short) chart.sparseMatrixGrammar.packingFunction
                    .unpackLeftChild(chart.packedChildren[bottomEntryIndex]);
            bottomEntryIndex = Arrays.binarySearch(chart.nonTerminalIndices, offset, offset
                    + chart.numNonTerminals[cellIndex], bottomEntry);
        }

        // Recurse through child cells
        if (chart.sparseMatrixGrammar.packingFunction.unpackRightChild(chart.packedChildren[bottomEntryIndex]) != Production.LEXICAL_PRODUCTION) {
            recoverClasses(chart, start, chart.midpoints[bottomEntryIndex],
                    (short) chart.sparseMatrixGrammar.packingFunction
                            .unpackLeftChild(chart.packedChildren[bottomEntryIndex]), tmpClasses);
            recoverClasses(chart, chart.midpoints[bottomEntryIndex], end,
                    chart.sparseMatrixGrammar.packingFunction.unpackRightChild(chart.packedChildren[bottomEntryIndex]),
                    tmpClasses);
        }
    }

    /**
     * @param start
     * @param end
     * @return True the boolean classification of the supplied cell, as determined by the subclass of
     *         {@link BinaryConstituentBoundarySequence}
     */
    protected abstract boolean includeCell(short start, short end);

    /**
     * @param chart
     * @param start
     * @param end
     * @param nonterminalOffset
     * @return True the boolean classification of the supplied cell, as determined by the subclass of
     *         {@link BinaryConstituentBoundarySequence}
     */
    protected abstract boolean classifyCell(final PackedArrayChart chart, short start, short end,
            final int nonterminalOffset);

    public int[] goldCellIndices() {
        return goldCellIndices;
    }

    @Override
    public boolean goldClass(final int cellIndex) {
        final int i = Arrays.binarySearch(goldCellIndices, cellIndex);
        return i >= 0 ? goldClasses[i] : false;
    }

    @Override
    public void allocatePredictionStorage() {
        this.predictedClasses = new boolean[sentenceLength * (sentenceLength + 1) / 2];
    }

    @Override
    public void clearPredictionStorage() {
        // Conserve memory between uses
        this.predictedClasses = null;
    }

    @Override
    public boolean predictedClass(final int cellIndex) {
        return predictedClasses[cellIndex];
    }

    @Override
    public boolean[] predictedClasses() {
        return predictedClasses;
    }

    @Override
    public void setPredictedClass(final int cellIndex, final boolean classification) {
        predictedClasses[cellIndex] = classification;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(512);

        for (int i = 0; i < goldCellIndices.length; i++) {
            final int cellIndex = goldCellIndices[i];
            final short[] startAndEnd = Chart.startAndEnd(cellIndex, sentenceLength);
            sb.append("(" + startAndEnd[0] + "," + startAndEnd[1] + " " + (goldClasses[i] ? "T" : "F"));
            if (predictedClasses != null) {
                sb.append(predictedClasses[cellIndex] ? "/T" : "/F");

            }
            sb.append(")   ");
        }
        return sb.toString();
    }
}

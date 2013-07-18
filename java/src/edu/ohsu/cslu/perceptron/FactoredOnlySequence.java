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

import java.util.Arrays;

import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.parser.chart.PackedArrayChart;

/**
 * Represents the observed and predicted contents of cells in a parse tree - specifically, whether the cell contains (or
 * should contain) a factored non-terminal.
 * 
 * @author Aaron Dunlop
 * @since Jul 17, 2013
 */
public class FactoredOnlySequence extends ConstituentBoundarySequence implements BinarySequence {

    /**
     * Gold classes from the training tree, one for each factored-only cell (those that have a factored non-terminal in
     * the 1-best parse tree).All other cells are considered to be closed or non-factored, and are omitted from the data
     * structure to conserve heap memory during training.
     */
    protected int[] goldFactoredOnlyCellIndices;

    /**
     * Predicted classes, one for each cell in the chart (note that this is many more than are stored in
     * {@link #goldFactoredOnlyCellIndices}, so we must special-case comparisons when evaluating classification
     * accuracy.)
     */
    protected boolean[] predictedClasses;

    public FactoredOnlySequence(final PackedArrayChart chart, final BinaryTree<String> parseTree,
            final BeamWidthClassifier classifier) {

        super(parseTree, classifier.lexicon, classifier.decisionTreeUnkClassSet, classifier.vocabulary);

        this.length = sentenceLength * (sentenceLength + 1) / 2;

        //
        // Populate classes for each open cell
        //
        final boolean[] tmpClasses = new boolean[sentenceLength * (sentenceLength + 1) / 2];
        recoverClasses(chart, (short) 0, (short) chart.size(), chart.sparseMatrixGrammar.startSymbol, tmpClasses);

        // Copy from temporary storage (one entry per chart cell) to a compact structure with one entry for each cell in
        // the 1-best parse output
        int factoredCells = 0;
        for (int i = 0; i < tmpClasses.length; i++) {
            if (tmpClasses[i]) {
                factoredCells++;
            }
        }
        this.goldFactoredOnlyCellIndices = new int[factoredCells];

        for (int cellIndex = 0, i = 0; cellIndex < tmpClasses.length; cellIndex++) {
            if (tmpClasses[cellIndex]) {
                this.goldFactoredOnlyCellIndices[i] = cellIndex;
                i++;
            }
        }
    }

    /**
     * Recurses down through the chart, recovering the beam-width class of each populated cell (unpopulated cells
     * default to class 0, which is assumed to denote a beam of 0). Populates <code>tmpClasses</code>, indexed by
     * cellIndex. Subsequent processing may omit the empty cells to compact that storage.
     * 
     * @param chart
     * @param start
     * @param end
     * @param parent
     * @param fom
     * @param beamWidthClasses
     * @param tmpClasses
     */
    private void recoverClasses(final PackedArrayChart chart, final short start, final short end, final short parent,
            final boolean[] tmpClasses) {

        final int cellIndex = chart.cellIndex(start, end);
        final int offset = chart.offset(cellIndex);

        int bottomEntryIndex = Arrays.binarySearch(chart.nonTerminalIndices, offset, offset
                + chart.numNonTerminals[cellIndex], parent);

        if (chart.sparseMatrixGrammar.grammarFormat.isFactored(chart.sparseMatrixGrammar.nonTermSet.getSymbol(parent))) {
            tmpClasses[cellIndex] = true;
        } else {
            // Iterate through the unary chain (if any) to the bottom entry
            short bottomEntry = parent;

            while (chart.sparseMatrixGrammar.packingFunction.unpackRightChild(chart.packedChildren[bottomEntryIndex]) == Production.UNARY_PRODUCTION) {
                bottomEntry = (short) chart.sparseMatrixGrammar.packingFunction
                        .unpackLeftChild(chart.packedChildren[bottomEntryIndex]);
                bottomEntryIndex = Arrays.binarySearch(chart.nonTerminalIndices, offset, offset
                        + chart.numNonTerminals[cellIndex], bottomEntry);
            }
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

    @Override
    public boolean goldClass(final int cellIndex) {
        return Arrays.binarySearch(goldFactoredOnlyCellIndices, cellIndex) >= 0;
    }

    public void allocatePredictedClasses() {
        this.predictedClasses = new boolean[sentenceLength * (sentenceLength + 1) / 2];
    }

    public void clearPredictedClasses() {
        // Conserve memory between uses
        this.predictedClasses = null;
    }

    @Override
    public boolean predictedClass(final int cellIndex) {
        return predictedClasses[cellIndex];
    }

    @Override
    public void setPredictedClass(final int cellIndex, final boolean classification) {
        predictedClasses[cellIndex] = classification;
    }

}

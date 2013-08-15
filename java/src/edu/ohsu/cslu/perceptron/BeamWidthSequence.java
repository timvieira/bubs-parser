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

import it.unimi.dsi.fastutil.shorts.ShortArraySet;
import it.unimi.dsi.fastutil.shorts.ShortSet;

import java.util.Arrays;

import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.parser.chart.Chart;
import edu.ohsu.cslu.parser.chart.PackedArrayChart;
import edu.ohsu.cslu.parser.fom.FigureOfMeritModel.FigureOfMerit;

/**
 * Represents the observed and predicted beam-width classes of cells in a parse tree.
 * 
 * @see AdaptiveBeamClassifier
 * 
 * @author Aaron Dunlop
 * @since Jul 11, 2013
 */
public class BeamWidthSequence extends ConstituentBoundarySequence implements MulticlassSequence {

    /**
     * Gold classes from the training tree, one for each classified cell (those that participate in the 1-best parse
     * tree). Parallel array to {@link #cellIndices}. All other cells are considered to be class 0 (closed), and are
     * omitted from the data structure to conserve heap memory during training.
     */
    protected final short[] classes;

    /** Parallel array to {@link #classes}, denoting the cell indices of each classified cell */
    protected int[] cellIndices;

    /**
     * Predicted classes, one for each cell in the chart (note that this is many more than are stored in
     * {@link #classes}, so we must special-case comparisons when evaluating classification accuracy.)
     */
    protected short[] predictedClasses;

    private final AdaptiveBeamClassifier classifier;

    /**
     * Constructs from an array of tokens, mapped according to the classifier's lexicon. Used during inference.
     * 
     * @param mappedTokens
     * @param posTags
     * @param classifier
     */
    public BeamWidthSequence(final int[] mappedTokens, final short[] posTags, final AdaptiveBeamClassifier classifier) {

        super(mappedTokens, posTags, classifier.lexicon, classifier.decisionTreeUnkClassSet);

        // All cells spanning more than one word
        this.length = sentenceLength * (sentenceLength + 1) / 2 - sentenceLength;
        this.classes = null;
        this.predictedClasses = new short[length];
        this.classifier = classifier;
    }

    /**
     * Used during training. Constructs from a bracketed tree, populating {@link #classes} with open/closed
     * classifications for each chart cell.
     * 
     * @param parseTree
     * @param classifier
     */
    public BeamWidthSequence(final PackedArrayChart chart, final BinaryTree<String> parseTree,
            final AdaptiveBeamClassifier classifier) {

        super(parseTree, classifier.lexicon, classifier.decisionTreeUnkClassSet, classifier.vocabulary);
        this.classifier = classifier;

        //
        // Populate classes for each open cell
        //
        final short[] tmpClasses = new short[sentenceLength * (sentenceLength + 1) / 2];
        Arrays.fill(tmpClasses, (short) -1);

        recoverBeamWidths(chart, (short) 0, (short) chart.size(), chart.sparseMatrixGrammar.startSymbol,
                classifier.ccParser.figureOfMerit, classifier.beamWidthClasses, tmpClasses);

        // One entry per open cell
        final int arrayLength = sentenceLength * 2 - 1;
        // The default class is 0, and is populated at array initialization
        this.classes = new short[arrayLength];
        this.cellIndices = new int[arrayLength];

        // Copy from temporary storage (one entry per chart cell) to a compact structure with one entry for each cell in
        // the 1-best parse output
        this.length = sentenceLength * (sentenceLength + 1) / 2;
        for (int cellIndex = 0, i = 0; cellIndex < tmpClasses.length; cellIndex++) {
            if (tmpClasses[cellIndex] >= 0) {
                this.classes[i] = tmpClasses[cellIndex];
                this.cellIndices[i] = cellIndex;
                i++;
            }
        }
        edu.ohsu.cslu.util.Arrays.sort(cellIndices, classes);
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
    private void recoverBeamWidths(final PackedArrayChart chart, final short start, final short end,
            final short parent, final FigureOfMerit fom, final short[] beamWidthClasses, final short[] tmpClasses) {

        final int cellIndex = chart.cellIndex(start, end);
        final int offset = chart.offset(cellIndex);

        // Construct a set containing all non-terminals in the unary chain (it should be a very small set, so an
        // ArraySet is probably the most efficient implementation here)
        final ShortSet entries = new ShortArraySet();
        short bottomEntry = parent;
        entries.add(parent);
        int bottomEntryIndex = Arrays.binarySearch(chart.nonTerminalIndices, offset, offset
                + chart.numNonTerminals[cellIndex], parent);

        while (chart.sparseMatrixGrammar.packingFunction.unpackRightChild(chart.packedChildren[bottomEntryIndex]) == Production.UNARY_PRODUCTION) {
            bottomEntry = (short) chart.sparseMatrixGrammar.packingFunction
                    .unpackLeftChild(chart.packedChildren[bottomEntryIndex]);
            entries.add(bottomEntry);
            bottomEntryIndex = Arrays.binarySearch(chart.nonTerminalIndices, offset, offset
                    + chart.numNonTerminals[cellIndex], bottomEntry);
        }

        // Copy cell entries into temporary storage, compute the FOM for each entry, and sort by FOM
        final short[] nts = new short[chart.numNonTerminals[cellIndex]];
        final float[] foms = new float[nts.length];

        for (int i = 0, j = offset; i < nts.length; i++, j++) {
            nts[i] = chart.nonTerminalIndices[j];
            if (end - start == 1) {
                foms[i] = fom.calcLexicalFOM(start, end, nts[i], chart.insideProbabilities[j]);
            } else {
                foms[i] = fom.calcFOM(start, end, nts[i], chart.insideProbabilities[j]);
            }
        }

        // Sort by FOM
        edu.ohsu.cslu.util.Arrays.sort(foms, nts);

        // Iterate downward through the sorted entries until we observe all entries in the unary chain
        int beamWidth = 0;
        for (int i = nts.length - 1; i >= 0 && !entries.isEmpty(); i--, beamWidth++) {
            entries.remove(nts[i]);
        }

        if (beamWidth >= beamWidthClasses.length) {
            tmpClasses[cellIndex] = (short) (beamWidthClasses[beamWidthClasses.length - 1] + 1);
        } else {
            tmpClasses[cellIndex] = beamWidthClasses[beamWidth];
        }

        // Recurse through child cells
        if (chart.sparseMatrixGrammar.packingFunction.unpackRightChild(chart.packedChildren[bottomEntryIndex]) != Production.LEXICAL_PRODUCTION) {
            recoverBeamWidths(chart, start, chart.midpoints[bottomEntryIndex],
                    (short) chart.sparseMatrixGrammar.packingFunction
                            .unpackLeftChild(chart.packedChildren[bottomEntryIndex]), fom, beamWidthClasses, tmpClasses);
            recoverBeamWidths(chart, chart.midpoints[bottomEntryIndex], end,
                    chart.sparseMatrixGrammar.packingFunction.unpackRightChild(chart.packedChildren[bottomEntryIndex]),
                    fom, beamWidthClasses, tmpClasses);
        }
    }

    public final short goldClass(final int cellIndex) {
        // Find the cell index
        final int i = Arrays.binarySearch(cellIndices, cellIndex);
        return i >= 0 ? classes[i] : 0;
    }

    @Override
    public void allocatePredictionStorage() {
        this.predictedClasses = new short[sentenceLength * (sentenceLength + 1) / 2];
    }

    @Override
    public void clearPredictionStorage() {
        // Conserve memory between uses
        this.predictedClasses = null;
    }

    @Override
    public short predictedClass(final int cellIndex) {
        return predictedClasses[cellIndex];
    }

    @Override
    public short[] predictedClasses() {
        return predictedClasses;
    }

    @Override
    public void setPredictedClass(final int cellIndex, final short newClass) {
        predictedClasses[cellIndex] = newClass;
    }

    @Override
    public SymbolSet<String> tagSet() {
        final SymbolSet<String> tagSet = new SymbolSet<String>();
        for (final int beam : classifier.classBoundaryBeamWidths) {
            tagSet.addSymbol(Integer.toString(beam));
        }
        return tagSet;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(256);
        for (int i = 0; i < classes.length; i++) {
            final short[] startAndEnd = Chart.startAndEnd(cellIndices[i], sentenceLength, false);
            if (predictedClasses != null) {
                sb.append(String.format("%d,%d  gold class=%d (%d)  predictedClass=%s (%d)\n", startAndEnd[0],
                        startAndEnd[1], classes[i], classifier.beamWidth(classes[i]), predictedClasses[i],
                        classifier.beamWidth(predictedClasses[i])));
            } else {
                sb.append(String.format("%d,%d  gold class=%d (%d)\n", startAndEnd[0], startAndEnd[1], classes[i],
                        classifier.beamWidth(classes[i])));
            }
        }
        return sb.toString();
    }
}

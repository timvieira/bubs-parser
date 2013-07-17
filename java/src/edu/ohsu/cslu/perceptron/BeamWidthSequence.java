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
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.chart.PackedArrayChart;
import edu.ohsu.cslu.parser.fom.FigureOfMeritModel.FigureOfMerit;

/**
 * @author Aaron Dunlop
 * @since Jul 11, 2013
 */
public class BeamWidthSequence extends ConstituentBoundarySequence implements MulticlassSequence {

    /**
     * Gold classes from the training tree, one for each classified cell (those that participate in the 1-best parse
     * tree). Parallel array to {@link #cellIndices}. All other cells are considered to be class 0 (closed), and are
     * omitted from the data structure to conserve heap memory.
     */
    protected final short[] classes;

    /** Parallel array to {@link #classes}, denoting the cell indices of each classified cell */
    protected int[] cellIndices;

    /**
     * Predicted classes, one for each cell in the chart (note that this is many more than are stored in
     * {@link #classes}, so we must special-case comparisons when evaluating classification accuracy.)
     */
    protected short[] predictedClasses;

    // /**
    // * Constructs from an array of tokens, mapped according to the classifier's lexicon. Used during inference.
    // *
    // * @param mappedTokens
    // * @param posTags
    // * @param classifier
    // */
    // public BeamWidthSequence(final int[] mappedTokens, final short[] posTags,
    // final BeamWidthClassifier classifier) {
    //
    // super(mappedTokens, posTags, classifier.lexicon, classifier.decisionTreeUnkClassSet);
    // this.classBoundaryBeamWidths = classifier.classBoundaryBeamWidths;
    //
    // // this.mappedTokens = mappedTokens;
    // // this.sentenceLength = (short) mappedTokens.length;
    // // this.posTags = posTags;
    // //
    // // this.mappedUnkSymbols = new int[sentenceLength];
    // // for (int i = 0; i < sentenceLength; i++) {
    // // // TODO It's odd and inefficient to take mapped tokens and un-map them to their String representation, just
    // // // so we can re-map their UNK-classes.
    // // mappedUnkSymbols[i] = unkClassSet.getIndex(DecisionTreeTokenClassifier.berkeleyGetSignature(
    // // lexicon.getSymbol(mappedTokens[i]), i == 0, lexicon));
    // // }
    //
    // // All cells spanning more than one word
    // this.length = sentenceLength * (sentenceLength + 1) / 2 - sentenceLength;
    // this.classes = null;
    //
    // this.predictedClasses = new short[length];
    // Arrays.fill(predictedClasses, (short) -1);
    // }

    /**
     * Used during training. Constructs from a bracketed tree, populating {@link #classes} with open/closed
     * classifications for each chart cell.
     * 
     * @param parseTree
     * @param classifier
     */
    public BeamWidthSequence(final BinaryTree<String> parseTree, final BeamWidthClassifier classifier) {
        super(parseTree, classifier.lexicon, classifier.decisionTreeUnkClassSet, classifier.vocabulary);

        // tokens = parseTree.leafLabels();
        //
        // this.sentenceLength = (short) tokens.length;
        // this.mappedTokens = new int[sentenceLength];
        // this.mappedUnkSymbols = new int[sentenceLength];
        // this.posTags = new short[sentenceLength];
        //
        // // Map all tokens, UNK symbols, and parts-of-speech
        // for (int i = 0; i < sentenceLength; i++) {
        // if (lexicon.isFinalized()) {
        // mappedTokens[i] = lexicon.getIndex(tokens[i]);
        // mappedUnkSymbols[i] = unkClassSet.getIndex(DecisionTreeTokenClassifier.berkeleyGetSignature(tokens[i],
        // i == 0, lexicon));
        // } else {
        // mappedTokens[i] = lexicon.addSymbol(tokens[i]);
        // mappedUnkSymbols[i] = unkClassSet.addSymbol(DecisionTreeTokenClassifier.berkeleyGetSignature(tokens[i],
        // i == 0, lexicon));
        // }
        // }

        final ParseTask parseTask = classifier.ccParser.parseSentence(parseTree.toString());
        if (parseTask.parseFailed()) {
            throw new IllegalArgumentException("Parse failed");
        }
        final PackedArrayChart chart = classifier.ccParser.chart;

        //
        // Populate classes and predictedClasses for each open cell
        //
        final short[] tmpClasses = new short[sentenceLength * (sentenceLength + 1) / 2];
        Arrays.fill(tmpClasses, (short) -1);

        classifyCell(chart, (short) 0, (short) chart.size(), chart.sparseMatrixGrammar.startSymbol,
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

        final int i = 2;
        // // TODO Populate POS tags?
        // int start = 0;
        // for (final BinaryTree<String> node : parseTree.leafTraversal()) {
        // if (this.vocabulary.isFinalized()) {
        // posTags[start] = (short) vocabulary.getIndex(node.parentLabel());
        // } else {
        // posTags[start] = (short) vocabulary.addSymbol(node.parentLabel());
        // }
        // // Increment the start index every time we process a leaf
        // start++;
        // }
    }

    private void classifyCell(final PackedArrayChart chart, final short start, final short end, final short parent,
            final FigureOfMerit fom, final short[] beamWidthClasses, final short[] tmpClasses) {

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
            foms[i] = fom.calcFOM(start, end, nts[i], chart.insideProbabilities[j]);
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
            classifyCell(chart, start, chart.midpoints[bottomEntryIndex],
                    (short) chart.sparseMatrixGrammar.packingFunction
                            .unpackLeftChild(chart.packedChildren[bottomEntryIndex]), fom, beamWidthClasses, tmpClasses);
            classifyCell(chart, chart.midpoints[bottomEntryIndex], end,
                    chart.sparseMatrixGrammar.packingFunction.unpackRightChild(chart.packedChildren[bottomEntryIndex]),
                    fom, beamWidthClasses, tmpClasses);
        }
    }

    public final short goldClass(final int cellIndex) {
        // Find the cell index
        final int i = Arrays.binarySearch(cellIndices, cellIndex);
        return i >= 0 ? classes[i] : 0;
    }

    public void allocatePredictedClasses() {
        this.predictedClasses = new short[sentenceLength * (sentenceLength + 1) / 2];
    }

    public void clearPredictedClasses() {
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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(256);
        for (int i = 0; i < classes.length; i++) {
            sb.append(lexicon.getSymbol(mappedTokens[i]));
            sb.append(' ');
            sb.append(classes[i]);
            sb.append(')');

            if (i < (classes.length - 1)) {
                sb.append(' ');
            }
        }
        sb.append('\n');

        for (int i = 0; i < classes.length; i++) {
            final short[] startAndEnd = ConstituentBoundaryFeatureExtractor.startAndEnd(i, length, true);
            sb.append(String.format("%d,%d %s  ", startAndEnd[0], startAndEnd[1], classes[i]));
        }
        return sb.toString();
    }
}

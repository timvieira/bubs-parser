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

package edu.ohsu.cslu.parser.cellselector;

import it.unimi.dsi.fastutil.shorts.ShortArrayList;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;

import cltool4j.BaseLogger;
import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.datastructs.vectors.BitVector;
import edu.ohsu.cslu.datastructs.vectors.PackedBitVector;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.cellselector.CellSelector.ChainableCellSelector;
import edu.ohsu.cslu.parser.chart.Chart;
import edu.ohsu.cslu.perceptron.AdaptiveBeamClassifier;
import edu.ohsu.cslu.perceptron.AdaptiveBeamClassifier.UnaryConstraintSequence;
import edu.ohsu.cslu.perceptron.BeamWidthSequence;
import edu.ohsu.cslu.perceptron.MulticlassTagSequence;
import edu.ohsu.cslu.perceptron.Tagger;
import edu.ohsu.cslu.perceptron.UnaryConstraintClassifier;

/**
 * Implements 'Adaptive-beam' modeling, using a series of discriminative models to classify the beam width of each cell
 * as open or closed. The method is fully described in Bodenstab et al., 2011,
 * "Beam-Width Prediction for Efficient Context-Free Parsing". This class is a reimplementation of the original work,
 * and depends on a model trained with {@link AdaptiveBeamClassifier} (and the {@link Tagger} embedded therein for POS
 * tagging).
 * 
 * @see CompleteClosureModel
 * 
 * @author Aaron Dunlop
 * @since Jul 17, 2013
 */
public class AdaptiveBeamModel extends ChainableCellSelectorModel implements CellSelectorModel {

    private static final long serialVersionUID = 1L;

    private AdaptiveBeamClassifier classifier;
    private Tagger posTagger;

    private final static boolean FACTORED_ONLY_CONSTRAINTS_DISABLED = GlobalConfigProperties.singleton()
            .getBooleanProperty(ParserDriver.OPT_DISABLE_FACTORED_ONLY_CLASSIFIER, false);

    private final static boolean UNARY_CONSTRAINTS_DISABLED = GlobalConfigProperties.singleton().getBooleanProperty(
            ParserDriver.OPT_DISABLE_UNARY_CLASSIFIER, false);

    /**
     * Standard constructor
     * 
     * @param classifierModel
     * @param grammar
     * @param childModel
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public AdaptiveBeamModel(final File classifierModel, final Grammar grammar, final CellSelectorModel childModel)
            throws IOException, ClassNotFoundException {

        super(childModel);
        this.classifier = new AdaptiveBeamClassifier(grammar);
        classifier.readModel(new FileInputStream(classifierModel));
        this.posTagger = classifier.posTagger();
    }

    /**
     * Used when training a {@link AdaptiveBeamClassifier}
     * 
     * @param classifier
     */
    public AdaptiveBeamModel(final AdaptiveBeamClassifier classifier) {
        super(null);
        this.classifier = classifier;
        this.posTagger = classifier.posTagger();
    }

    public CellSelector createCellSelector() {
        return new BeamWidthSelector(childModel != null ? childModel.createCellSelector() : null);
    }

    public class BeamWidthSelector extends ChainableCellSelector {

        private short[] beamWidths;
        // private boolean[] factoredOnly;
        private short sentenceLength;

        private BitVector factoredOnly;
        private BitVector unariesDisallowed;

        public BeamWidthSelector(final CellSelector child) {
            super(child);
        }

        @Override
        public void initSentence(final ChartParser<?, ?> p, final ParseTask task) {
            super.initSentence(p, task);
            sentenceLength = (short) p.chart.size();

            final int cells = sentenceLength * (sentenceLength + 1) / 2;
            this.beamWidths = new short[cells];
            this.factoredOnly = new PackedBitVector(cells);

            final UnaryConstraintClassifier unaryConstraintClassifier = UNARY_CONSTRAINTS_DISABLED ? null : classifier
                    .unaryConstraintClassifier();
            // TODO We really only need to allocate a unary classification for each span-1 cell
            this.unariesDisallowed = unaryConstraintClassifier != null ? new PackedBitVector(cells) : null;

            // POS-tag the sentence with a discriminative tagger
            // TODO Use the same lexicon, tagSet, etc. We already have a mapped int[] representation of the sentence, we
            // shouldn't need to do it again. But this is safe to start with.
            final MulticlassTagSequence tagSequence = new MulticlassTagSequence(task.sentence, posTagger);
            task.posTags = posTagger.classify(tagSequence);

            // Classify each chart cell, in left-to-right, bottom-up order
            final ShortArrayList tmpCellIndices = new ShortArrayList(cells);

            final BeamWidthSequence sequence = new BeamWidthSequence(task.tokens, task.posTags, classifier);
            sequence.allocatePredictionStorage();

            final AdaptiveBeamClassifier.UnaryConstraintSequence unaryConstraintSequence = unaryConstraintClassifier != null ? new UnaryConstraintSequence(
                    task.tokens, unaryConstraintClassifier) : null;

            for (short span = 1; span <= sentenceLength; span++) {
                for (short start = 0; start < sentenceLength - span + 1; start++) {
                    final short end = (short) (start + span);
                    final int cellIndex = Chart.cellIndex(start, end, sentenceLength, false);

                    final BitVector featureVector = classifier.featureExtractor().featureVector(sequence, cellIndex);
                    final float[] dotProducts = classifier.dotProducts(featureVector);
                    final short beamClass = classifier.beamClass(dotProducts);
                    sequence.setPredictedClass(cellIndex, beamClass);
                    final short beamWidth = classifier.beamWidth(beamClass);
                    beamWidths[cellIndex] = beamWidth;

                    // All span-1 cells are open
                    if (span == 1 || beamWidth > 0) {
                        tmpCellIndices.add(start);
                        tmpCellIndices.add((short) (start + span));
                    }

                    // Classify unaries in span-1 cells
                    if (span == 1 && unaryConstraintClassifier != null) {
                        // TODO Index by start instead of cell index
                        unariesDisallowed.set(cellIndex,
                                unaryConstraintClassifier.classify(unaryConstraintSequence, start));
                    }

                    // Classify factored-only
                    if (span > 1 && beamWidth > 0) {
                        factoredOnly.set(cellIndex, classifier.factoredOnly(dotProducts));
                    }
                }
            }

            // Populate cellIndices with open cells
            this.cellIndices = tmpCellIndices.toShortArray();
            this.openCells = cellIndices.length / 2;

            if (BaseLogger.singleton().isLoggable(Level.FINE)) {
                BaseLogger.singleton().fine(
                        String.format("Sentence length: %d. Total cells: %d  Open cells: %d", sentenceLength, cells,
                                tmpCellIndices.size() / 2));

                if (BaseLogger.singleton().isLoggable(Level.FINER)) {
                    final StringBuilder sb = new StringBuilder(256);
                    final int[] counts = new int[classifier.classes()];
                    for (int cellIndex = 0; cellIndex < beamWidths.length; cellIndex++) {
                        counts[sequence.predictedClass(cellIndex)]++;
                    }
                    for (short i = 0; i < counts.length; i++) {
                        sb.append("Beam=" + classifier.beamWidth(i) + ": " + counts[i] + "   ");
                    }
                    BaseLogger.singleton().fine(sb.toString());
                }
            }
        }

        @Override
        public boolean isCellOpen(final short start, final short end) {
            if (childCellSelector != null && !childCellSelector.isCellOpen(start, end)) {
                return false;
            }

            return !constraintsEnabled
                    || (beamWidths[Chart.cellIndex(start, end, sentenceLength, false)] > 0 && !isCellOnlyFactored(
                            start, end));
        }

        @Override
        public boolean isCellOnlyFactored(final short start, final short end) {
            return !FACTORED_ONLY_CONSTRAINTS_DISABLED
                    && (!constraintsEnabled || factoredOnly.getBoolean(Chart.cellIndex(start, end, sentenceLength)));
        }

        @Override
        public boolean isUnaryOpen(final short start, final short end) {
            return UNARY_CONSTRAINTS_DISABLED || !constraintsEnabled
                    || !unariesDisallowed.getBoolean(Chart.cellIndex(start, end, sentenceLength));
        }

        @Override
        public int getBeamWidth(final int cellIndex) {
            return constraintsEnabled ? beamWidths[cellIndex] : Short.MAX_VALUE;
        }

        @Override
        public int getBeamWidth(final short start, final short end) {
            return constraintsEnabled ? beamWidths[Chart.cellIndex(start, end, sentenceLength, false)]
                    : Short.MAX_VALUE;
        }
    }
}

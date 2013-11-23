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
import edu.ohsu.cslu.datastructs.narytree.NaryTree.Binarization;
import edu.ohsu.cslu.datastructs.vectors.BitVector;
import edu.ohsu.cslu.datastructs.vectors.PackedBitVector;
import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.cellselector.CellSelector.ChainableCellSelector;
import edu.ohsu.cslu.parser.chart.Chart;
import edu.ohsu.cslu.perceptron.AdaptiveBeamClassifier;
import edu.ohsu.cslu.perceptron.AdaptiveBeamClassifier.UnaryConstraintSequence;
import edu.ohsu.cslu.perceptron.CompleteClosureClassifier;
import edu.ohsu.cslu.perceptron.CompleteClosureSequence;
import edu.ohsu.cslu.perceptron.MulticlassTagSequence;
import edu.ohsu.cslu.perceptron.Tagger;
import edu.ohsu.cslu.perceptron.UnaryConstraintClassifier;

/**
 * Implements 'Complete Closure', using a discriminative model to classify each cell as open or closed. The method is
 * fully described in Bodenstab et al., 2011, "Beam-Width Prediction for Efficient Context-Free Parsing". This class is
 * a reimplementation of the original work, and depends on a model trained with {@link CompleteClosureClassifier} (and
 * the {@link Tagger} embedded therein for POS tagging).
 * 
 * @see AdaptiveBeamModel
 * 
 * @author Aaron Dunlop
 * @since Feb 14, 2013
 */
public class CompleteClosureModel extends ChainableCellSelectorModel implements CellSelectorModel {

    private static final long serialVersionUID = 1L;

    private final static boolean UNARY_CONSTRAINTS_DISABLED = GlobalConfigProperties.singleton().getBooleanProperty(
            ParserDriver.OPT_DISABLE_UNARY_CLASSIFIER, false);

    private CompleteClosureClassifier classifier;
    private Tagger posTagger;

    /**
     * Standard constructor
     * 
     * @param classifierModel
     * @param childModel
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public CompleteClosureModel(final File classifierModel, final CellSelectorModel childModel) throws IOException,
            ClassNotFoundException {

        super(childModel);
        this.classifier = new CompleteClosureClassifier();
        classifier.readModel(new FileInputStream(classifierModel));
        this.posTagger = classifier.posTagger;
    }

    /**
     * Used when training a {@link AdaptiveBeamClassifier}
     * 
     * @param classifier
     */
    public CompleteClosureModel(final CompleteClosureClassifier classifier) {
        super(null);
        this.classifier = classifier;
        this.posTagger = classifier.posTagger;
    }

    public CellSelector createCellSelector() {
        return new CompleteClosureSelector(childModel != null ? childModel.createCellSelector() : null);
    }

    public Binarization binarization() {
        return classifier.binarization();
    }

    public class CompleteClosureSelector extends ChainableCellSelector {

        private BitVector unariesDisallowed;

        public CompleteClosureSelector(final CellSelector child) {
            super(child);
        }

        @Override
        public void initSentence(final ChartParser<?, ?> p, final ParseTask task) {
            super.initSentence(p, task);
            final short sentenceLength = (short) p.chart.size();

            final UnaryConstraintClassifier unaryConstraintClassifier = UNARY_CONSTRAINTS_DISABLED ? null : classifier
                    .unaryConstraintClassifier();
            this.unariesDisallowed = unaryConstraintClassifier != null ? new PackedBitVector(sentenceLength) : null;

            // POS-tag the sentence with a discriminative tagger
            // TODO Use the same lexicon, tagSet, etc. We already have a mapped int[] representation of the sentence, we
            // shouldn't need to do it again. But this is safe to start with.
            final MulticlassTagSequence tagSequence = new MulticlassTagSequence(task.sentence, posTagger);
            task.posTags = posTagger.classify(tagSequence);

            // Classify each chart cell, in left-to-right, bottom-up order
            final ShortArrayList tmpCellIndices = new ShortArrayList(sentenceLength * sentenceLength);

            // All span-1 cells are open
            for (short start = 0; start < sentenceLength; start++) {
                tmpCellIndices.add(start);
                tmpCellIndices.add((short) (start + 1));
            }

            // Classify span-1 cells for unary constraints
            if (unaryConstraintClassifier != null) {
                // The unary constraint classifier shares the POS-tagger's lexicon, so we'll use the token mappings from
                // the tagger sequence
                final AdaptiveBeamClassifier.UnaryConstraintSequence unaryConstraintSequence = new UnaryConstraintSequence(
                        tagSequence.mappedTokens(), unaryConstraintClassifier);

                for (short start = 0; start < sentenceLength; start++) {
                    unariesDisallowed.set(start, unaryConstraintClassifier.classify(unaryConstraintSequence, start));
                }
            }

            // Classify all span>1 cells as open or closed
            final CompleteClosureSequence sequence = task.inputTree != null ? new CompleteClosureSequence(
                    task.inputTree.binarize(task.grammar.grammarFormat, task.grammar.binarization()), classifier)
                    : new CompleteClosureSequence(task.tokens, task.posTags, classifier);

            for (short span = 2; span <= sentenceLength; span++) {
                for (short start = 0; start < sentenceLength - span + 1; start++) {

                    final boolean closed = classifier.classify(sequence,
                            Chart.cellIndex(start, start + span, sentenceLength, true));

                    if (!closed) {
                        tmpCellIndices.add(start);
                        tmpCellIndices.add((short) (start + span));
                    }
                }
            }

            // Populate cellIndices with open cells
            this.cellIndices = tmpCellIndices.toShortArray();
            this.openCells = cellIndices.length / 2;

            if (BaseLogger.singleton().isLoggable(Level.FINE)) {
                BaseLogger.singleton().fine(
                        String.format("Sentence length: %d. Total cells: %d  Open cells: %d", sentenceLength,
                                sentenceLength * (sentenceLength + 1) / 2, tmpCellIndices.size() / 2));
            }
        }

        @Override
        public boolean isUnaryOpen(final short start, final short end) {
            return UNARY_CONSTRAINTS_DISABLED || !constraintsEnabled || (end - start > 1) || unariesDisallowed == null
                    || !unariesDisallowed.getBoolean(start);
        }
    }
}

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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

import cltool4j.BaseLogger;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.SymbolSet;

/**
 * Unary constraint classifier, as described in Roark et al., 2012
 * "Finite-State Chart Constraints for Reduced Complexity Context-Free Parsing Pipelines". This implementation trains
 * and tests unary-constraint models only for span-1 cells (consistent with the approach in Roark et al.). We
 * experimented with classifying every open cell in the chart, but it appears that it's very difficult to correctly
 * classify cells spanning more than one word, reducing precision of positive classifications (and thus incorrectly
 * closing some cells to unaries).
 * 
 * Implementation note: we consider a 'true' binary classification to be a closed cell, so we generally target negative
 * recall (i.e., the number of open cells correctly classified as such). To limit inference failures, that
 * negative-recall target ({@link BinaryClassifier#targetNegativeRecall}) should be very high - e.g., .99-.999.
 */
public class UnaryConstraintClassifier extends BinaryClassifier<BinaryTagSequence> {

    private static final long serialVersionUID = 1L;

    SymbolSet<String> unigramSuffixSet;
    SymbolSet<String> bigramSuffixSet;

    /**
     * Constructs a UnaryConstraintClassifier using the various {@link SymbolSet}s from a {@link Grammar} instance (note
     * that the context-free rules of the {@link Grammar} are not used)
     * 
     * @param featureTemplates
     * @param grammar
     */
    public UnaryConstraintClassifier(final String featureTemplates, final Grammar grammar) {
        this(featureTemplates, grammar.lexSet, grammar.unkClassSet());
    }

    /**
     * Constructs a UnaryConstraintClassifier using pre-initialized {@link SymbolSet}s
     * 
     * @param featureTemplates
     * @param lexicon
     * @param unkClassSet
     */
    public UnaryConstraintClassifier(final String featureTemplates, final SymbolSet<String> lexicon,
            final SymbolSet<String> unkClassSet) {

        super(lexicon, unkClassSet);
        final SymbolSet<String> tagSet = new SymbolSet<String>();
        tagSet.addSymbol("F");
        tagSet.addSymbol("T");
        tagSet.defaultReturnValue(tagSet.addSymbol(Grammar.nullSymbolStr));

        this.unigramSuffixSet = new SymbolSet<String>();
        this.unigramSuffixSet.defaultReturnValue(Grammar.nullSymbolStr);

        this.bigramSuffixSet = new SymbolSet<String>();
        this.bigramSuffixSet.defaultReturnValue(Grammar.nullSymbolStr);

        this.featureExtractor = new BinaryTaggerFeatureExtractor(featureTemplates, lexicon, unkClassSet,
                nonterminalVocabulary, tagSet);
    }

    /**
     * @param trainingCorpusSequences
     * @param devCorpusSequences
     */
    public final void train(final ArrayList<BinaryTagSequence> trainingCorpusSequences,
            final ArrayList<BinaryTagSequence> devCorpusSequences) {
        //
        // Iterate over training corpus, training the model
        //
        for (int i = 1, j = 0; i <= trainingIterations; i++, j = 0) {

            for (final BinaryTagSequence sequence : trainingCorpusSequences) {
                // Train on all span-1 cells
                for (short start = 0; start < sequence.length; start++) {
                    train(sequence.goldClass(start), featureExtractor.featureVector(sequence, start));
                }

                progressBar(100, 5000, j++);
            }

            // Evaluate on the dev-set
            if (!devCorpusSequences.isEmpty()) {
                System.out.println();
                final edu.ohsu.cslu.perceptron.BinaryClassifier.BinaryClassifierResult result = classify(devCorpusSequences);
                BaseLogger.singleton().info(
                        String.format(
                                "Iteration=%d Devset Accuracy=%.2f  P=%.3f  R=%.3f  neg-P=%.3f  neg-R=%.3f  Time=%d\n",
                                i, result.accuracy() * 100f, result.precision() * 100f, result.recall() * 100f,
                                result.negativePrecision() * 100f, result.negativeRecall() * 100f, result.time));
            }
        }

        //
        // Search for a bias that satisfies the requested precision or recall
        //
        if (targetPrecision != 0) {
            precisionBiasSearch(devCorpusSequences, featureExtractor);
        } else if (targetNegativeRecall != 0) {
            super.negativeRecallBiasSearch(devCorpusSequences, featureExtractor);
            evaluateDevset(devCorpusSequences);
        }
    }

    /**
     * Overrides the superclass implementation to classify only open cells
     * 
     * @param sequences
     * @return results of classifying the input sequences (if they contain gold classifications)
     */
    @Override
    protected BinaryClassifierResult classify(final ArrayList<BinaryTagSequence> sequences) {

        final long t0 = System.currentTimeMillis();
        final BinaryClassifierResult result = new BinaryClassifierResult();

        for (final BinaryTagSequence sequence : sequences) {
            result.totalSequences++;

            sequence.allocatePredictionStorage();
            // Classify all span-1 cells
            for (short start = 0; start < sequence.length; start++) {
                classify(sequence, start, result);
            }
            sequence.clearPredictionStorage();
        }
        result.time = System.currentTimeMillis() - t0;
        return result;
    }

    final void evaluateDevset(final ArrayList<BinaryTagSequence> devCorpusSequences) {
        final long t0 = System.currentTimeMillis();
        final BinaryClassifierResult result = new BinaryClassifierResult();

        int sentencesWithMisclassifiedNegative = 0;

        for (final BinaryTagSequence sequence : devCorpusSequences) {
            result.totalSequences++;
            sequence.allocatePredictionStorage();
            boolean misclassifiedNegative = false;

            // Classify all span-1 cells
            for (short start = 0; start < sequence.length; start++) {
                classify(sequence, start, result);
                if (sequence.goldClass(start) == false && sequence.predictedClass(start) == true) {
                    misclassifiedNegative = true;
                }
            }

            if (misclassifiedNegative) {
                sentencesWithMisclassifiedNegative++;
            }
            sequence.clearPredictionStorage();
        }
        result.time = System.currentTimeMillis() - t0;

        // Compute and report final classification statistics on the development set
        final float sentenceNegativeRecall = (devCorpusSequences.size() - sentencesWithMisclassifiedNegative) * 1f
                / devCorpusSequences.size();

        BaseLogger.singleton().info(
                String.format("Cells classified positive (including span-1): %d/%d  %d correct (%.3f%% )",
                        result.classifiedPositive, result.positiveExamples, result.correctPositive,
                        result.correctPositive * 100f / result.positiveExamples));
        BaseLogger.singleton().info(
                String.format("Sentence-level recall (fraction with all open cells classified correctly): %.3f%%",
                        sentenceNegativeRecall * 100f));
    }

    @Override
    protected String DEFAULT_FEATURE_TEMPLATES() {
        return AdaptiveBeamClassifier.DEFAULT_FEATURE_TEMPLATES;
    }

    @Override
    protected final void train(final BufferedReader input) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected final void run() throws Exception {
        throw new UnsupportedOperationException();
    }
}
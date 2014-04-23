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

import java.util.ArrayList;
import java.util.Arrays;

import cltool4j.BaseLogger;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.datastructs.vectors.BitVector;
import edu.ohsu.cslu.grammar.SymbolSet;

/**
 * Trains and/or eveluated an averaged-perceptron ranking model.
 * 
 * @author Aaron Dunlop
 */
public abstract class Ranker<S extends MulticlassSequence, F extends FeatureExtractor<S>, I> extends
        MulticlassClassifier<S, F, I> {

    private static final long serialVersionUID = 1L;

    @Option(name = "-ps", metaVar = "size", usage = "Page size (if a paged MRR score is desired)")
    private int pageSize = 0;

    @Option(name = "-baseline", separator = ",", metaVar = "classes", usage = "Baseline (fixed) ordering. If specified, the ranking model is ignored, and the dev-set is instead evaluated using this fixed ordering.")
    private short[] baselineOrdering;

    public Ranker() {
        super();
    }

    public Ranker(final String featureTemplates, final SymbolSet<String> lexicon, final SymbolSet<String> unkClassSet,
            final SymbolSet<String> tagSet) {
        super(featureTemplates, lexicon, unkClassSet, tagSet);
    }

    /**
     * Copy-and-paste from {@link MulticlassClassifier}'s classification method, adapted to rank candidates (instead of
     * just selecting the 1-best) and return a {@link RankerResult}.
     * 
     * @param devCorpusSequences
     * @param devCorpusFeatures
     * @param result
     * @return
     */
    private RankerResult rank(final ArrayList<? extends MulticlassSequence> devCorpusSequences,
            final ArrayList<BitVector[]> devCorpusFeatures, RankerResult result) {

        if (result == null) {
            result = new RankerResult();
        }

        // Test the development set
        final long t0 = System.currentTimeMillis();

        if (baselineOrdering != null) {

        }

        // For debugging
        @SuppressWarnings("unused")
        int incorrect = 0;
        for (int j = 0; j < devCorpusFeatures.size(); j++) {
            result.sequences++;
            final MulticlassSequence sequence = devCorpusSequences.get(j);
            final BitVector[] featureVectors = devCorpusFeatures.get(j);

            for (int k = 0; k < featureVectors.length; k++) {
                if (featureVectors[k] != null) {
                    final short goldClass = sequence.goldClass(k);

                    final short[] ranking = baselineOrdering != null ? baselineOrdering : rank(featureVectors[k]);
                    sequence.setPredictedClass(k, ranking[0]);

                    if (ranking[0] == goldClass) {
                        result.correct++;
                        result.reciprocalRankSum += 1;
                        result.pagedReciprocalRankSum += 1;
                    } else {
                        incorrect++;

                        for (short i = 0; i < ranking.length; i++) {
                            if (ranking[i] == goldClass) {
                                result.reciprocalRankSum += 1f / i;
                                if (pageSize != 0) {
                                    result.pagedReciprocalRankSum += 1f / ((i / pageSize) + 1);
                                }
                            }
                        }
                    }
                    result.instances++;
                }
            }
            Arrays.fill(sequence.predictedClasses(), (short) 0);
        }
        result.time += (int) (System.currentTimeMillis() - t0);
        return result;
    }

    public short[] rank(final BitVector featureVector) {

        if (parallelArrayOffsetMap == null) {
            return perceptronModel.rank(featureVector);
        }

        return rank(dotProducts(featureVector));
    }

    /**
     * Adapted from {@link MulticlassClassifier#classify(float[])}.
     * 
     * @param dotProducts
     * @return Rankings of each class per the computed dot products.
     */
    protected short[] rank(final float[] dotProducts) {
        final short[] classes = new short[dotProducts.length];
        for (short i = 0; i < classes.length; i++) {
            classes[i] = i;
        }
        edu.ohsu.cslu.util.Arrays.sort(dotProducts, classes);
        edu.ohsu.cslu.util.Arrays.reverse(classes);
        return classes;
    }

    @Override
    protected RankerResult evaluateDevset(final ArrayList<S> devCorpusSequences,
            final ArrayList<BitVector[]> devCorpusFeatures, final int trainingIteration) {

        final RankerResult devResult = rank(devCorpusSequences, devCorpusFeatures, null);
        if (pageSize != 0) {
            BaseLogger.singleton().info(
                    String.format("Iteration=%d Devset 1-best accuracy=%.2f  MRR=%.2f  Paged MRR=%.2f  Time=%d\n",
                            trainingIteration, devResult.accuracy() * 100f, devResult.meanReciprocalRank(),
                            devResult.pagedMeanReciprocalRank(), devResult.time));
        } else {
            BaseLogger.singleton().info(
                    String.format("Iteration=%d Devset 1-best accuracy=%.2f  MRR=%.2f  Time=%d\n", trainingIteration,
                            devResult.accuracy() * 100f, devResult.meanReciprocalRank(), devResult.time));
        }
        return devResult;
    }

    /**
     * Accumulates ranking results - MRR and a paged version thereof
     */
    protected static class RankerResult extends MulticlassClassifierResult {

        private float reciprocalRankSum, pagedReciprocalRankSum;

        public RankerResult() {
        }

        public RankerResult(final int sequences, final int instances, final int correct, final int time) {
            super(sequences, instances, correct, time);
        }

        public float meanReciprocalRank() {
            return reciprocalRankSum / instances;
        }

        public float pagedMeanReciprocalRank() {
            return pagedReciprocalRankSum / instances;
        }
    }
}
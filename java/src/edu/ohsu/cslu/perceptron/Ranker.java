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

package edu.ohsu.cslu.perceptron;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Level;

import cltool4j.BaseLogger;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.datastructs.matrices.IntMatrix;
import edu.ohsu.cslu.datastructs.vectors.BitVector;
import edu.ohsu.cslu.datastructs.vectors.DenseIntVector;
import edu.ohsu.cslu.datastructs.vectors.IntVector;
import edu.ohsu.cslu.util.MutableEnumeration;

/**
 * Trains and/or evaluates an averaged-perceptron ranking model. Computes 1-best accuracy and mean reciprocal rank
 * (MRR). If a page size is specified (with the '-ps' option), a paged version of MRR is also computed.
 * 
 * @author Aaron Dunlop
 */
public abstract class Ranker<S extends MulticlassSequence, F extends FeatureExtractor<S>, I> extends
        MulticlassClassifier<S, F, I> {

    private static final long serialVersionUID = 1L;

    @Option(name = "-ps", metaVar = "size", usage = "Page size (if a paged MRR score is desired)")
    private int pageSize = 0;

    @Option(name = "-baseline", separator = ",", optionalChoiceGroup = "templates", metaVar = "classes", usage = "Classes to use in fixed or random baseline ordering. If specified, the ranking model is ignored, and the dev-set is instead evaluated using this set of classes (in the order specified, or in a random order if '-ri' is also specified).")
    private short[] baselineClasses;

    @Option(name = "-ri", requires = "-baseline", metaVar = "iterations", usage = "Random baseline ordering. If specified, the ranking model is ignored, and the dev-set is evaluated <iterations> times using a uniform random ranking.")
    private int randomIterations;

    /** Source of randomness for {@link #randomRanking()} */
    private Random random = new Random(System.currentTimeMillis());

    public Ranker() {
        super();
    }

    public Ranker(final String featureTemplates, final MutableEnumeration<String> lexicon,
            final MutableEnumeration<String> unkClassSet, final MutableEnumeration<String> tagSet) {
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
            result = createResult();
        }

        // Test the development set
        final long t0 = System.currentTimeMillis();

        // 'incorrect' is for debugging, and unused
        @SuppressWarnings("unused")
        final int incorrect = 0;
        for (int j = 0; j < devCorpusFeatures.size(); j++) {
            result.addSequence();
            final MulticlassSequence sequence = devCorpusSequences.get(j);
            final BitVector[] featureVectors = devCorpusFeatures.get(j);

            for (int k = 0; k < featureVectors.length; k++) {
                if (featureVectors[k] != null) {
                    final short goldClass = sequence.goldClass(k);

                    final short[] ranking;
                    if (randomIterations > 0) {
                        ranking = randomRanking();
                    } else if (baselineClasses != null) {
                        ranking = baselineClasses;
                    } else {
                        ranking = rank(featureVectors[k]);
                    }
                    sequence.setPredictedClass(k, ranking[0]);

                    final int goldClassRank = edu.ohsu.cslu.util.Arrays.linearSearch(ranking, goldClass);
                    result.addInstance(goldClass, ranking[0], goldClassRank, pageSize, sequence.ordinalValue());
                }
            }
            Arrays.fill(sequence.predictedClasses(), (short) 0);
        }
        result.addTime((int) (System.currentTimeMillis() - t0));
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
        final short[] classes = new short[tagSet.size()];
        for (short i = 0; i < classes.length; i++) {
            classes[i] = i;
        }
        edu.ohsu.cslu.util.Arrays.reverseSort(dotProducts, classes);
        return classes;
    }

    protected short[] randomRanking() {

        final short[] classes = baselineClasses.clone();

        // Fisher-Yates shuffle (adapted from http://stackoverflow.com/questions/1519736/random-shuffling-of-an-array)
        // java.util includes a method for Collections, but not for arrays
        for (int i = classes.length - 1; i > 0; i--) {
            final int index = random.nextInt(i + 1);
            final short tmp = classes[index];
            classes[index] = classes[i];
            classes[i] = tmp;
        }

        return classes;
    }

    @Override
    protected RankerResult evaluateDevset(final ArrayList<S> devCorpusSequences,
            final ArrayList<BitVector[]> devCorpusFeatures, final int trainingIteration) {

        RankerResult devResult = rank(devCorpusSequences, devCorpusFeatures, null);

        // If we're doing random ordering, repeat the random sample several more times and accumulate the results
        for (int i = 1; i < randomIterations; i++) {
            devResult = devResult.sum(rank(devCorpusSequences, devCorpusFeatures, null));
        }
        BaseLogger.singleton().info(
                String.format("Iteration=%d Devset %s  Time=%d\n", trainingIteration, devResult.toString(),
                        devResult.time()));

        if (BaseLogger.singleton().isLoggable(Level.FINE)) {
            BaseLogger.singleton().fine(devResult.resultByClassReport());
        }

        return devResult;
    }

    @Override
    protected RankerResult createResult() {
        return new RankerResult(tagSet);
    }

    /**
     * Accumulates ranking results - MRR and a paged version thereof
     */
    protected static class RankerResult extends MulticlassClassifierResult {

        private int onPage1;
        private final IntVector onPage1ByClass;
        private final Object2IntOpenHashMap<String> onPage1ByLabel = new Object2IntOpenHashMap<>();
        private float reciprocalRankSum, pagedReciprocalRankSum;

        public RankerResult(final MutableEnumeration<String> tagSet) {
            super(tagSet);
            this.onPage1ByClass = new DenseIntVector(tagSet.size());
        }

        public RankerResult(final MutableEnumeration<String> tagSet, final int sequences, final int instances,
                final int correct, final int onPage1, final IntVector instancesByClass, final IntVector correctByClass,
                final IntVector onPage1ByClass, final IntMatrix confusionMatrix,
                final Object2IntOpenHashMap<String> instancesByLabel,
                final Object2IntOpenHashMap<String> correctByLabel, final float reciprocalRankSum,
                final float pagedReciprocalRankSum, final int time) {

            super(tagSet, sequences, instances, correct, instancesByClass, correctByClass, confusionMatrix,
                    instancesByLabel, correctByLabel, time);
            this.onPage1 = onPage1;
            this.onPage1ByClass = onPage1ByClass;
            this.reciprocalRankSum = reciprocalRankSum;
            this.pagedReciprocalRankSum = pagedReciprocalRankSum;
        }

        /**
         * 
         * @param goldClass
         * @param predictedClass
         * @param goldClassRank Rank of the gold class, 0-indexed
         * @param pageSize
         * @param ordinalLabel A label of the instance (possibly, but not necessary, one used by one or more features).
         *            If non-null, accuracies will be accumulated by label and available for reporting
         */
        public void addInstance(final short goldClass, final short predictedClass, final int goldClassRank,
                final int pageSize, final String ordinalLabel) {

            super.addInstance(goldClass, predictedClass, ordinalLabel);
            reciprocalRankSum += 1f / (goldClassRank + 1);

            if (pageSize > 0) {
                pagedReciprocalRankSum += 1f / ((goldClassRank / pageSize) + 1);

                if (goldClassRank < pageSize) {
                    onPage1++;
                    onPage1ByClass.set(goldClass, onPage1ByClass.getInt(goldClass) + 1);
                    if (ordinalLabel != null) {
                        onPage1ByLabel.put(ordinalLabel, onPage1ByLabel.getInt(ordinalLabel) + 1);
                    }
                }
            }
        }

        public float meanReciprocalRank() {
            return reciprocalRankSum / instances;
        }

        public float pagedMeanReciprocalRank() {
            return pagedReciprocalRankSum / instances;
        }

        /**
         * @return The portion of all instances which were ranked on the first page of search results. Only valid if
         *         {@link Ranker#pageSize} is set.
         */
        public float page1Accuracy() {
            return onPage1 * 1f / instances;
        }

        public float page1Accuracy(final int goldClass) {
            return onPage1ByClass.getFloat(goldClass) / instancesByClass.getFloat(goldClass);
        }

        public float page1Accuracy(final String label) {
            return onPage1ByLabel.getInt(label) * 1f / instancesByLabel.getInt(label);
        }

        @Override
        public RankerResult sum(final MulticlassClassifierResult other) {
            final RankerResult rr = (RankerResult) other;

            return new RankerResult(tagSet, sequences + rr.sequences, instances + rr.instances, correct + rr.correct,
                    onPage1 + rr.onPage1, (IntVector) instancesByClass.add(other.instancesByClass),
                    (IntVector) correctByClass.add(other.correctByClass),
                    (IntVector) onPage1ByClass.add(rr.onPage1ByClass),
                    (IntMatrix) confusionMatrix.add(rr.confusionMatrix), sum(instancesByLabel, other.instancesByLabel),
                    sum(correctByLabel, other.correctByLabel), reciprocalRankSum + rr.reciprocalRankSum,
                    pagedReciprocalRankSum + rr.pagedReciprocalRankSum, time + other.time);
        }

        @Override
        public String resultByClassReport() {

            final StringBuilder sb = new StringBuilder(512);

            // If we counted accuracy by an ordinal label, report that breakdown
            if (!instancesByLabel.isEmpty()) {

                for (final String label : instancesByLabel.keySet()) {
                    if (pagedReciprocalRankSum > 0) {
                        sb.append(String.format(
                                "%-20s:   Instances: %5d  1-best=%5d  1-best accuracy=%.2f  Page-1 accuracy=%.2f\n",
                                label, instancesByLabel.getInt(label), correctByLabel.getInt(label), accuracy(label),
                                page1Accuracy(label)));
                    } else {
                        sb.append(String.format(
                                "%-20s (Class %d):   Instances: %5d  1-best=%5d  1-best accuracy=%.2f\n", label,
                                instancesByLabel.getInt(label), correctByLabel.getInt(label), accuracy(label)));
                    }
                }

            } else {
                // Report by gold class
                for (short goldClass = 0; goldClass < instancesByClass.length(); goldClass++) {
                    if (instancesByClass.getInt(goldClass) > 0) {
                        if (pagedReciprocalRankSum > 0) {
                            sb.append(String
                                    .format("%-20s (Class %d):   Instances: %5d  1-best=%5d  1-best accuracy=%.2f  Page-1 accuracy=%.2f\n",
                                            tagSet.getSymbol(goldClass), goldClass, instancesByClass.getInt(goldClass),
                                            correctByClass.getInt(goldClass), accuracy(goldClass),
                                            page1Accuracy(goldClass)));
                        } else {
                            sb.append(String.format(
                                    "%-20s (Class %d):   Instances: %5d  1-best=%5d  1-best accuracy=%.2f\n",
                                    tagSet.getSymbol(goldClass), goldClass, instancesByClass.getInt(goldClass),
                                    correctByClass.getInt(goldClass), accuracy(goldClass)));
                        }
                    }
                }
            }

            return sb.toString();
        }

        @Override
        public String toString() {
            if (onPage1 != 0) {
                return String.format("1-best accuracy=%.2f  Page-1 accuracy=%.2f  MRR=%.2f  Paged MRR=%.2f",
                        accuracy() * 100f, page1Accuracy() * 100f, meanReciprocalRank(), pagedMeanReciprocalRank());
            }
            return String.format("1-best accuracy=%.2f  MRR=%.2f\n", accuracy() * 100f, meanReciprocalRank());
        }
    }
}
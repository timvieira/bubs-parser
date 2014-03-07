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

package edu.berkeley.nlp.PCFGLA;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cltool4j.BaseLogger;
import cltool4j.GlobalConfigProperties;
import edu.berkeley.nlp.PCFGLA.GrammarMerger.MergeCandidate;
import edu.berkeley.nlp.PCFGLA.GrammarMerger.MergeRanking.MergeObjectiveFunction;
import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.parser.fom.BoundaryPosModel;

/**
 * Evaluates sets of merge candidates in combination; usually a set sampled from the 'middle' of the ranking by
 * estimated likelihood loss. Although this class resides in the {@link MergeObjectiveFunction} hierarchy, and shares
 * much infrastructure with {@link DiscriminativeMergeObjectiveFunction}, it does <em>not</em> implement
 * {@link MergeObjectiveFunction#initMergeCandidates(java.util.List, double[][], float)}, and thus must be handled as a
 * special case in
 * {@link GrammarMerger#selectMergePairs(Grammar, Lexicon, double[][], float[][][], int[][][], float, edu.berkeley.nlp.PCFGLA.GrammarMerger.MergeRanking, int, float)}
 * .
 * 
 * @author Aaron Dunlop
 */
public class SamplingMergeObjective extends InferenceInformedMergeObjectiveFunction {

    private final static String PROPERTY_SAMPLING_ITERATIONS = "sampleIterations";

    @Override
    public void initMergeCandidates(final List<MergeCandidate> mergeCandidates,
            final double[][] substateConditionalProbabilities, final float minimumRuleProbability) {
        throw new UnsupportedOperationException("Not supported by " + getClass().getName());
    }

    public List<MergeCandidate> sample(final List<MergeCandidate> mergeCandidates,
            final double[][] substateConditionalProbabilities, final float mergeFraction) {

        // The set we'll sample from is the percentage we consider parsing (set by a command-line hyperparameter)
        final int toSampleFrom = Math.round(mergeCandidates.size() * PARSE_FRACTION);

        // Sample uniformly from the 'middle' of the likelihood estimate range
        final int sampleRangeStart = (mergeCandidates.size() - toSampleFrom) / 2;
        final int sampleRangeEnd = sampleRangeStart + toSampleFrom;
        final List<MergeCandidate> middleCandidates = mergeCandidates.subList(sampleRangeStart, sampleRangeEnd);

        // The size of each sample is the number of merge candidates to re-merge
        final int mergeCount = Math.round(mergeCandidates.size() * mergeFraction);
        final int sampleSize = mergeCount - sampleRangeStart;
        if (sampleSize <= 0) {
            throw new IllegalArgumentException("Illegal sample size");
        }

        // We know we'll merge the candidates from the bottom of the list
        final List<MergeCandidate> knownMergeTargets = mergeCandidates.subList(0, sampleRangeStart);

        final int SAMPLING_ITERATIONS = GlobalConfigProperties.singleton().getIntProperty(PROPERTY_SAMPLING_ITERATIONS,
                10);
        final List<ScoredSample> scoredSamples = new ArrayList<ScoredSample>();

        for (int i = 0; i < SAMPLING_ITERATIONS; i++) {

            Collections.shuffle(middleCandidates);

            // Sample from the 'middle' candidates, and add the ones we know we'll merge (we might as well merge those
            // as well here, to better represent performance with the post-merge grammar) Note: we need to manually copy
            // the list here instead of using subList(), because we're re-shuffling the same list on each sampling
            // iteration, thus modifying the underlying list.
            final List<MergeCandidate> sample = new ArrayList<MergeCandidate>();
            for (int j = 0; j < sampleSize; j++) {
                sample.add(middleCandidates.get(j));
            }
            sample.addAll(knownMergeTargets);

            //
            // Merge the candidate substates
            //
            // Note: The merge function mutates the lexicon in place, so we need to pass in a clone of the lexicon (with
            // at least the mutable lexicon state copied)
            final Lexicon mergedLexicon = splitLexicon.shallowClone();
            final Grammar mergedGrammar = GrammarMerger.merge(splitGrammar, mergedLexicon,
                    GrammarMerger.mergePairs(sample, splitGrammar), substateConditionalProbabilities);
            mergedGrammar.computePairsOfUnaries(true);

            // TODO Do we need to do this? (Copied from GrammarTrainer...)
            // // Retrain lexicon to finish the lexicon merge (updates the unknown-word model)
            // mergedLexicon = new Lexicon(mergedGrammar.numSubStates, mergedLexicon.getSmoothingParams(),
            // mergedLexicon.getSmoother(),
            // !trainingCorpusIncludesUnks, mergedLexicon.getMinRuleProbability());
            // final ArrayParser parser = new ArrayParser(mergedGrammar, mergedLexicon);
            // emIteration(parser, grammar, maxLexicon, null, lexicon, trainStateSetTrees, rareWordThreshold);
            // // remove the unlikely tags
            // lexicon.removeUnlikelyTags(lexicon.minRuleProbability, -1.0);

            // Convert the grammar to BUBS sparse-matrix format and train a Boundary POS FOM
            final long t0 = System.currentTimeMillis();
            final LeftCscSparseMatrixGrammar sparseMatrixGrammar = convertGrammarToSparseMatrix(mergedGrammar,
                    mergedLexicon);
            final BoundaryPosModel posFom = trainPosFom(sparseMatrixGrammar);

            final long t1 = System.currentTimeMillis();

            // Parse the development set using the complete-closure model and lexical FOM
            final float[] parseResult = parseDevSet(sparseMatrixGrammar, posFom, beamWidth);
            final long t2 = System.currentTimeMillis();

            final float accuracyDelta = parseResult[0] - splitF1;
            final float efficiencyDelta = parseResult[1] - splitSpeed;

            scoredSamples.add(new ScoredSample(i, sample, accuracyDelta, efficiencyDelta));

            BaseLogger
                    .singleton()
                    .info(String
                            .format("Sample %d; merged %d candidates : Training time: %d ms  F1 = %.3f (%.3f)  Speed = %.3f (%.3f)  Parse time: %d ms",
                                    i, sample.size(), t1 - t0, parseResult[0] * 100, accuracyDelta * 100,
                                    parseResult[1], efficiencyDelta, t2 - t1));
        }

        // Sort and assign ordinal rankings to samples by F1 and inference speed
        Collections.sort(scoredSamples, ScoredSample.accuracyComparator());
        for (int i = 0; i < scoredSamples.size(); i++) {
            scoredSamples.get(i).accuracyRanking = i;
        }

        Collections.sort(scoredSamples, ScoredSample.efficiencyComparator());
        for (int i = 0; i < scoredSamples.size(); i++) {
            scoredSamples.get(i).efficiencyRanking = i;
        }

        Collections.sort(scoredSamples, ScoredSample.combinedRankingComparator());

        final ScoredSample selectedSample = scoredSamples.get(scoredSamples.size() - 1);
        BaseLogger.singleton().info(
                String.format("Selected sample %d; F1 = %.3f (%.3f)  Speed = %.3f w/s (%.3f)",
                        selectedSample.sampleIndex, (splitF1 + selectedSample.accuracyDelta) * 100,
                        selectedSample.accuracyDelta * 100, splitSpeed + selectedSample.efficiencyDelta,
                        selectedSample.efficiencyDelta));
        return selectedSample.mergeCandidates;
    }

    /**
     * Represents a sample of {@link MergeCandidate}s and the scores obtained when parsing with that sample
     */
    private static class ScoredSample {

        private final int sampleIndex;
        private final List<MergeCandidate> mergeCandidates;

        /**
         * Difference from performance with fully split grammar (generally negative, since merging will usually degrade
         * accuracy); the best ranking sample will have the highest {@link #accuracyDelta}.
         */
        private final float accuracyDelta;

        /**
         * Difference from performance with fully split grammar (generally positive, since merging will usually improve
         * efficiency); the best ranking sample will have the highest {@link #efficiencyDelta}.
         */
        private final float efficiencyDelta;

        private int accuracyRanking, efficiencyRanking;

        public ScoredSample(final int sampleIndex, final List<MergeCandidate> mergeCandidates,
                final float accuracyDelta, final float efficiencyDelta) {
            this.sampleIndex = sampleIndex;
            this.mergeCandidates = mergeCandidates;
            this.accuracyDelta = accuracyDelta;
            this.efficiencyDelta = efficiencyDelta;
        }

        public static Comparator<ScoredSample> accuracyComparator() {
            return new Comparator<ScoredSample>() {

                @Override
                public int compare(final ScoredSample o1, final ScoredSample o2) {
                    // Sort the best candidates (the highest accuracy delta) to the end of the list
                    return Float.compare(o1.accuracyDelta, o2.accuracyDelta);
                }
            };
        }

        public static Comparator<ScoredSample> efficiencyComparator() {
            return new Comparator<ScoredSample>() {

                @Override
                public int compare(final ScoredSample o1, final ScoredSample o2) {
                    // Sort the best candidates (the highest efficiency delta) to the end of the list
                    return Float.compare(o1.efficiencyDelta, o2.efficiencyDelta);
                }
            };
        }

        public static Comparator<ScoredSample> combinedRankingComparator() {
            return new Comparator<ScoredSample>() {

                @Override
                public int compare(final ScoredSample o1, final ScoredSample o2) {
                    // We can usually store global config properties in final statics, but that breaks down for unit
                    // tests which modify the property from test to test. It's a little more expensive to access it
                    // here, but we don't compare MergeCandidates that often, so it shouldn't be too expensive even when
                    // sorting.
                    final float EFFICIENCY_LAMBDA = GlobalConfigProperties.singleton().getFloatProperty(
                            "efficiencyLambda", 0);

                    final float sample1Score = o1.accuracyRanking * (1 - EFFICIENCY_LAMBDA) + o1.efficiencyRanking
                            * EFFICIENCY_LAMBDA;
                    final float sample2Score = o2.accuracyRanking * (1 - EFFICIENCY_LAMBDA) + o2.efficiencyRanking
                            * EFFICIENCY_LAMBDA;

                    return Float.compare(sample1Score, sample2Score);
                }
            };
        }
    }
}

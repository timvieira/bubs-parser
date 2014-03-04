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

import java.util.Collections;
import java.util.List;

import cltool4j.BaseLogger;
import edu.berkeley.nlp.PCFGLA.GrammarMerger.MergeCandidate;
import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.parser.fom.BoundaryPosModel;

/**
 * Performs pruned inference on a development set using each {@link MergeCandidate}, and ranks the candidates by a
 * combination of those two factors (Note that in this case, {@link MergeCandidate#estimatedAccuracyRanking()} and
 * {@link MergeCandidate#estimatedSpeedRanking()} are determined empirically and not in fact estimates).
 * 
 * @author Aaron Dunlop
 */
public class DiscriminativeMergeObjectiveFunction extends InferenceInformedMergeObjectiveFunction {

    @Override
    public void initMergeCandidates(final List<MergeCandidate> mergeCandidates,
            final double[][] substateConditionalProbabilities, final float minimumRuleProbability) {

        final int totalMergeCandidates = mergeCandidates.size();

        // Assign estimated speed rankings to be the same as estimated accuracy rankings (already populated by log
        // likelihood)
        for (final MergeCandidate mc : mergeCandidates) {
            mc.setEstimatedSpeedRanking(mc.estimatedAccuracyRanking());
        }

        for (final MergeCandidate mergeCandidate : mergeCandidates) {
            // Skip inference for a portion of the merge candidates by estimated likelihood-loss. Return infinite merge
            // cost for those with the greatest likelihood loss and negative-infinite cost for those with the least.
            if (PARSE_FRACTION < 1) {
                final int toParse = Math.round(totalMergeCandidates * PARSE_FRACTION);

                if (mergeCandidate.estimatedAccuracyRanking() < totalMergeCandidates / 2 - toParse / 2) {
                    mergeCandidate.estimatedAccuracyDelta = Float.NEGATIVE_INFINITY;
                    mergeCandidate.estimatedInferenceSpeedDelta = Float.NEGATIVE_INFINITY;
                    continue;

                } else if (mergeCandidate.estimatedAccuracyRanking() > totalMergeCandidates / 2 + toParse / 2) {
                    mergeCandidate.estimatedAccuracyDelta = Float.POSITIVE_INFINITY;
                    mergeCandidate.estimatedInferenceSpeedDelta = Float.POSITIVE_INFINITY;
                    continue;
                }
            }

            final String sState = NUMBERER.symbol(mergeCandidate.state);

            // Merge the candidate substates
            final Grammar mergedGrammar = splitGrammar.merge(mergeCandidate, substateConditionalProbabilities);
            final Lexicon mergedLexicon = splitLexicon.merge(mergeCandidate);

            // Convert the grammar to BUBS sparse-matrix format and train a Boundary POS FOM
            final long t0 = System.currentTimeMillis();
            final LeftCscSparseMatrixGrammar sparseMatrixGrammar = convertGrammarToSparseMatrix(mergedGrammar,
                    mergedLexicon);
            final BoundaryPosModel posFom = trainPosFom(sparseMatrixGrammar);

            final long t1 = System.currentTimeMillis();

            // Parse the development set using the complete-closure model and lexical FOM
            final float[] parseResult = parseDevSet(sparseMatrixGrammar, posFom, beamWidth);
            final long t2 = System.currentTimeMillis();

            mergeCandidate.estimatedAccuracyDelta = splitF1 - parseResult[0];
            mergeCandidate.estimatedInferenceSpeedDelta = splitSpeed - parseResult[1];

            BaseLogger
                    .singleton()
                    .info(String
                            .format("Testing merge of %s_%d and %s_%d : Training time: %d ms  F1 = %.3f (%.3f)  Speed = %.3f (%.3f)  Parse time: %d ms",
                                    sState, mergeCandidate.substate1, sState, mergeCandidate.substate2, t1 - t0,
                                    parseResult[0] * 100, mergeCandidate.estimatedAccuracyDelta * -100, parseResult[1],
                                    -mergeCandidate.estimatedInferenceSpeedDelta, t2 - t1));
        }

        // Sort and assign ordinal rankings by F1 and inference speed
        Collections.sort(mergeCandidates, MergeCandidate.estimatedAccuracyComparator());
        for (int i = 0; i < mergeCandidates.size(); i++) {
            mergeCandidates.get(i).setEstimatedAccuracyRanking(i);
        }

        Collections.sort(mergeCandidates, MergeCandidate.estimatedSpeedComparator());
        for (int i = 0; i < mergeCandidates.size(); i++) {
            mergeCandidates.get(i).setEstimatedSpeedRanking(i);
        }
    }
}

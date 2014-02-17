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

import cltool4j.GlobalConfigProperties;
import edu.berkeley.nlp.PCFGLA.GrammarMerger.MergeCandidate;
import edu.berkeley.nlp.PCFGLA.GrammarMerger.MergeRanking.MergeObjectiveFunction;

/**
 * 
 * @author Aaron Dunlop
 */
public class ModeledMergeObjectiveFunction extends MergeObjectiveFunction {

    /** Linear model coefficients for factors affecting efficiency */
    private final static float BINARY_RULE_COUNT_COEFFICIENT = GlobalConfigProperties.singleton().getFloatProperty(
            "binaryRuleCoefficient", 0);
    private final static float UNARY_RULE_COUNT_COEFFICIENT = GlobalConfigProperties.singleton().getFloatProperty(
            "unaryRuleCoefficient", 0);
    private final static float V_POS_COEFFICIENT = GlobalConfigProperties.singleton().getFloatProperty(
            "vPosCoefficient", 0);
    private final static float V_PHRASE_COEFFICIENT = GlobalConfigProperties.singleton().getFloatProperty(
            "vPhraseCoefficient", 0);
    private final static float MEAN_POS_LEXICAL_CHILDREN_COEFFICIENT = GlobalConfigProperties.singleton()
            .getFloatProperty("meanPosLexicalChildrenCoefficient", 0);
    private final static float MED_ROW_DENSITY_COEFFICIENT = GlobalConfigProperties.singleton().getFloatProperty(
            "medRowDensityCoefficient", 0);
    private final static float MED_COLUMN_DENSITY_COEFFICIENT = GlobalConfigProperties.singleton().getFloatProperty(
            "medColDensityCoefficient", 0);

    private Grammar splitGrammar;
    private Lexicon splitLexicon;

    private float splitGrammarMedianRowDensity, splitGrammarMedianColumnDensity;

    @Override
    public void initMergeCycle(final Grammar grammar, final Lexicon lexicon, final int cycle) {
        this.splitGrammar = grammar;
        this.splitLexicon = lexicon;

        final float[] rowAndColumnDensities = grammar.medianRowAndColumnDensities();
        this.splitGrammarMedianRowDensity = rowAndColumnDensities[0];
        this.splitGrammarMedianColumnDensity = rowAndColumnDensities[1];
    }

    @Override
    public void initMergeCandidates(final List<MergeCandidate> mergeCandidates,
            final double[][] substateConditionalProbabilities, final float minimumRuleProbability) {

        for (final MergeCandidate mergeCandidate : mergeCandidates) {

            // Merge the candidate substates and compute row and column density deltas
            final Grammar mergedGrammar = splitGrammar.merge(mergeCandidate, substateConditionalProbabilities);
            final float[] medianRowAndColumnDensities = mergedGrammar.medianRowAndColumnDensities();

            float posLexicalChildTerm = 0;
            if (MEAN_POS_LEXICAL_CHILDREN_COEFFICIENT != 0) {
                final Lexicon mergedLexicon = splitLexicon.merge(mergeCandidate);
                posLexicalChildTerm = MEAN_POS_LEXICAL_CHILDREN_COEFFICIENT
                        * mergedLexicon.meanPosLexicalChildren(minimumRuleProbability);
            }

            mergeCandidate.estimatedInferenceSpeedDelta = BINARY_RULE_COUNT_COEFFICIENT
                    * mergeCandidate.binaryRuleCountDelta + UNARY_RULE_COUNT_COEFFICIENT
                    * mergeCandidate.unaryRuleCountDelta + V_POS_COEFFICIENT * mergeCandidate.vPosDelta
                    + V_PHRASE_COEFFICIENT * mergeCandidate.vPhraseDelta + MED_ROW_DENSITY_COEFFICIENT
                    * (medianRowAndColumnDensities[0] - splitGrammarMedianRowDensity) + MED_COLUMN_DENSITY_COEFFICIENT
                    * (medianRowAndColumnDensities[1] - splitGrammarMedianColumnDensity) + posLexicalChildTerm;
        }

        // Estimated accuracy ranking is already populated by likelihood loss. Populate estimated efficiency ranking by
        // a modeled estimate using the correlation coefficients from a linear regression model.

        // Sort and assign ordinal rankings for estimated inference speed
        Collections.sort(mergeCandidates, MergeCandidate.estimatedSpeedComparator());
        for (int i = 0; i < mergeCandidates.size(); i++) {
            mergeCandidates.get(i).setEstimatedSpeedRanking(i);
        }
    }
}

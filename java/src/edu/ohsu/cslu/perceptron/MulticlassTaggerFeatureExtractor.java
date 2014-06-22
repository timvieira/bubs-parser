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

import edu.ohsu.cslu.datastructs.vectors.BitVector;
import edu.ohsu.cslu.datastructs.vectors.LargeSparseBitVector;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.util.MutableEnumeration;
import edu.ohsu.cslu.util.Strings;

/**
 * @author Aaron Dunlop
 * @since Jul 25, 2013
 */
public class MulticlassTaggerFeatureExtractor extends TaggerFeatureExtractor<MulticlassTagSequence> {

    private static final long serialVersionUID = 1L;

    public MulticlassTaggerFeatureExtractor(final String featureTemplates, final MutableEnumeration<String> lexicon,
            final MutableEnumeration<String> unkClassSet, final MutableEnumeration<String> posSet,
            final MutableEnumeration<String> unigramSuffixSet, final MutableEnumeration<String> bigramSuffixSet,
            final MutableEnumeration<String> tagSet) {
        super(featureTemplates, lexicon, unkClassSet, posSet, unigramSuffixSet, bigramSuffixSet, tagSet);
    }

    public MulticlassTaggerFeatureExtractor(final String featureTemplates, final MutableEnumeration<String> lexicon,
            final MutableEnumeration<String> unkClassSet, final MutableEnumeration<String> posSet, final MutableEnumeration<String> tagSet) {
        super(featureTemplates, lexicon, unkClassSet, posSet, tagSet);
    }

    @Override
    public BitVector featureVector(final MulticlassTagSequence sequence, final int position) {

        final long[] featureIndices = new long[templates.length];

        for (int i = 0; i < templates.length; i++) {
            long feature = 0;
            final TaggerFeatureExtractor.TemplateElement[] template = templates[i];
            for (int j = 0; j < template.length; j++) {
                final TaggerFeatureExtractor.TemplateElement t = template[j];
                final int index = position + t.offset;

                switch (t) {

                case tm1:
                case tm2:
                case tm3:
                    feature *= tagSetSize;
                    feature += ((index < 0 || index >= sequence.predictedClasses.length) ? nullTag
                            : sequence.predictedClasses[index]);
                    break;

                case um2:
                case um1:
                case u:
                case up1:
                case up2:
                    feature *= unkClassSetSize;
                    feature += ((index < 0 || index >= sequence.mappedTokens.length) ? nullToken
                            : sequence.mappedUnkSymbols[index]);
                    break;

                case wm2:
                case wm1:
                case w:
                case wp1:
                case wp2:
                    feature *= lexiconSize;
                    feature += ((index < 0 || index >= sequence.mappedTokens.length) ? nullToken
                            : sequence.mappedTokens[index]);
                    break;

                // Lots of binary features
                case numm1:
                case num:
                case nump1:
                    feature *= 2;
                    feature += (index < 0 || index >= sequence.mappedTokens.length) ? 0 : Strings
                            .numeralPercentage(sequence.tokens[index]) > 0 ? 1 : 0;
                    break;

                case num20:
                case num40:
                case num60:
                case num80:
                case num100:
                    feature *= 2;
                    feature += (index < 0 || index >= sequence.mappedTokens.length) ? 0 : Strings
                            .numeralPercentage(sequence.tokens[index]) >= t.value ? 1 : 0;
                    break;

                case punctm1:
                case punct:
                case punctp1:
                    feature *= 2;
                    feature += (index < 0 || index >= sequence.mappedTokens.length) ? 0 : Strings
                            .punctuationPercentage(sequence.tokens[index]) > 0 ? 1 : 0;
                    break;

                case punct20:
                case punct40:
                case punct60:
                case punct80:
                case punct100:
                    feature *= 2;
                    feature += (index < 0 || index >= sequence.mappedTokens.length) ? 0 : Strings
                            .punctuationPercentage(sequence.tokens[index]) >= t.value ? 1 : 0;
                    break;

                case posm3:
                case posm2:
                case posm1:
                case pos:
                case posp1:
                case posp2:
                case posp3:
                    feature *= posSetSize;
                    feature += ((index < 0 || index >= sequence.mappedPosSymbols.length) ? nullToken
                            : sequence.mappedPosSymbols[index]);
                    break;

                case usm1:
                case us:
                case usp1:
                    feature *= unigramSuffixSetSize;
                    feature += ((index < 0 || index >= sequence.mappedUnigramSuffix.length) ? nullToken
                            : sequence.mappedUnigramSuffix[index]);
                    break;

                case bsm1:
                case bs:
                case bsp1:
                    feature *= bigramSuffixSetSize;
                    feature += ((index < 0 || index >= sequence.mappedBigramSuffix.length) ? nullToken
                            : sequence.mappedBigramSuffix[index]);
                    break;
                }
            }
            final long featureIndex = featureOffsets[i] + feature;
            assert featureIndex >= 0 && featureIndex < featureVectorLength;
            featureIndices[i] = featureIndex;
        }

        return featureVectorLength > Integer.MAX_VALUE ? new LargeSparseBitVector(featureVectorLength, featureIndices)
                : new SparseBitVector(featureVectorLength, featureIndices);
    }
}

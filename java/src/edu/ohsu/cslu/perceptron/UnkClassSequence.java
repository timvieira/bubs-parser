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

import edu.ohsu.cslu.grammar.DecisionTreeTokenClassifier;

/**
 * Represents a sequence of tokens and the information needed to assign any unknown tokens to unknown-word classes
 * 
 * @author Aaron Dunlop
 */
public class UnkClassSequence extends TagSequence {

    /**
     * Used by subclasses
     * 
     * @param tagger
     */
    public UnkClassSequence(final String sentence, final UnkClassTagger tagger) {
        super(sentence, tagger.lexicon, tagger.decisionTreeUnkClassSet, tagger.posSet, tagger.unigramSuffixSet,
                tagger.bigramSuffixSet, tagger.tagSet());
    }

    /**
     * Maps the specified POS-tag and token into the sequence data structures. Rare-word tokens begin with 'UNK-' and
     * include both the UNK-cluster and the original token.
     * 
     * @param position
     * @param pos
     * @param token
     */
    @Override
    void map(final int position, final String pos, String token) {

        // This tagger only tags rare / unknown words, but we map features of all tokens for use when tagging UNKs
        if (token.startsWith("UNK-")) {
            // Split the supplied token into the UNK-class and the original token
            final int splitIndex = token.indexOf('|');
            final String unkClass = token.substring(0, splitIndex);
            token = token.substring(splitIndex + 1);

            // UNK-class
            if (tagSet.isFinalized()) {
                tags[position] = (short) tagSet.getIndex(unkClass);
            } else {
                tags[position] = (short) tagSet.addSymbol(unkClass);
            }

        } else {
            tags[position] = -1;
        }
        predictedTags[position] = tags[position];

        // POS
        if (posSet.isFinalized()) {
            mappedPosSymbols[position] = (short) posSet.getIndex(pos);
        } else {
            mappedPosSymbols[position] = (short) posSet.addSymbol(pos);
        }

        // Token
        if (lexicon.isFinalized()) {
            mappedTokens[position] = lexicon.getIndex(token);
            mappedUnkSymbols[position] = unkClassSet.getIndex(DecisionTreeTokenClassifier.berkeleyGetSignature(token,
                    position == 0, lexicon));
        } else {
            mappedTokens[position] = lexicon.addSymbol(token);
            mappedUnkSymbols[position] = unkClassSet.addSymbol(DecisionTreeTokenClassifier.berkeleyGetSignature(token,
                    position == 0, lexicon));
        }

        // Suffixes
        if (unigramSuffixSet != null && token.length() > 0) {
            if (unigramSuffixSet.isFinalized()) {
                mappedUnigramSuffix[position] = unigramSuffixSet.getIndex(token.substring(token.length() - 1));
                if (token.length() > 1) {
                    mappedBigramSuffix[position] = bigramSuffixSet.getIndex(token.substring(token.length() - 2));
                }
            } else {
                mappedUnigramSuffix[position] = unigramSuffixSet.addSymbol(token.substring(token.length() - 1));
                if (token.length() > 1) {
                    mappedBigramSuffix[position] = bigramSuffixSet.addSymbol(token.substring(token.length() - 2));
                }
            }
        }
    }

    // public UnkClassSequence(final String arffLine, final UnkClassTagger tagger) {
    //
    // super(tagger.lexicon, tagger.unkClassSet, tagger.posSet, tagger.unigramSuffixSet, tagger.bigramSuffixSet,
    // tagger.tagSet, 1);
    //
    // this.length = 1;
    //
    // final String[] split = Strings.splitOn(arffLine, ',', '\\');
    // mapTagAndToken(0, split[split.length - 1], split[0]);
    // mapSuffixes(0, split[0]);
    //
    // if (posSet.isFinalized()) {
    // pos[0] = (short) posSet.getIndex(split[5]);
    // } else {
    // tags[0] = (short) tagSet.addSymbol(split[5]);
    // }
    // }
}

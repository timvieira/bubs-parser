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

package edu.ohsu.cslu.dep;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.datastructs.vectors.Vector;
import edu.ohsu.cslu.dep.DependencyGraph.Arc;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.grammar.Tokenizer;
import edu.ohsu.cslu.perceptron.FeatureExtractor;

/**
 * Extracts features for move-classification in Nivre-style dependency parsing from the current state of such a parser
 * (stack, arcs, etc.)
 * 
 * @author Aaron Dunlop
 */
public class NivreParserFeatureExtractor extends FeatureExtractor<NivreParserContext> {

    final static int PREVIOUS_START = 0;
    final static int NEXT_END = PREVIOUS_START + 1;

    final static int DISTANCE_1 = NEXT_END + 1;
    final static int DISTANCE_2 = DISTANCE_1 + 1;
    final static int DISTANCE_3 = DISTANCE_2 + 1;
    final static int DISTANCE_45 = DISTANCE_3 + 1;
    final static int DISTANCE_6 = DISTANCE_45 + 1;

    final SymbolSet<String> tokens;
    final SymbolSet<String> pos;
    final int featureVectorLength;

    final int tokenSetSize, posSetSize;
    final int bigramFeatureOffset, bigramFeatureOffset2, indicatorFeatureOffset;

    // 2nd on stack, top-of-stack, and next unshifted word
    final int previousWordOffset, wordOffset, nextWordOffset;

    public NivreParserFeatureExtractor(final SymbolSet<String> tokens, final SymbolSet<String> pos) {
        this.tokens = tokens;
        this.tokenSetSize = tokens.size();
        this.pos = pos;
        this.posSetSize = pos.size();

        // Features:
        //
        // Previous word (on the stack), current word (top-of-stack), next word (not-yet-shifted),
        //
        // UNK symbol for each of those 3 words (in the same range as the tokens themselves)
        //
        // POS for each of those 3 words
        //
        // Start-of-string indicator for previous word
        //
        // End-of-string indicator for next word
        //
        // Current word + previous POS
        // Current POS + previous word
        //
        // Distance between the top two words on the stack (the two under consideration for reduce operations)
        // Binned: 1, 2, 3, 4-5, 6+ words
        //

        this.previousWordOffset = 0;
        this.wordOffset = previousWordOffset + tokenSetSize + posSetSize;
        this.nextWordOffset = wordOffset + tokenSetSize + posSetSize;

        this.bigramFeatureOffset = nextWordOffset + tokenSetSize + posSetSize;
        this.bigramFeatureOffset2 = bigramFeatureOffset + posSetSize * tokenSetSize;

        this.indicatorFeatureOffset = bigramFeatureOffset2 + posSetSize * tokenSetSize;

        this.featureVectorLength = indicatorFeatureOffset + 7;
    }

    @Override
    public int featureCount() {
        return featureVectorLength;
    }

    @Override
    public SparseBitVector forwardFeatureVector(final NivreParserContext source, final int tokenIndex) {

        final IntArrayList featureIndices = new IntArrayList();

        if (source.stack.size() > 1) {
            // Previous word on stack
            final Arc previousWord = source.stack.get(1);
            // Top word on the stack
            final Arc currentWord = source.stack.peek();

            addFeatures(featureIndices, previousWord, previousWordOffset);

            // Current word on stack
            addFeatures(featureIndices, currentWord, wordOffset);

            if (currentWord != DependencyGraph.ROOT) {
                // Next word to be shifted
                final Arc nextWord = source.arcs[tokenIndex];

                addFeatures(featureIndices, nextWord, nextWordOffset);
            }

            featureIndices.add(bigramFeatureOffset + pos.getIndex(currentWord.pos) * tokenSetSize
                    + tokens.getIndex(previousWord.token));

            featureIndices.add(bigramFeatureOffset2 + pos.getIndex(previousWord.pos) * tokenSetSize
                    + tokens.getIndex(currentWord.token));

            if (previousWord.index == 0) {
                featureIndices.add(indicatorFeatureOffset + PREVIOUS_START);
            }
            if (tokenIndex == source.arcs.length - 1) {
                featureIndices.add(indicatorFeatureOffset + NEXT_END);
            }

            // Distance betwen top two words on stack
            switch (currentWord.index - previousWord.index) {
            case 1:
                featureIndices.add(indicatorFeatureOffset + DISTANCE_1);
                break;
            case 2:
                featureIndices.add(indicatorFeatureOffset + DISTANCE_2);
                break;
            case 3:
                featureIndices.add(indicatorFeatureOffset + DISTANCE_3);
                break;
            case 4:
            case 5:
                featureIndices.add(indicatorFeatureOffset + DISTANCE_45);
                break;
            default:
                featureIndices.add(indicatorFeatureOffset + DISTANCE_6);
                break;
            }

        }

        return new SparseBitVector(featureVectorLength, featureIndices.toIntArray());
    }

    private void addFeatures(final IntArrayList featureIndices, final Arc arc, final int offset) {

        if (tokens.contains(arc.token)) {
            featureIndices.add(offset + tokens.getIndex(arc.token)); // word
        }

        // UNK-symbol
        featureIndices.add(offset + tokens.getIndex(Tokenizer.berkeleyGetSignature(arc.token, arc.index == 0, tokens)));

        featureIndices.add(offset + tokenSetSize + pos.getIndex(arc.pos)); // POS
    }

    @Override
    public Vector forwardFeatureVector(final NivreParserContext source, final int tokenIndex, final float[] tagScores) {
        // TODO Auto-generated method stub
        return null;
    }

}

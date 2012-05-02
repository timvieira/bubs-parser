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

import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;
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

    final SymbolSet<String> tokens;
    final SymbolSet<String> pos;
    final int featureVectorLength;

    public NivreParserFeatureExtractor(final SymbolSet<String> tokens, final SymbolSet<String> pos) {
        this.tokens = tokens;
        this.pos = pos;

        // Features: previous word (on the stack), current word (top-of-stack), next word, current POS, next POS
        // + 1 for start-of-string and 1 for empty-stack
        this.featureVectorLength = tokens.size() * 3 + pos.size() * 3 + 2;
    }

    @Override
    public int featureCount() {
        return featureVectorLength;
    }

    @Override
    public SparseBitVector forwardFeatureVector(final NivreParserContext source, final int tokenIndex) {

        final int previousTokenOffset = 0;
        final int previousPosOffset = previousTokenOffset + tokens.size();

        final int currentTokenOffset = previousPosOffset + pos.size();
        final int currentPosOffset = currentTokenOffset + tokens.size();

        final int nextTokenOffset = currentPosOffset + pos.size();
        final int nextPosOffset = nextTokenOffset + tokens.size();

        final IntSet featureIndices = new IntAVLTreeSet();

        if (tokenIndex == 0) {
            // Start of sentence indicator
            featureIndices.add(featureVectorLength - 2);
        }

        if (source.stack.isEmpty()) {
            // Empty stack indicator
            featureIndices.add(featureVectorLength - 1);

        } else {
            if (source.stack.size() > 1) {
                // Previous word on stack
                final Arc previousWord = source.stack.get(1);
                addFeatures(featureIndices, previousWord, currentTokenOffset, currentPosOffset);
            }

            // Current word on stack
            final Arc currentWord = source.stack.peek();
            addFeatures(featureIndices, currentWord, currentTokenOffset, currentPosOffset);
        }

        if (source.stack.peek() != DependencyGraph.ROOT) {
            final Arc nextWord = source.arcs[tokenIndex];
            addFeatures(featureIndices, nextWord, nextTokenOffset, nextPosOffset);
        }

        return new SparseBitVector(featureVectorLength, featureIndices.toIntArray());
    }

    private void addFeatures(final IntSet featureIndices, final Arc arc, final int tokenOffset, final int posOffset) {

        if (tokens.contains(arc.token)) {
            featureIndices.add(tokenOffset + tokens.getInt(arc.token)); // word
        }

        // UNK-symbol
        featureIndices.add(tokenOffset
                + tokens.getInt(Tokenizer.berkeleyGetSignature(arc.token, arc.index == 0, tokens)));

        featureIndices.add(posOffset + pos.get(arc.coarsePos)); // tag
    }

    @Override
    public Vector forwardFeatureVector(final NivreParserContext source, final int tokenIndex, final float[] tagScores) {
        // TODO Auto-generated method stub
        return null;
    }

}

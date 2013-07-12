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

import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.grammar.DecisionTreeTokenClassifier;
import edu.ohsu.cslu.grammar.SymbolSet;

/**
 * @author Aaron Dunlop
 * @since Jul 11, 2013
 */
public class ConstituentBoundarySequence extends BaseSequence {

    /** Preterminals (part-of-speech tags) of the sentence */
    short[] posTags;

    final short sentenceLength;

    /**
     * Constructs from an array of tokens, mapped according to the classifier's lexicon. Used during inference.
     * 
     * @param mappedTokens
     * @param posTags
     * @param lexicon
     * @param unkClassSet
     */
    public ConstituentBoundarySequence(final int[] mappedTokens, final short[] posTags,
            final SymbolSet<String> lexicon, final SymbolSet<String> unkClassSet) {

        super(lexicon, unkClassSet);

        this.mappedTokens = mappedTokens;
        this.sentenceLength = (short) mappedTokens.length;
        this.posTags = posTags;

        this.mappedUnkSymbols = new int[sentenceLength];
        for (int i = 0; i < sentenceLength; i++) {
            mappedUnkSymbols[i] = unkClassSet.getIndex(DecisionTreeTokenClassifier.berkeleyGetSignature(tokens[i],
                    i == 0, lexicon));
        }
    }

    /**
     * Constructs from a bracketed tree.
     * 
     * @param parseTree
     * @param lexicon
     * @param unkClassSet
     * @param vocabulary
     */
    protected ConstituentBoundarySequence(final BinaryTree<String> parseTree, final SymbolSet<String> lexicon,
            final SymbolSet<String> unkClassSet, final SymbolSet<String> vocabulary) {

        super(lexicon, unkClassSet);

        this.tokens = parseTree.leafLabels();

        this.sentenceLength = (short) tokens.length;
        this.mappedTokens = new int[sentenceLength];
        this.mappedUnkSymbols = new int[sentenceLength];
        this.posTags = new short[sentenceLength];

        // Map all tokens, UNK symbols, and parts-of-speech
        for (int i = 0; i < sentenceLength; i++) {
            if (lexicon.isFinalized()) {
                mappedTokens[i] = lexicon.getIndex(tokens[i]);
                mappedUnkSymbols[i] = unkClassSet.getIndex(DecisionTreeTokenClassifier.berkeleyGetSignature(tokens[i],
                        i == 0, lexicon));
            } else {
                mappedTokens[i] = lexicon.addSymbol(tokens[i]);
                mappedUnkSymbols[i] = unkClassSet.addSymbol(DecisionTreeTokenClassifier.berkeleyGetSignature(tokens[i],
                        i == 0, lexicon));
            }
        }

        // All cells spanning more than one word
        this.length = sentenceLength * (sentenceLength + 1) / 2 - sentenceLength;
    }
}

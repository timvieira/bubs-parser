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

import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.perceptron.BeginConstituentFeatureExtractor.Sentence;
import edu.ohsu.cslu.util.MutableEnumeration;

/**
 * Extracts features for classifying whether a lexical item can begin a multiword constituent. Depends only on lexical
 * items (as mapped by the supplied {@link Grammar}) and on prior tags.
 * 
 * TODO Add suffix features as well
 * 
 * @author Aaron Dunlop
 * @since Oct 15, 2010
 */
public class BeginConstituentFeatureExtractor extends FeatureExtractor<Sentence> {

    private static final long serialVersionUID = 1L;

    private final Grammar grammar;
    private final int markovOrder;
    private final int vectorLength;

    private final MutableEnumeration<String> twoCharacterSuffixes = new MutableEnumeration<String>();
    private final MutableEnumeration<String> threeCharacterSuffixes = new MutableEnumeration<String>();

    public BeginConstituentFeatureExtractor(final Grammar grammar, final int markovOrder) {
        this.grammar = grammar;
        this.markovOrder = markovOrder;
        final int windowSize = markovOrder * 2 + 1;
        // this.featureCount = grammar.numLexSymbols() * windowSize + markovOrder * 2;
        for (final String token : grammar.lexSet) {
            twoCharacterSuffixes.addSymbol(token.substring(token.length() - 2));
            threeCharacterSuffixes.addSymbol(token.substring(token.length() - 3));
        }
        this.vectorLength = grammar.numLexSymbols() * windowSize + markovOrder * 2 + twoCharacterSuffixes.size()
                + threeCharacterSuffixes.size();
    }

    @Override
    public long vectorLength() {
        return vectorLength;
    }

    @Override
    public int templateCount() {
        // TODO Fix this
        return 0;
    }

    /**
     * Feature Order:
     * <ol>
     * <li>0..n : token i - o (i = index, o = Markov order)</li>
     * <li>n+1..2n : token i - o + 1</li>
     * ...
     * <li>2on+1..(2o+1)n : token i + o</li>
     * 
     * <li>bigram suffix i - o</li>
     * <li>bigram suffix i - o + 1</li>
     * ...
     * <li>bigram suffix i + o</li>
     * 
     * <li>trigram suffix i - o</li>
     * <li>trigram suffix i - o + 1</li>
     * ...
     * <li>trigram suffix i + o</li>
     * 
     * <li>tag i - o</li>
     * ...
     * <li>tag i - 1</li>
     * </ol>
     * 
     * @param source
     * @param tokenIndex
     * @param tagScores
     * @return a feature vector representing the specified token and tags
     */
    public SparseBitVector forwardFeatureVector(final Sentence source, final int tokenIndex, final float[] tagScores) {
        final Sentence s = source;
        final int windowSize = markovOrder * 2 + 1;

        // TODO Clean up vector creation a bit
        int offset = 0;
        // Token features
        final int[] vector = new int[windowSize * 3 + markovOrder];
        for (int j = 0; j < windowSize; j++) {
            vector[j] = offset + j * grammar.numLexSymbols() + s.mappedTokens[tokenIndex + j];
        }
        offset += windowSize * grammar.numLexSymbols();

        // // Suffix features
        // for (int j = 0; j < windowSize; j++) {
        // vector[j] = offset + ;
        // }

        // Tag features
        offset += windowSize * (twoCharacterSuffixes.size() + threeCharacterSuffixes.size());
        for (int j = 0; j < markovOrder; j++) {
            final int tagIndex = j - markovOrder + tokenIndex;
            if (tagIndex >= 0 && tagScores[tagIndex] > 0) { // TODO > 0? > -Infinity?
                vector[windowSize + j] = offset + j * 2;
            } else {
                vector[windowSize + j] = offset + j * 2 + 1;
            }
        }

        return new SparseBitVector(vectorLength, vector);
    }

    @Override
    public SparseBitVector featureVector(final Sentence source, final int tokenIndex) {
        return null;
    }

    public class Sentence {

        /**
         * Parallel array of tokens (mapped according to the grammar lexicon) and booleans labeling tokens which start
         * or end multiword constituents. The parallel array is of length n + 2o, where n is the length of the sentence
         * and o is the Markov order. That is, we allow o entries prior to the beginning of the sentence and o following
         * the end, so a Markov-order-2 tagger for a 6-word sentence will be represented by a 10-element array.
         */
        public final String[] tokens;
        public final int[] mappedTokens;

        /** Indexed from 0..n (since null symbols cannot begin multiword constituents */
        public final boolean[] beginsMultiwordConstituent;

        public Sentence(final String sentence) {

            if (sentence.startsWith("(")) {
                final NaryTree<String> tree = NaryTree.read(sentence, String.class);
                final int sentenceLength = tree.leaves();

                tokens = new String[sentenceLength + 2 * markovOrder];
                mappedTokens = new int[sentenceLength + 2 * markovOrder];
                System.arraycopy(grammar.tokenClassifier.lexiconIndices(tree, grammar.lexSet), 0, mappedTokens,
                        markovOrder, sentenceLength);
                beginsMultiwordConstituent = new boolean[sentenceLength];

                for (int k = 0; k < markovOrder; k++) {
                    final short nullWord = grammar.mapNonterminal(Grammar.nullSymbolStr);
                    mappedTokens[k] = nullWord;
                    mappedTokens[mappedTokens.length - k - 1] = nullWord;
                }

                int k = markovOrder;
                for (final NaryTree<String> leaf : tree.leafTraversal()) {
                    beginsMultiwordConstituent[k - markovOrder] = leaf.isLeftmostChild();
                    tokens[k++] = leaf.label();
                }
            } else {
                final String[] tmpTokens = sentence.split(" ");

                mappedTokens = new int[tmpTokens.length + 2 * markovOrder];
                System.arraycopy(grammar.tokenClassifier.lexiconIndices(sentence, grammar.lexSet), 0, mappedTokens,
                        markovOrder, tmpTokens.length);

                tokens = new String[tmpTokens.length + 2 * markovOrder];
                System.arraycopy(tmpTokens, 0, tokens, markovOrder, tmpTokens.length);
                beginsMultiwordConstituent = null;

                for (int k = 0; k < markovOrder; k++) {
                    final short nullWord = grammar.mapNonterminal(Grammar.nullSymbolStr);
                    mappedTokens[k] = nullWord;
                    mappedTokens[mappedTokens.length - k - 1] = nullWord;
                }
            }
        }

        public SparseBitVector[] featureVectors() {
            final SparseBitVector[] featureVectors = new SparseBitVector[tokens.length - 2 * markovOrder];
            for (int i = 0; i < featureVectors.length; i++) {
                featureVectors[i] = forwardFeatureVector(this, i, null);
            }
            return featureVectors;
        }

        public boolean[] goldTags() {
            return beginsMultiwordConstituent;
        }

        public int length() {
            return beginsMultiwordConstituent.length;
        }
    }
}

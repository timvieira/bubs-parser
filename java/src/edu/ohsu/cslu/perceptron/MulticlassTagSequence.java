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

import java.util.Arrays;

import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.grammar.DecisionTreeTokenClassifier;
import edu.ohsu.cslu.util.MutableEnumeration;

/**
 * @author Aaron Dunlop
 * @since Jul 11, 2013
 */
public class MulticlassTagSequence extends BaseSequence implements MulticlassSequence {

    protected final MutableEnumeration<String> tagSet;
    final short[] goldClasses;
    final short[] predictedClasses;

    /**
     * Constructs from a tree, a bracketed string (e.g. (DT The) (NN fish) ...), or from a space-delimited (untagged)
     * string. Used by {@link CompleteClosureClassifier} when an existing {@link Tagger} is already initialized.
     * 
     * @param sentence
     * @param classifier
     */
    public MulticlassTagSequence(final String sentence, final Tagger classifier) {
        this(sentence, classifier.lexicon, classifier.decisionTreeUnkClassSet, null, null, null, classifier.tagSet());
    }

    /**
     * Constructs from a tree, a bracketed string (e.g. (DT The) (NN fish) ...), or from a space-delimited (untagged)
     * string.
     * 
     * @param sentence
     * @param lexicon
     * @param unkClassSet
     * @param tagSet
     */
    public MulticlassTagSequence(final String sentence, final MutableEnumeration<String> lexicon,
            final MutableEnumeration<String> unkClassSet, final MutableEnumeration<String> posSet,
            final MutableEnumeration<String> unigramSuffixSet, final MutableEnumeration<String> bigramSuffixSet,
            final MutableEnumeration<String> tagSet) {

        super(lexicon, unkClassSet, posSet, unigramSuffixSet, bigramSuffixSet);

        this.tagSet = tagSet;

        // If the sentence starts with '(', first try parsing it as a tree
        if (sentence.charAt(0) == '(') {
            NaryTree<String> tree = null;
            try {
                tree = NaryTree.read(sentence.trim(), String.class);
            } catch (final IllegalArgumentException e) {
            }

            // Awkward control flow, to convince the compiler that we're only initializing final fields once
            if (tree != null) {
                this.length = tree.leaves();
                this.tokens = new String[length];
                this.mappedTokens = new int[length];
                this.mappedUnkSymbols = new int[length];
                this.goldClasses = new short[length];
                this.predictedClasses = new short[length];
                if (unigramSuffixSet != null) {
                    this.mappedUnigramSuffix = new int[length];
                    this.mappedBigramSuffix = new int[length];
                }
                if (posSet != null) {
                    this.mappedPosSymbols = new short[length];
                }

                int position = 0;
                for (final NaryTree<String> leaf : tree.leafTraversal()) {
                    tokens[position] = leaf.label();
                    map(position, leaf.parentLabel(), leaf.label());
                    position++;
                }

            } else {
                // Tree parsing failed; treat it as a series of bracketed tag/word pairs. E.g. (DT The) (NN fish)...
                final String[] split = sentence.replaceAll(" ?\\(", "").split("\\)");
                this.length = split.length;
                this.tokens = new String[length];
                this.mappedTokens = new int[length];
                this.mappedUnkSymbols = new int[length];
                this.goldClasses = new short[length];
                this.predictedClasses = new short[length];
                if (unigramSuffixSet != null) {
                    this.mappedUnigramSuffix = new int[length];
                    this.mappedBigramSuffix = new int[length];
                }
                if (posSet != null) {
                    this.mappedPosSymbols = new short[length];
                }

                for (int position = 0; position < split.length; position++) {
                    final String[] tokenAndPos = split[position].split(" ");
                    tokens[position] = tokenAndPos[1];
                    map(position, tokenAndPos[0], tokenAndPos[1]);
                }
            }

        } else {
            // It didn't start with '('; assume it is untagged
            final String[] split = sentence.split(" ");
            this.length = split.length;
            tokens = new String[length];
            this.mappedTokens = new int[length];
            this.mappedUnkSymbols = new int[length];
            this.goldClasses = new short[length];
            this.predictedClasses = new short[length];
            if (unigramSuffixSet != null) {
                this.mappedUnigramSuffix = new int[length];
                this.mappedBigramSuffix = new int[length];
            }
            if (posSet != null) {
                this.mappedPosSymbols = new short[length];
            }
            Arrays.fill(goldClasses, (short) -1);

            for (int i = 0; i < split.length; i++) {
                tokens[i] = split[i];
                if (lexicon.isFinalized()) {
                    mappedTokens[i] = lexicon.getIndex(split[i]);
                    mappedUnkSymbols[i] = unkClassSet.getIndex(DecisionTreeTokenClassifier.berkeleyGetSignature(
                            tokens[i], i == 0, lexicon));
                } else {
                    mappedTokens[i] = lexicon.addSymbol(split[i]);
                    mappedUnkSymbols[i] = unkClassSet.addSymbol(DecisionTreeTokenClassifier.berkeleyGetSignature(
                            tokens[i], i == 0, lexicon));
                }
                mapSuffixes(i, tokens[i]);
            }
        }
    }

    // /**
    // *
    // * @param tokens
    // * @param mappedTokens
    // * @param classifier
    // */
    // public MulticlassTagSequence(final String[] tokens, final int[] mappedTokens, final Tagger classifier) {
    //
    // super(classifier.lexicon, classifier.decisionTreeUnkClassSet, classifier.posSet, classifier.unigramSuffixSet,
    // classifier.bigramSuffixSet);
    //
    // this.length = tokens.length;
    // this.tokens = tokens;
    // this.mappedTokens = mappedTokens;
    // this.mappedUnkSymbols = new int[length];
    // this.goldClasses = new short[length];
    // this.predictedClasses = new short[length];
    // if (unigramSuffixSet != null) {
    // this.mappedUnigramSuffix = new int[length];
    // this.mappedBigramSuffix = new int[length];
    // }
    //
    // for (int i = 0; i < tokens.length; i++) {
    // final String token = tokens[i];
    // mappedUnkSymbols[i] = unkClassSet.getIndex(DecisionTreeTokenClassifier.berkeleyGetSignature(token,
    // i == 0, lexicon));
    // mappedUnigramSuffix[i] = unigramSuffixSet.getIndex(token.substring(token.length() - 1));
    // if (token.length() > 1) {
    // mappedBigramSuffix[i] = bigramSuffixSet.getIndex(token.substring(token.length() - 2));
    // }
    // }
    //
    // }

    /**
     * Maps the specified POS-tag and token into the sequence data structures.
     * 
     * @param position
     * @param pos
     * @param token
     */
    void map(final int position, final String pos, final String token) {

        // POS
        if (tagSet.isFinalized()) {
            goldClasses[position] = (short) tagSet.getIndex(pos);
        } else {
            goldClasses[position] = (short) tagSet.addSymbol(pos);
        }
        predictedClasses[position] = goldClasses[position];

        // Token
        final String unkClass = DecisionTreeTokenClassifier.berkeleyGetSignature(token, position == 0, lexicon);
        if (lexicon.isFinalized()) {
            mappedTokens[position] = lexicon.getIndex(token);
            mappedUnkSymbols[position] = unkClassSet.getIndex(unkClass);
        } else {
            mappedTokens[position] = lexicon.addSymbol(token);
            mappedUnkSymbols[position] = unkClassSet.addSymbol(unkClass);
        }

        // Suffixes
        mapSuffixes(position, token);
    }

    private void mapSuffixes(final int position, final String token) {
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

    public int[] mappedTokens() {
        return mappedTokens;
    }

    @Override
    public short goldClass(final int position) {
        return goldClasses[position];
    }

    @Override
    public short[] predictedClasses() {
        return predictedClasses;
    }

    @Override
    public short predictedClass(final int position) {
        return predictedClasses[position];
    }

    @Override
    public void setPredictedClass(final int position, final short newClass) {
        predictedClasses[position] = newClass;
    }

    @Override
    public String toString() {
        final boolean knownGoldClasses = goldClasses[0] >= 0;

        final StringBuilder sb = new StringBuilder(256);
        for (int i = 0; i < length; i++) {
            sb.append('(');
            if (knownGoldClasses) {
                sb.append(goldClasses[i] < 0 ? "---" : tagSet.getSymbol(goldClasses[i]));
            } else {
                sb.append(predictedClasses[i] < 0 ? "---" : tagSet.getSymbol(predictedClasses[i]));
            }
            sb.append(' ');
            sb.append(tokens[i]);
            sb.append(')');

            if (i < (length - 1)) {
                sb.append(' ');
            }
        }
        return sb.toString();
    }
}

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

import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.grammar.DecisionTreeTokenClassifier;
import edu.ohsu.cslu.grammar.SymbolSet;

/**
 * Represents a sequence of tokens, each with a binary classification (e.g. for unary constraint classification, as in
 * {@link UnaryConstraintClassifier}.
 * 
 * @author Aaron Dunlop
 * @since Jul 25, 2013
 */
public abstract class BinaryTagSequence extends BaseSequence implements BinarySequence {

    final boolean[] goldClasses;
    final boolean[] predictedClasses;

    /**
     * Constructs from a bracketed string (e.g. (DT The) (NN fish) ... or from a space-delimited (untagged) string. Used
     * during training.
     * 
     * @param tree
     * @param classifier
     * @param posSet
     * @param unigramSuffixSet
     * @param bigramSuffixSet
     */
    public BinaryTagSequence(final BinaryTree<String> tree,
            final BinaryClassifier<? extends BinaryTagSequence> classifier, final SymbolSet<String> posSet,
            final SymbolSet<String> unigramSuffixSet, final SymbolSet<String> bigramSuffixSet) {

        super(classifier.lexicon, classifier.decisionTreeUnkClassSet, posSet, unigramSuffixSet, bigramSuffixSet);

        this.length = tree.leaves();
        this.predictedClasses = new boolean[length];
        this.goldClasses = new boolean[length];
        this.mappedTokens = new int[length];
        this.mappedUnkSymbols = new int[length];
        this.mappedUnigramSuffix = new int[length];
        this.mappedBigramSuffix = new int[length];

        int i = 0;
        for (final BinaryTree<String> leaf : tree.leafTraversal()) {

            map(i, leaf.label());
            goldClasses[i] = classifyLeaf(leaf);
            i++;
        }
    }

    /**
     * Constructs from a sequence of tokens. Used during inference.
     * 
     * @param mappedTokens
     * @param classifier
     * @param posSet
     * @param unigramSuffixSet
     * @param bigramSuffixSet
     */
    public BinaryTagSequence(final int[] mappedTokens, final BinaryClassifier<? extends BinaryTagSequence> classifier,
            final SymbolSet<String> posSet, final SymbolSet<String> unigramSuffixSet,
            final SymbolSet<String> bigramSuffixSet) {

        super(classifier.lexicon, classifier.decisionTreeUnkClassSet, posSet, unigramSuffixSet, bigramSuffixSet);

        this.length = mappedTokens.length;
        this.predictedClasses = new boolean[length];
        this.goldClasses = null;
        this.mappedTokens = new int[length];
        this.mappedUnkSymbols = new int[length];
        this.mappedUnigramSuffix = new int[length];
        this.mappedBigramSuffix = new int[length];

        for (int i = 0; i < length; i++) {
            map(i, classifier.lexicon.getSymbol(mappedTokens[i]));
        }
    }

    /**
     * @param leaf
     * @return The gold classification of the leaf node
     */
    protected abstract boolean classifyLeaf(BinaryTree<String> leaf);

    /**
     * Maps the specified POS-tag and token into the sequence data structures.
     * 
     * @param position
     * @param token
     */
    void map(final int position, final String token) {

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

    @Override
    public boolean goldClass(final int position) {
        return goldClasses[position];
    }

    @Override
    public boolean[] predictedClasses() {
        return predictedClasses;
    }

    @Override
    public boolean predictedClass(final int position) {
        return predictedClasses[position];
    }

    @Override
    public void setPredictedClass(final int position, final boolean newClass) {
        predictedClasses[position] = newClass;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(256);
        for (int i = 0; i < length; i++) {
            sb.append('(');
            sb.append(goldClasses[i] ? "T" : "F");
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

/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
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
package edu.ohsu.cslu.grammar;

import java.io.Serializable;

/**
 * Represents a grammar rule
 * 
 * @author Nathan Bodenstab
 * @author Aaron Dunlop
 * @since 2009
 */
public class Production implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    // if rightChild == -1, it's a unary prod, if -2, it's a lexical prod
    public final static int UNARY_PRODUCTION = -1;
    public final static int LEXICAL_PRODUCTION = -2;

    public final int parent, leftChild, rightChild;
    public final float prob;

    // Used in toString() for debugging. Not required for normal use
    public final SymbolSet<String> vocabulary;
    public final SymbolSet<String> lexicon;

    // TODO Remove lexicon for unary and binary productions?

    // Binary production
    public Production(final int parent, final int leftChild, final int rightChild, final float prob,
            final SymbolSet<String> vocabulary, final SymbolSet<String> lexicon) {
        assert parent != -1 && leftChild != -1 && rightChild != -1;
        this.parent = parent;
        this.leftChild = leftChild;
        this.rightChild = rightChild;
        if (Float.isNaN(prob)) {
            throw new IllegalArgumentException("Probability is NaN");
        }
        this.prob = prob;

        this.vocabulary = vocabulary;
        this.lexicon = lexicon;
    }

    // Binary production
    public Production(final int parent, final int leftChild, final int rightChild, final float prob,
            final Grammar grammar) {
        this(parent, leftChild, rightChild, prob, grammar.nonTermSet, grammar.lexSet);

        grammar.getNonterminal(leftChild).isLeftChild = true;
        grammar.getNonterminal(rightChild).isRightChild = true;
    }

    // Binary production
    public Production(final String parent, final String leftChild, final String rightChild, final float prob,
            final SymbolSet<String> vocabulary, final SymbolSet<String> lexicon) {
        this(vocabulary.addSymbol(parent), vocabulary.addSymbol(leftChild), vocabulary.addSymbol(rightChild), prob,
                vocabulary, lexicon);
    }

    // Binary production
    public Production(final String parent, final String leftChild, final String rightChild, final float prob,
            final Grammar grammar) {
        this(grammar.nonTermSet.addSymbol(parent), grammar.nonTermSet.addSymbol(leftChild), grammar.nonTermSet
                .addSymbol(rightChild), prob, grammar);

    }

    // Unary or lexical production
    public Production(final int parent, final int child, final float prob, final boolean isLex,
            final SymbolSet<String> vocabulary, final SymbolSet<String> lexicon) {

        assert parent != -1 && child != -1;
        assert !Float.isInfinite(prob);
        assert !Float.isNaN(prob);
        assert prob <= 0f;

        this.parent = parent;
        this.leftChild = child;
        this.vocabulary = vocabulary;
        this.lexicon = lexicon;
        this.rightChild = isLex ? LEXICAL_PRODUCTION : UNARY_PRODUCTION;
        this.prob = prob;
    }

    // Unary or lexical production
    public Production(final int parent, final int child, final float prob, final boolean isLex, final Grammar grammar) {
        this(parent, child, prob, isLex, grammar.nonTermSet, grammar.lexSet);
        if (isLex) {
            grammar.getNonterminal(parent).isPOS = true;
            if (parent > grammar.maxPOSIndex) {
                grammar.maxPOSIndex = parent;
            }
        }
    }

    // Unary or lexical production
    public Production(final String parent, final String child, final float prob, final boolean isLex,
            final Grammar grammar) {
        this(grammar.nonTermSet.addSymbol(parent), isLex ? grammar.lexSet.addSymbol(child) : grammar.nonTermSet
                .addSymbol(child), prob, isLex, grammar);
    }

    // Unary or lexical production
    public Production(final String parent, final String child, final float prob, final boolean isLex,
            final SymbolSet<String> vocabulary, final SymbolSet<String> lexicon) {
        this.parent = vocabulary.addSymbol(parent);
        this.leftChild = isLex ? lexicon.addSymbol(child) : vocabulary.addSymbol(child);
        this.rightChild = isLex ? LEXICAL_PRODUCTION : UNARY_PRODUCTION;
        this.vocabulary = vocabulary;
        this.lexicon = lexicon;
        this.prob = prob;
    }

    public final Production copy() {
        return new Production(parent, leftChild, rightChild, prob, vocabulary, lexicon);
    }

    public boolean equals(final Production otherProd) {
        if (parent != otherProd.parent)
            return false;
        if (leftChild != otherProd.leftChild)
            return false;
        if (rightChild != otherProd.rightChild)
            return false;

        return true;
    }

    public int child() {
        return leftChild;
    }

    public final boolean isUnaryProd() {
        return rightChild == UNARY_PRODUCTION;
    }

    public final boolean isLexProd() {
        return rightChild == LEXICAL_PRODUCTION;
    }

    public boolean isBinaryProd() {
        return isUnaryProd() == false && isLexProd() == false;
    }

    public final String parentToString() {
        return vocabulary.getSymbol(parent);
    }

    public String childrenToString() {
        if (isLexProd()) {
            return lexicon.getSymbol(leftChild);
        } else if (isUnaryProd()) {
            return lexicon.getSymbol(leftChild);
        }
        return vocabulary.getSymbol(leftChild) + " " + vocabulary.getSymbol(rightChild);
    }

    @Override
    public String toString() {
        if (isBinaryProd()) {
            if (vocabulary != null) {
                return String.format("%s -> %s %s %.4f", vocabulary.getSymbol(parent), vocabulary.getSymbol(leftChild),
                        vocabulary.getSymbol(rightChild), prob);
            }
            return String.format("%d -> %d %d %.4f", parent, leftChild, rightChild, prob);
        }

        if (isLexProd()) {
            if (lexicon != null) {
                return String.format("%s -> %s %.4f", vocabulary.getSymbol(parent), lexicon.getSymbol(leftChild), prob);
            }
            return String.format("%d -> %d %.4f", parent, leftChild, prob);
        }

        // Unary
        if (vocabulary != null) {
            return String.format("%s -> %s %.4f", vocabulary.getSymbol(parent), vocabulary.getSymbol(leftChild), prob);
        }
        return String.format("%d -> %d %.4f", parent, leftChild, prob);
    }
}

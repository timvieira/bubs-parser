package edu.ohsu.cslu.grammar;

import java.io.Serializable;

/**
 * Represents a grammar rule
 * 
 * @author Nathan Bodenstab
 * @author Aaron Dunlop
 * @since 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class Production implements Serializable, Cloneable {

    // if rightChild == -1, it's a unary prod, if -2, it's a lexical prod
    public final static int UNARY_PRODUCTION = -1;
    public final static int LEXICAL_PRODUCTION = -2;

    public final int parent, leftChild, rightChild;
    public final float prob;

    // Used in toString() for debugging. Not required for normal use
    public final SymbolSet<String> vocabulary;
    public final SymbolSet<String> lexicon;

    // Binary production
    private Production(final int parent, final int leftChild, final int rightChild, final float prob,
            final SymbolSet<String> vocabulary, final SymbolSet<String> lexicon) {
        assert parent != -1 && leftChild != -1 && rightChild != -1;
        this.parent = parent;
        this.leftChild = leftChild;
        this.rightChild = rightChild;
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
    public Production(final int parent, final int child, final float prob, final boolean isLex, final Grammar grammar) {
        assert parent != -1 && child != -1;
        this.parent = parent;
        this.leftChild = child;
        this.vocabulary = grammar.nonTermSet;
        this.lexicon = grammar.lexSet;

        if (isLex) {
            this.rightChild = LEXICAL_PRODUCTION;
            grammar.getNonterminal(parent).isPOS = true;
            if (parent > grammar.maxPOSIndex) {
                grammar.maxPOSIndex = parent;
            }
        } else {
            this.rightChild = UNARY_PRODUCTION;
        }
        this.prob = prob;
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
        if (vocabulary == null || lexicon == null) {
            if (isBinaryProd()) {
                return String.format("%d -> %d %d %.4f", parent, leftChild, rightChild, prob);
            }
            return String.format("%d -> %d %.4f", parent, leftChild, prob);
        }

        if (isBinaryProd()) {
            return String.format("%s -> %s %s %.4f", vocabulary.getSymbol(parent), vocabulary.getSymbol(leftChild),
                    vocabulary.getSymbol(rightChild), prob);
        }

        if (isLexProd()) {
            return String.format("%s -> %s %.4f", vocabulary.getSymbol(parent), lexicon.getSymbol(leftChild), prob);
        }

        // Unary
        return String.format("%s -> %s %.4f", vocabulary.getSymbol(parent), vocabulary.getSymbol(leftChild), prob);
    }
}
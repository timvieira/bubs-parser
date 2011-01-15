package edu.ohsu.cslu.ella;

import java.io.Serializable;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.SymbolSet;

/**
 * Represents a grammar rule.
 * 
 * TODO Merge with {@link Grammar.Production}.
 * 
 * @author Aaron Dunlop
 * @since Jan 14, 2011
 * 
 * @version $Revision$ $Date$ $Author$
 */
public final class Production implements Serializable, Cloneable {

    public final int parent, leftChild, rightChild;
    public final float probability;
    public final boolean lexical;

    // Used in toString() for debugging. Not required for normal use
    public final SymbolSet<String> vocabulary;
    public final SymbolSet<String> lexicon;

    // Binary production
    public Production(final int parent, final int leftChild, final int rightChild, final float prob,
            final SymbolSet<String> vocabulary, final SymbolSet<String> lexicon) {
        this.parent = parent;
        this.leftChild = leftChild;
        this.rightChild = rightChild;
        this.probability = prob;
        this.lexical = false;
        this.vocabulary = vocabulary;
        this.lexicon = lexicon;
    }

    // Unary production
    public Production(final int parent, final int child, final boolean lexical, final float prob,
            final SymbolSet<String> vocabulary, final SymbolSet<String> lexicon) {

        this.parent = parent;
        this.leftChild = child;
        this.rightChild = -1;
        this.probability = prob;
        this.lexical = lexical;
        this.vocabulary = vocabulary;
        this.lexicon = lexicon;
    }

    // Binary production
    public Production(final int parent, final int leftChild, final int rightChild, final float prob) {
        this(parent, leftChild, rightChild, prob, null, null);
    }

    // Unary production
    public Production(final int parent, final int child, final boolean lexical, final float prob) {
        this(parent, child, lexical, prob, null, null);
    }

    @Override
    public final Production clone() {
        return new Production(parent, leftChild, rightChild, probability);
    }

    public boolean equals(final Production other) {
        return (parent == other.parent && leftChild == other.leftChild && rightChild == other.rightChild);
    }

    public int child() {
        return leftChild;
    }

    public boolean isBinaryProd() {
        return rightChild >= 0;
    }

    @Override
    public String toString() {
        if (vocabulary == null || lexicon == null) {
            if (isBinaryProd()) {
                return String.format("%d -> %d %d %.4f", parent, leftChild, rightChild, probability);
            }
            return String.format("%d -> %d %.4f", parent, leftChild, probability);
        }

        if (isBinaryProd()) {
            return String.format("%s -> %s %s %.4f", vocabulary.getSymbol(parent), vocabulary.getSymbol(leftChild),
                    vocabulary.getSymbol(rightChild), probability);
        }

        if (lexical) {
            return String.format("%s -> %s %s %.4f", vocabulary.getSymbol(parent), lexicon.getSymbol(leftChild),
                    probability);
        }

        // Unary
        return String.format("%s -> %s %s %.4f", vocabulary.getSymbol(parent), vocabulary.getSymbol(leftChild),
                probability);
    }
}

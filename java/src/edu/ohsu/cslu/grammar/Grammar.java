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
import java.util.Collection;

import edu.ohsu.cslu.datastructs.narytree.NaryTree.Binarization;

/**
 * Represents a Probabilistic Context Free Grammar (PCFG). Such grammars may be built up programatically or may be
 * inferred from a corpus of data. Various PCFG representations are implemented in subclasses.
 * 
 * A PCFG consists of
 * <ul>
 * <li>A set of non-terminal symbols V (alternatively referred to as 'categories')</li>
 * <li>A special start symbol (S-dagger) from V</li>
 * <li>A set of terminal symbols T</li>
 * <li>A set of rule productions P mapping from V to (V union T)</li>
 * </ul>
 * 
 * Such grammars are useful in modeling and analyzing the structure of natural language or the (secondary) structure of
 * many biological sequences.
 * 
 * Although branching of arbitrary size is possible in a grammar under this definition, this class hierarchy models
 * grammars which have been factored into binary-branching form, enabling much more efficient computational approaches
 * to be used.
 * 
 * TODO Fix this comment
 * 
 * Note - there will always be more productions than categories, but all categories except the special S-dagger category
 * are also productions. The index of a category will be the same when used as a production as it is when used as a
 * category.
 * 
 * TODO Implement an even simpler grammar for use as an intermediate form when transforming one grammar to another.
 * 
 * @author Nathan Bodenstab
 * @author Aaron Dunlop
 * @since 2010
 * 
 *        $Id$
 */
public abstract class Grammar implements Serializable {

    /** Marks the switch from PCFG to lexicon entries in the grammar file */
    public final static String LEXICON_DELIMITER = "===== LEXICON =====";

    public final static String nullSymbolStr = "<null>";

    // == Grammar Basics ==
    public SymbolSet<String> lexSet = new SymbolSet<String>();
    public Tokenizer tokenizer = new Tokenizer(lexSet);
    public Vocabulary nonTermSet;

    public GrammarFormatType grammarFormat;

    public int startSymbol;
    public String startSymbolStr;
    public int horizontalMarkov;
    public int verticalMarkov;

    // Maps from 0-based indices to entries in nonTermSet. Used to reduce absolute
    // index value for feature extraction. Used in FOM initialization and in
    // extracting chart cell features for beam width prediction.
    public SymbolSet<Short> posSet;
    public SymbolSet<Short> phraseSet; // phraseSet + posSet == nonTermSet

    public abstract Production getLexicalProduction(final short parent, final int lex);

    public abstract Collection<Production> getUnaryProductionsWithChild(final int child);

    public abstract Collection<Production> getLexicalProductionsWithChild(final int child);

    /**
     * Returns the log probability of a binary rule.
     * 
     * @param parent
     * @param leftChild
     * @param rightChild
     * @return Log probability of the specified rule.
     */
    public abstract float binaryLogProbability(final short parent, final short leftChild, final short rightChild);

    /**
     * Returns the log probability of a binary rule.
     * 
     * @param parent
     * @param leftChild
     * @param rightChild
     * @return Log probability of the specified rule.
     */
    public abstract float binaryLogProbability(final String parent, final String leftChild, final String rightChild);

    /**
     * Returns the log probability of the specified parent / child production
     * 
     * @param parent Parent index
     * @param child Child index
     * @return Log probability
     */
    public abstract float unaryLogProbability(final short parent, final short child);

    /**
     * Returns the log probability of the specified parent / child production
     * 
     * @param parent Parent index
     * @param child Child index
     * @return Log probability
     */
    public abstract float unaryLogProbability(final String parent, final String child);

    /**
     * Returns the log probability of a lexical rule.
     * 
     * @param parent
     * @param child
     * @return Log probability of the specified rule.
     */
    public abstract float lexicalLogProbability(final short parent, final int child);

    /**
     * Returns the log probability of a lexical rule.
     * 
     * @param parent
     * @param child
     * @return Log probability of the specified rule.
     */
    public abstract float lexicalLogProbability(final String parent, final String child);

    /**
     * @return Binarization direction
     */
    public abstract Binarization binarization();

    /**
     * Convenience method
     * 
     * @return The number of non-terminals in this grammar
     */
    public abstract int numNonTerms();

    /**
     * Convenience method
     * 
     * @return The number of lexical entries (words) mapped by this grammar
     */
    public abstract int numLexSymbols();

    /**
     * Convenience method
     * 
     * @param nonterminal
     * @return String representation of the specified nonterminal
     */
    public final String mapNonterminal(final short nonterminal) {
        return nonTermSet.getSymbol(nonterminal);
    }

    /**
     * Convenience method
     * 
     * @param nonterminal
     * @return Integer representation of the specified nonterminal
     */
    public final short mapNonterminal(final String nonterminal) {
        return (short) nonTermSet.getIndex(nonterminal);
    }

    /**
     * Convenience method
     * 
     * @param wordIndex
     * @return String representation of the specified word index
     */
    public final String mapLexicalEntry(final int wordIndex) {
        return lexSet.getSymbol(wordIndex);
    }

    /**
     * Convenience method
     * 
     * @param word
     * @return Integer representation of the specified word
     */
    public final int mapLexicalEntry(final String word) {
        return lexSet.getIndex(word);
    }

    /**
     * Convenience method. Returns true if the lexicon contains the specified word.
     * 
     * @param word
     * @return true if the lexicon contains the specified word
     */
    public final boolean hasWord(final String word) {
        return lexSet.hasSymbol(word);
    }

    /**
     * Returns all valid parents of the specified lexical item. See {@link #lexicalLogProbabilities(int)}.
     * 
     * @param wordIndex
     * @return all valid parents of the specified lexical item
     */
    public abstract short[] lexicalParents(int wordIndex);

    /**
     * Returns the probabilities of all valid parents of the specified lexical item. See {@link #lexicalParents(int)}.
     * 
     * @param wordIndex
     * @return all the probabilities of all valid parents of the specified lexical item
     */
    public abstract float[] lexicalLogProbabilities(int wordIndex);

    /**
     * @return the maximum index of a preterminal (POS).
     */
    public abstract short maxPOSIndex();

    /**
     * Returns true if the specified nonterminal is a preterminal
     * 
     * @param nonTerminal
     * @return true if the specified nonterminal is a preterminal
     */
    public abstract boolean isPos(final short nonTerminal);

    /**
     * Returns true if the specified nonterminal is the left child of at least one binary rule
     * 
     * @param nonTerminal
     * @return true if the specified nonterminal is the left child of at least one binary rule
     */
    public abstract boolean isLeftChild(final short nonTerminal);

    /**
     * Returns true if the specified nonterminal is the right child of at least one binary rule
     * 
     * @param nonTerminal
     * @return true if the specified nonterminal is the right child of at least one binary rule
     */
    public abstract boolean isRightChild(final short nonTerminal);

    /**
     * @return a string representation of summary grammar information
     */
    public abstract String getStats();

    /**
     * Convenience method
     * 
     * @return the index of the null symbol
     */
    public short nullSymbol() {
        return (short) nonTermSet.getIndex(nullSymbolStr);
    }
}

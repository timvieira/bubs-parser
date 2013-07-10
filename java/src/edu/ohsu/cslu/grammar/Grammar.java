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

import it.unimi.dsi.fastutil.shorts.ShortArrayList;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.TreeSet;

import edu.ohsu.cslu.datastructs.narytree.NaryTree.Binarization;
import edu.ohsu.cslu.parser.cellselector.CellSelectorModel;
import edu.ohsu.cslu.parser.fom.FigureOfMeritModel;

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
 * TODO Implement an even simpler grammar for use as an intermediate form when transforming one grammar to another.
 * 
 * @author Nathan Bodenstab
 * @author Aaron Dunlop
 * @since 2010
 * 
 *        $Id$
 */
public abstract class Grammar implements Serializable {

    private static final long serialVersionUID = 4L;

    /** Marks the switch from PCFG to lexicon entries in the grammar file */
    public final static String LEXICON_DELIMITER = "===== LEXICON =====";

    public final static String nullSymbolStr = "<null>";

    /**
     * Signature of the first 2 bytes of a binary Java Serialized Object. Allows us to use the same command-line option
     * for serialized and text grammars and auto-detect the format
     */
    protected final static short OBJECT_SIGNATURE = (short) 0xACED;

    // == Grammar Basics ==
    public SymbolSet<String> lexSet = new SymbolSet<String>();

    /**
     * Unknown-word classes for all entries in {@link #lexSet}. Used by some {@link FigureOfMeritModel} and
     * {@link CellSelectorModel} implementations (primarily for discriminative feature extraction). Lazy-initialized on
     * the first access.
     */
    private SymbolSet<String> unkClassSet;

    /**
     * Coarse (unsplit) non-terminal vocabulary. Used by some {@link FigureOfMeritModel} and {@link CellSelectorModel}
     * implementations (primarily for discriminative feature extraction). Lazy-initialized on the first access.
     */
    private SymbolSet<String> coarseVocabulary;

    /**
     * Preterminal set (parts-of-speech). Used by some {@link FigureOfMeritModel} and {@link CellSelectorModel}
     * implementations (primarily for discriminative feature extraction). Lazy-initialized on the first access.
     */
    private SymbolSet<String> posSymbolSet;

    /**
     * Coarse (unsplit) preterminal set (parts-of-speech). Used by some {@link FigureOfMeritModel} and
     * {@link CellSelectorModel} implementations (primarily for discriminative feature extraction). Lazy-initialized on
     * the first access.
     */
    private SymbolSet<String> coarsePosSymbolSet;

    public TokenClassifier tokenClassifier;
    // TODO This field should really be final, but it's initialized in subclass constructors
    public Vocabulary nonTermSet;

    public GrammarFormatType grammarFormat;

    public short startSymbol;
    public String startSymbolStr;
    public int horizontalMarkov;
    public int verticalMarkov;

    /**
     * A compact representation of non-terminal indices for all POS tags. This array excludes phrase-level
     * non-terminals, allowing creation of smaller arrays when only parts-of-speech are needed. Used in FOM
     * initialization and in extracting chart cell features for beam width prediction. {@link #phraseSet} +
     * {@link #posSet} = {@link #nonTermSet}.
     */
    public short[] posSet;
    /** Maps from non-terminal indices to indices in {@link #posSet}. */
    public short[] posIndexMap;

    /**
     * A compact representation of non-terminal indices for all phrase-level tags. This array excludes POS
     * non-terminals, allowing creation of smaller arrays when only phrase-level tags are needed. Used in FOM
     * initialization and in extracting chart cell features for beam width prediction. {@link #phraseSet} +
     * {@link #posSet} = {@link #nonTermSet}.
     */
    public short[] phraseSet;

    public short nullSymbol = -1;
    public int nullWord = -1;

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
     * @return the number of binary productions in this grammar
     */
    public abstract int numBinaryProds();

    /**
     * @return the number of unary productions in this grammar
     */
    public abstract int numUnaryProds();

    /**
     * @return the number of lexical productions in this grammar
     */
    public abstract int numLexProds();

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
        return lexSet.containsKey(word);
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
     * Convenience method. Returns the integer mapping for the null token in the non-terminal set (see also
     * {@link #nullToken()}, which returns an analogous mapping from the lexicon).
     * 
     * @return the index of the null symbol
     */
    public short nullSymbol() {
        return (short) nonTermSet.getIndex(nullSymbolStr);
    }

    /**
     * Convenience method. Returns the integer mapping for the null token in the lexicon (see also {@link #nullSymbol()}
     * , which returns an analogous mapping from the non-terminal set).
     * 
     * @return the index of the null symbol in the lexicon
     */
    public int nullToken() {
        return lexSet.getIndex(nullSymbolStr);
    }

    /**
     * @return Unknown-word classes for all entries in {@link #lexSet}. Used by some {@link FigureOfMeritModel} and
     *         {@link CellSelectorModel} implementations (primarily for discriminative feature extraction). The returned
     *         set does <em>not</em> contain special UNK tokens for sentence-initial words.
     */
    public SymbolSet<String> unkClassSet() {

        if (unkClassSet == null) {
            unkClassSet = new SymbolSet<String>();
            unkClassSet.addSymbol(nullSymbolStr);
            unkClassSet.defaultReturnValue(nullSymbolStr);

            for (final String token : lexSet) {
                unkClassSet.addSymbol(DecisionTreeTokenClassifier.berkeleyGetSignature(token, false, lexSet));
                unkClassSet.addSymbol(DecisionTreeTokenClassifier.berkeleyGetSignature(token, true, lexSet));
            }
        }
        return unkClassSet;
    }

    /**
     * @return Coarse (unsplit) non-terminal vocabulary. Used by some {@link FigureOfMeritModel} and
     *         {@link CellSelectorModel} implementations (primarily for discriminative feature extraction).
     */
    public SymbolSet<String> coarseVocabulary() {

        if (coarseVocabulary == null) {
            // Grammar implementations differ in the order they store their vocabularies. To ensure the coarse
            // representation is consistent, first populate a temporary sorted set.
            final TreeSet<String> tmp = new TreeSet<String>();
            for (final String nt : nonTermSet) {
                tmp.add(grammarFormat.getBaseNT(nt, false));
            }

            coarseVocabulary = new SymbolSet<String>();
            coarseVocabulary.addSymbol(nullSymbolStr);
            coarseVocabulary.defaultReturnValue(nullSymbolStr);
            for (final String coarseNt : tmp) {
                coarseVocabulary.addSymbol(coarseNt);
            }

        }
        return coarseVocabulary;
    }

    /**
     * @return Preterminal set (parts-of-speech). Used by some {@link FigureOfMeritModel} and {@link CellSelectorModel}
     *         implementations (primarily for discriminative feature extraction).
     */
    public SymbolSet<String> posSymbolSet() {
        if (posSymbolSet == null) {
            // Grammar implementations differ in the order they store their vocabularies. To ensure that this
            // representation is consistent, first populate a temporary sorted set.
            final TreeSet<String> tmp = new TreeSet<String>();
            for (final short posIndex : posSet) {
                tmp.add(nonTermSet.getSymbol(posIndex));
            }

            posSymbolSet = new SymbolSet<String>();
            posSymbolSet.addSymbol(nullSymbolStr);
            posSymbolSet.defaultReturnValue(nullSymbolStr);

            for (final String pos : tmp) {
                posSymbolSet.addSymbol(pos);
            }
        }
        return posSymbolSet;
    }

    /**
     * @return Coarse (unsplit) preterminal set (parts-of-speech). Used by some {@link FigureOfMeritModel} and
     *         {@link CellSelectorModel} implementations (primarily for discriminative feature extraction).
     */
    public SymbolSet<String> coarsePosSymbolSet() {

        if (coarsePosSymbolSet == null) {
            // Grammar implementations differ in the order they store their vocabularies. To ensure that the coarse
            // representation is consistent, first populate a temporary sorted set.
            final TreeSet<String> tmp = new TreeSet<String>();
            for (final short posIndex : posSet) {
                tmp.add(grammarFormat.getBaseNT(nonTermSet.getSymbol(posIndex), false));
            }

            coarsePosSymbolSet = new SymbolSet<String>();
            coarsePosSymbolSet.addSymbol(nullSymbolStr);
            coarsePosSymbolSet.defaultReturnValue(nullSymbolStr);

            for (final String coarsePos : tmp) {
                coarsePosSymbolSet.addSymbol(coarsePos);
            }
        }
        return coarsePosSymbolSet;
    }

    /**
     * Initializes {@link Grammar#posSet}, {@link Grammar#posIndexMap}, and {@link Grammar#phraseSet}
     * 
     * @param pos Set of all parts-of-speech
     * @param phrase Set of all phrase-level labels
     */
    protected void initPosAndPhraseSets(final HashSet<String> pos, final HashSet<String> phrase) {

        final ShortArrayList tmpPosList = new ShortArrayList();
        final ShortArrayList tmpPhraseList = new ShortArrayList();
        this.posIndexMap = new short[numNonTerms()];

        for (short i = 0; i < numNonTerms(); i++) {
            if (pos.contains(nonTermSet.getSymbol(i))) {
                tmpPosList.add(i);
                posIndexMap[i] = (short) (tmpPosList.size() - 1);
            }

            // NB: some treebank entries are mislabeled w/ POS tags in the tree an non-terms as POS tags
            // This messes things up if we enforce disjoint sets.
            if (phrase.contains(nonTermSet.getSymbol(i))) {
                tmpPhraseList.add(i);
            }
        }

        this.posSet = tmpPosList.toShortArray();
        this.phraseSet = tmpPhraseList.toShortArray();
    }
}

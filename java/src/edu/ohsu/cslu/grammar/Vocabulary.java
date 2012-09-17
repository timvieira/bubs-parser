package edu.ohsu.cslu.grammar;

import it.unimi.dsi.fastutil.shorts.Short2ShortOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;

import java.util.Collection;

/**
 * TODO Should we use 'base' or 'unsplit' to denote markov-0 categories?
 * 
 * @author Aaron Dunlop
 */
public class Vocabulary extends SymbolSet<String> {

    private static final long serialVersionUID = 1L;

    /** Indices of unsplit categories in the base Markov-order-0 grammar, indexed by non-terminal indices */
    protected Short2ShortOpenHashMap baseNonTerminalIndices = new Short2ShortOpenHashMap();

    /** Base Markov-order-0 vocabulary */
    protected final Vocabulary baseVocabulary;

    private final GrammarFormatType grammarFormat;

    private final ShortOpenHashSet factoredIndices = new ShortOpenHashSet();

    protected short startSymbol;

    private Vocabulary(final GrammarFormatType grammarFormat, final boolean baseVocabulary) {
        this.grammarFormat = grammarFormat;
        this.baseVocabulary = baseVocabulary ? null : new Vocabulary(grammarFormat, true);
    }

    protected Vocabulary(final GrammarFormatType grammarFormat, final Vocabulary baseVocabulary) {
        this.grammarFormat = grammarFormat;
        this.baseVocabulary = baseVocabulary;
    }

    public Vocabulary(final GrammarFormatType grammarFormat) {
        this(grammarFormat, false);
    }

    public Vocabulary(final Collection<String> symbols, final GrammarFormatType grammarFormat) {
        this(grammarFormat);
        for (final String symbol : symbols) {
            addSymbol(symbol);
        }
    }

    public Vocabulary(final String[] symbols, final GrammarFormatType grammarFormat) {
        this(grammarFormat);
        for (final String symbol : symbols) {
            addSymbol(symbol);
        }
    }

    @Override
    public int addSymbol(final String symbol) {
        final short index = (short) super.addSymbol(symbol);
        short baseIndex = 0;
        if (baseVocabulary != null) {
            baseIndex = (short) baseVocabulary.addSymbol(grammarFormat.getBaseNT(symbol, false));
            baseNonTerminalIndices.put(index, baseIndex);
        } else {
            baseNonTerminalIndices.put(index, index);
        }

        // Added by Aaron for (reasonably) fast access to factored non-terminals
        if (grammarFormat != null && grammarFormat.isFactored(symbol)) {
            factoredIndices.add(index);
        }
        return index;
    }

    public final void setStartSymbol(final short nonTerminal) {
        this.startSymbol = nonTerminal;
        if (baseVocabulary != null) {
            baseVocabulary.setStartSymbol(baseNonTerminalIndices.get(nonTerminal));
        }
    }

    public final boolean isFactored(final short nonTerminal) {
        // TODO If this is used a lot, we could replace the ShortOpenHashSet with a boolean[] or a PackedBitVector
        return factoredIndices.contains(nonTerminal);
    }

    public final short getBaseIndex(final short nonTerminal) {
        return baseNonTerminalIndices.get(nonTerminal);
    }

    public final short startSymbol() {
        return startSymbol;
    }

    public final Vocabulary baseVocabulary() {
        return baseVocabulary;
    }

    // public final boolean isBaseFactored(final int baseNonTerminal) {
    // return baseFactoredIndices.contains(baseNonTerminal);
    // }
}

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
package edu.ohsu.cslu.grammar;

import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;

import java.util.Collection;

import edu.ohsu.cslu.lela.SplitVocabulary;
import edu.ohsu.cslu.util.MutableEnumeration;

/**
 * TODO Should we use 'base' or 'unsplit' to denote markov-0 categories?
 * 
 * @author Aaron Dunlop
 */
public class Vocabulary extends MutableEnumeration<String> {

    private static final long serialVersionUID = 1L;

    /** Base Markov-order-0 vocabulary */
    protected Vocabulary baseVocabulary;

    /**
     * Indices of unsplit categories in the base Markov-order-0 grammar, indexed by non-terminal indices. Only populated
     * when {@link #baseVocabulary} is populated.
     */
    protected short[] baseNonTerminalIndices;

    private final GrammarFormatType grammarFormat;

    private final ShortOpenHashSet factoredIndices = new ShortOpenHashSet();

    private short startSymbol;

    protected Vocabulary(final GrammarFormatType grammarFormat, final Vocabulary baseVocabulary) {
        this.grammarFormat = grammarFormat;
        this.baseVocabulary = baseVocabulary;
    }

    public Vocabulary(final GrammarFormatType grammarFormat) {
        this(grammarFormat, null);
    }

    /**
     * Used by {@link SplitVocabulary}. Initializes a base vocabulary
     * 
     * @param symbols
     * @param grammarFormat
     */
    protected Vocabulary(final Collection<String> symbols, final GrammarFormatType grammarFormat) {
        // Initialize a base vocabulary
        this.grammarFormat = grammarFormat;
        this.baseVocabulary = new Vocabulary(grammarFormat);
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
        // TODO Check before re-adding and profile
        final short index = (short) super.addSymbol(symbol);
        // short baseIndex = 0;
        // if (baseVocabulary != null) {
        // baseIndex = (short) baseVocabulary.addSymbol(grammarFormat.getBaseNT(symbol, false));
        // baseNonTerminalIndices.put(index, baseIndex);
        // }

        // Added by Aaron for (reasonably) fast access to factored non-terminals
        if (grammarFormat != null && grammarFormat.isFactored(symbol)) {
            factoredIndices.add(index);
        }
        return index;
    }

    public final void setStartSymbol(final short nonTerminal) {
        this.startSymbol = nonTerminal;
        if (baseVocabulary != null) {
            baseVocabulary.setStartSymbol(baseNonTerminalIndices[nonTerminal]);
        }
    }

    public final boolean isFactored(final short nonTerminal) {
        // TODO If this is used a lot, we could replace the ShortOpenHashSet with a boolean[] or a PackedBitVector
        return factoredIndices.contains(nonTerminal);
    }

    public final short getBaseIndex(final short nonTerminal) {
        return baseNonTerminalIndices[nonTerminal];
    }

    public final short startSymbol() {
        return startSymbol;
    }

    public final synchronized Vocabulary baseVocabulary() {
        if (baseVocabulary == null) {
            baseVocabulary = new Vocabulary(grammarFormat);
            baseNonTerminalIndices = new short[size()];
            for (short nt = 0; nt < list.size(); nt++) {
                final short baseNt = (short) baseVocabulary.addSymbol(grammarFormat.getBaseNT(list.get(nt), false));
                baseNonTerminalIndices[nt] = baseNt;
            }

        }
        return baseVocabulary;
    }
}

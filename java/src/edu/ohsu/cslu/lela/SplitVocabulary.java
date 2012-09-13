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
package edu.ohsu.cslu.lela;

import it.unimi.dsi.fastutil.shorts.Short2ShortOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;

import java.util.Arrays;
import java.util.Collection;

import edu.ohsu.cslu.grammar.GrammarFormatType;
import edu.ohsu.cslu.grammar.Vocabulary;

/**
 * Represents symbols of a state-split vocabulary. e.g. the base NP type might be split into NP_0, NP_1, NP_2, ...
 * 
 * @author Aaron Dunlop
 * @since Feb 3, 2011
 */
public class SplitVocabulary extends Vocabulary {

    private static final long serialVersionUID = 1L;

    /** Parent (pre-split or pre-merge) vocabulary */
    final Vocabulary parentVocabulary;

    /**
     * Maps from the indices of a parent vocabulary to indices in this {@link SplitVocabulary}. Only populated if this
     * vocabulary was created by merging symbols in a parent vocabulary.
     */
    final Short2ShortOpenHashMap parent2IndexMap;

    /**
     * Indices of non-terminals which were created by merging symbols in the parent vocabulary. Only populated if this
     * vocabulary was formed by merging symbols in a parent vocabulary.
     */
    final ShortOpenHashSet mergedIndices;

    /** Split indices for each member of the vocabulary. e.g., 0 for ROOT_0, 5 for NP_5. */
    final byte[] splitIndices;

    /** The number of splits of each base (Markov-0) non-terminal. e.g. 1 (usually) for ',', many for NP */
    final byte[] ntSplitCounts;

    public SplitVocabulary(final Vocabulary parentVocabulary) {
        super(GrammarFormatType.Berkeley);
        this.parentVocabulary = parentVocabulary;
        this.parent2IndexMap = null;
        this.mergedIndices = null;

        this.splitIndices = null;
        this.ntSplitCounts = null;
    }

    public SplitVocabulary(final Collection<String> symbols, final Vocabulary parentVocabulary,
            final Short2ShortOpenHashMap parent2IndexMap, final ShortOpenHashSet mergedIndices) {
        super(symbols, GrammarFormatType.Berkeley);
        this.parentVocabulary = parentVocabulary;
        this.parent2IndexMap = parent2IndexMap;
        this.mergedIndices = mergedIndices;

        this.splitIndices = null;
        this.ntSplitCounts = null;
    }

    /**
     * Creates an unsplit (Markov-0) vocabulary
     * 
     * @param symbols
     */
    public SplitVocabulary(final Collection<String> symbols) {
        super(symbols, GrammarFormatType.Berkeley);

        this.parentVocabulary = null;
        this.parent2IndexMap = null;
        this.mergedIndices = null;

        this.splitIndices = new byte[size()];
        Arrays.fill(splitIndices, (byte) 0);
        this.ntSplitCounts = new byte[size()];
        Arrays.fill(splitIndices, (byte) 1);
    }

    /** For unit testing */
    public SplitVocabulary(final String[] symbols) {
        super(symbols, GrammarFormatType.Berkeley);
        this.parentVocabulary = null;
        this.parent2IndexMap = null;
        this.mergedIndices = null;
        this.splitIndices = null;
        this.ntSplitCounts = null;
    }
}

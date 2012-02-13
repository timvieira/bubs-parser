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

    public SplitVocabulary(final Vocabulary parentVocabulary) {
        super(GrammarFormatType.Berkeley);
        this.parentVocabulary = parentVocabulary;
        this.parent2IndexMap = null;
        this.mergedIndices = null;
    }

    public SplitVocabulary(final String[] symbols) {
        super(symbols, GrammarFormatType.Berkeley);
        this.parentVocabulary = null;
        this.parent2IndexMap = null;
        this.mergedIndices = null;
    }

    public SplitVocabulary(final Collection<String> symbols, final Vocabulary parentVocabulary,
            final Short2ShortOpenHashMap parent2IndexMap, final ShortOpenHashSet mergedIndices) {
        super(symbols, GrammarFormatType.Berkeley);
        this.parentVocabulary = parentVocabulary;
        this.parent2IndexMap = parent2IndexMap;
        this.mergedIndices = mergedIndices;
    }

    public SplitVocabulary(final Collection<String> symbols) {
        this(symbols, null, null, null);
    }

    /**
     * Returns the index (in this vocabulary) of the first non-terminal matching the specified index in the parent
     * vocabulary. If this vocabulary was created by splitting a parent vocabulary, the first matching split is
     * returned. If it was created by merging, then the target of the merge is returned.
     * 
     * TODO Currently unused. Delete?
     * 
     * @param parentVocabularyIndex
     * @return The index (in this vocabulary) of the first non-terminal matching the specified index in the parent
     *         vocabulary
     */
    public short firstSplitNonterminalIndex(final short parentVocabularyIndex) {
        return (short) (parent2IndexMap != null ? (parent2IndexMap.get(parentVocabularyIndex) << 1)
                : (parentVocabularyIndex << 1));
    }
}

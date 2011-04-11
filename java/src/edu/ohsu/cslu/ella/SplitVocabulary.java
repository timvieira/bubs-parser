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
package edu.ohsu.cslu.ella;

import it.unimi.dsi.fastutil.shorts.Short2ShortOpenHashMap;

import java.util.Collection;

import edu.ohsu.cslu.grammar.SymbolSet;

/**
 * Represents symbols of a state-split vocabulary. e.g. the base NP type might be split into NP_0, NP_1, NP_2,
 * ...
 * 
 * @author Aaron Dunlop
 * @since Feb 3, 2011
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class SplitVocabulary extends SymbolSet<String> {

    /**
     * Maps non-terminal indices to positions within the sets of sub-categories derived from the same base
     * category. e.g. NP_0 -> 0, VP_3 -> 3, PP_5 -> 5.
     */
    short[] subcategoryIndices;

    /** Indices of unsplit categories in the base Markov-order-0 grammar, indexed by non-terminal indices */
    short[] baseCategoryIndices;

    /**
     * Indices of the first split categories derived from each category in the Markov-order-0 grammar (indexed
     * by base non-terminal indices)
     */
    short[] firstSubcategoryIndices;

    /**
     * Records the number of splits for each non-terminal (i.e., the number of sub-categories descended from
     * the same unsplit category in the base Markov-order-0 grammar). The start symbol has 1 split, all others
     * will normally be multiples of 2 (the exception being immediately after a merge operation).
     */
    short[] splitCount;

    /** The maximum number of splits from any single unsplit category in the base Markov-order-0 grammar. */
    final short maxSplits;

    final SplitVocabulary parentVocabulary;

    /** Base Markov-order-0 vocabulary */
    final SymbolSet<String> baseVocabulary;

    /**
     * Maps from the indices of a parent vocabulary to indices in this {@link SplitVocabulary}. Only populated
     * if this vocabulary was created by merging symbols in an earlier vocabulary.
     */
    final Short2ShortOpenHashMap mergedIndices;

    public SplitVocabulary(final SplitVocabulary parentVocabulary, final short maxSplits) {
        super();
        this.subcategoryIndices = null;
        this.maxSplits = maxSplits;
        this.parentVocabulary = parentVocabulary;
        this.baseVocabulary = parentVocabulary.baseVocabulary;
        this.mergedIndices = null;
    }

    public SplitVocabulary(final String[] symbols) {
        super(symbols);
        this.parentVocabulary = null;
        this.baseVocabulary = this;
        this.mergedIndices = null;

        recomputeSplits();
        this.maxSplits = maxSplits();
    }

    public SplitVocabulary(final Collection<String> symbols, final SplitVocabulary parentVocabulary,
            final Short2ShortOpenHashMap mergedIndices) {
        super(symbols);
        this.parentVocabulary = parentVocabulary;
        // If no parent is specified, assume this vocabulary is the base vocabulary
        this.baseVocabulary = parentVocabulary != null ? parentVocabulary.baseVocabulary : this;
        this.mergedIndices = mergedIndices;

        recomputeSplits();
        this.maxSplits = maxSplits();
    }

    public SplitVocabulary(final Collection<String> symbols) {
        this(symbols, null, null);
    }

    void recomputeSplits() {
        // Compute:
        //
        // -- Subcategory indices (i.e., the position of each category within the set of splits derived from
        // each base
        // (Markov-0) category; NP_1 -> 1, VP_3 -> 3, etc.)
        //
        // -- Base categories (i.e., the indices in the base vocabulary of the category from which each split
        // is
        // derived)
        //
        // -- Indices of the first subcategory derived from each base (Markov-0) category

        this.subcategoryIndices = new short[size()];
        this.baseCategoryIndices = new short[size()];
        this.firstSubcategoryIndices = new short[baseVocabulary.size()];
        for (short i = 0, j = 0; i < size(); i++) {
            final String[] split = list.get(i).split("_");
            if (split.length > 1) {
                subcategoryIndices[i] = Short.parseShort(split[1]);
            } else {
                subcategoryIndices[i] = 0;
            }
            if (subcategoryIndices[i] == 0) {
                firstSubcategoryIndices[j++] = i;
            }
            baseCategoryIndices[i] = (short) baseVocabulary.getIndex(split[0]);
        }

        // Populate split counts
        this.splitCount = new short[size()];
        short currentNtSplits = (short) (subcategoryIndices[size() - 1] + 1);
        for (int i = size() - 1; i >= 0; i--) {
            splitCount[i] = currentNtSplits;

            if (i > 0 && subcategoryIndices[i] == 0) {
                currentNtSplits = (short) (subcategoryIndices[i - 1] + 1);
            }
        }
    }

    private short maxSplits() {
        short max = 0;
        for (int i = 0; i < subcategoryIndices.length; i++) {
            if (subcategoryIndices[i] + 1 > max) {
                max = subcategoryIndices[i];
            }
        }
        return (short) (max + 1);
    }
}

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
import java.util.regex.Pattern;

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

    private final static Pattern SUBSTATE_PATTERN = Pattern.compile("^.*_[0-9]+$");

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

    /** Split indices for each member of the vocabulary. E.g., 0 for ROOT_0, 5 for NP_5. */
    final byte[] splitIndices;

    /**
     * The number of splits of each base (Markov-0) non-terminal. E.g., 1 (usually) for ',', many for NP. Indexed by
     * base (Markov-0) non-terminal index.
     */
    final byte[] baseNtSplitCounts;

    /**
     * The number of splits of each base (Markov-0) non-terminal. E.g., 1 (usually) for ',', many for NP. Indexed by
     * non-terminal index.
     */
    final byte[] ntSplitCounts;

    /** The index of the first split for each base (Markov-0) non-terminal. */
    final short[] firstSplitIndices;

    /** The maximum number of splits of any non-terminal in the vocabulary */
    final byte maxSplits;

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

        this.maxSplits = 1;

        this.firstSplitIndices = new short[size()];
        for (short i = 0; i < firstSplitIndices.length; i++) {
            firstSplitIndices[i] = i;
        }

        this.baseNtSplitCounts = new byte[size()];
        Arrays.fill(baseNtSplitCounts, (byte) 1);

        this.splitIndices = new byte[size()];
        Arrays.fill(splitIndices, (byte) 0);
        this.ntSplitCounts = new byte[size()];
        Arrays.fill(splitIndices, (byte) 1);
    }

    /**
     * Construct a new {@link SplitVocabulary} from an existing latent-variable {@link Vocabulary}.
     * 
     * @param vocabulary
     */
    public SplitVocabulary(final Vocabulary vocabulary) {
        this(vocabulary, false);
    }

    /**
     * Construct a new {@link SplitVocabulary} from a regular {@link Vocabulary}, optionally splitting each symbol in 2.
     * 
     * @param vocabulary
     * @param split If true, treat the supplied vocabulary as a parent vocabulary and split it vocabulary in 2. If
     *            false, the supplied vocabulary is assumed to be <em>already</em> split (i.e., a latent-variable
     *            vocabulary).
     */
    private SplitVocabulary(final Vocabulary vocabulary, final boolean split) {
        super(GrammarFormatType.Berkeley, vocabulary.baseVocabulary() != null ? vocabulary.baseVocabulary()
                : vocabulary);
        this.parentVocabulary = vocabulary;
        this.parent2IndexMap = null;
        this.mergedIndices = null;

        if (split) {
            // Add a dummy non-terminal for the start symbol. The start symbol is always index 0, and using index 1
            // makes
            // computing other splits simpler. We'll always re-merge the dummy symbol.
            final String sStartSymbol = vocabulary.getSymbol(startSymbol());
            addSymbol(sStartSymbol);
            addSymbol(sStartSymbol + "_1");
            for (int i = 1; i < vocabulary.size(); i++) {
                final String[] substates = substates(vocabulary.getSymbol(i));
                addSymbol(substates[0]);
                addSymbol(substates[1]);
            }
        } else {
            for (int i = 0; i < vocabulary.size(); i++) {
                addSymbol(vocabulary.getSymbol(i));
            }
        }

        this.firstSplitIndices = new short[baseVocabulary.size()];
        this.splitIndices = new byte[size()];
        this.ntSplitCounts = new byte[size()];
        this.baseNtSplitCounts = new byte[baseVocabulary.size()];

        this.maxSplits = init();
    }

    /**
     * Used when merging the vocabulary
     * 
     * @param symbols Post-merge symbol set
     * @param parentVocabulary Pre-merge vocabulary
     * @param parent2IndexMap Map from pre-merge index -> post-merge index
     * @param mergedIndices The set of post-merge indices which were merged 'into'
     */
    SplitVocabulary(final Collection<String> symbols, final Vocabulary parentVocabulary,
            final Short2ShortOpenHashMap parent2IndexMap, final ShortOpenHashSet mergedIndices) {

        super(symbols, GrammarFormatType.Berkeley);

        this.parentVocabulary = parentVocabulary;
        this.parent2IndexMap = parent2IndexMap;
        this.mergedIndices = mergedIndices;

        this.firstSplitIndices = new short[baseVocabulary.size()];
        this.splitIndices = new byte[size()];
        this.ntSplitCounts = new byte[size()];
        this.baseNtSplitCounts = new byte[baseVocabulary.size()];
        Arrays.fill(baseNtSplitCounts, (byte) 1);

        this.maxSplits = init();
    }

    private byte init() {

        Arrays.fill(firstSplitIndices, (short) -1);
        byte tmpMaxSplits = 0;

        for (short nt = 0; nt < splitIndices.length; nt++) {

            final String[] tmp = getSymbol(nt).split("_");

            // Latent annotation of the split (e.g., 0 for X_0 , 1 for X_1)
            final byte latentAnnotation = tmp.length > 1 ? Byte.parseByte(tmp[1]) : 0;
            splitIndices[nt] = latentAnnotation;

            final short baseNt = getBaseIndex(nt);
            if (latentAnnotation >= baseNtSplitCounts[baseNt]) {
                baseNtSplitCounts[baseNt] = (byte) (latentAnnotation + 1);
            }
            if (latentAnnotation > tmpMaxSplits) {
                tmpMaxSplits = latentAnnotation;
            }
            if (firstSplitIndices[baseNt] == -1) {
                firstSplitIndices[baseNt] = nt;
            }
        }

        for (short nt = 0; nt < splitIndices.length; nt++) {
            final short baseNt = getBaseIndex(nt);
            ntSplitCounts[nt] = baseNtSplitCounts[baseNt];
        }

        return (byte) (tmpMaxSplits + 1);
    }

    /**
     * Creates a new vocabulary, splitting each non-terminal into two substates
     * 
     * @return Split vocabulary
     */
    public SplitVocabulary split() {
        return new SplitVocabulary(this, true);
    }

    private String[] substates(final String state) {
        if (SUBSTATE_PATTERN.matcher(state).matches()) {
            final String[] rootAndIndex = state.split("_");
            final int substateIndex = Integer.parseInt(rootAndIndex[1]);
            return new String[] { rootAndIndex[0] + '_' + (substateIndex * 2),
                    rootAndIndex[0] + '_' + (substateIndex * 2 + 1) };
        }
        return new String[] { state + "_0", state + "_1" };
    }

    @Override
    public String toString() {
        if (baseVocabulary == null) {
            return super.toString();
        }

        final StringBuilder sb = new StringBuilder();
        for (short nt = 0; nt < size(); nt++) {
            sb.append(String.format("%d : %s (%s : %d)\n", nt, getSymbol(nt),
                    baseVocabulary.getSymbol(getBaseIndex(nt)), getBaseIndex(nt)));
        }
        return sb.toString();
    }
}

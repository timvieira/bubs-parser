package edu.ohsu.cslu.ella;

import java.util.Collection;

import edu.ohsu.cslu.grammar.SymbolSet;

public class SplitVocabulary extends SymbolSet<String> {

    /**
     * Maps non-terminal indices to positions within the sets of sub-categories derived from the same base category.
     * e.g. NP_0 -> 0, VP_3 -> 3, PP_5 -> 5.
     */
    short[] subcategoryIndices;

    /**
     * Records the number of splits for each non-terminal. e.g. the start symbol has 1 split, all others should be
     * multiples of 2.
     */
    short[] splits;

    final short maxSplits;

    public SplitVocabulary(final short maxSplits) {
        super();
        this.subcategoryIndices = null;
        this.maxSplits = maxSplits;
    }

    public SplitVocabulary(final String[] symbols) {
        super(symbols);
        recomputeSplits();
        this.maxSplits = maxSplits();
    }

    public SplitVocabulary(final Collection<String> symbols) {
        super(symbols);
        recomputeSplits();
        this.maxSplits = maxSplits();
    }

    void recomputeSplits() {
        this.subcategoryIndices = new short[size()];
        for (int i = 0; i < size(); i++) {
            final String[] split = list.get(i).split("_");
            if (split.length > 1) {
                subcategoryIndices[i] = Short.parseShort(split[1]);
            } else {
                subcategoryIndices[i] = 0;
            }
        }

        this.splits = new short[size()];
        short currentNtSplits = (short) (subcategoryIndices[size() - 1] + 1);
        for (int i = size() - 1; i >= 0; i--) {
            splits[i] = currentNtSplits;

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

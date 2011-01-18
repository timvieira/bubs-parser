package edu.ohsu.cslu.ella;

import java.util.Collection;

import edu.ohsu.cslu.grammar.SymbolSet;

public class SplitVocabulary extends SymbolSet<String> {

    /**
     * Maps non-terminal indices to positions within the sets of sub-categories derived from the same base category.
     * e.g. NP_0 -> 0, VP_3 -> 3, PP_5 -> 5.
     */
    short[] subcategoryIndices;

    final short maxSplits;

    public SplitVocabulary(final short maxSplits) {
        super();
        this.subcategoryIndices = null;
        this.maxSplits = maxSplits;
    }

    public SplitVocabulary(final String[] symbols) {
        super(symbols);
        recomputeSubcategoryIndices();
        this.maxSplits = maxSplits();
    }

    public SplitVocabulary(final Collection<String> symbols) {
        super(symbols);
        recomputeSubcategoryIndices();
        this.maxSplits = maxSplits();
    }

    void recomputeSubcategoryIndices() {
        this.subcategoryIndices = new short[size()];
        for (int i = 0; i < size(); i++) {
            final String[] split = list.get(i).split("_");
            if (split.length > 1) {
                subcategoryIndices[i] = Short.parseShort(split[1]);
            } else {
                subcategoryIndices[i] = 0;
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

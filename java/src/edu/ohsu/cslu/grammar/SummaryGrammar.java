package edu.ohsu.cslu.grammar;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.FileReader;
import java.io.Reader;

import edu.ohsu.cslu.parser.ParserOptions.GrammarFormatType;

public class SummaryGrammar extends SortedGrammar {

    private final int V_l, V_r;

    public SummaryGrammar(final String grammarFile, final String lexiconFile,
            final GrammarFormatType grammarFormat) throws Exception {
        this(new FileReader(grammarFile), new FileReader(lexiconFile), grammarFormat);
    }

    public SummaryGrammar(final Reader grammarFile, final Reader lexiconFile,
            final GrammarFormatType grammarFormat) throws Exception {
        super(grammarFile, lexiconFile, grammarFormat);

        final IntSet vlSet = new IntOpenHashSet();
        final IntSet vrSet = new IntOpenHashSet();
        for (final Production p : binaryProductions) {
            vlSet.add(p.leftChild);
            vrSet.add(p.rightChild);
        }
        V_l = vlSet.size();
        V_r = vrSet.size();
    }

    @Override
    public String getStats() {
        return super.getStats() + "V_l: " + V_l + '\n' + "V_r: " + V_r + '\n';
    }

}

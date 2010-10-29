package edu.ohsu.cslu.grammar;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.util.zip.GZIPInputStream;


import cltool4j.BaseCommandlineTool;
import cltool4j.args4j.Argument;

public class SummarizeGrammar extends BaseCommandlineTool {

    @Argument(index = 0, metaVar = "grammar", usage = "Grammar file (text, gzipped text, or binary serialized")
    private String grammarFile;

    public static void main(final String[] args) {
        run(args);
    }

    @Override
    protected void run() throws Exception {

        // Handle gzipped and non-gzipped grammar files
        final InputStream grammarInputStream = grammarFile.endsWith(".gz") ? new GZIPInputStream(new FileInputStream(
                grammarFile)) : new FileInputStream(grammarFile);

        // Read the generic grammar in either text or binary-serialized format.
        final Grammar genericGrammar = Grammar.read(grammarInputStream);

        final SummaryGrammar grammar = new SummaryGrammar(genericGrammar);
        System.out.print(grammar.getStats());
    }

    private class SummaryGrammar extends Grammar {

        private int V_l, V_r;

        public SummaryGrammar(final Reader grammarFile) throws Exception {
            super(grammarFile);
            init();
        }

        public SummaryGrammar(final Grammar g) {
            super(g);
            init();
        }

        private void init() {
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
}

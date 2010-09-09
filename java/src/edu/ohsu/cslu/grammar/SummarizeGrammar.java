package edu.ohsu.cslu.grammar;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.zip.GZIPInputStream;

import org.kohsuke.args4j.Argument;

import cltool.BaseCommandlineTool;

public class SummarizeGrammar extends BaseCommandlineTool {

    @Argument(index = 0, metaVar = "grammar", usage = "Grammar file (text, gzipped text, or binary serialized")
    private String grammarFile;

    public static void main(final String[] args) {
        run(args);
    }

    @Override
    protected void run() throws Exception {

        Reader grammarReader;

        if (grammarFile != null) {
            if (grammarFile.endsWith(".gz")) {
                grammarReader = new InputStreamReader(new GZIPInputStream(new FileInputStream(grammarFile)));
            } else {
                grammarReader = new FileReader(grammarFile);
            }
        } else {
            grammarReader = new InputStreamReader(System.in);
        }

        final SummaryGrammar grammar = new SummaryGrammar(grammarReader);
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

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
package edu.ohsu.cslu.grammar;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import cltool4j.BaseCommandlineTool;
import cltool4j.args4j.Argument;
import cltool4j.args4j.EnumAliasMap;
import cltool4j.args4j.Option;

public class SummarizeGrammar extends BaseCommandlineTool {

    @Argument(index = 0, metaVar = "grammar", usage = "Grammar file (text, gzipped text, or binary serialized")
    private String grammarFile;

    @Option(name = "-m", metaVar = "mode", usage = "Output mode")
    private Mode mode = Mode.SUMMARY;

    @Option(name = "-t", usage = "Time summary")
    private boolean time;

    public static void main(final String[] args) {
        run(args);
    }

    @Override
    protected void run() throws Exception {

        final long startTime = System.currentTimeMillis();

        // Handle gzipped and non-gzipped grammar files
        final InputStream grammarInputStream = grammarFile.endsWith(".gz") ? new GZIPInputStream(new FileInputStream(
                grammarFile)) : new FileInputStream(grammarFile);

        // Read the generic grammar in either text or binary-serialized format.
        final Grammar genericGrammar = Grammar.read(grammarInputStream);

        switch (mode) {
        case SUMMARY:
            System.out.println(new SummaryGrammar(genericGrammar).getStats());
            break;
        case NTS:
            for (final String nt : genericGrammar.nonTermSet) {
                System.out.println(nt);
            }
            break;
        case PARENTS:
            System.out.print(new SummaryGrammar(genericGrammar).parents());
            break;
        case PRE_TERMINALS:
            System.out.print(new SummaryGrammar(genericGrammar).preTerminals());
            break;
        }

        if (time) {
            System.out.format("Summary Time: %d\n", System.currentTimeMillis() - startTime);
        }
    }

    private class SummaryGrammar extends Grammar {

        private int V_l, V_r;

        private final Set<String> parents = new HashSet<String>();
        private final Set<String> preTerminals = new HashSet<String>();

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
                parents.add(p.parentToString());
            }
            for (final Production p : unaryProductions) {
                parents.add(p.parentToString());
            }

            for (final Production p : lexicalProductions) {
                preTerminals.add(p.parentToString());
            }

            parents.removeAll(preTerminals);

            V_l = vlSet.size();
            V_r = vrSet.size();
        }

        public String parents() {
            final StringBuilder sb = new StringBuilder(2048);
            for (final String parent : parents) {
                sb.append(parent);
                sb.append('\n');
            }
            sb.deleteCharAt(sb.length() - 1);
            return sb.toString();
        }

        public String preTerminals() {
            final StringBuilder sb = new StringBuilder(2048);
            for (final String preTerminal : preTerminals) {
                sb.append(preTerminal);
                sb.append('\n');
            }
            sb.deleteCharAt(sb.length() - 1);
            return sb.toString();
        }

        @Override
        public String getStats() {
            return super.getStats() + "V_l: " + V_l + ' ' + "V_r: " + V_r;
        }

    }

    private enum Mode {
        SUMMARY("s"), NTS("n"), PARENTS("p"), PRE_TERMINALS("pt");

        private Mode(final String alias) {
            EnumAliasMap.singleton().addAliases(this, alias);
        }
    }
}

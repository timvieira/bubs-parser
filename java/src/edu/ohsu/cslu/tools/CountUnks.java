/*
 * Copyright 2010-2014, Oregon Health & Science University
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
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Further documentation and contact information is available at
 *   https://code.google.com/p/bubs-parser/ 
 */

package edu.ohsu.cslu.tools;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.io.File;
import java.util.TreeSet;

import cltool4j.BaseCommandlineTool;
import cltool4j.args4j.Argument;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.grammar.DecisionTreeTokenClassifier;
import edu.ohsu.cslu.grammar.SymbolSet;

/**
 * Counts unknown word occurrences in a development / test corpus.
 * 
 * Usage: CountUnks <training corpus> <test corpus>
 * 
 * @author Aaron Dunlop
 */
public class CountUnks extends BaseCommandlineTool {

    @Argument(index = 0, metaVar = "file", usage = "Training corpus")
    private File trainingCorpus;

    @Override
    protected void run() throws Exception {

        final SymbolSet<String> lexicon = new SymbolSet<String>();

        for (final String line : fileLines(trainingCorpus)) {
            for (final String token : tokens(line)) {
                lexicon.addSymbol(token);
            }
        }

        final Object2IntOpenHashMap<String> unkCounts = new Object2IntOpenHashMap<String>();
        int totalWords = 0, totalUnks = 0, totalSentences = 0, sentencesContainingUnk = 0;

        for (final String line : inputLines()) {

            boolean sentenceInitial = true;
            boolean sentenceContainsUnk = false;
            totalSentences++;

            for (final String token : tokens(line)) {
                totalWords++;
                if (!lexicon.containsKey(token)) {
                    final String unkClass = DecisionTreeTokenClassifier.berkeleyGetSignature(token, sentenceInitial, lexicon);
                    unkCounts.put(unkClass, unkCounts.getInt(unkClass) + 1);
                    totalUnks++;
                    sentenceContainsUnk = true;
                }
                sentenceInitial = false;
            }
            if (sentenceContainsUnk) {
                sentencesContainingUnk++;
            }
        }
        final TreeSet<UnkCount> treeSet = new TreeSet<UnkCount>();
        for (final String key : unkCounts.keySet()) {
            treeSet.add(new UnkCount(key, unkCounts.getInt(key)));
        }

        System.out.format("Total    : %d / %d (%.3f%%)\n", totalUnks, totalWords, 100.0 * totalUnks / totalWords);
        System.out.format("Sentences: %d / %d (%.3f%%)\n\n", sentencesContainingUnk, totalSentences, 100.0
                * sentencesContainingUnk / totalSentences);

        for (final UnkCount c : treeSet) {
            System.out.format("%20s : %4d (%.1f%%)\n", c.unkClass, c.count, 100.0 * c.count / totalUnks);
        }
    }

    private String[] tokens(final String line) {
        if (line.charAt(0) == '(' && (line.startsWith("((") || line.startsWith("(TOP") || line.startsWith("(ROOT"))) {
            // Assume tree format
            final NaryTree<String> tree = NaryTree.read(line, String.class);
            return tree.leafLabels();
        }

        return line.split(" +");
    }

    public static void main(final String[] args) {
        run(args);
    }

    private class UnkCount implements Comparable<UnkCount> {
        final String unkClass;
        final int count;

        public UnkCount(final String unkClass, final int count) {
            super();
            this.unkClass = unkClass;
            this.count = count;
        }

        @Override
        public int compareTo(final UnkCount o) {
            if (count > o.count) {
                return -1;
            } else if (count < o.count) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}

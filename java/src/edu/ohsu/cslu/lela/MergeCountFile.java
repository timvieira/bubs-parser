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

package edu.ohsu.cslu.lela;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;

import cltool4j.BaseCommandlineTool;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.grammar.Grammar;

public class MergeCountFile extends BaseCommandlineTool {

    @Option(name = "-m", usage = "Merge index file")
    private File mergeIndexFile;

    @Option(name = "-c", usage = "Count file")
    private File countFile;

    @Override
    protected void run() throws Exception {

        final Object2IntOpenHashMap<String> maxNtSplits = new Object2IntOpenHashMap<String>();
        final HashMap<String, String> mergeMap = new HashMap<String, String>();

        // Read in count file and record the maximum index of each nonterminal
        BufferedReader br = new BufferedReader(new FileReader(countFile));

        boolean inLexicon = false;
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            if (line.equals(Grammar.LEXICON_DELIMITER)) {
                inLexicon = true;
                continue;
            }
            final String[] split = line.split(" ");
            if (inLexicon) {
                // Lexical
                updateMax(split[0], maxNtSplits);
            } else {
                if (split.length == 4) {
                    // Unary
                    updateMax(split[0], maxNtSplits);
                    updateMax(split[2], maxNtSplits);
                } else {
                    // Binary
                    updateMax(split[0], maxNtSplits);
                    updateMax(split[2], maxNtSplits);
                    updateMax(split[3], maxNtSplits);
                }
            }
        }
        br.close();

        // Read in merge file
        br = new BufferedReader(new FileReader(mergeIndexFile));
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            final String[] s = line.split(" ");
            final String nt = s[0];
            int merges = 0;

            int i = 1;
            for (int split = 0; split <= maxNtSplits.get(nt); split++) {
                if (i < s.length && split == Integer.parseInt(s[i])) {
                    merges++;
                    i++;
                }
                mergeMap.put(nt + "_" + split, nt + "_" + (split - merges));
            }
        }

        // Reread count file and merge the NTs
        br = new BufferedReader(new FileReader(countFile));

        inLexicon = false;
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            if (line.equals(Grammar.LEXICON_DELIMITER)) {
                System.out.println(line);
                inLexicon = true;
                continue;
            }
            final String[] split = line.split(" ");
            if (!mergeMap.containsKey(split[0])) {
                System.err.println("Missing map for " + split[0]);
            }
            if (inLexicon) {
                // Lexical
                System.out.format("%s -> %s %s\n", mergeMap.get(split[0]), split[2], split[3]);
            } else {
                if (split.length == 4) {
                    // Unary
                    System.out.format("%s -> %s %s\n", mergeMap.get(split[0]), mergeMap.get(split[2]), split[3]);
                } else {
                    // Binary
                    System.out.format("%s -> %s %s %s\n", mergeMap.get(split[0]), mergeMap.get(split[2]),
                            mergeMap.get(split[3]), split[4]);
                }
            }
        }
        br.close();
    }

    /**
     * @param string
     * @param maxSplits TODO
     */
    private void updateMax(final String nt, final Object2IntOpenHashMap<String> maxNtSplits) {
        final String[] s = nt.split("_");
        final int split = Integer.parseInt(s[1]);
        if (!maxNtSplits.containsKey(s[0]) || split > maxNtSplits.getInt(s[0])) {
            maxNtSplits.put(s[0], split);
        }
    }

    public static void main(final String[] args) {
        run(args);
    }

}

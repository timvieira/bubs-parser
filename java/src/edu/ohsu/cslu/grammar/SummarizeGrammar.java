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
package edu.ohsu.cslu.grammar;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.io.BufferedReader;
import java.util.HashSet;

import cltool4j.BaseCommandlineTool;
import edu.ohsu.cslu.util.Math;
import edu.ohsu.cslu.util.Strings;

/**
 * Reads a BUBS-format grammar and outputs the following statistics:
 * 
 * <pre>
 *  vpos            : POS (preterminal) count
 *  vphrase         : Phrase-level non-terminal count
 *  meanlexchild    : Mean of the lexical children of a preterminal
 *  medlexchild     : Median of the lexical-child count of the preterminals
 *  medrowdensity   : Median row-density of the binary grammar matrix
 *                    (number of binary productions for a parent NT)
 *  medcoldensity   : Median column-density of the binary grammar matrix
 *                    (number of parents of a child pair)
 * </pre>
 * 
 * Note: The phrase-level parent set and the preterminal set are assumed to be disjoint, even though this is not a
 * requirement of a formal PCFG. A grammar violating this assumption should not result in an error, but the summary
 * counts are not guaranteed for such a grammar.
 * 
 * Similarly, grammar productions are assumed to occur only once in the source grammar. If a production occurs more than
 * once (e.g. 'S -> NP VP -.693' followed later in the file by 'S -> NP VP -1.38'), the production will be
 * double-counted.
 * 
 * @author Aaron Dunlop
 */
public class SummarizeGrammar extends BaseCommandlineTool {

    public static void main(final String[] args) {
        run(args);
    }

    @Override
    protected void run() throws Exception {

        final BufferedReader br = inputAsBufferedReader();
        // Read and discard the summary line
        br.readLine();

        final SymbolSet<String> vocabulary = new SymbolSet<String>();
        final HashSet<String> posSet = new HashSet<String>();
        final HashSet<String> phraseSet = new HashSet<String>();

        // Maps <child 1>_<child_2> to a count of observed parents
        final Object2IntOpenHashMap<String> grammarColumnEntries = new Object2IntOpenHashMap<String>();

        // Maps parent to a count of observed children
        final Object2IntOpenHashMap<String> grammarRowEntries = new Object2IntOpenHashMap<String>();

        for (String line = br.readLine(); !line.equals(Grammar.LEXICON_DELIMITER); line = br.readLine()) {
            final String[] tokens = Strings.splitOnSpace(line);

            // Skip blank lines
            if (line.trim().equals("")) {
                continue;
            }

            if (tokens.length == 4) {
                // We don't compute any unary statistics. Just record the non-terminals
                final String parent = tokens[0];
                final String child1 = tokens[2];

                vocabulary.addSymbol(parent);
                vocabulary.addSymbol(child1);
                phraseSet.add(parent);

            } else if (tokens.length == 5) {
                // Binary production: expecting: A -> B C prob
                final String parent = tokens[0];
                final String child1 = tokens[2];
                final String child2 = tokens[3];

                vocabulary.addSymbol(parent);
                vocabulary.addSymbol(child1);
                vocabulary.addSymbol(child2);
                phraseSet.add(parent);

                grammarColumnEntries.add(child1 + '_' + child2, 1);
                grammarRowEntries.add(parent, 1);
            } else {
                throw new IllegalArgumentException("Unexpected line in grammar PCFG\n\t" + line);
            }
        }

        final Object2IntOpenHashMap<String> posLexicalChildren = new Object2IntOpenHashMap<String>();

        // Read Lexicon after finding DELIMITER
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            final String[] tokens = Strings.splitOnSpace(line);

            // Skip blank lines
            if (line.trim().equals("")) {
                continue;
            }

            if (tokens.length != 4) {
                throw new IllegalArgumentException("Unexpected line in grammar lexicon\n\t" + line);
            }

            final String pos = tokens[0];
            vocabulary.addSymbol(pos);
            posSet.add(pos);
            posLexicalChildren.add(pos, 1);
        }

        // Size of POS and phrase-level non-terminal sets
        System.out.format("vpos : %d\n", posSet.size());
        System.out.format("vphrase : %d\n", phraseSet.size());

        // Mean and median of the lexical-child count of each POS
        final int[] posLexicalChildrenCounts = posLexicalChildren.values().toIntArray();
        System.out.format("meanlexchild : %.3f\n", Math.mean(posLexicalChildrenCounts));
        System.out.format("medlexchild : %.1f\n", Math.median(posLexicalChildrenCounts));

        // Median row and column density of the binary grammar matrix
        System.out.format("medrowdensity : %.3f\n", Math.median(grammarRowEntries.values().toIntArray()));
        System.out.format("medcoldensity : %.3f\n", Math.median(grammarColumnEntries.values().toIntArray()));
    }

}

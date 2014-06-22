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

import java.io.BufferedReader;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import cltool4j.BaseCommandlineTool;
import cltool4j.args4j.Argument;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.parser.Util;

/**
 * Merges multiple independent berkeley-format grammar files. Assumes no grammar will include more than 100 splits for a
 * nonterminal.
 */
public class MergeGrammars extends BaseCommandlineTool {

    @Argument(multiValued = true)
    File[] grammarFiles;

    final int MAX_SPLITS = 100;

    @Override
    protected void run() throws Exception {
        // Divide each rule headed by a grammar start symbol by the number of grammars we're merging (e.g., if merging 4
        // grammars, the probability of each rule with the start symbol as a parent should be divided by 4. Other rule
        // probabilities are unchanged).
        final float decrement = (float) Math.log(1.0 / grammarFiles.length);

        // Open readers for each grammar and read the header line
        final BufferedReader[] readers = new BufferedReader[grammarFiles.length];
        HashMap<String, String> att0 = null;
        int vocabSize = 0, nBinary = 0, nUnary = 0, nLex = 0;

        for (int i = 0; i < grammarFiles.length; i++) {
            readers[i] = fileAsBufferedReader(grammarFiles[i]);
            final HashMap<String, String> attributes = Util.readKeyValuePairs(readers[i].readLine());
            if (i == 0) {
                att0 = attributes;
            }
            vocabSize += Integer.parseInt(attributes.get("vocabSize"));
            nBinary += Integer.parseInt(attributes.get("nBinary"));
            nUnary += Integer.parseInt(attributes.get("nUnary"));
            nLex += Integer.parseInt(attributes.get("nLex"));
        }

        // Assume all grammars use the same language, start symbol, etc. as the first grammar...
        @SuppressWarnings("null")
        final String startSymbol = att0.get("start");
        System.out
                .format("lang=%s format=Berkeley unkThresh=%s start=%s hMarkov=%s vMarkov=%s date=%s vocabSize=%d nBinary=%d nUnary=%d nLex=%d\n",
                        att0.get("lang"), att0.get("unkThresh"), att0.get("start"), att0.get("hMarkov"),
                        att0.get("vMarkov"), new SimpleDateFormat("yyyy/mm/dd").format(new Date()), vocabSize, nBinary,
                        nUnary, nLex);

        //
        // Output phrase-level rulues for each grammar
        //
        for (int i = 0; i < grammarFiles.length; i++) {
            final int ntOffset = i * MAX_SPLITS;
            for (String line = readers[i].readLine(); !line.equals(Grammar.LEXICON_DELIMITER); line = readers[i]
                    .readLine()) {
                final String[] split = line.split(" ");
                if (split[0].equals(startSymbol)) {
                    // Assume unary. Output normalized probability
                    System.out.format("%s -> %s %.4f\n", startSymbol, incrementedNt(split[2], ntOffset),
                            Float.parseFloat(split[3]) + decrement);
                } else if (split.length == 4) {
                    // Unary
                    System.out.format("%s -> %s %s\n", incrementedNt(split[0], ntOffset),
                            incrementedNt(split[2], ntOffset), split[3]);
                } else {
                    // Binary
                    System.out.format("%s -> %s %s %s\n", incrementedNt(split[0], ntOffset),
                            incrementedNt(split[2], ntOffset), incrementedNt(split[3], ntOffset), split[4]);
                }
            }
        }

        System.out.println(Grammar.LEXICON_DELIMITER);

        //
        // And lexical rules (assume no lexical rule is headed by the start symbol)
        //
        for (int i = 0; i < grammarFiles.length; i++) {
            final int ntOffset = i * MAX_SPLITS;
            for (String line = readers[i].readLine(); line != null; line = readers[i].readLine()) {
                if (line.isEmpty()) {
                    continue;
                }
                final String[] split = line.split(" ");
                System.out.format("%s -> %s %s\n", incrementedNt(split[0], ntOffset), split[2], split[3]);
            }

            readers[i].close();
        }
    }

    private String incrementedNt(final String nt, final int ntOffset) {
        final String[] split = nt.split("_");
        return split[0] + "_" + (Integer.parseInt(split[1]) + ntOffset);
    }

    public static void main(final String[] args) {
        run(args);
    }

}

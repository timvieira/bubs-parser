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

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;

import cltool4j.BaseCommandlineTool;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.datastructs.narytree.NaryTree.Binarization;
import edu.ohsu.cslu.grammar.DecisionTreeTokenClassifier;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.GrammarFormatType;
import edu.ohsu.cslu.grammar.Language;
import edu.ohsu.cslu.parser.Util;
import edu.ohsu.cslu.util.MutableEnumeration;

/**
 * Induces a basic PCFG from a treebank
 * 
 * @author Aaron Dunlop
 */
public class InduceCountGrammar extends BaseCommandlineTool {

    @Option(name = "-u", metaVar = "file,weight", usage = "Tree file which should be upweighted relative to other counts (filename,weight e.g. foo.trees,2)")
    private String[] upweightedFiles;

    @Option(name = "-gf", aliases = { "--grammar-format" }, metaVar = "format", usage = "Grammar Format; required if binarization is specified")
    private GrammarFormatType grammarFormatType = GrammarFormatType.Berkeley;

    @Option(name = "-unk", aliases = { "-rw" }, metaVar = "threshold", usage = "Smooth production probabilities with unknown-word probabilities for rare words")
    private int rareWordThreshold = 20;

    @Option(name = "-c", aliases = { "--count-file" }, metaVar = "file", usage = "Rule counts derived from some other corpus or grammar induction process")
    private File countFile;

    @Option(name = "-cfw", aliases = { "--count-file-weight" }, metaVar = "weight", usage = "Weight to apply when incorporating count file. 0 = unused = 0, 1 = balanced, 100 = 100:1 upweighting")
    private float countFileWeight = 1f;

    @Option(name = "-ot", aliases = { "--open-class-threshold" }, metaVar = "threshold", usage = "Learn unknown-word probabilities for tags producing at least n words")
    private int openClassPreterminalThreshold = 50;

    @Option(name = "-l", aliases = { "--language" }, metaVar = "language", usage = "Language. Output in grammar file headers.")
    private Language language = Language.English;

    @Option(name = "-b", aliases = { "--binarization" }, metaVar = "type", usage = "Binarization direction.")
    private Binarization binarization;

    @Override
    protected void run() throws IOException {

        // Induce grammar from training corpus
        final StringCountGrammar scg = new StringCountGrammar(new BufferedReader(new InputStreamReader(System.in)),
                binarization, grammarFormatType);

        // Incorporate upweighted files (if supplied)
        if (upweightedFiles != null) {
            for (final String f : upweightedFiles) {
                final String[] split = f.split(",");
                final int increment = split.length > 1 ? Integer.parseInt(split[1]) : 1;
                final BufferedReader br = new BufferedReader(new FileReader(split[0]));
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    scg.readLine(line, binarization, grammarFormatType, increment);
                }
            }
        }

        final float corpusCounts = scg.totalRuleCounts();

        // Read in count file and incorporate (if supplied)
        if (countFile != null) {
            // Read in count file and sum total counts
            float countFileCounts = 0;

            BufferedReader br = new BufferedReader(new FileReader(countFile));
            // Compare start symbols (or assign from the count file if the input was empty)
            final HashMap<String, String> keyVals = Util.readKeyValuePairs(br.readLine().trim());
            if (scg.startSymbol == null) {
                scg.startSymbol = keyVals.get("start");
            } else if (scg.startSymbol.equals(keyVals.get("start"))) {
                throw new IllegalArgumentException("Mismatched start symbols: " + scg.startSymbol + " vs "
                        + keyVals.get("start"));
            }

            for (String line = br.readLine(); line != null; line = br.readLine()) {
                if (line.equals(Grammar.LEXICON_DELIMITER)) {
                    continue;
                }
                final String[] split = line.split(" ");
                countFileCounts += Float.parseFloat(split[split.length - 1]);
            }
            br.close();

            final float upweighting = corpusCounts > 0 ? corpusCounts / countFileCounts * countFileWeight : 1f;

            // Reread count file and incorporate into the count grammar
            br = new BufferedReader(new FileReader(countFile));

            // Discard the header line
            br.readLine();

            boolean inLexicon = false;
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                if (line.equals(Grammar.LEXICON_DELIMITER)) {
                    inLexicon = true;
                    continue;
                }
                final String[] split = line.split(" ");
                if (inLexicon) {
                    // Lexical
                    scg.incrementLexicalCount(split[0].intern(), split[2].intern(), Float.parseFloat(split[3])
                            * upweighting);
                } else {
                    if (split.length == 4) {
                        // Unary
                        scg.incrementUnaryCount(split[0].intern(), split[2].intern(), Float.parseFloat(split[3])
                                * upweighting);
                    } else {
                        // Binary
                        scg.incrementBinaryCount(split[0].intern(), split[2].intern(), split[3].intern(),
                                Float.parseFloat(split[4]) * upweighting);
                    }
                }
            }
            br.close();
        }

        final FractionalCountGrammar fcg = scg.toFractionalCountGrammar();

        // Add UNK productions
        final FractionalCountGrammar grammarWithUnks = fcg.addUnkCounts(unkClassMap(fcg.lexicon),
                openClassPreterminalThreshold, 0f, .1f, .5f);

        grammarWithUnks.write(new PrintWriter(System.out), false, language, grammarFormatType, rareWordThreshold);
    }

    private Int2IntOpenHashMap unkClassMap(final MutableEnumeration<String> lexicon) {
        final Int2IntOpenHashMap unkClassMap = new Int2IntOpenHashMap();
        for (int i = 0; i < lexicon.size(); i++) {
            unkClassMap.put(i, lexicon.addSymbol(DecisionTreeTokenClassifier.berkeleyGetSignature(lexicon.getSymbol(i), false, lexicon)));
        }
        return unkClassMap;
    }

    public static void main(final String[] args) {
        run(args);
    }
}

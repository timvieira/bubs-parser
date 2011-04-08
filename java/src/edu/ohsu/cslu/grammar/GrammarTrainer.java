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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import cltool4j.BaseCommandlineTool;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.parser.ParseTree;
import edu.ohsu.cslu.tools.TreeTools;

public class GrammarTrainer extends BaseCommandlineTool {

    @Option(name = "-rightFactor", usage = "Right factor grammar (default: left factor)")
    private boolean rightFactor = false;

    @Option(name = "-hMarkov", usage = "Horizontal Markov order of output Grammar")
    private int horizontalMarkov = 0;

    @Option(name = "-vMarkov", usage = "Vertical Markov order of output Grammar")
    private int verticalMarkov = 0;

    @Option(name = "-annotatePOS", usage = "Output Grammar has annotation on POS tags")
    private boolean annotatePOS = false;

    @Option(name = "-minUnkCount", usage = "")
    private int minUnkCount = 5;

    private static BufferedReader inputStream = new BufferedReader(new InputStreamReader(System.in));

    // private static BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(System.out));

    @Override
    protected void run() throws Exception {

        final List<ParseTree> trainingTrees = new LinkedList<ParseTree>();

        for (String line = inputStream.readLine(); line != null; line = inputStream.readLine()) {
            final ParseTree tree = ParseTree.readBracketFormat(line);
            TreeTools.binarizeTree(tree, rightFactor, horizontalMarkov, verticalMarkov, annotatePOS,
                    GrammarFormatType.CSLU);
            // System.out.println(tree.toString());
            trainingTrees.add(tree);
        }

        final Grammar grammar = induceGrammar(trainingTrees);
        System.out.println(grammar.toString());
    }

    private Grammar induceGrammar(final List<ParseTree> trainingTrees) {

        final SymbolSet<String> lexicon = generateLexicon(trainingTrees, minUnkCount);
        final Tokenizer tokenizer = new Tokenizer(lexicon);

        final HashMap<String, Integer> lhsCount = new HashMap<String, Integer>();
        final HashMap<String, Integer> prodCount = new HashMap<String, Integer>();

        // count production frequency
        for (final ParseTree tree : trainingTrees) {
            // Replace infrequent words with UNK
            int sentIndex = 0;
            for (final ParseTree leaf : tree.getLeafNodes()) {
                if (lexicon.contains(leaf.contents) == false) {
                    leaf.contents = tokenizer.wordToUnkString(leaf.contents, sentIndex);
                }
                sentIndex++;
            }

            for (final ParseTree node : tree.preOrderTraversal()) {
                if (node.children.size() > 0) {
                    addCount(lhsCount, node.contents, 1);
                    addCount(prodCount, node.contents + " " + node.childrenToString(), 1);
                }
            }
        }

        // final Grammar grammar = new Grammar();
        // return grammar;
        return null;
    }

    private void addCount(final HashMap<String, Integer> counts, final String key, final int value) {
        if (counts.containsKey(key)) {
            counts.put(key, counts.get(key) + value);
        } else {
            counts.put(key, value);
        }
    }

    private SymbolSet<String> generateLexicon(final List<ParseTree> trees, final int _minUnkCount) {
        final HashMap<String, Integer> wordCounts = new HashMap<String, Integer>();
        for (final ParseTree tree : trees) {
            for (final String word : tree.getLeafNodesContent()) {
                if (wordCounts.containsKey(word)) {
                    wordCounts.put(word, wordCounts.get(word) + 1);
                } else {
                    wordCounts.put(word, 1);
                }
            }
        }

        final SymbolSet<String> lexicon = new SymbolSet<String>();
        for (final Entry<String, Integer> entry : wordCounts.entrySet()) {
            if (entry.getValue() >= _minUnkCount) {
                lexicon.addSymbol(entry.getKey());
            }
        }

        return lexicon;
    }

    public static void main(final String[] args) throws Exception {
        run(args);
    }

}

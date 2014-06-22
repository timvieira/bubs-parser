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
package edu.ohsu.cslu.parser.fom;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;

import edu.ohsu.cslu.counters.SimpleCounterSet;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.parser.ParseTree;
import edu.ohsu.cslu.parser.Parser.ResearchParserType;

public class PriorFOM extends FigureOfMeritModel {

    private float[] priorProb;
    Grammar grammar;

    public PriorFOM(final FOMType type, final Grammar grammar, final BufferedReader modelStream) throws IOException {

        super(type);
        this.grammar = grammar;

        final int numNT = grammar.numNonTerms();
        priorProb = new float[numNT];
        Arrays.fill(priorProb, Float.NEGATIVE_INFINITY); // Init values to log(0) = -Inf

        readModel(modelStream);
    }

    @Override
    public FigureOfMerit createFOM() {
        return new PriorFOMSelector();
    }

    public void readModel(final BufferedReader inStream) throws IOException {
        String line;
        while ((line = inStream.readLine()) != null) {
            final String[] tokens = line.split("\\s+");
            if (tokens.length > 0 && !tokens[0].equals("#")) {
                final int ntIndex = grammar.nonTermSet.getIndex(tokens[0]);
                final float logProb = Float.parseFloat(tokens[1]);
                priorProb[ntIndex] = logProb;
            }

        }
    }

    public static void train(final BufferedReader inStream, final BufferedWriter outStream, final String grammarFile)
            throws Exception {
        PriorFOM.train(inStream, outStream, grammarFile, 0.5, false);
    }

    public static void train(final BufferedReader inStream, final BufferedWriter outStream, final String grammarFile,
            final double smoothingCount, final boolean writeCounts) throws Exception {
        String line;
        ParseTree tree;
        final SimpleCounterSet<String> ntCount = new SimpleCounterSet<String>();

        // To train a BoundaryInOut FOM model we need a grammar and
        // binarized gold input trees with NTs from same grammar
        final Grammar grammar = readGrammar(grammarFile, ResearchParserType.ECPCellCrossList, null);

        while ((line = inStream.readLine()) != null) {
            tree = ParseTree.readBracketFormat(line);
            if (tree.isBinaryTree() == false) {
                System.err.println("ERROR: Training trees must be binarized exactly as used in decoding grammar");
                System.exit(1);
            }

            for (final ParseTree node : tree.preOrderTraversal()) {
                if (node.isLeaf() == false) {
                    if (grammar.nonTermSet.containsKey(node.contents) == false) {
                        throw new IOException("Nonterminal '" + node.contents
                                + "' in input tree not found in grammar.  Exiting.");
                    }
                    ntCount.increment(node.contents, "all");
                }
            }
        }

        final int numNT = grammar.numNonTerms() + grammar.posSet.length;

        // smooth counts
        if (smoothingCount > 0) {
            ntCount.smoothAddConst(smoothingCount, numNT);
        }

        // Write model to file
        float score;
        outStream.write("# model=FOM type=Prior addXsmoothing=" + smoothingCount + "\n");

        for (final String ntStr : grammar.nonTermSet) {
            if (writeCounts) {
                score = ntCount.getCount(ntStr, "all");
            } else {
                score = (float) Math.log(ntCount.getProb(ntStr, "all"));
            }
            if (score > Float.NEGATIVE_INFINITY) {
                outStream.write(ntStr + "\t" + score + "\n");
            }
        }

        outStream.close();
    }

    public class PriorFOMSelector extends FigureOfMerit {

        private static final long serialVersionUID = 1L;

        @Override
        public float calcFOM(final int start, final int end, final short parent, final float insideProbability) {
            return normInside(start, end, insideProbability) + priorProb[parent];
        }

        @Override
        public final float calcLexicalFOM(final int start, final int end, final short parent,
                final float insideProbability) {
            return insideProbability + priorProb[parent];
        }
    }
}

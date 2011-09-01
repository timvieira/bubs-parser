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
package edu.ohsu.cslu.parser;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.logging.Level;

import cltool4j.BaseLogger;
import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.Chart;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;

public abstract class ChartParser<G extends Grammar, C extends Chart> extends Parser<G> {

    public C chart;

    public ChartParser(final ParserDriver opts, final G grammar) {
        super(opts, grammar);
    }

    @Override
    public BinaryTree<String> findBestParse(final int[] tokens) {
        if (collectDetailedStatistics) {
            final long t0 = System.currentTimeMillis();
            init(tokens);
            parseTask.chartInitMs = System.currentTimeMillis() - t0;
        } else {
            init(tokens);
        }

        insidePass();
        if (BaseLogger.singleton().isLoggable(Level.FINEST)) {
            BaseLogger.singleton().finest(chart.toString());
        }
        return extract();
    }

    /**
     * Initialize the chart structure, edge selector, and cell selector
     * 
     * @param tokens
     */
    protected void init(final int[] tokens) {
        initSentence(tokens);
        addLexicalProductions(tokens);

        if (fomModel != null) {
            if (collectDetailedStatistics) {
                final long t1 = System.currentTimeMillis();
                fomModel.init(parseTask);
                parseTask.fomInitMs = System.currentTimeMillis() - t1;
            } else {
                fomModel.init(parseTask);
            }
        }

        if (collectDetailedStatistics) {
            final long t2 = System.currentTimeMillis();
            cellSelector.initSentence(this);
            parseTask.ccInitMs = System.currentTimeMillis() - t2;
        } else {
            cellSelector.initSentence(this);
        }
    }

    /**
     * Executes the inside / viterbi parsing pass
     */
    protected final void insidePass() {
        while (cellSelector.hasNext()) {
            final short[] startAndEnd = cellSelector.next();
            computeInsideProbabilities(startAndEnd[0], startAndEnd[1]);
        }
    }

    protected final BinaryTree<String> extract() {
        if (collectDetailedStatistics) {
            final long t3 = System.currentTimeMillis();
            final BinaryTree<String> parseTree = chart.extractBestParse(grammar.startSymbol);
            parseTask.extractTimeMs = System.currentTimeMillis() - t3;
            return parseTree;
        }

        return chart.extractBestParse(grammar.startSymbol);
    }

    /**
     * Each subclass will implement this method to perform the inner-loop grammar intersection.
     * 
     * @param start
     * @param end
     */
    protected abstract void computeInsideProbabilities(short start, short end);

    @SuppressWarnings("unchecked")
    protected void initSentence(final int[] tokens) {
        chart = (C) new CellChart(tokens, this);
    }

    protected void addLexicalProductions(final int tokens[]) {
        // add lexical productions to the base cells of the chart
        for (int i = 0; i < chart.size(); i++) {
            final ChartCell cell = chart.getCell(i, i + 1);
            for (final Production lexProd : grammar.getLexicalProductionsWithChild(tokens[i])) {
                cell.updateInside(lexProd, cell, null, lexProd.prob);
            }
        }
    }

    @Override
    public float getInside(final int start, final int end, final int nt) {
        return chart.getInside(start, end, nt);
    }

    @Override
    public float getOutside(final int start, final int end, final int nt) {
        return chart.getOutside(start, end, nt);
    }

    @Override
    public String getStats() {
        return chart.getStats()
                + (collectDetailedStatistics ? String.format(" fomInitTime=%d cellSelectorInitTime=%d",
                        parseTask.fomInitMs, parseTask.ccInitMs) : "");
    }

    public int getCellFeaturesCount(final String[] featureNames) {
        return getCellFeatures(-99, -99, featureNames, null).vectorLength();
    }

    public SparseBitVector getCellFeatures(final int start, final int end, final String[] featureNames) {
        return getCellFeatures(start, end, featureNames, null);
    }

    public SparseBitVector getCellFeatures(final int start, final int end, final String[] featureNames,
            final NaryTree<String> goldTree) {
        int numFeats = 0;
        final IntList featIndices = new IntArrayList(10);

        final int numTags = grammar.posSet.size();
        final int numWords = Grammar.lexSet.size();

        // TODO Create a feature enum. Pre-tokenize the feature template once per sentence into an EnumSet (in
        // CellSelector.initSentence()) and make this a large switch statement. Should help with
        // initialization time,
        // although it's not a huge priority, since that init time is only ~5% of the total time.
        for (final String featStr : featureNames) {

            // Left tags
            if (featStr.startsWith("lt")) {
                if (featStr.equals("lt")) {
                    featIndices.add(numFeats + getPOSFeat(start));
                    numFeats += numTags;
                } else if (featStr.equals("lt+1")) {
                    featIndices.add(numFeats + getPOSFeat(start + 1));
                    numFeats += numTags;
                } else if (featStr.equals("lt+2")) {
                    featIndices.add(numFeats + getPOSFeat(start + 2));
                    numFeats += numTags;
                } else if (featStr.equals("lt-1")) {
                    featIndices.add(numFeats + getPOSFeat(start - 1));
                    numFeats += numTags;
                } else if (featStr.equals("lt-2")) {
                    featIndices.add(numFeats + getPOSFeat(start - 2));
                    numFeats += numTags;
                } else if (featStr.equals("lt_lt-1")) {
                    featIndices.add(numFeats + getPOSFeat(start) + numTags * getPOSFeat(start - 1));
                    numFeats += numTags * numTags;
                }

            } else if (featStr.startsWith("rt")) {
                // Right tags -- to get the last tag inside the constituent, we need to subtract 1
                if (featStr.equals("rt")) {
                    featIndices.add(numFeats + getPOSFeat(end - 1));
                    numFeats += numTags;
                } else if (featStr.equals("rt+1")) {
                    featIndices.add(numFeats + getPOSFeat(end));
                    numFeats += numTags;
                } else if (featStr.equals("rt+2")) {
                    featIndices.add(numFeats + getPOSFeat(end + 1));
                    numFeats += numTags;
                } else if (featStr.equals("rt-1")) {
                    featIndices.add(numFeats + getPOSFeat(end - 2));
                    numFeats += numTags;
                } else if (featStr.equals("rt-2")) {
                    featIndices.add(numFeats + getPOSFeat(end - 3));
                    numFeats += numTags;
                } else if (featStr.equals("rt_rt+1")) {
                    featIndices.add(numFeats + getPOSFeat(end) + numTags * getPOSFeat(end + 1));
                    numFeats += numTags * numTags;
                }

            } else if (featStr.startsWith("lw")) {
                // Left words
                if (featStr.equals("lw")) {
                    featIndices.add(numFeats + getWordFeat(start));
                    numFeats += numWords;
                } else if (featStr.equals("lw-1")) {
                    featIndices.add(numFeats + getWordFeat(start - 1));
                    numFeats += numWords;
                } else if (featStr.equals("lw_lt")) {
                    featIndices.add(numFeats + getWordFeat(start) + numWords * getPOSFeat(start));
                    numFeats += numWords * numTags;
                } else if (featStr.equals("lw-1_lt-1")) {
                    featIndices.add(numFeats + getWordFeat(start - 1) + numWords * getPOSFeat(start - 1));
                    numFeats += numWords * numTags;
                }

            } else if (featStr.startsWith("rw")) {
                // Right words
                if (featStr.equals("rw")) {
                    featIndices.add(numFeats + getWordFeat(end - 1));
                    numFeats += numWords;
                } else if (featStr.equals("rw+1")) {
                    featIndices.add(numFeats + getWordFeat(end));
                    numFeats += numWords;
                } else if (featStr.equals("rw_rt")) {
                    featIndices.add(numFeats + getWordFeat(end - 1) + numWords * getPOSFeat(end - 1));
                    numFeats += numWords * numTags;
                } else if (featStr.equals("rw+1_rt+1")) {
                    featIndices.add(numFeats + getWordFeat(end) + numWords * getPOSFeat(end));
                    numFeats += numWords * numTags;
                }
            } else if (featStr.equals("loc")) { // cell location

                final int span = end - start;
                final int sentLen = parseTask.sentenceLength();
                for (int i = 1; i <= 5; i++) {
                    if (span == i) {
                        featIndices.add(numFeats); // span length 1-5
                    }
                    numFeats++;
                    if (span >= i * 10) {
                        featIndices.add(numFeats); // span > 10,20,30,40,50
                    }
                    numFeats++;
                    if ((float) span / sentLen >= i / 5.0) {
                        featIndices.add(numFeats); // relative span width btwn 0 and 1
                    }
                    numFeats++;
                }

                if (span == sentLen) {
                    featIndices.add(numFeats); // TOP cell
                }
                numFeats++;

            } else {
                throw new IllegalArgumentException("ERROR parsing feature template.  Not expecting '" + featStr + "'");
            }
        }

        return new SparseBitVector(numFeats, featIndices.toIntArray());
    }

    // map from sparse POS index to compact ordering by using grammar.posSet
    private int getPOSFeat(final int tokIndex) {
        if (tokIndex < 0 || tokIndex >= parseTask.sentenceLength()) {
            return grammar.posSet.getIndex(grammar.nullSymbol);
        }
        // return grammar.posSet.getIndex(((BoundaryInOutSelector) this.fomModel).get1bestPOSTag(tokIndex));
        return grammar.posSet.getIndex(parseTask.tags[tokIndex]);
    }

    private int getWordFeat(final int tokIndex) {
        if (tokIndex < 0 || tokIndex >= parseTask.sentenceLength()) {
            return grammar.nullWord;
        }
        return parseTask.tokens[tokIndex];
    }
}

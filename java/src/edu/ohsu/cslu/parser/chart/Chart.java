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
package edu.ohsu.cslu.parser.chart;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.ParseTree;

public abstract class Chart {

    protected int size;
    public int[] tokens;
    protected Grammar grammar;
    public ParseTask parseTask;

    protected Chart() {
    }

    public Chart(final ParseTask parseTask, final Grammar grammar) {
        this.parseTask = parseTask;
        this.grammar = grammar;
        this.tokens = parseTask.tokens;
        this.size = tokens.length;
    }

    /**
     * The number of cells in the bottom (lexical) row of this chart
     */
    public final int size() {
        return size;
    }

    /**
     * Returns the specified cell.
     * 
     * @param start
     * @param end
     * @return the specified cell.
     */
    public abstract ChartCell getCell(final int start, final int end);

    /**
     * Returns the root cell
     * 
     * @return the root cell
     */
    public ChartCell getRootCell() {
        return getCell(0, size);
    }

    /**
     * Updates the inside probability of the specified non-terminal in the a cell.
     * 
     * @param start
     * @param end
     * @param nonTerminal Non-terminal index
     * @param insideProbability New inside probability
     */
    public abstract void updateInside(int start, int end, int nonTerminal, float insideProbability);

    /**
     * Returns the inside probability of the specified non-terminal in the a cell.
     * 
     * @param start
     * @param end
     * @param nonTerminal Non-terminal index
     * @return the inside probability of the specified non-terminal in the a cell.
     */
    public abstract float getInside(int start, int end, int nonTerminal);

    public float getOutside(final int start, final int end, final int nonTermainl) {
        throw new IllegalArgumentException("Chart instance does not support getOutside(start,end,nt)");
    }

    public boolean hasCompleteParse(final int startSymbol) {
        return getRootCell().getBestEdge(startSymbol) != null;
    }

    public BinaryTree<String> extractBestParse(final int startSymbol) {
        return extractBestParse(0, size, startSymbol);
    }

    public BinaryTree<String> extractBestParse(final int start, final int end, final int nonTermIndex) {
        ChartEdge bestEdge;
        BinaryTree<String> curNode = null;
        final ChartCell cell = getCell(start, end);

        if (cell != null) {
            bestEdge = cell.getBestEdge(nonTermIndex);
            if (bestEdge != null) {
                curNode = new BinaryTree<String>(bestEdge.prod.parentToString());
                if (bestEdge.prod.isUnaryProd()) {
                    curNode.addChild(extractBestParse(start, end, bestEdge.prod.leftChild));
                } else if (bestEdge.prod.isLexProd()) {
                    curNode.addChild(new BinaryTree<String>(bestEdge.prod.childrenToString()));
                } else { // binary production
                    curNode.addChild(extractBestParse(start, bestEdge.midpt(), bestEdge.prod.leftChild));
                    curNode.addChild(extractBestParse(bestEdge.midpt(), end, bestEdge.prod.rightChild));
                }
            }
        }

        return curNode;
    }

    public ParseTree extractBestParseNoBkptrs(final int start, final int end, final int nonTermIndex) {
        // start at TOP and traverse down. Loop over possible midpoints for each visited cell.
        // Unaries are a problem, but same as in Goodman/Berkeley decoding.
        return null;
    }

    public String getStats() {
        int con = 0, add = 0;

        for (int start = 0; start < size(); start++) {
            for (int end = start + 1; end < size() + 1; end++) {
                con += getCell(start, end).numEdgesConsidered;
                add += getCell(start, end).numEdgesAdded;
            }
        }

        return " chartEdges=" + add + " processedEdges=" + con;
    }

    public static abstract class ChartCell {

        protected final short start, end;
        public int numEdgesConsidered = 0, numEdgesAdded = 0, numSpanVisits = 0;

        public ChartCell(final int start, final int end) {
            this.start = (short) start;
            this.end = (short) end;
        }

        /**
         * Returns the log probability of the specified non-terminal
         * 
         * @param nonTerminal
         * @return log probability of the specified non-terminal
         */
        public abstract float getInside(final int nonTerminal);

        /**
         * Returns the most probable edge producing the specified non-terminal. Most {@link ChartCell} implementations
         * will only maintain one edge per non-terminal, but some implementations may maintain multiple edges (e.g., for
         * k-best parsing).
         * 
         * @param nonTerminal
         * @return the most probable populated edge producing the specified non-terminal
         */
        public abstract ChartEdge getBestEdge(final int nonTerminal);

        public abstract void updateInside(final ChartEdge edge);

        public abstract void updateInside(final Production p, final ChartCell leftCell, final ChartCell rightCell,
                final float insideProb);

        /**
         * @return the word index of the first word covered by this cell
         */
        public final short start() {
            return start;
        }

        /**
         * @return the word index of the last word covered by this cell
         */
        public final short end() {
            return end;
        }

        /**
         * @return the width of the span covered by this cell
         */
        public int width() {
            return end() - start();
        }

        /**
         * @return the number of populated non-terminals in this cell
         */
        public abstract int getNumNTs();

        @Override
        public boolean equals(final Object o) {
            return this == o;
        }

        @Override
        public String toString() {
            return getClass().getName() + "[" + start() + "][" + end() + "] with " + getNumNTs() + " edges";
        }

        public void finalizeCell() {
        }
    }

    public static class ChartEdge {

        public Production prod;
        public ChartCell leftCell, rightCell;

        // TODO: other options to keeping leftCell and rightCell:
        // 1) keep int start,midpt,end
        // 2) keep ChartCell ptr and midpt

        // binary production
        public ChartEdge(final Production prod, final ChartCell leftCell, final ChartCell rightCell) {
            assert leftCell.end() == rightCell.start();
            assert leftCell.start() < rightCell.end();

            this.prod = prod;
            this.leftCell = leftCell;
            this.rightCell = rightCell;
        }

        // unary production
        public ChartEdge(final Production prod, final ChartCell childCell) {
            this.prod = prod;
            this.leftCell = childCell;
            this.rightCell = null;
        }

        public float inside() {
            if (prod.isBinaryProd()) {
                return prod.prob + leftCell.getInside(prod.leftChild) + rightCell.getInside(prod.rightChild);
            } else if (prod.isUnaryProd()) {
                return prod.prob + leftCell.getInside(prod.child());
            }
            return prod.prob; // lexical
        }

        public final int start() {
            return leftCell.start();
        }

        public final int end() {
            if (rightCell == null) {
                return leftCell.end();
            }
            return rightCell.end();
        }

        public final int midpt() {
            if (rightCell == null) {
                if (leftCell == null) {
                    throw new RuntimeException("right/leftCell must be set to use start(), end(), and midpt()");
                }
                throw new RuntimeException("Do not use midpt() with unary productions.  They do not have midpoints.");
            }
            return leftCell.end();
        }

        public ChartEdge copy() {
            return new ChartEdge(this.prod, this.leftCell, this.rightCell);
        }

        public int spanLength() {
            return end() - start();
        }

        @Override
        public String toString() {
            String start = "-", midpt = "-", end = "-", prodStr = "null";

            if (leftCell != null) {
                start = "" + leftCell.start();
                if (rightCell != null) {
                    midpt = "" + leftCell.end();
                } else {
                    end = "" + leftCell.end();
                }
            }
            if (rightCell != null) {
                end = "" + rightCell.end();
                assert leftCell.end() == rightCell.start();
            }
            if (prod != null) {
                prodStr = prod.toString();
            }

            return String.format("[%s,%s,%s] %s inside=%.4f", start, midpt, end, prodStr, inside());
        }

        @Override
        public boolean equals(final Object other) {
            try {
                if (this == other) {
                    return true;
                }

                if (other == null) {
                    return false;
                }

                final Chart.ChartEdge otherEdge = (Chart.ChartEdge) other;
                if (prod == null && otherEdge.prod != null) {
                    return false;
                }

                if (!prod.equals(otherEdge.prod)) {
                    return false;
                }

                // not comparing left/right cell object pointers because I want to be able to compare
                // cells from different charts
                if (start() != otherEdge.start()) {
                    return false;
                }

                if (end() != otherEdge.end()) {
                    return false;
                }

                if (prod.isBinaryProd() && (midpt() != otherEdge.midpt())) {
                    return false;
                }

                return true;
            } catch (final Exception e) {
                return false;
            }
        }
    }

    public SparseBitVector getCellFeatures(final int start, final int end, final String[] featureNames) {
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
        return grammar.posSet.getIndex(parseTask.fomTags[tokIndex]);
    }

    private int getWordFeat(final int tokIndex) {
        if (tokIndex < 0 || tokIndex >= parseTask.sentenceLength()) {
            return grammar.nullWord;
        }
        return parseTask.tokens[tokIndex];
    }
}

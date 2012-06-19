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

import java.util.LinkedList;
import java.util.List;

import cltool4j.args4j.EnumAliasMap;
import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.parser.ParseTask;

public abstract class Chart {

    protected int size;
    protected Grammar grammar;
    public ParseTask parseTask;
    SymbolSet<String> featHash = new SymbolSet<String>();

    protected Chart() {
    }

    public Chart(final ParseTask parseTask, final Grammar grammar) {
        this.parseTask = parseTask;
        this.grammar = grammar;
        this.size = parseTask.tokens.length;
    }

    /**
     * The number of cells in the bottom (lexical) row of this chart
     */
    public final int size() {
        return size;
    }

    /**
     * The number of cells in the bottom (lexical) row of this chart
     */
    public final int cells() {
        return size * (size + 1) / 2;
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
     * Removes existing chart entries and re-initializes chart state.
     * 
     * @param task
     */
    public abstract void reset(final ParseTask task);

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
        return getRootCell().getInside(startSymbol) > Float.NEGATIVE_INFINITY;
    }

    /**
     * Returns the index of the specified cell in the parallel chart arrays (note that this computation must agree with
     * that of {@link #cellOffset(int, int)}
     * 
     * @param start
     * @param end
     * @param size
     * @return the index of the specified cell in the parallel chart arrays
     */
    public static int cellIndex(final int start, final int end, final int size) {

        if (start < 0 || start > size) {
            throw new IllegalArgumentException("Illegal start: " + start);
        }

        if (end <= start || end > size) {
            throw new IllegalArgumentException("Illegal end: " + end);
        }

        // final int row = end - start - 1;
        // return size * row - ((row - 1) * row / 2) + start;
        return size * start - ((start - 1) * start / 2) + end - start - 1;
    }

    /**
     * Returns the index of the specified cell in the parallel chart arrays (note that this computation must agree with
     * that of {@link #cellOffset(int, int)}
     * 
     * @param start
     * @param end
     * @return the index of the specified cell in the parallel chart arrays
     */
    public final int cellIndex(final int start, final int end) {
        return cellIndex(start, end, size);
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

    public NaryTree<String> extractBestParseNoBkptrs(final int start, final int end, final int nonTermIndex) {
        // start at TOP and traverse down. Loop over possible midpoints for each visited cell.
        // Unaries are a problem, but same as in Goodman/Berkeley decoding.
        return null;
    }

    /**
     * Extracts a 'recovery' tree when the parse has failed (i.e. the parse process failed to populate the top chart
     * cell, so {@link #extractBestParse(int, int, int)} would return null). Parse failures should normally be quite
     * rare, but can be caused by too-aggressive pruning or a too-sparse grammar.
     * 
     * @param recoveryStrategy Recovery strategy
     * @throws Exception
     */
    public NaryTree<String> extractRecoveryParse(final RecoveryStrategy recoveryStrategy) {

        // Hallucinate start symbol -> S at the top of the tree (S is the most-likely guess for an entry at the top of
        // the tree)
        final NaryTree<String> tree = new NaryTree<String>(grammar.startSymbolStr);
        final NaryTree<String> s = tree.addChild("S");

        // Add the most-probable parse fragments found lower in the tree as direct children of the S
        s.addChildNodes(extractRecoveryParseNodes(0, size, recoveryStrategy));

        return tree;
    }

    private List<NaryTree<String>> extractRecoveryParseNodes(final int start, final int end,
            final RecoveryStrategy recoveryStrategy) {

        final LinkedList<NaryTree<String>> list = new LinkedList<NaryTree<String>>();

        // Start at the specified cell and find the most probable entry.
        final ChartCell cell = getCell(start, end);
        if (cell.getNumUnfactoredNTs() > 0) {
            list.add(extractParseFragment(cell));
        } else {

            // The cell is empty; search down the chart looking for a populated cell.
            switch (recoveryStrategy) {

            case RightBiased:
                // Favor the right-most populated candidate cell - this bias is specific to right-branching languages
                for (int span = end - start - 1; span >= 1; span--) {
                    for (int e = end; e >= start + span; e--) {

                        final ChartCell c = getCell(e - span, e);
                        if (c.getNumUnfactoredNTs() > 0) {

                            if (c.start > start) {
                                // Add left sibling(s)
                                list.addAll(extractRecoveryParseNodes(start, c.start, recoveryStrategy));
                            }

                            // Add the subtree rooted at this cell
                            list.add(extractParseFragment(c));

                            if (c.end < end) {
                                // Add right sibling(s)
                                list.addAll(extractRecoveryParseNodes(c.end, end, recoveryStrategy));
                            }
                            return list;
                        }
                    }
                }
            }
        }
        return list;
    }

    /**
     * @param cell
     * @return unfactored tree rooted by the most-probable entry in the specified cell
     */
    private NaryTree<String> extractParseFragment(final ChartCell cell) {
        // Find the most probable entry in the chart cell
        // TODO Favor or disfavor unaries?
        short maxNt = -1;
        final float maxInside = Float.NEGATIVE_INFINITY;
        for (short nt = 0; nt < grammar.numNonTerms(); nt++) {
            if (!grammar.nonTermSet.isFactored(nt) && cell.getInside(nt) > maxInside) {
                maxNt = nt;
            }
        }

        // Extract and unfactor the tree fragment rooted by the most-probable entry
        return extractBestParse(cell.start, cell.end, maxNt).unfactor(grammar.grammarFormat);
    }

    public String getStats() {
        int con = 0, add = 0;

        for (int start = 0; start < size(); start++) {
            for (int end = start + 1; end < size() + 1; end++) {
                con += getCell(start, end).numEdgesConsidered;
                add += getCell(start, end).numEdgesAdded;
            }
        }

        return "chartEdges=" + add + " processedEdges=" + con;
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

        /**
         * @return The number of unfactored non-terminals populated in this cell
         */
        public abstract int getNumUnfactoredNTs();

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

    public static class SimpleChartEdge {
        public short A = -1, B = -1, C = -1;
        public short start = -1, mid = -1, end = -1;
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

        // Computing the inside prob dynamically allows edges in a global agenda
        // to change. This can be good and bad ...
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

    /* @formatter:off */

    // L=Left, R=Right, T=POSTag, W=Word
    // LX = left X positions
    // X_Y = joint features
    // example: LTR2 = Left Tag Right 2 = The POS tag right two positions from the consttuent's left boundary
    public static enum Feature {
        lt,
        ltl1,
        ltl2,
        ltr1,
        ltr2,
        lt_ltl1,
        rt,
        rtl1,
        rtl2,
        rtr1,
        rtr2,
        rt_rtr1,
        lw,
        lwl1,
        lw_lt,
        lwl1_ltl1,
        rw,
        rwr1,
        rw_rt,
        rwr1_rtr1,
        Span,
        Rule
    };

    public static List<Feature> featureTemplateStrToEnum(final String[] featureNames) {
        final List<Feature> feats = new LinkedList<Feature>();
        for (final String featStr : featureNames) {
            if (featStr.equals("lt"))
                feats.add(Feature.lt);
            else if (featStr.equals("lt-1"))
                feats.add(Feature.ltl1);
            else if (featStr.equals("lt-2"))
                feats.add(Feature.ltl2);
            else if (featStr.equals("lt+1"))
                feats.add(Feature.ltr1);
            else if (featStr.equals("lt+2"))
                feats.add(Feature.ltr2);
            else if (featStr.equals("lt_lt-1"))
                feats.add(Feature.lt_ltl1);
            else if (featStr.equals("rt"))
                feats.add(Feature.rt);
            else if (featStr.equals("rt-1"))
                feats.add(Feature.rtl1);
            else if (featStr.equals("rt-2"))
                feats.add(Feature.rtl2);
            else if (featStr.equals("rt+1"))
                feats.add(Feature.rtr1);
            else if (featStr.equals("rt+2"))
                feats.add(Feature.rtr2);
            else if (featStr.equals("rt_rt+1"))
                feats.add(Feature.rt_rtr1);
            else if (featStr.equals("lw"))
                feats.add(Feature.lw);
            else if (featStr.equals("lw-1"))
                feats.add(Feature.lwl1);
            else if (featStr.equals("lw_lt"))
                feats.add(Feature.lw_lt);
            else if (featStr.equals("lw-1_lt-1"))
                feats.add(Feature.lwl1_ltl1);
            else if (featStr.equals("rw"))
                feats.add(Feature.rw);
            else if (featStr.equals("rw+1"))
                feats.add(Feature.rwr1);
            else if (featStr.equals("rw_rt"))
                feats.add(Feature.rw_rt);
            else if (featStr.equals("rw+1_rt+1"))
                feats.add(Feature.rwr1_rtr1);
            else if (featStr.equals("loc"))
                feats.add(Feature.Span);
            else if (featStr.equals("rule"))
                feats.add(Feature.Rule);
            else {
                throw new IllegalArgumentException("ERROR parsing feature template.  Not expecting '" + featStr + "'");
            }
        }
        return feats;
    }

    /* @formatter:on */

    public static void printFeatMap(final Grammar gram) {
        System.out.println("FEAT NULLPOS " + gram.posSet.getIndex(gram.nullSymbol()));
        System.out.println("FEAT NULLWORD " + gram.nullWord);
        for (final Short posIndex : gram.posSet) {
            System.out.println("FEAT POS " + gram.mapNonterminal(posIndex) + " " + gram.posSet.getIndex(posIndex));
        }
        for (final String nt : gram.nonTermSet) {
            System.out.println("FEAT NT " + nt + " " + gram.nonTermSet.getIndex(nt));
        }
        for (final String lex : gram.lexSet) {
            System.out.println("FEAT LEX " + lex + " " + gram.lexSet.getIndex(lex));
        }
    }

    public SparseBitVector getEdgeFeatures(final SimpleChartEdge edge) {
        return getEdgeFeatures(edge.start, edge.mid, edge.end, edge.A, edge.B, edge.C);
    }

    // TODO: VERY Inefficient!!! Need to move to an integer-based lookup and not use strings.
    // how to make this compact??? could go over training set and find all instances, then map them
    // to a list of indicies. Could hash. ...
    public SparseBitVector getEdgeFeatures(final short start, final short mid, final short end, final short A,
            final short B, final short C) {
        // int numFeats = 104729;
        final IntList featIndices = new IntArrayList(10);
        int largestFeatIndex = -1;

        // int n=grammar.numNonTerms();

        // for (final Feature f : features) {
        // switch (f) {
        // case Rule:
        final String strRule = String.format("%s%s%s", A, B, C);
        featIndices.add(featHash.addSymbol(strRule));

        final int leftWord = getWordFeat(start);
        final int rightWord = getWordFeat(end - 1);
        final int span = end - start;
        final String strWordEdge = String.format("%s lw=%s rw=%s s=%s", A, leftWord, rightWord, span);
        featIndices.add(featHash.addSymbol(strWordEdge));

        // NB: Can only use POS feats of we do 1-best tagging (with POS FOM)

        // final int leftPOS = getPOSFeat(start);
        // final int rightPOS = getPOSFeat(end - 1);
        // final String strPOSEdge = String.format("%s lp=%s rp=%s s=%s", A, leftPOS, rightPOS, span);
        // featIndices.add(featHash.addSymbol(strPOSEdge));

        // final int leftInPOS = getPOSFeat(start + 1);
        // final int rightInPOS = getPOSFeat(end - 2);
        // final String strPOSInEdge = String.format("%s lpi=%s rpi=%s s=%s", A, leftInPOS, rightInPOS, span);
        // featIndices.add(featHash.addSymbol(strPOSInEdge));

        for (final int x : featIndices) {
            if (x > largestFeatIndex)
                largestFeatIndex = x;
        }

        // break;
        // }
        // }
        return new SparseBitVector(largestFeatIndex + 1, featIndices.toIntArray());
    }

    public SparseBitVector getCellFeatures(final int start, final int end, final List<Feature> features) {
        int numFeats = 0;
        final IntList featIndices = new IntArrayList(10);

        final int numTags = grammar.posSet.size();
        final int numWords = grammar.lexSet.size();

        for (final Feature f : features) {
            switch (f) {
            // Left tags
            case lt:
                featIndices.add(numFeats + getPOSFeat(start));
                numFeats += numTags;
                break;
            case ltr1:
                featIndices.add(numFeats + getPOSFeat(start + 1));
                numFeats += numTags;
                break;
            case ltr2:
                featIndices.add(numFeats + getPOSFeat(start + 2));
                numFeats += numTags;
                break;
            case ltl1:
                featIndices.add(numFeats + getPOSFeat(start - 1));
                numFeats += numTags;
                break;
            case ltl2:
                featIndices.add(numFeats + getPOSFeat(start - 2));
                numFeats += numTags;
                break;
            case lt_ltl1:
                featIndices.add(numFeats + getPOSFeat(start) + numTags * getPOSFeat(start - 1));
                numFeats += numTags * numTags;
                break;
            // Right tags -- to get the last tag inside the constituent, we need to subtract 1
            case rt:
                featIndices.add(numFeats + getPOSFeat(end - 1));
                numFeats += numTags;
                break;
            case rtr1:
                featIndices.add(numFeats + getPOSFeat(end));
                numFeats += numTags;
                break;
            case rtr2:
                featIndices.add(numFeats + getPOSFeat(end + 1));
                numFeats += numTags;
                break;
            case rtl1:
                featIndices.add(numFeats + getPOSFeat(end - 2));
                numFeats += numTags;
                break;
            case rtl2:
                featIndices.add(numFeats + getPOSFeat(end - 3));
                numFeats += numTags;
                break;
            case rt_rtr1:
                featIndices.add(numFeats + getPOSFeat(end) + numTags * getPOSFeat(end + 1));
                numFeats += numTags * numTags;
                break;
            // Left words
            case lw:
                featIndices.add(numFeats + getWordFeat(start));
                numFeats += numWords;
                break;
            case lwl1:
                featIndices.add(numFeats + getWordFeat(start - 1));
                numFeats += numWords;
                break;
            case lw_lt:
                featIndices.add(numFeats + getWordFeat(start) + numWords * getPOSFeat(start));
                numFeats += numWords * numTags;
                break;
            case lwl1_ltl1:
                featIndices.add(numFeats + getWordFeat(start - 1) + numWords * getPOSFeat(start - 1));
                numFeats += numWords * numTags;
                break;
            // Right words
            case rw:
                featIndices.add(numFeats + getWordFeat(end - 1));
                numFeats += numWords;
                break;
            case rwr1:
                featIndices.add(numFeats + getWordFeat(end));
                numFeats += numWords;
                break;
            case rw_rt:
                featIndices.add(numFeats + getWordFeat(end - 1) + numWords * getPOSFeat(end - 1));
                numFeats += numWords * numTags;
                break;
            case rwr1_rtr1:
                featIndices.add(numFeats + getWordFeat(end) + numWords * getPOSFeat(end));
                numFeats += numWords * numTags;
                break;
            case Span:
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
                break;

            default:
                throw new IllegalArgumentException("ERROR parsing Feature list.  Not expecting '" + f + "'");
            }
        }

        return new SparseBitVector(numFeats, featIndices.toIntArray());
    }

    public SparseBitVector getCellFeaturesOld(final int start, final int end, final String[] featureNames) {
        int numFeats = 0;
        final IntList featIndices = new IntArrayList(10);

        final int numTags = grammar.posSet.size();
        final int numWords = grammar.lexSet.size();

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
            return grammar.posSet.getIndex(grammar.nullSymbol());
        }
        return grammar.posSet.getIndex((short) parseTask.fomTags[tokIndex]);
    }

    private int getWordFeat(final int tokIndex) {
        if (tokIndex < 0 || tokIndex >= parseTask.sentenceLength()) {
            // System.out.println("old=" + grammar.mapNonterminal(Grammar.nullSymbolStr) + " new=" + grammar.nullWord);
            // return grammar.mapNonterminal(Grammar.nullSymbolStr);
            return grammar.nullWord;
        }
        return parseTask.tokens[tokIndex];
    }

    public static enum RecoveryStrategy {
        RightBiased("rb"); // For the moment, we only implement one recovery strategy.

        private RecoveryStrategy(final String... aliases) {
            EnumAliasMap.singleton().addAliases(this, aliases);
        }
    }

    // // Maps all features to a bit value (1,2,4,8,...) This same mapping of all features MUST be used
    // // for both training and testing, although every feature from this mapping does not need to
    // // be activated.
    // public HashMap<String, Integer> mapFeaturStrToBit(String [] allFeatureStrs) {
    // HashMap<String, Integer> featMap = new HashMap<String, Integer>();
    // for (int i=0; i<allFeatureStrs.length; i++) {
    // featMap.put(allFeatureStrs[i], (int)Math.pow(2, i));
    // }
    // return featMap;
    // }
    //
    // // This only needs to be computed once per parsing session over then entire corpus. This simply
    // // maps the feature names to a unique bit vector (int) to facilitate easy training/testing of
    // // different feature sets.
    // public int featureStrToBitVector(String[] allFeatureStrs, String[] activeFeatureStrs) {
    // HashMap<String, Integer> featMap = mapFeaturStrToBit(allFeatureStrs);
    // int featValue=0;
    // for (final String featStr : activeFeatureStrs) {
    // if(featMap.containsKey(featStr)) {
    // featValue |= featMap.get(featStr);
    // } else {
    // throw new IllegalArgumentException("ERROR parsing feature template.  Not expecting '" + featStr + "'");
    // }
    // }
    // return featValue;
    // }
}

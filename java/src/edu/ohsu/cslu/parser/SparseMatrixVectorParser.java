package edu.ohsu.cslu.parser;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;

import java.util.Arrays;

import edu.ohsu.cslu.grammar.PackedSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.BaseGrammar.Production;
import edu.ohsu.cslu.grammar.Tokenizer.Token;
import edu.ohsu.cslu.parser.traversal.ChartTraversal.ChartTraversalType;
import edu.ohsu.cslu.parser.util.ParseTree;

public class SparseMatrixVectorParser extends ChartParserByTraversal implements MaximumLikelihoodParser {

    private final PackedSparseMatrixGrammar spMatrixGrammar;

    public SparseMatrixVectorParser(final PackedSparseMatrixGrammar grammar, final ChartTraversalType traversalType) {
        super(grammar, traversalType);
        this.spMatrixGrammar = grammar;
    }

    @Override
    public ParseTree findMLParse(final String sentence) throws Exception {
        return findBestParse(sentence);
    }

    @Override
    protected void initParser(final int sentLength) {
        chartSize = sentLength;
        chart = new BaseChartCell[chartSize][chartSize + 1];

        // The chart is (chartSize+1)*chartSize/2
        for (int start = 0; start < chartSize; start++) {
            for (int end = start + 1; end < chartSize + 1; end++) {
                chart[start][end] = new SparseVectorChartCell(start, end, (PackedSparseMatrixGrammar) grammar, chart);
            }
        }
        rootChartCell = chart[0][chartSize];
    }

    @Override
    protected void addLexicalProductions(final Token[] sent) throws Exception {
        super.addLexicalProductions(sent);
        for (int start = 0; start < chartSize; start++) {
            ((SparseVectorChartCell) chart[start][start + 1]).finalizeCell();
        }
    }

    @Override
    protected void visitCell(final ChartCell cell) {

        final SparseVectorChartCell spvChartCell = (SparseVectorChartCell) cell;
        final int start = cell.start();
        final int end = cell.end();

        final long t0 = System.currentTimeMillis();

        CrossProductVector crossProduct = new CrossProductVector(new long[0]);

        int totalProducts = 0;

        // Iterate over all possible midpoints, unioning together the cross-product of discovered
        // non-terminals in each left/right child pair
        // midpoint = index of right child
        for (int mid = start + 1; mid <= end - 1; mid++) {
            final SparseVectorChartCell leftCell = (SparseVectorChartCell) chart[start][mid];
            final SparseVectorChartCell rightCell = (SparseVectorChartCell) chart[mid][end];

            crossProduct = crossProduct.union(new CrossProductVector(leftCell, rightCell, mid), start, end);

            final int leftChildSize = leftCell.size();
            final int rightChildSize = rightCell.size();

            totalProducts += leftChildSize * rightChildSize;
        }

        final long t1 = System.currentTimeMillis();
        final double crossProductTime = t1 - t0;

        // Multiply the unioned vector with the grammar matrix and populate the current cell with the
        // vector resulting from the matrix-vector multiplication
        spvChartCell.spmvMultiply(crossProduct);

        final long t2 = System.currentTimeMillis();
        final double spmvTime = t2 - t1;

        // TODO We won't need to do this once we're storing directly into the packed array
        spvChartCell.finalizeCell();

        // Handle unary productions
        for (final Production p : ((PackedSparseMatrixGrammar) grammar).unaryProds) {
            final ChartEdge parentEdge = cell.getBestEdge(p.leftChild);
            if ((parentEdge != null) && (parentEdge.p.isUnaryProd() == false)) {
                final float prob = p.prob + parentEdge.insideProb;
                spvChartCell.addEdge(new ChartEdge(p, cell, prob));
            }
        }

        final long t3 = System.currentTimeMillis();
        final double unaryTime = t3 - t2;

        final int crossProductSize = crossProduct.size();
        final int edges = spvChartCell.size();

        spvChartCell.finalizeCell();

        System.out.format("Visited cell: %2d,%2d (%5d ms). Cross-product: %6d/%6d combinations (%5.0f ms, %4.2f/ms), Multiply: %5d edges (%5.0f ms, %4.2f /ms)\n", start, end, t3
                - t0, crossProductSize, totalProducts, crossProductTime, crossProductSize / crossProductTime, edges, spmvTime, edges / spmvTime);

    }

    private final static long pack(final int leftChild, final int rightChild) {
        return ((long) leftChild) << 32 | (rightChild & 0xffffffffl);
    }

    private final static long pack(final int i, final float f) {
        return ((long) i) << 32 | (Float.floatToIntBits(f) & 0xffffffffl);
    }

    private final class CrossProductVector {

        private final long[] entries;
        private int size = 0;

        public CrossProductVector(final long[] entries) {
            this.entries = entries;
            this.size = entries.length >> 1;
        }

        public CrossProductVector(final SparseVectorChartCell leftCell, final SparseVectorChartCell rightCell, final int midpoint) {
            final int leftChildSize = leftCell.size();
            final int rightChildSize = rightCell.size();
            final int maxEntries = Math.min(spMatrixGrammar.validProductionPairs(), leftChildSize * rightChildSize);
            entries = new long[maxEntries << 1];

            int index = 0;
            for (int i = 0; i < leftChildSize; i++) {

                final int leftChild = leftCell.nonterminal(i);
                if (!spMatrixGrammar.isValidLeftChild(leftChild)) {
                    continue;
                }
                final float leftProbability = leftCell.probability(i);

                for (int j = 0; j < rightChildSize; j++) {
                    final float rightProbability = rightCell.probability(j);

                    final long children = pack(leftChild, rightCell.nonterminal(j));
                    if (!spMatrixGrammar.isValidProductionPair(children)) {
                        continue;
                    }
                    entries[index++] = children;
                    entries[index++] = pack(midpoint, leftProbability + rightProbability);
                }
            }
            size = index >> 1;
        }

        public final CrossProductVector union(final CrossProductVector other, final int start, final int end) {
            final int thisLength = size << 1;
            final int otherSize = other.size();
            final int otherLength = otherSize << 1;

            final long[] newEntries = new long[thisLength + otherLength];
            final long[] otherEntries = other.entries;
            int newIndex = 0, thisIndex = 0, otherIndex = 0;

            while (thisIndex < thisLength && otherIndex < otherLength) {

                if (entries[thisIndex] < otherEntries[otherIndex]) {
                    newEntries[newIndex++] = entries[thisIndex++];
                    newEntries[newIndex++] = entries[thisIndex++];

                } else if (entries[thisIndex] > otherEntries[otherIndex]) {
                    newEntries[newIndex++] = otherEntries[otherIndex++];
                    newEntries[newIndex++] = otherEntries[otherIndex++];

                } else if (entries[thisIndex] == otherEntries[otherIndex]) {

                    // Pick the higher probability and advance both indices
                    final float probability = Float.intBitsToFloat((int) entries[thisIndex + 1]);
                    final float otherProbability = Float.intBitsToFloat((int) otherEntries[otherIndex + 1]);

                    if (probability > otherProbability) {
                        newEntries[newIndex++] = entries[thisIndex];
                        newEntries[newIndex++] = entries[thisIndex + 1];

                    } else {
                        newEntries[newIndex++] = otherEntries[otherIndex];
                        newEntries[newIndex++] = otherEntries[otherIndex + 1];

                    }
                    thisIndex = thisIndex + 2;
                    otherIndex = otherIndex + 2;
                }
            }

            // TODO: Store an end index so we won't have to copy every time
            // Copy from the end of each array to the new array
            System.arraycopy(entries, thisIndex, newEntries, newIndex, thisLength - thisIndex);
            newIndex += thisLength - thisIndex;
            System.arraycopy(otherEntries, otherIndex, newEntries, newIndex, otherLength - otherIndex);
            newIndex += otherLength - otherIndex;

            final long[] trimmedNewEntries = new long[newIndex];
            System.arraycopy(newEntries, 0, trimmedNewEntries, 0, newIndex);

            return new CrossProductVector(trimmedNewEntries);
        }

        public final int size() {
            return size;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder(256);
            for (int i = 0; i < (size << 1); i = i + 2) {
                final int leftChild = (int) (entries[i] >> 32);
                final int rightChild = (int) entries[i];
                final int midpoint = (int) (entries[i + 1] >> 32);
                final float probability = Float.intBitsToFloat((int) entries[i + 1]);

                final PackedSparseMatrixGrammar psmg = (PackedSparseMatrixGrammar) grammar;

                if (rightChild == -1) {
                    // Unary production
                    sb.append(String.format("%s %.3f (%d)\n", psmg.mapNonterminal(leftChild), probability, midpoint));

                } else if (rightChild == -2) {
                    // Lexical production
                    sb.append(String.format("%s %.3f (%d)\n", psmg.mapLexicalEntry(leftChild), probability, midpoint));

                } else {
                    // Binary production
                    sb.append(String.format("%s,%s %.3f (%d)\n", psmg.mapNonterminal(leftChild), psmg.mapNonterminal(rightChild), probability, midpoint));
                }
            }
            return sb.toString();
        }
    }

    public static class SparseVectorChartCell extends BaseChartCell {

        private final PackedSparseMatrixGrammar spMatrixGrammar;

        // TODO Store directly into denseProductionArray to avoid all the hashing

        // Only used for lexical productions
        private Int2FloatOpenHashMap probabilities;
        private Int2IntOpenHashMap midpoints;
        private Int2LongOpenHashMap children;

        /**
         * Stores discovered productions. Each production is stored in 3 longs. 0: production << 32 | probability (using {@link Float#floatToIntBits(float)}) 1: midpoint (high 32
         * bits are unused) 2: children (as a long)
         */
        private long[] entries;

        /** Number of populated indices in {@link SparseVectorChartCell#entries} (actual entry count * 3) */
        private int entriesLength;

        private final BaseChartCell[][] chart;

        public SparseVectorChartCell(final int start, final int end, final PackedSparseMatrixGrammar grammar, final BaseChartCell[][] chart) {
            super(start, end, grammar);
            this.spMatrixGrammar = grammar;
            this.chart = chart;
            this.entries = new long[grammar.numNonTerms() * 3];

            probabilities = new Int2FloatOpenHashMap();
            midpoints = new Int2IntOpenHashMap();
            children = new Int2LongOpenHashMap();
            probabilities.defaultReturnValue(Float.NEGATIVE_INFINITY);
            midpoints.defaultReturnValue(-1);
        }

        public SparseVectorChartCell(final int start, final int end, final PackedSparseMatrixGrammar grammar) {
            this(start, end, grammar, null);
        }

        public void finalizeCell() {

            entriesLength = probabilities.size() * 3;

            final int[] productionIndices = probabilities.keySet().toIntArray();
            Arrays.sort(productionIndices);

            int i = 0;
            for (final int productionIndex : productionIndices) {
                entries[i] = ((long) productionIndex) << 32 | (Float.floatToIntBits(probabilities.get(productionIndex)) & 0xffffffffl);
                entries[i + 1] = midpoints.get(productionIndex);
                entries[i + 2] = children.get(productionIndex);
                i = i + 3;
            }
        }

        public final int size() {
            return entriesLength / 3;
        }

        public final int nonterminal(final int index) {
            return (int) (entries[index * 3] >> 32);
        }

        public final float probability(final int index) {
            return Float.intBitsToFloat((int) entries[index * 3]);
        }

        public final int midpoint(final int index) {
            return (int) entries[index * 3 + 1];
        }

        public final long grammarRule(final int index) {
            return entries[index * 3 + 2];
        }

        public void spmvMultiply(final CrossProductVector crossProductVector) {

            final long[] crossProductEntries = crossProductVector.entries;
            entriesLength = 0;

            // Iterate over possible parents
            for (int parent = 0; parent < spMatrixGrammar.numNonTerms(); parent++) {

                final long[] grammarEntriesForParent = spMatrixGrammar.entries(parent);

                Production winningProduction = null;
                int winningMidpoint = 0;
                float winningProbability = Float.NEGATIVE_INFINITY;

                int crossProductIndex = 0, grammarIndex = 0;

                while (crossProductIndex < crossProductEntries.length && grammarIndex < grammarEntriesForParent.length) {

                    final long children = crossProductEntries[crossProductIndex];
                    final long grammarEntry = grammarEntriesForParent[grammarIndex];

                    if (children < grammarEntry) {
                        crossProductIndex += 2;
                    } else if (children > grammarEntry) {
                        grammarIndex += 2;
                    } else {

                        final int leftChild = (int) (children >> 32);
                        final int rightChild = (int) children;
                        // final String stringChildren = spMatrixGrammar.mapNonterminal(parent) + " -> "
                        // + spMatrixGrammar.mapNonterminal(leftChild) + ","
                        // + spMatrixGrammar.mapNonterminal(rightChild) + "("
                        // + (int) (crossProductEntries[crossProductIndex + 1] >> 32) + ")";

                        final float grammarProbability = Float.intBitsToFloat((int) grammarEntriesForParent[grammarIndex + 1]);
                        final float crossProductProbability = Float.intBitsToFloat((int) crossProductEntries[crossProductIndex + 1]);
                        final float jointProbability = grammarProbability + crossProductProbability;

                        if (jointProbability < winningProbability) {
                            grammarIndex += 2;
                            crossProductIndex += 2;
                            continue;
                        }

                        winningProduction = spMatrixGrammar.new Production(parent, leftChild, rightChild, jointProbability);
                        winningProbability = jointProbability;
                        winningMidpoint = (int) (crossProductEntries[crossProductIndex + 1] >> 32);

                        grammarIndex += 2;
                        crossProductIndex += 2;
                    }
                }

                if (winningProduction != null) {
                    entries[entriesLength] = ((long) winningProduction.parent) << 32 | (Float.floatToIntBits(winningProduction.prob) & 0xffffffffl);
                    entries[entriesLength + 1] = winningMidpoint;
                    entries[entriesLength + 2] = pack(winningProduction.leftChild, winningProduction.rightChild);
                    entriesLength += 3;

                    addEdge(winningProduction, winningProduction.prob, winningMidpoint);
                }
            }
        }

        @Override
        public boolean addEdge(final ChartEdge edge) {
            return addEdge(edge.p, edge.insideProb, edge.leftCell, edge.rightCell);
        }

        @Override
        public boolean addEdge(final Production p, final float insideProb, final ChartCell leftCell, final ChartCell rightCell) {
            final int parent = p.parent;
            numEdgesConsidered++;

            final float currentProbability = probabilities.get(parent);
            if (insideProb > currentProbability) {

                // Midpoint == start for unary productions
                final int midpoint = leftCell.end();

                midpoints.put(parent, midpoint);
                probabilities.put(parent, insideProb);

                if (p.isLexProd()) {
                    // Store -2 in right child to mark lexical productions
                    children.put(parent, pack(p.leftChild, Production.LEXICAL_PRODUCTION));
                } else {
                    children.put(parent, pack(p.leftChild, p.rightChild));
                }
                numEdgesAdded++;
                return true;
            }

            return false;
        }

        public boolean addEdge(final Production p, final float insideProb, final int midpoint) {
            final int parent = p.parent;
            numEdgesConsidered++;

            final float currentProbability = probabilities.get(parent);
            if (insideProb > currentProbability) {

                midpoints.put(parent, midpoint);
                probabilities.put(parent, insideProb);
                if (p.isLexProd()) {
                    // Store -2 in right child to mark lexical productions
                    children.put(parent, pack(p.leftChild, Production.LEXICAL_PRODUCTION));
                } else {
                    children.put(parent, pack(p.leftChild, p.rightChild));
                }

                numEdgesAdded++;
                return true;
            }

            return false;
        }

        @Override
        public ChartEdge getBestEdge(final int nonTermIndex) {
            // TODO This seems to produce correct parses, but the number of edges added differs between these
            // two implementations. That seems odd...
            // final int midpoint = midpoints.get(nonTermIndex);
            // if (midpoint < 0) {
            // return null;
            // }
            //
            // final float probability = probabilities.get(nonTermIndex);
            // if (probability == Float.NEGATIVE_INFINITY) {
            // return null;
            // }
            //
            // final SparseVectorChartCell leftChildCell = (SparseVectorChartCell)
            // chart[getStart()][midpoint];
            // final SparseVectorChartCell rightChildCell = midpoint < chart.length
            // ? (SparseVectorChartCell) chart[midpoint][getEnd()] : null;
            //
            // final long grammarRule = children.get(nonTermIndex);
            // final int leftChild = (int) (grammarRule >> 32);
            // final int rightChild = (int) grammarRule;
            //
            // if (rightChild == -2) {
            // // Lexical production
            // final Production p = grammar.new Production(nonTermIndex, leftChild, probability, true);
            // return new ChartEdge(p, leftChildCell, rightChildCell, probability);
            // }
            //
            // final Production p = grammar.new Production(nonTermIndex, leftChild, rightChild, probability);
            // return new ChartEdge(p, leftChildCell, rightChildCell, probability);
            for (int i = 0; i < entriesLength; i += 3) {
                if (entries[i] >> 32 == nonTermIndex) {
                    final int midpoint = (int) entries[i + 1];

                    final float probability = Float.intBitsToFloat((int) entries[i]);

                    final SparseVectorChartCell leftChildCell = (SparseVectorChartCell) chart[start][midpoint];
                    final SparseVectorChartCell rightChildCell = midpoint < chart.length ? (SparseVectorChartCell) chart[midpoint][end] : null;

                    final long children = entries[i + 2];
                    final int leftChild = (int) (children >> 32);
                    final int rightChild = (int) children;

                    if (rightChild == Production.LEXICAL_PRODUCTION) {
                        // Lexical production
                        final Production p = grammar.new Production(nonTermIndex, leftChild, probability, true);
                        return new ChartEdge(p, leftChildCell, rightChildCell, probability);
                    }

                    final Production p = grammar.new Production(nonTermIndex, leftChild, rightChild, probability);
                    return new ChartEdge(p, leftChildCell, rightChildCell, probability);
                }
            }
            return null;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder(256);

            sb.append("SparseChartCell[" + start + "][" + end + "] with " + getNumEdgeEntries() + " (of " + grammar.numNonTerms() + ") edges\n");
            for (final int nonterminal : children.keySet()) {
                final long grammarRule = children.get(nonterminal);
                final float probability = probabilities.get(nonterminal);
                final int leftChild = (int) (grammarRule >> 32);
                final int rightChild = (int) grammarRule;
                if (rightChild == -1) {
                    // Unary Production
                    sb.append(spMatrixGrammar.mapNonterminal(nonterminal) + " -> " + spMatrixGrammar.mapNonterminal(leftChild) + " " + " (" + probability + ")\n");
                } else if (rightChild == -2) {
                    // Lexical Production
                    sb.append(spMatrixGrammar.mapNonterminal(nonterminal) + " -> " + spMatrixGrammar.mapLexicalEntry(leftChild) + " " + " (" + probability + ")\n");
                } else {
                    sb.append(spMatrixGrammar.mapNonterminal(nonterminal) + " -> " + spMatrixGrammar.mapNonterminal(leftChild) + " " + spMatrixGrammar.mapNonterminal(rightChild)
                            + " (" + probability + ")\n");
                }
            }
            return sb.toString();
        }

        @Override
        public int getNumEdgeEntries() {
            return probabilities.size();
        }
    }
}

package edu.ohsu.cslu.parser;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.shorts.Short2FloatOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2IntOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ShortOpenHashMap;

import java.util.Arrays;

import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.BaseGrammar.Production;
import edu.ohsu.cslu.grammar.Tokenizer.Token;
import edu.ohsu.cslu.parser.traversal.ChartTraversal.ChartTraversalType;

public class CsrSparseMatrixVectorParser extends SparseMatrixVectorParser {

    private final CsrSparseMatrixGrammar spMatrixGrammar;

    public CsrSparseMatrixVectorParser(final CsrSparseMatrixGrammar grammar, final ChartTraversalType traversalType) {
        super(grammar, traversalType);
        this.spMatrixGrammar = grammar;
    }

    @Override
    protected void initParser(final int sentLength) {
        chartSize = sentLength;
        chart = new BaseChartCell[chartSize][chartSize + 1];

        // The chart is (chartSize+1)*chartSize/2
        for (int start = 0; start < chartSize; start++) {
            for (int end = start + 1; end < chartSize + 1; end++) {
                chart[start][end] = new SparseVectorChartCell(start, end, (CsrSparseMatrixGrammar) grammar, chart);
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
        // TODO Change ChartCell.start() and end() to return shorts (since we shouldn't have to handle sentences longer than 32767)
        final short start = (short) cell.start();
        final short end = (short) cell.end();

        final long t0 = System.currentTimeMillis();

        CrossProductVector crossProduct = new CrossProductVector(new int[0], new float[0], new short[0], 0);

        int totalProducts = 0;

        // Iterate over all possible midpoints, unioning together the cross-product of discovered
        // non-terminals in each left/right child pair
        // midpoint = index of right child
        for (short mid = (short) (start + 1); mid <= end - 1; mid++) {
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
        for (final Production p : ((CsrSparseMatrixGrammar) grammar).unaryProds) {
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

    private final class CrossProductVector {

        private final int[] children;
        private final float[] probabilities;
        private final short[] midpoints;
        private int size = 0;

        public CrossProductVector(final int[] children, final float[] probabilities, final short[] midpoints, final int size) {
            this.children = children;
            this.probabilities = probabilities;
            this.midpoints = midpoints;
            this.size = size;
        }

        public CrossProductVector(final SparseVectorChartCell leftCell, final SparseVectorChartCell rightCell, final short midpoint) {
            final int leftChildSize = leftCell.validLeftChildren.length;
            final int rightChildSize = rightCell.validRightChildren.length;
            final int maxEntries = Math.min(spMatrixGrammar.validProductionPairs(), leftChildSize * rightChildSize);
            children = new int[maxEntries];
            probabilities = new float[maxEntries];
            midpoints = new short[maxEntries];

            int index = 0;
            for (int i = 0; i < leftChildSize; i++) {

                final short leftChild = (short) leftCell.validLeftChildren[i];
                final float leftProbability = leftCell.validLeftChildrenProbabilities[i];

                for (int j = 0; j < rightChildSize; j++) {
                    final float rightProbability = rightCell.validRightChildrenProbabilities[j];

                    final int c = CsrSparseMatrixGrammar.pack(leftChild, (short) rightCell.validRightChildren[j]);
                    if (!spMatrixGrammar.isValidProductionPair(c)) {
                        continue;
                    }
                    children[index] = c;
                    probabilities[index] = leftProbability + rightProbability;
                    midpoints[index++] = midpoint;
                }
            }
            size = index;
        }

        public final CrossProductVector union(final CrossProductVector other, final int start, final int end) {
            final int otherSize = other.size();

            final int[] newChildren = new int[size + otherSize];
            final float[] newProbabilities = new float[size + otherSize];
            final short[] newMidpoints = new short[size + otherSize];

            final int[] otherChildren = other.children;
            final float[] otherProbabilities = other.probabilities;
            final short[] otherMidpoints = other.midpoints;

            int newIndex = 0, thisIndex = 0, otherIndex = 0;

            while (thisIndex < size && otherIndex < otherSize) {

                if (children[thisIndex] < otherChildren[otherIndex]) {
                    newChildren[newIndex] = children[thisIndex];
                    newProbabilities[newIndex] = probabilities[thisIndex];
                    newMidpoints[newIndex++] = midpoints[thisIndex++];

                } else if (children[thisIndex] > otherChildren[otherIndex]) {
                    newChildren[newIndex] = otherChildren[otherIndex];
                    newProbabilities[newIndex] = otherProbabilities[otherIndex];
                    newMidpoints[newIndex++] = otherMidpoints[otherIndex++];

                } else if (children[thisIndex] == otherChildren[otherIndex]) {

                    // Pick the higher probability and advance both indices
                    final float probability = probabilities[thisIndex];
                    final float otherProbability = otherProbabilities[otherIndex];

                    if (probability > otherProbability) {
                        newChildren[newIndex] = children[thisIndex];
                        newProbabilities[newIndex] = probability;
                        newMidpoints[newIndex] = midpoints[thisIndex];

                    } else {
                        newChildren[newIndex] = otherChildren[otherIndex];
                        newProbabilities[newIndex] = otherProbability;
                        newMidpoints[newIndex] = otherMidpoints[otherIndex];
                    }

                    thisIndex++;
                    otherIndex++;
                    newIndex++;
                }
            }

            // Copy from the end of this vector's arrays to the new array
            System.arraycopy(children, thisIndex, newChildren, newIndex, size - thisIndex);
            System.arraycopy(probabilities, thisIndex, newProbabilities, newIndex, size - thisIndex);
            System.arraycopy(midpoints, thisIndex, newMidpoints, newIndex, size - thisIndex);
            newIndex += (size - thisIndex);

            // And from the end of the other vector's arrays to the new array
            System.arraycopy(otherChildren, otherIndex, newChildren, newIndex, otherSize - otherIndex);
            System.arraycopy(otherProbabilities, otherIndex, newProbabilities, newIndex, otherSize - otherIndex);
            System.arraycopy(otherMidpoints, otherIndex, newMidpoints, newIndex, otherSize - otherIndex);
            newIndex += (otherSize - otherIndex);

            return new CrossProductVector(newChildren, newProbabilities, newMidpoints, newIndex);
        }

        public final int size() {
            return size;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder(256);
            for (int i = 0; i < size; i++) {
                final int leftChild = children[i] >>> 16;
                final short rightChild = (short) (children[i] & 0xffff);
                final int midpoint = midpoints[i];
                final float probability = probabilities[i];

                final CsrSparseMatrixGrammar smg = (CsrSparseMatrixGrammar) grammar;

                if (rightChild == Production.UNARY_PRODUCTION) {
                    // Unary production
                    sb.append(String.format("%s (%d) %.3f (%d)\n", smg.mapNonterminal(leftChild), leftChild, probability, midpoint));

                } else if (rightChild == Production.LEXICAL_PRODUCTION) {
                    // Lexical production
                    sb.append(String.format("%s (%d) %.3f (%d)\n", smg.mapLexicalEntry(leftChild), leftChild, probability, midpoint));

                } else {
                    // Binary production
                    sb.append(String.format("%s (%d),%s (%d) %.3f (%d)\n", smg.mapNonterminal(leftChild), leftChild, smg.mapNonterminal(rightChild), rightChild, probability,
                            midpoint));
                }
            }
            return sb.toString();
        }
    }

    public static class SparseVectorChartCell extends BaseChartCell {

        private final CsrSparseMatrixGrammar spMatrixGrammar;

        // TODO Store directly into denseProductionArray to avoid all the hashing

        private Short2FloatOpenHashMap probabilityMap;
        private Short2ShortOpenHashMap midpointMap;
        private Short2IntOpenHashMap childrenMap;

        private short[] parents;
        private float[] probabilities;
        private short[] midpoints;
        private int[] children;

        private int size;

        public int[] validLeftChildren;
        public float[] validLeftChildrenProbabilities;

        public int[] validRightChildren;
        public float[] validRightChildrenProbabilities;

        private final BaseChartCell[][] chart;

        public SparseVectorChartCell(final int start, final int end, final CsrSparseMatrixGrammar grammar, final BaseChartCell[][] chart) {
            super(start, end, grammar);
            this.spMatrixGrammar = grammar;
            this.chart = chart;

            this.parents = new short[grammar.numNonTerms()];
            this.probabilities = new float[grammar.numNonTerms()];
            this.midpoints = new short[grammar.numNonTerms()];
            this.children = new int[grammar.numNonTerms()];

            probabilityMap = new Short2FloatOpenHashMap();
            midpointMap = new Short2ShortOpenHashMap();
            childrenMap = new Short2IntOpenHashMap();
            probabilityMap.defaultReturnValue(Float.NEGATIVE_INFINITY);
            midpointMap.defaultReturnValue((short) -1);
        }

        public SparseVectorChartCell(final int start, final int end, final CsrSparseMatrixGrammar grammar) {
            this(start, end, grammar, null);
        }

        public void finalizeCell() {

            size = probabilityMap.size();
            final IntList validLeftChildList = new IntArrayList(probabilityMap.size());
            final FloatList validLeftChildProbabilityList = new FloatArrayList(probabilityMap.size());
            final IntList validRightChildList = new IntArrayList(probabilityMap.size());
            final FloatList validRightChildProbabilityList = new FloatArrayList(probabilityMap.size());

            final short[] productionIndices = probabilityMap.keySet().toShortArray();
            Arrays.sort(productionIndices);

            int i = 0;
            for (final short productionIndex : productionIndices) {
                final float probability = probabilityMap.get(productionIndex);
                parents[i] = productionIndex;
                probabilities[i] = probability;
                midpoints[i] = midpointMap.get(productionIndex);
                children[i] = childrenMap.get(productionIndex);
                i++;

                if (spMatrixGrammar.isValidLeftChild(productionIndex)) {
                    validLeftChildList.add(productionIndex);
                    validLeftChildProbabilityList.add(probability);
                }
                if (spMatrixGrammar.isValidRightChild(productionIndex)) {
                    validRightChildList.add(productionIndex);
                    validRightChildProbabilityList.add(probability);
                }
            }
            validLeftChildren = validLeftChildList.toIntArray();
            validLeftChildrenProbabilities = validLeftChildProbabilityList.toFloatArray();
            validRightChildren = validRightChildList.toIntArray();
            validRightChildrenProbabilities = validRightChildProbabilityList.toFloatArray();
        }

        public final int size() {
            return size;
        }

        public final short nonterminal(final int index) {
            return parents[index];
        }

        public final float probability(final int index) {
            return probabilities[index];
        }

        public final short midpoint(final int index) {
            return midpoints[index];
        }

        public final int grammarRule(final int index) {
            return children[index];
        }

        public void spmvMultiply(final CrossProductVector crossProductVector) {

            final int[] crossProductChildren = crossProductVector.children;
            final float[] crossProductProbabilities = crossProductVector.probabilities;
            final short[] crossProductMidpoints = crossProductVector.midpoints;
            size = 0;

            // Iterate over possible parents
            for (int parent = 0; parent < spMatrixGrammar.numNonTerms(); parent++) {

                final int[] grammarChildrenForParent = spMatrixGrammar.children(parent);
                final float[] grammarProbabilitiesForParent = spMatrixGrammar.probabilities(parent);

                Production winningProduction = null;
                short winningMidpoint = 0;
                float winningProbability = Float.NEGATIVE_INFINITY;

                int crossProductIndex = 0, grammarIndex = 0;

                while (crossProductIndex < crossProductVector.size && grammarIndex < grammarChildrenForParent.length) {

                    final int children = crossProductChildren[crossProductIndex];
                    final int grammarEntry = grammarChildrenForParent[grammarIndex];

                    if (children < grammarEntry) {
                        crossProductIndex++;
                    } else if (children > grammarEntry) {
                        grammarIndex++;
                    } else {

                        final int leftChild = (children >>> 16);
                        final int rightChild = children & 0xffff;
                        // final String stringChildren = spMatrixGrammar.mapNonterminal(parent) + " -> "
                        // + spMatrixGrammar.mapNonterminal(leftChild) + ","
                        // + spMatrixGrammar.mapNonterminal(rightChild) + "("
                        // + (int) (crossProductEntries[crossProductIndex + 1] >> 32) + ")";

                        final float grammarProbability = grammarProbabilitiesForParent[grammarIndex];
                        final float crossProductProbability = crossProductProbabilities[crossProductIndex];
                        final float jointProbability = grammarProbability + crossProductProbability;

                        if (jointProbability < winningProbability) {
                            grammarIndex++;
                            crossProductIndex++;
                            continue;
                        }

                        winningProduction = spMatrixGrammar.new Production(parent, leftChild, rightChild, jointProbability);
                        winningProbability = jointProbability;
                        winningMidpoint = crossProductMidpoints[crossProductIndex];

                        grammarIndex++;
                        crossProductIndex++;
                    }
                }

                if (winningProduction != null) {
                    parents[size] = (short) winningProduction.parent;
                    probabilities[size] = winningProduction.prob;
                    midpoints[size] = winningMidpoint;
                    children[size] = CsrSparseMatrixGrammar.pack((short) winningProduction.leftChild, (short) winningProduction.rightChild);
                    size++;

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
            final short parent = (short) p.parent;
            numEdgesConsidered++;

            final float currentProbability = probabilityMap.get(parent);
            if (insideProb > currentProbability) {

                // Midpoint == start for unary productions
                final short midpoint = (short) leftCell.end();

                midpointMap.put(parent, midpoint);
                probabilityMap.put(parent, insideProb);

                if (p.isLexProd()) {
                    // Store -2 in right child to mark lexical productions
                    childrenMap.put(parent, CsrSparseMatrixGrammar.pack((short) p.leftChild, (short) Production.LEXICAL_PRODUCTION));
                } else {
                    childrenMap.put(parent, CsrSparseMatrixGrammar.pack((short) p.leftChild, (short) p.rightChild));
                }
                numEdgesAdded++;
                return true;
            }

            return false;
        }

        public boolean addEdge(final Production p, final float insideProb, final int midpoint) {
            final short parent = (short) p.parent;
            numEdgesConsidered++;

            final float currentProbability = probabilityMap.get(parent);
            if (insideProb > currentProbability) {

                midpointMap.put(parent, (short) midpoint);
                probabilityMap.put(parent, insideProb);
                if (p.isLexProd()) {
                    // Store -2 in right child to mark lexical productions
                    childrenMap.put(parent, CsrSparseMatrixGrammar.pack((short) p.leftChild, (short) Production.LEXICAL_PRODUCTION));
                } else {
                    childrenMap.put(parent, CsrSparseMatrixGrammar.pack((short) p.leftChild, (short) p.rightChild));
                }

                numEdgesAdded++;
                return true;
            }

            return false;
        }

        @Override
        public ChartEdge getBestEdge(final int nonTermIndex) {
            for (int i = 0; i < size; i++) {
                if (parents[i] == nonTermIndex) {
                    final int midpoint = midpoints[i];

                    final float probability = probabilities[i];

                    final SparseVectorChartCell leftChildCell = (SparseVectorChartCell) chart[start][midpoint];
                    final SparseVectorChartCell rightChildCell = midpoint < chart.length ? (SparseVectorChartCell) chart[midpoint][end] : null;

                    final int leftChild = children[i] >>> 16;
                    if (leftChild < 0) {
                        throw new IllegalArgumentException("Negative left child");
                    }
                    final short rightChild = (short) (children[i] & 0xffff);

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
            for (final short nonterminal : childrenMap.keySet()) {
                final int grammarRule = childrenMap.get(nonterminal);
                final float probability = probabilityMap.get(nonterminal);
                final int leftChild = grammarRule >>> 16;
                final short rightChild = (short) (grammarRule & 0xffff);
                if (rightChild == Production.UNARY_PRODUCTION) {
                    // Unary Production
                    sb.append(spMatrixGrammar.mapNonterminal(nonterminal) + " -> " + spMatrixGrammar.mapNonterminal(leftChild) + " " + " (" + probability + ")\n");
                } else if (rightChild == Production.LEXICAL_PRODUCTION) {
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
            return probabilityMap.size();
        }
    }
}

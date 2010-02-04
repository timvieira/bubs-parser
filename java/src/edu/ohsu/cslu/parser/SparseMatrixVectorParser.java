package edu.ohsu.cslu.parser;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;

import java.util.Arrays;

import edu.ohsu.cslu.grammar.BaseSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.BaseGrammar.Production;
import edu.ohsu.cslu.parser.traversal.ChartTraversal.ChartTraversalType;
import edu.ohsu.cslu.parser.util.ParseTree;

public abstract class SparseMatrixVectorParser extends ChartParserByTraversal implements MaximumLikelihoodParser {

    private final BaseSparseMatrixGrammar sparseMatrixGrammar;
    private final float[] crossProductProbabilities;
    private final short[] crossProductMidpoints;

    public long totalCrossProductTime = 0;
    public long totalSpMVTime = 0;

    public SparseMatrixVectorParser(final BaseSparseMatrixGrammar grammar, final ChartTraversalType traversalType) {
        super(grammar, traversalType);

        this.sparseMatrixGrammar = grammar;
        crossProductProbabilities = new float[grammar.packedArraySize()];
        crossProductMidpoints = new short[grammar.packedArraySize()];
    }

    /**
     * Takes the cross-product of all potential child-cell combinations. Unions those cross-products together, saving the maximum probability child combinations.
     * 
     * @param start
     * @param end
     * @return Unioned cross-product
     */
    protected final CrossProductVector crossProductUnion(final int start, final int end) {

        Arrays.fill(crossProductProbabilities, Float.NEGATIVE_INFINITY);
        int size = 0;

        // Iterate over all possible midpoints, unioning together the cross-product of discovered
        // non-terminals in each left/right child pair
        for (short midpoint = (short) (start + 1); midpoint <= end - 1; midpoint++) {
            final SparseVectorChartCell leftCell = (SparseVectorChartCell) chart[start][midpoint];
            final SparseVectorChartCell rightCell = (SparseVectorChartCell) chart[midpoint][end];

            final int[] leftChildren = leftCell.validLeftChildren;
            final float[] leftChildrenProbabilities = leftCell.validLeftChildrenProbabilities;
            final short[] rightChildren = rightCell.validRightChildren;
            final float[] rightChildrenProbabilities = rightCell.validRightChildrenProbabilities;

            for (int i = 0; i < leftChildren.length; i++) {

                final int leftChild = leftChildren[i];
                final float leftProbability = leftChildrenProbabilities[i];

                for (int j = 0; j < rightChildren.length; j++) {

                    final float jointProbability = leftProbability + rightChildrenProbabilities[j];
                    final int child = sparseMatrixGrammar.pack(leftChild, rightChildren[j]);
                    final float currentProbability = crossProductProbabilities[child];

                    if (currentProbability == Float.NEGATIVE_INFINITY) {
                        crossProductProbabilities[child] = jointProbability;
                        crossProductMidpoints[child] = midpoint;
                        size++;
                    } else if (jointProbability > currentProbability) {
                        crossProductProbabilities[child] = jointProbability;
                        crossProductMidpoints[child] = midpoint;
                    }
                }
            }
        }

        return new CrossProductVector(sparseMatrixGrammar, crossProductProbabilities, crossProductMidpoints, size);
    }

    @Override
    public ParseTree findMLParse(final String sentence) throws Exception {
        return findBestParse(sentence);
    }

    @Override
    public String getStats() {
        return super.getStats() + String.format(" Cross-product time=%d ms; SpMV time=%d ms", totalCrossProductTime, totalSpMVTime);
    }

    protected abstract static class SparseVectorChartCell extends BaseChartCell {

        private final BaseSparseMatrixGrammar sparseMatrixGrammar;
        protected final BaseChartCell[][] chart;

        /** Indexed by parent non-terminal */
        protected float[] probabilities;
        protected short[] midpoints;
        protected int[] children;

        /** Stores packed children and their probabilities */
        public int[] validLeftChildren;
        public float[] validLeftChildrenProbabilities;

        public short[] validRightChildren;
        public float[] validRightChildrenProbabilities;

        protected SparseVectorChartCell(final BaseChartCell[][] chart, final int start, final int end, final BaseSparseMatrixGrammar grammar) {
            super(start, end, grammar);
            this.chart = chart;
            this.sparseMatrixGrammar = grammar;
        }

        public void finalizeCell() {

            // TODO: Size these arrays sensibly
            final int leftChildListSize = sparseMatrixGrammar.numNonTerms() >> 2;
            final int rightChildListSize = leftChildListSize >> 2;
            final IntList validLeftChildList = new IntArrayList(leftChildListSize);
            final FloatList validLeftChildProbabilityList = new FloatArrayList(leftChildListSize);
            final ShortList validRightChildList = new ShortArrayList(rightChildListSize);
            final FloatList validRightChildProbabilityList = new FloatArrayList(rightChildListSize);

            for (int nonterminal = 0; nonterminal < sparseMatrixGrammar.numNonTerms(); nonterminal++) {
                final float probability = probabilities[nonterminal];

                if (probability != Float.NEGATIVE_INFINITY) {

                    if (sparseMatrixGrammar.isValidLeftChild(nonterminal)) {
                        validLeftChildList.add(nonterminal);
                        validLeftChildProbabilityList.add(probability);
                    }
                    if (sparseMatrixGrammar.isValidRightChild(nonterminal)) {
                        validRightChildList.add((short) nonterminal);
                        validRightChildProbabilityList.add(probability);
                    }
                }
            }

            validLeftChildren = validLeftChildList.toIntArray();
            validLeftChildrenProbabilities = validLeftChildProbabilityList.toFloatArray();
            validRightChildren = validRightChildList.toShortArray();
            validRightChildrenProbabilities = validRightChildProbabilityList.toFloatArray();
        }

        @Override
        public boolean addEdge(final ChartEdge edge) {
            return addEdge(edge.p, edge.insideProb, edge.leftCell, edge.rightCell);
        }

        @Override
        public boolean addEdge(final Production p, final float insideProb, final ChartCell leftCell, final ChartCell rightCell) {
            final int parent = p.parent;
            numEdgesConsidered++;

            if (insideProb > probabilities[parent]) {

                // Midpoint == end for unary productions
                midpoints[parent] = (short) leftCell.end();
                probabilities[parent] = insideProb;
                children[parent] = sparseMatrixGrammar.pack(p.leftChild, (short) p.rightChild);

                numEdgesAdded++;
                return true;
            }

            return false;
        }

        @Override
        public ChartEdge getBestEdge(final int nonTermIndex) {
            if (probabilities[nonTermIndex] == Float.NEGATIVE_INFINITY) {
                return null;
            }

            final int leftChild = sparseMatrixGrammar.unpackLeftChild(children[nonTermIndex]);
            final short rightChild = sparseMatrixGrammar.unpackRightChild(children[nonTermIndex]);

            final int midpoint = midpoints[nonTermIndex];
            final float probability = probabilities[nonTermIndex];

            final SparseVectorChartCell leftChildCell = (SparseVectorChartCell) chart[start][midpoint];
            final SparseVectorChartCell rightChildCell = midpoint < chart.length ? (SparseVectorChartCell) chart[midpoint][end] : null;

            if (rightChild == Production.LEXICAL_PRODUCTION) {
                // Lexical production
                final Production p = grammar.new Production(nonTermIndex, leftChild, probability, true);
                return new ChartEdge(p, leftChildCell, rightChildCell, probability);
            }

            final Production p = grammar.new Production(nonTermIndex, leftChild, rightChild, probability);
            return new ChartEdge(p, leftChildCell, rightChildCell, probability);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder(256);

            sb.append("SparseChartCell[" + start + "][" + end + "] with " + getNumEdgeEntries() + " (of " + grammar.numNonTerms() + ") edges\n");

            for (int nonterminal = 0; nonterminal < sparseMatrixGrammar.numNonTerms(); nonterminal++) {
                if (probabilities[nonterminal] != Float.NEGATIVE_INFINITY) {
                    final int childProductions = children[nonterminal];
                    final float probability = probabilities[nonterminal];
                    final int midpoint = midpoints[nonterminal];

                    final int leftChild = sparseMatrixGrammar.unpackLeftChild(childProductions);
                    final short rightChild = sparseMatrixGrammar.unpackRightChild(childProductions);

                    if (rightChild == Production.UNARY_PRODUCTION) {
                        // Unary Production
                        sb.append(String.format("%s -> %s (%.5f, %d)\n", sparseMatrixGrammar.mapNonterminal(nonterminal), sparseMatrixGrammar.mapNonterminal(leftChild),
                                probability, midpoint));
                    } else if (rightChild == Production.LEXICAL_PRODUCTION) {
                        // Lexical Production
                        sb.append(String.format("%s -> %s (%.5f, %d)\n", sparseMatrixGrammar.mapNonterminal(nonterminal), sparseMatrixGrammar.mapLexicalEntry(leftChild),
                                probability, midpoint));
                    } else {
                        sb.append(String.format("%s -> %s %s (%.5f, %d)\n", sparseMatrixGrammar.mapNonterminal(nonterminal), sparseMatrixGrammar.mapNonterminal(leftChild),
                                sparseMatrixGrammar.mapNonterminal(rightChild), probability, midpoint));
                    }
                }
            }
            return sb.toString();
        }

        @Override
        public int getNumEdgeEntries() {
            int entries = 0;
            for (int nonterminal = 0; nonterminal < sparseMatrixGrammar.numNonTerms(); nonterminal++) {
                if (probabilities[nonterminal] != Float.NEGATIVE_INFINITY) {
                    entries++;
                }
            }
            return entries;
        }
    }

    public final static class CrossProductVector {

        private final BaseSparseMatrixGrammar grammar;
        final float[] probabilities;
        final short[] midpoints;
        private int size = 0;

        public CrossProductVector(final BaseSparseMatrixGrammar grammar, final float[] probabilities, final short[] midpoints, final int size) {
            this.grammar = grammar;
            this.probabilities = probabilities;
            this.midpoints = midpoints;
            this.size = size;
        }

        public final int size() {
            return size;
        }

        public final float probability(final int children) {
            return probabilities[children];
        }

        public final short midpoint(final int children) {
            return midpoints[children];
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder(256);
            for (int i = 0; i < probabilities.length; i++) {
                if (probabilities[i] != Float.NEGATIVE_INFINITY) {
                    final int leftChild = grammar.unpackLeftChild(i);
                    final short rightChild = grammar.unpackRightChild(i);
                    final int midpoint = midpoints[i];
                    final float probability = probabilities[i];

                    if (rightChild == Production.UNARY_PRODUCTION) {
                        // Unary production
                        sb.append(String.format("%s (%d) %.3f (%d)\n", grammar.mapNonterminal(leftChild), leftChild, probability, midpoint));

                    } else if (rightChild == Production.LEXICAL_PRODUCTION) {
                        // Lexical production
                        sb.append(String.format("%s (%d) %.3f (%d)\n", grammar.mapLexicalEntry(leftChild), leftChild, probability, midpoint));

                    } else {
                        // Binary production
                        sb.append(String.format("%s (%d),%s (%d) %.3f (%d)\n", grammar.mapNonterminal(leftChild), leftChild, grammar.mapNonterminal(rightChild), rightChild,
                                probability, midpoint));
                    }
                }
            }
            return sb.toString();
        }
    }
}

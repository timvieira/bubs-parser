package edu.ohsu.cslu.parser;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.cellselector.CellSelector;

public abstract class SparseMatrixVectorParser<G extends SparseMatrixGrammar, C extends Chart> extends ExhaustiveChartParser<G, C> {

    // protected Chart<DenseVectorChartCell> chart;
    private float[] crossProductProbabilities;
    private short[] crossProductMidpoints;

    public long totalCartesianProductTime = 0;
    public long totalCartesianProductUnionTime = 0;
    public long totalSpMVTime = 0;

    public SparseMatrixVectorParser(final G grammar, final CellSelector cellSelector) {
        super(grammar, cellSelector);
    }

    /**
     * Multiplies the grammar matrix (stored sparsely) by the supplied cross-product vector (stored densely), and populates this chart cell.
     * 
     * @param crossProductVector
     * @param chartCell
     */
    public abstract void binarySpmvMultiply(final CrossProductVector crossProductVector, final DenseVectorChartCell chartCell);

    /**
     * Multiplies the unary grammar matrix (stored sparsely) by the contents of this cell (stored densely), and populates this chart cell. Used to populate unary rules.
     * 
     * @param chartCell
     */
    public abstract void unarySpmvMultiply(final DenseVectorChartCell chartCell);

    @Override
    protected void initParser(final int sentLength) {
        // super.initParser(sentLength);
        chart = (C) new Chart(sentLength, DenseVectorChartCell.class, grammar);

        totalSpMVTime = 0;
        totalCartesianProductTime = 0;
        totalCartesianProductUnionTime = 0;
    }

    // TODO Do this with a matrix multiply?
    @Override
    protected List<ChartEdge> addLexicalProductions(final int[] sent) throws Exception {
        super.addLexicalProductions(sent);
        for (int start = 0; start < chart.size(); start++) {
            ((DenseVectorChartCell) chart.getCell(start, start + 1)).finalizeCell();
        }
        return null;
    }

    /**
     * Takes the cross-product of all potential child-cell combinations. Unions those cross-products together, saving the maximum probability child combinations.
     * 
     * @param start
     * @param end
     * @return Unioned cross-product
     */
    protected CrossProductVector crossProductUnion(final int start, final int end) {

        if (crossProductProbabilities == null) {
            crossProductProbabilities = new float[grammar.packedArraySize()];
            crossProductMidpoints = new short[grammar.packedArraySize()];
        }

        Arrays.fill(crossProductProbabilities, Float.NEGATIVE_INFINITY);
        int size = 0;

        // Iterate over all possible midpoints, unioning together the cross-product of discovered
        // non-terminals in each left/right child pair
        for (short midpoint = (short) (start + 1); midpoint <= end - 1; midpoint++) {
            final DenseVectorChartCell leftCell = (DenseVectorChartCell) chart.getCell(start, midpoint);
            final DenseVectorChartCell rightCell = (DenseVectorChartCell) chart.getCell(midpoint, end);

            final int[] leftChildren = leftCell.validLeftChildren;
            final float[] leftChildrenProbabilities = leftCell.validLeftChildrenProbabilities;
            final short[] rightChildren = rightCell.validRightChildren;
            final float[] rightChildrenProbabilities = rightCell.validRightChildrenProbabilities;

            for (int i = 0; i < leftChildren.length; i++) {

                final int leftChild = leftChildren[i];
                final float leftProbability = leftChildrenProbabilities[i];

                for (int j = 0; j < rightChildren.length; j++) {

                    final float jointProbability = leftProbability + rightChildrenProbabilities[j];
                    final int child = grammar.pack(leftChild, rightChildren[j]);
                    final float currentProbability = crossProductProbabilities[child];

                    if (jointProbability > currentProbability) {
                        crossProductProbabilities[child] = jointProbability;
                        crossProductMidpoints[child] = midpoint;

                        if (currentProbability == Float.NEGATIVE_INFINITY) {
                            size++;
                        }
                    }
                }
            }
        }

        return new CrossProductVector(grammar, crossProductProbabilities, crossProductMidpoints, size);
    }

    @Override
    public String getStats() {
        return String.format("%.3f, %d, %d, %d", totalTime / 1000f, totalCartesianProductTime, totalCartesianProductUnionTime, totalSpMVTime);
    }

    public static class DenseVectorChartCell extends ChartCell {

        protected final SparseMatrixGrammar sparseMatrixGrammar;

        /** Indexed by parent non-terminal */
        protected final float[] probabilities;
        protected final short[] midpoints;
        protected final int[] children;

        /** Stores packed children and their probabilities */
        public int numValidLeftChildren;
        public int[] validLeftChildren;
        public float[] validLeftChildrenProbabilities;

        public int numValidRightChildren;
        public short[] validRightChildren;
        public float[] validRightChildrenProbabilities;

        public DenseVectorChartCell(final int start, final int end, final Chart chart) {
            super(start, end, chart);
            this.sparseMatrixGrammar = (SparseMatrixGrammar) chart.grammar;

            final int arraySize = sparseMatrixGrammar.numNonTerms();
            this.probabilities = new float[arraySize];
            Arrays.fill(probabilities, Float.NEGATIVE_INFINITY);
            this.midpoints = new short[arraySize];
            this.children = new int[arraySize];
        }

        public void finalizeCell() {

            validLeftChildren = new int[numValidLeftChildren];
            validLeftChildrenProbabilities = new float[numValidLeftChildren];
            validRightChildren = new short[numValidRightChildren];
            validRightChildrenProbabilities = new float[numValidRightChildren];

            int leftIndex = 0, rightIndex = 0;

            for (int nonterminal = 0; nonterminal < sparseMatrixGrammar.numNonTerms(); nonterminal++) {
                final float probability = probabilities[nonterminal];

                if (probability != Float.NEGATIVE_INFINITY) {

                    if (sparseMatrixGrammar.isValidLeftChild(nonterminal)) {
                        validLeftChildren[leftIndex] = nonterminal;
                        validLeftChildrenProbabilities[leftIndex++] = probability;
                    }
                    if (sparseMatrixGrammar.isValidRightChild(nonterminal)) {
                        validRightChildren[rightIndex] = (short) nonterminal;
                        validRightChildrenProbabilities[rightIndex++] = probability;
                    }
                }
            }
        }

        @Override
        public boolean addEdge(final ChartEdge edge) {
            return addEdge(edge.prod, edge.leftCell, edge.rightCell, edge.inside);
        }

        @Override
        public boolean addEdge(final Production p, final ChartCell leftCell, final ChartCell rightCell, final float insideProb) {
            final int parent = p.parent;
            numEdgesConsidered++;

            if (probabilities[parent] == Float.NEGATIVE_INFINITY) {
                if (sparseMatrixGrammar.isValidLeftChild(parent)) {
                    numValidLeftChildren++;
                }
                if (sparseMatrixGrammar.isValidRightChild(parent)) {
                    numValidRightChildren++;
                }
            }

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

            final DenseVectorChartCell leftChildCell = (DenseVectorChartCell) chart.getCell(start(), midpoint);
            final DenseVectorChartCell rightChildCell = midpoint < chart.size() ? (DenseVectorChartCell) chart.getCell(midpoint, end()) : null;

            Production p;
            if (rightChild == Production.LEXICAL_PRODUCTION) {
                p = sparseMatrixGrammar.new Production(nonTermIndex, leftChild, probability, true);
            } else if (rightChild == Production.UNARY_PRODUCTION) {
                p = sparseMatrixGrammar.new Production(nonTermIndex, leftChild, probability, false);
            } else {
                p = sparseMatrixGrammar.new Production(nonTermIndex, leftChild, rightChild, probability);
            }
            return new ChartEdge(p, leftChildCell, rightChildCell, probability);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder(256);

            sb.append("SparseChartCell[" + start() + "][" + end() + "] with " + getNumEdgeEntries() + " (of " + sparseMatrixGrammar.numNonTerms() + ") edges\n");

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

        @Override
        public Collection<ChartEdge> getEdges() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public LinkedList<Integer> getPosEntries() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean hasEdge(final ChartEdge edge) throws Exception {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean hasEdge(final int nonTermIndex) {
            // TODO Auto-generated method stub
            return false;
        }
    }

    public final static class CrossProductVector {

        private final SparseMatrixGrammar grammar;
        final float[] probabilities;
        final short[] midpoints;
        private int size = 0;

        public CrossProductVector(final SparseMatrixGrammar grammar, final float[] probabilities, final short[] midpoints, final int size) {
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

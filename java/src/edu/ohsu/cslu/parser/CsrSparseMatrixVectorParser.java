package edu.ohsu.cslu.parser;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;

import java.util.Arrays;

import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.BaseGrammar.Production;
import edu.ohsu.cslu.grammar.Tokenizer.Token;
import edu.ohsu.cslu.parser.traversal.ChartTraversal.ChartTraversalType;

/**
 * SparseMatrixVectorParser implementation which uses a grammar stored in compressed-sparse-row (CSR) format. Stores cell populations and cross-product densely, for efficient array
 * access and to avoid hashing (even though it's not quite as memory-efficient).
 * 
 * @author Aaron Dunlop
 * @since Jan 28, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class CsrSparseMatrixVectorParser extends SparseMatrixVectorParser {

    private final CsrSparseMatrixGrammar spMatrixGrammar;
    public long totalCrossProductTime = 0;
    public long totalSpMVTime = 0;
    private final CrossProductVector crossProductVector;

    public CsrSparseMatrixVectorParser(final CsrSparseMatrixGrammar grammar, final ChartTraversalType traversalType) {
        super(grammar, traversalType);
        this.spMatrixGrammar = grammar;
        crossProductVector = new CrossProductVector(spMatrixGrammar.packedArraySize());
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

        // int totalProducts = 0;

        crossProductVector.clear();

        // Iterate over all possible midpoints, unioning together the cross-product of discovered
        // non-terminals in each left/right child pair
        // midpoint = index of right child
        for (short midpoint = (short) (start + 1); midpoint <= end - 1; midpoint++) {
            final SparseVectorChartCell leftCell = (SparseVectorChartCell) chart[start][midpoint];
            final SparseVectorChartCell rightCell = (SparseVectorChartCell) chart[midpoint][end];

            crossProductVector.union(leftCell, rightCell, midpoint);

            // TODO: Calculate totalProducts again (once we start storing size again in chart cell)
            // final int leftChildSize = leftCell.size();
            // final int rightChildSize = rightCell.size();
            //
            // totalProducts += leftChildSize * rightChildSize;
        }

        final long t1 = System.currentTimeMillis();
        final double crossProductTime = t1 - t0;

        // Multiply the unioned vector with the grammar matrix and populate the current cell with the
        // vector resulting from the matrix-vector multiplication
        spvChartCell.spmvMultiply(crossProductVector, spMatrixGrammar.binaryRuleMatrix(), spMatrixGrammar.binaryProbabilities());

        final long t2 = System.currentTimeMillis();
        final double binarySpmvTime = t2 - t1;

        // Handle unary productions
        // TODO: This only goes through unary rules one time, so it can't create unary chains unless such chains are encoded in the grammar. Iterating a few times would probably
        // work, although it's a big-time hack.
        spvChartCell.spmvMultiply(spMatrixGrammar.unaryRuleMatrix(), spMatrixGrammar.unaryProbabilities());

        final long t3 = System.currentTimeMillis();
        final double unarySpmvTime = t3 - t2;

        // TODO We won't need to do this once we're storing directly into the packed array
        spvChartCell.finalizeCell();

        final int crossProductSize = crossProductVector.size();
        // final int edges = spvChartCell.size();

        spvChartCell.finalizeCell();

        // System.out.format("Visited cell: %2d,%2d (%5d ms). Cross-product: %6d/%6d combinations (%5.0f ms, %4.2f/ms), Multiply: %5d edges (%5.0f ms, %4.2f /ms)\n", start, end, t3
        // - t0, crossProductSize, totalProducts, crossProductTime, crossProductSize / crossProductTime, edges, spmvTime, edges / spmvTime);
        totalCrossProductTime += crossProductTime;
        totalSpMVTime += binarySpmvTime + unarySpmvTime;
    }

    private final class CrossProductVector {

        private final float[] probabilities;
        private final short[] midpoints;
        private int size = 0;

        public CrossProductVector(final int size) {
            this.probabilities = new float[size];
            this.midpoints = new short[size];

            clear();
        }

        public final void union(final SparseVectorChartCell leftCell, final SparseVectorChartCell rightCell, final short midpoint) {
            final int[] leftChildren = leftCell.validLeftChildren;
            final float[] leftChildrenProbabilities = leftCell.validLeftChildrenProbabilities;
            final short[] rightChildren = rightCell.validRightChildren;
            final float[] rightChildrenProbabilities = rightCell.validRightChildrenProbabilities;

            final int leftChildSize = leftChildren.length;
            final int rightChildSize = rightChildren.length;

            for (int i = 0; i < leftChildSize; i++) {

                final int leftChild = leftChildren[i];
                final float leftProbability = leftChildrenProbabilities[i];

                for (int j = 0; j < rightChildSize; j++) {

                    final float jointProbability = leftProbability + rightChildrenProbabilities[j];
                    final int child = spMatrixGrammar.pack(leftChild, rightChildren[j]);

                    final float currentProbability = probabilities[child];

                    // TODO We could add a BitVector to check for valid child production pairs, but that amount to n^3 grammar intersections instead of n^2

                    if (currentProbability == Float.NEGATIVE_INFINITY) {
                        probabilities[child] = jointProbability;
                        midpoints[child] = midpoint;
                        size++;
                    } else if (jointProbability > currentProbability) {
                        probabilities[child] = jointProbability;
                        midpoints[child] = midpoint;
                    }
                }
            }
        }

        public final void clear() {
            Arrays.fill(probabilities, Float.NEGATIVE_INFINITY);
        }

        public final int size() {
            return size;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder(256);
            for (int i = 0; i < probabilities.length; i++) {
                if (probabilities[i] != Float.NEGATIVE_INFINITY) {
                    final int leftChild = spMatrixGrammar.unpackLeftChild(i);
                    final short rightChild = spMatrixGrammar.unpackRightChild(i);
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
            }
            return sb.toString();
        }
    }

    public static class SparseVectorChartCell extends BaseChartCell {

        private final CsrSparseMatrixGrammar spMatrixGrammar;

        /** Indexed by parent non-terminal */
        private float[] probabilities;
        private short[] midpoints;
        private int[] children;

        // TODO: Store the actual number of populated non-terminals
        // private int size;

        public int[] validLeftChildren;
        public float[] validLeftChildrenProbabilities;

        public short[] validRightChildren;
        public float[] validRightChildrenProbabilities;

        private final BaseChartCell[][] chart;

        public SparseVectorChartCell(final int start, final int end, final CsrSparseMatrixGrammar grammar, final BaseChartCell[][] chart) {
            super(start, end, grammar);
            this.spMatrixGrammar = grammar;
            this.chart = chart;

            final int arraySize = grammar.numNonTerms();
            this.probabilities = new float[arraySize];
            Arrays.fill(probabilities, Float.NEGATIVE_INFINITY);
            this.midpoints = new short[arraySize];
            this.children = new int[arraySize];

            // TODO: Set size to the actual number of populated non-terminals in spmvMultiply()
            // this.size = grammar.numNonTerms();
        }

        public SparseVectorChartCell(final int start, final int end, final CsrSparseMatrixGrammar grammar) {
            this(start, end, grammar, null);
        }

        public void finalizeCell() {

            // TODO: Size these arrays sensibly
            final IntList validLeftChildList = new IntArrayList(spMatrixGrammar.numNonTerms() >> 2);
            final FloatList validLeftChildProbabilityList = new FloatArrayList(spMatrixGrammar.numNonTerms() >> 2);
            final ShortList validRightChildList = new ShortArrayList(spMatrixGrammar.numNonTerms() >> 4);
            final FloatList validRightChildProbabilityList = new FloatArrayList(spMatrixGrammar.numNonTerms() >> 4);

            for (int nonterminal = 0; nonterminal < spMatrixGrammar.numNonTerms(); nonterminal++) {
                final float probability = probabilities[nonterminal];

                if (probability != Float.NEGATIVE_INFINITY) {

                    if (spMatrixGrammar.isValidLeftChild(nonterminal)) {
                        validLeftChildList.add(nonterminal);
                        validLeftChildProbabilityList.add(probability);
                    }
                    if (spMatrixGrammar.isValidRightChild(nonterminal)) {
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

        // public final int size() {
        // return size;
        // }

        /**
         * Multiplies the grammar matrix (stored sparsely) by the supplied cross-product vector (stored densely), and populates this chart cell.
         * 
         * @param crossProductVector
         * @param grammarRuleMatrix
         * @param grammarProbabilities
         */
        public void spmvMultiply(final CrossProductVector crossProductVector, final int[][] grammarRuleMatrix, final float[][] grammarProbabilities) {

            final float[] crossProductProbabilities = crossProductVector.probabilities;
            final short[] crossProductMidpoints = crossProductVector.midpoints;

            // Iterate over possible parents
            for (int parent = 0; parent < spMatrixGrammar.numNonTerms(); parent++) {

                final int[] grammarChildrenForParent = grammarRuleMatrix[parent];
                final float[] grammarProbabilitiesForParent = grammarProbabilities[parent];

                // Production winningProduction = null;
                float winningProbability = Float.NEGATIVE_INFINITY;
                int winningChildren = Integer.MIN_VALUE;
                short winningMidpoint = 0;

                for (int i = 0; i < grammarChildrenForParent.length; i++) {
                    final int grammarChildren = grammarChildrenForParent[i];

                    final float grammarProbability = grammarProbabilitiesForParent[i];
                    final float crossProductProbability = crossProductProbabilities[grammarChildren];
                    final float jointProbability = grammarProbability + crossProductProbability;

                    if (jointProbability > winningProbability) {
                        winningProbability = jointProbability;
                        winningChildren = grammarChildren;
                        winningMidpoint = crossProductMidpoints[grammarChildren];
                    }
                }

                if (winningProbability != Float.NEGATIVE_INFINITY) {
                    this.children[parent] = winningChildren;
                    this.probabilities[parent] = winningProbability;
                    this.midpoints[parent] = winningMidpoint;
                }
            }
        }

        /**
         * Multiplies the grammar matrix (stored sparsely) by the contents of this cell (stored densely), and populates this chart cell. Used to populate unary rules.
         * 
         * @param grammarRuleMatrix
         * @param grammarProbabilities
         */
        public void spmvMultiply(final int[][] grammarRuleMatrix, final float[][] grammarProbabilities) {

            // System.out.println(this.toString());

            // Iterate over possible parents
            for (int parent = 0; parent < spMatrixGrammar.numNonTerms(); parent++) {
                final String parentString = spMatrixGrammar.mapNonterminal(parent);

                final int[] grammarChildrenForParent = grammarRuleMatrix[parent];
                final float[] grammarProbabilitiesForParent = grammarProbabilities[parent];

                // Production winningProduction = null;
                float winningProbability = this.probabilities[parent];
                int winningChildren = Integer.MIN_VALUE;
                short winningMidpoint = 0;

                for (int i = 0; i < grammarChildrenForParent.length; i++) {
                    final int packedChildren = grammarChildrenForParent[i];
                    final int child = spMatrixGrammar.unpackLeftChild(packedChildren);

                    final float grammarProbability = grammarProbabilitiesForParent[i];
                    final float crossProductProbability = this.probabilities[child];
                    final float jointProbability = grammarProbability + crossProductProbability;

                    if (jointProbability > winningProbability) {
                        winningProbability = jointProbability;
                        winningChildren = packedChildren;
                        winningMidpoint = (short) end();
                    }
                }

                if (winningChildren != Integer.MIN_VALUE) {
                    this.children[parent] = winningChildren;
                    this.probabilities[parent] = winningProbability;
                    this.midpoints[parent] = winningMidpoint;
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

            if (insideProb > probabilities[parent]) {

                // Midpoint == end for unary productions
                midpoints[parent] = (short) leftCell.end();
                probabilities[parent] = insideProb;
                children[parent] = spMatrixGrammar.pack(p.leftChild, (short) p.rightChild);

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

            final int leftChild = spMatrixGrammar.unpackLeftChild(children[nonTermIndex]);
            final short rightChild = spMatrixGrammar.unpackRightChild(children[nonTermIndex]);

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

            for (int nonterminal = 0; nonterminal < spMatrixGrammar.numNonTerms(); nonterminal++) {
                if (probabilities[nonterminal] != Float.NEGATIVE_INFINITY) {
                    final int childProductions = children[nonterminal];
                    final float probability = probabilities[nonterminal];
                    final int midpoint = midpoints[nonterminal];

                    final int leftChild = spMatrixGrammar.unpackLeftChild(childProductions);
                    final short rightChild = spMatrixGrammar.unpackRightChild(childProductions);

                    if (rightChild == Production.UNARY_PRODUCTION) {
                        // Unary Production
                        sb.append(String.format("%s -> %s (%.5f, %d)\n", spMatrixGrammar.mapNonterminal(nonterminal), spMatrixGrammar.mapNonterminal(leftChild), probability,
                                midpoint));
                    } else if (rightChild == Production.LEXICAL_PRODUCTION) {
                        // Lexical Production
                        sb.append(String.format("%s -> %s (%.5f, %d)\n", spMatrixGrammar.mapNonterminal(nonterminal), spMatrixGrammar.mapLexicalEntry(leftChild), probability,
                                midpoint));
                    } else {
                        sb.append(String.format("%s -> %s %s (%.5f, %d)\n", spMatrixGrammar.mapNonterminal(nonterminal), spMatrixGrammar.mapNonterminal(leftChild), spMatrixGrammar
                                .mapNonterminal(rightChild), probability, midpoint));
                    }
                }
            }
            return sb.toString();
        }

        @Override
        public int getNumEdgeEntries() {
            int entries = 0;
            for (int nonterminal = 0; nonterminal < spMatrixGrammar.numNonTerms(); nonterminal++) {
                if (probabilities[nonterminal] != Float.NEGATIVE_INFINITY) {
                    entries++;
                }
            }
            return entries;
        }
    }
}

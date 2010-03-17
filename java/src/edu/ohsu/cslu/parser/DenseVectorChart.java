package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;

public class DenseVectorChart extends CellChart {

    public DenseVectorChart(final int size, final boolean viterbiMax, final Parser parser) {
        this.size = size;
        this.viterbiMax = true;
        this.parser = parser;

        chart = new ChartCell[size][size + 1];
        for (int start = 0; start < size; start++) {
            for (int end = start + 1; end < size + 1; end++) {
                chart[start][end] = new DenseVectorChartCell(start, end);
            }
        }
    }

    public class DenseVectorChartCell extends ChartCell {

        protected final SparseMatrixGrammar sparseMatrixGrammar;

        /** Indexed by parent non-terminal */
        // protected final float[] inside;
        protected final short[] midpoints;
        protected final int[] children;

        /** Stores packed children and their inside */
        public int numValidLeftChildren;
        public int[] validLeftChildren;
        public float[] validLeftChildrenProbabilities;

        public int numValidRightChildren;
        public short[] validRightChildren;
        public float[] validRightChildrenProbabilities;

        public DenseVectorChartCell(final int start, final int end) {
            super(start, end);
            this.sparseMatrixGrammar = (SparseMatrixGrammar) parser.grammar;

            final int arraySize = sparseMatrixGrammar.numNonTerms();
            // this.inside = new float[arraySize];
            // Arrays.fill(inside, Float.NEGATIVE_INFINITY);
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
                final float probability = inside[nonterminal];

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
        // public boolean addEdge(final ChartEdge edge) {
        public void updateInside(final ChartEdge edge) {
            // return addEdge(edge.prod, edge.leftCell, edge.rightCell, edge.inside);
            updateInside(edge.prod, edge.leftCell, edge.rightCell, edge.inside());
            // return addEdge(edge.prod, edge.leftCell, edge.rightCell);
        }

        @Override
        // public boolean addEdge(final Production p, final ChartCell leftCell, final ChartCell rightCell, final float insideProb) {
        public void updateInside(final Production p, final ChartCell leftCell, final ChartCell rightCell, final float insideProb) {
            final int parent = p.parent;
            numEdgesConsidered++;

            if (inside[parent] == Float.NEGATIVE_INFINITY) {
                if (sparseMatrixGrammar.isValidLeftChild(parent)) {
                    numValidLeftChildren++;
                }
                if (sparseMatrixGrammar.isValidRightChild(parent)) {
                    numValidRightChildren++;
                }
            }

            // final float insideProb = p.prob + leftCell.getInside(p.leftChild) + rightCell.getInside(p.rightChild);
            if (insideProb > inside[parent]) {

                // Midpoint == end for unary productions
                midpoints[parent] = (short) leftCell.end();
                inside[parent] = insideProb;
                children[parent] = sparseMatrixGrammar.pack(p.leftChild, (short) p.rightChild);

                numEdgesAdded++;
                // return true;
            }

            // return false;
        }

        @Override
        public ChartEdge getBestEdge(final int nonTermIndex) {
            if (inside[nonTermIndex] == Float.NEGATIVE_INFINITY) {
                return null;
            }

            final int leftChild = sparseMatrixGrammar.unpackLeftChild(children[nonTermIndex]);
            final short rightChild = sparseMatrixGrammar.unpackRightChild(children[nonTermIndex]);

            final int midpoint = midpoints[nonTermIndex];
            final float probability = inside[nonTermIndex];

            final DenseVectorChartCell leftChildCell = (DenseVectorChartCell) getCell(start(), midpoint);
            final DenseVectorChartCell rightChildCell = midpoint < size() ? (DenseVectorChartCell) getCell(midpoint, end()) : null;

            Production p;
            if (rightChild == Production.LEXICAL_PRODUCTION) {
                p = sparseMatrixGrammar.new Production(nonTermIndex, leftChild, probability, true);
            } else if (rightChild == Production.UNARY_PRODUCTION) {
                p = sparseMatrixGrammar.new Production(nonTermIndex, leftChild, probability, false);
            } else {
                p = sparseMatrixGrammar.new Production(nonTermIndex, leftChild, rightChild, probability);
            }
            // return new ChartEdge(p, leftChildCell, rightChildCell, probability);
            return new ChartEdge(p, leftChildCell, rightChildCell);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder(256);

            sb.append("SparseChartCell[" + start() + "][" + end() + "] with " + getNumNTs() + " (of " + sparseMatrixGrammar.numNonTerms() + ") edges\n");

            for (int nonterminal = 0; nonterminal < sparseMatrixGrammar.numNonTerms(); nonterminal++) {
                if (inside[nonterminal] != Float.NEGATIVE_INFINITY) {
                    final int childProductions = children[nonterminal];
                    final float probability = inside[nonterminal];
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
        public int getNumNTs() {
            int entries = 0;
            for (int nonterminal = 0; nonterminal < sparseMatrixGrammar.numNonTerms(); nonterminal++) {
                if (inside[nonterminal] != Float.NEGATIVE_INFINITY) {
                    entries++;
                }
            }
            return entries;
        }
    }

}

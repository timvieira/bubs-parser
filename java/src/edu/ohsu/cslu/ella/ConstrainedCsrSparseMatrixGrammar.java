package edu.ohsu.cslu.ella;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SymbolSet;

public class ConstrainedCsrSparseMatrixGrammar extends CsrSparseMatrixGrammar {

    final int[][] csrBinaryLeftChildStartIndices;

    public ConstrainedCsrSparseMatrixGrammar(final ArrayList<Production> binaryProductions,
            final ArrayList<Production> unaryProductions, final ArrayList<Production> lexicalProductions,
            final SymbolSet<String> vocabulary, final SymbolSet<String> lexicon, final GrammarFormatType grammarFormat,
            final Class<? extends CartesianProductFunction> functionClass) {
        super(binaryProductions, unaryProductions, lexicalProductions, vocabulary, lexicon, grammarFormat,
                functionClass);
        csrBinaryLeftChildStartIndices = binaryLeftChildStartIndices();
    }

    public ConstrainedCsrSparseMatrixGrammar(final Grammar g,
            final Class<? extends CartesianProductFunction> functionClass) {
        super(g, functionClass);
        csrBinaryLeftChildStartIndices = binaryLeftChildStartIndices();
    }

    public ConstrainedCsrSparseMatrixGrammar(final Reader grammarFile,
            final Class<? extends CartesianProductFunction> cartesianProductFunctionClass) throws IOException {
        super(grammarFile, cartesianProductFunctionClass);
        csrBinaryLeftChildStartIndices = binaryLeftChildStartIndices();
    }

    public ConstrainedCsrSparseMatrixGrammar(final Reader grammarFile) throws IOException {
        this(grammarFile, null);
    }

    public ConstrainedCsrSparseMatrixGrammar(final String grammarFile) throws IOException {
        this(new FileReader(grammarFile));
    }

    private int[][] binaryLeftChildStartIndices() {
        return null;
        // final int[][] startIndices = new int[numNonTerms()][];
        // int previousJ = 0;
        //
        // for (int parent = 0; parent < numNonTerms(); parent++) {
        // if (csrBinaryRowIndices[parent + 1] == csrBinaryRowIndices[parent]) {
        // startIndices[parent] = new int[0];
        // } else {
        // startIndices[parent] = new int[numNonTerms() + 1];
        // Arrays.fill(startIndices[parent], previousJ);
        //
        // short previousLeftChild = -1;
        // for (int j = csrBinaryRowIndices[parent]; j < csrBinaryRowIndices[parent + 1]; j++) {
        // final short leftChild = (short) cartesianProductFunction.unpackLeftChild(csrBinaryColumnIndices[j]);
        // if (leftChild != previousLeftChild) {
        // startIndices[parent][leftChild] = j;
        // previousLeftChild = leftChild;
        // }
        // }
        //
        // for (int k = 1; k < numNonTerms(); k++) {
        // if (startIndices[parent][k] == 0) {
        // startIndices[parent][k] = startIndices[parent][k - 1];
        // }
        // }
        // previousJ = startIndices[parent][numNonTerms() - 1] + 1;
        // startIndices[parent][numNonTerms()] = previousJ;
        // }
        // }
        // return startIndices;
    }
}

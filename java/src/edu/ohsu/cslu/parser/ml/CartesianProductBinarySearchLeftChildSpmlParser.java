package edu.ohsu.cslu.parser.ml;

import edu.ohsu.cslu.grammar.CscSparseMatrixGrammar;
import edu.ohsu.cslu.parser.ParserOptions;

public class CartesianProductBinarySearchLeftChildSpmlParser extends CartesianProductBinarySearchSpmlParser {

    public CartesianProductBinarySearchLeftChildSpmlParser(final CscSparseMatrixGrammar grammar) {
        super(grammar);
    }

    public CartesianProductBinarySearchLeftChildSpmlParser(final ParserOptions opts, final CscSparseMatrixGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    protected final int binarySearchStart(final int leftChild) {
        return grammar.cscBinaryLeftChildStartIndices[leftChild];
    }

    @Override
    protected final int binarySearchEnd(final int leftChild) {
        return grammar.cscBinaryLeftChildEndIndices[leftChild] + 1;
    }

}

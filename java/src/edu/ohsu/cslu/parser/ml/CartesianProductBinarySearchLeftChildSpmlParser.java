package edu.ohsu.cslu.parser.ml;

import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.parser.ParserDriver;

public class CartesianProductBinarySearchLeftChildSpmlParser extends CartesianProductBinarySearchSpmlParser {

	public CartesianProductBinarySearchLeftChildSpmlParser(final ParserDriver opts, final LeftCscSparseMatrixGrammar grammar) {
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

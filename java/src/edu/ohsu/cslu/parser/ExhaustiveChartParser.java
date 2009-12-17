package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.parser.traversal.ChartTraversal.ChartTraversalType;

public abstract class ExhaustiveChartParser extends ChartParserByTraversal implements MaximumLikelihoodParser {

	public ExhaustiveChartParser(Grammar grammar, ChartTraversalType traversalType) {
		super(grammar, traversalType);
	}

}

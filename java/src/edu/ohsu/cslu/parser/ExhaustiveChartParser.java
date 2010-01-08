package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.ArrayGrammar;
import edu.ohsu.cslu.parser.traversal.ChartTraversal.ChartTraversalType;

public abstract class ExhaustiveChartParser extends ChartParserByTraversal implements MaximumLikelihoodParser {

    public ExhaustiveChartParser(ArrayGrammar grammar, ChartTraversalType traversalType) {
        super(grammar, traversalType);
    }

}

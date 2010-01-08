package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.Tokenizer.Token;
import edu.ohsu.cslu.parser.traversal.ChartTraversal;
import edu.ohsu.cslu.parser.traversal.ChartTraversal.ChartTraversalType;
import edu.ohsu.cslu.parser.util.ParseTree;

public abstract class ChartParserByTraversal extends ChartParser {

	protected ChartTraversalType traversalType;

	public ChartParserByTraversal(final Grammar grammar, final ChartTraversalType traversalType) {
		super(grammar);

		this.traversalType = traversalType;
	}

	@Override
	protected void initParser(final int sentLength) {
		super.initParser(sentLength);
	}

	// overwrite this method for the inner-loop implementation
	protected abstract void visitCell(ArrayChartCell cell);

	public ParseTree findParse(final String sentence) throws Exception {
		ArrayChartCell cell;
		final Token sent[] = grammar.tokenize(sentence);

		initParser(sent.length);
		addLexicalProductions(sent);

		final ChartTraversal chartTraversal = ChartTraversal.create(traversalType, this);
		while (chartTraversal.hasNext()) {
			cell = chartTraversal.next();
			visitCell(cell);
		}

		return extractBestParse();
	}

}

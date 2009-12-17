package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.Tokenizer.Token;
import edu.ohsu.cslu.parser.traversal.ChartTraversal;
import edu.ohsu.cslu.parser.traversal.ChartTraversal.ChartTraversalType;
import edu.ohsu.cslu.parser.util.ParseTree;

public abstract class ChartParserByTraversal extends ChartParser {

	protected ChartTraversalType traversalType;
	
	public ChartParserByTraversal(Grammar grammar, ChartTraversalType traversalType) {
		super(grammar);
		
		this.traversalType=traversalType;
	}

	protected void initParser(int sentLength) {
		super.initParser(sentLength);
	}
	
	// overwrite this method for the inner-loop implementation
	protected abstract void visitCell(ChartCell cell);

	public ParseTree findParse(String sentence) throws Exception {
		ChartCell cell;
		Token sent[] = grammar.tokenize(sentence);

		initParser(sent.length);
		addLexicalProductions(sent);
		
		ChartTraversal chartTraversal = ChartTraversal.create(traversalType, this);
		while (chartTraversal.hasNext()) {
			cell = chartTraversal.next();
			visitCell(cell);
		}
		
		return extractBestParse();
    }

}

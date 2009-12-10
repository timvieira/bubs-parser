package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.Grammar;

public abstract class Parser {
	protected Grammar grammar;
	protected ParserOptions opts;
	
	public Parser(Grammar grammar, ParserOptions opts) {
		this.grammar = grammar;
		this.opts = opts;
	}
	
	public abstract String getStats();
}

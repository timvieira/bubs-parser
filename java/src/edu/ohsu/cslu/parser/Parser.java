package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.parser.util.ParseTree;

public interface Parser {
	public ParseTree findParse(String sentence) throws Exception;

	public String getStats();
}

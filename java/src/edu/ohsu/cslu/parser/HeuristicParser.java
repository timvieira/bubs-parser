package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.parser.util.ParseTree;

public interface HeuristicParser {
	public ParseTree findGoodParse(String sentence) throws Exception;
}

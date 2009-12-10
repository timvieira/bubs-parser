package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.parser.util.ParseTree;

public interface MaximumLikelihoodParser {
	public ParseTree findMLParse(String sentence) throws Exception;
}
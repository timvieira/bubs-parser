package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.GrammarByChildMatrix;
import edu.ohsu.cslu.grammar.GrammarByLeftNonTermHash;
import edu.ohsu.cslu.grammar.GrammarByLeftNonTermList;
import edu.ohsu.cslu.parser.util.Log;
import edu.ohsu.cslu.parser.util.ParseTree;

public class ParserDriver {

	public enum ParserType { 
		ExhaustiveChartParserCellCrossList, 
		ExhaustiveChartParserCellCrossHash, 
		ExhaustiveChartParserCellCrossMatrix, 
		ExhaustiveChartParserGramLoop, 
		ExhaustiveChartParserGramLoopBerkFilter,
		AgendaMaximumLikelihoodChartParser }
	
	public static void main(String[] argv) throws Exception {
		ParseTree bestParseTree=null;
		String line;
		Parser parser=null;

		ParserOptions opts = new ParserOptions(argv);
		Grammar grammar;
		
		switch (opts.parserType) {
			case ExhaustiveChartParserCellCrossList: 
				grammar = new GrammarByLeftNonTermList(opts.pcfgFileName, opts.lexFileName);
				parser = new ECPCellCrossList((GrammarByLeftNonTermList)grammar, opts); 
				break; 
			case ExhaustiveChartParserCellCrossHash:
				grammar = new GrammarByLeftNonTermHash(opts.pcfgFileName, opts.lexFileName);
				parser = new ECPCellCrossHash((GrammarByLeftNonTermHash)grammar, opts);
				break;
			case ExhaustiveChartParserCellCrossMatrix:
				grammar = new GrammarByChildMatrix(opts.pcfgFileName, opts.lexFileName);
				parser = new ECPCellCrossMatrix((GrammarByChildMatrix)grammar, opts);
				break;
			case ExhaustiveChartParserGramLoop:
				grammar  = new Grammar(opts.pcfgFileName, opts.lexFileName);
				parser = new ECPGramLoop(grammar, opts); 
				break;
			case ExhaustiveChartParserGramLoopBerkFilter:
				grammar  = new Grammar(opts.pcfgFileName, opts.lexFileName);
				parser = new ECPGramLoopBerkFilter(grammar, opts); 
				break;
			case AgendaMaximumLikelihoodChartParser:
				grammar = new GrammarByLeftNonTermList(opts.pcfgFileName, opts.lexFileName);
				parser = new AgendaChartParser((GrammarByLeftNonTermList)grammar, opts);
				break;
			default:
				Log.info(0, "ERROR: no valid parser type specified");
				System.exit(1);
		}
		System.err.println("INFO: ParserType: "+opts.parserType);
		
		//System.out.println(parser.getBestParse("the aged bottle Other-xx flies fast").toString()); System.exit(1);
				
		while ((line = opts.inputStream.readLine()) != null) {
			if (parser instanceof MaximumLikelihoodParser) {
				bestParseTree = ((MaximumLikelihoodParser)parser).findMLParse(line.trim());
			} else if (parser instanceof HeuristicParser) {
				bestParseTree = ((HeuristicParser)parser).findGoodParse(line.trim());
			} else {
				Log.info(0, "ERROR: Parser does not implement necessary decoding interface.");
				System.exit(1);
			}
			
			if (bestParseTree == null) {
				System.out.println("No parse found.");
			} else {
				System.out.println(bestParseTree.toString(opts.printInsideProbs));
				System.out.println("STAT: inside="+bestParseTree.chartEdge.insideProb);
			}
			
			System.out.println(parser.getStats());
		}
	} 
}

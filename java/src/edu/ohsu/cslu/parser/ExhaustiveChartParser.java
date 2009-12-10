package edu.ohsu.cslu.parser;

import java.util.List;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.grammar.Tokenizer.Token;
import edu.ohsu.cslu.parser.util.ParseTree;


public abstract class ExhaustiveChartParser extends ChartParser implements MaximumLikelihoodParser {

	public ExhaustiveChartParser(Grammar grammar, ParserOptions opts) {
		super(grammar, opts);
	}

	// overwrite this method for the inner-loop implementation
	protected abstract void fillChart();

	public ParseTree findMLParse(String sentence) throws Exception {
		//String tokens[] = ParserUtil.tokenize(sentence);
		Token sent[] = grammar.tokenize(sentence);

		// TODO: allow POS input
		
		initParser(sent.length);
		addLexicalProductions(sent);
		fillChart();
		addFinalProductions();
		
       	//return extractBestParse(chart[0][chartSize], grammar.startSymbol);
		return extractBestParse();
    }
	
	protected void addLexicalProductions(Token sent[]) throws Exception {
        List<Production> validProductions;
        double edgeLogProb;
        
		// add lexical productions and unary productions to the base cells of the chart
        for (int i=0; i<chartSize; i++) {
        	for (Production lexProd : grammar.getLexProdsForToken(sent[i])) {
                chart[i][i+1].addEdge(new ChartEdge(lexProd, lexProd.prob, chart[i][i+1]));
                                
                validProductions = grammar.getUnaryProdsWithChild(lexProd.parent);
                if (validProductions != null) {
                    for (Production unaryProd : validProductions) {
                        edgeLogProb = unaryProd.prob + lexProd.prob;
                        chart[i][i+1].addEdge(new ChartEdge(unaryProd, edgeLogProb, chart[i][i+1]));
                    }
                }
            }
        }
	}
	
	private void addFinalProductions() {
        // add TOP productions
        ChartCell topCell = chart[0][chartSize];
        ChartEdge topCellEdge;
        double edgeLogProb;
        
        //for (ChartEdge topCellEdge : topCell.getAllBestEdges()) {
        for(int i=0; i<grammar.numNonTerms(); i++) {
        	topCellEdge = topCell.getBestEdge(i);
        	if (topCellEdge != null) {
        		for (Production p : grammar.getUnaryProdsWithChild(topCellEdge.p.parent)) {
	                if (p.parent == grammar.startSymbol) {
	                    edgeLogProb = p.prob + topCellEdge.insideProb;
	                    topCell.addEdge(new ChartEdge(p, edgeLogProb, topCell, topCell));
	                }
	            }
        	}
        }
	}
}

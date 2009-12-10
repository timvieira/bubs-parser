package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.Grammar.Production;

public class ECPGramLoop extends ExhaustiveChartParser {

	public ECPGramLoop(Grammar grammar, ParserOptions opts) {
		super(grammar, opts);
	}
	
	protected void fillChart() {
    	ChartCell parentCell, leftCell, rightCell;
		ChartEdge leftEdge, rightEdge, parentEdge;
    	double prob;
    	int end;
    	
        for (int span=2; span<=chartSize; span++) {
            for (int beg=0; beg<chartSize-span+1; beg++) { // beginning
                end=beg+span;
                parentCell=chart[beg][end];
            	for (int mid=beg+1; mid<=beg+span-1; mid++) { // mid point
                	// naive traversal through all grammar rules
                	leftCell=chart[beg][mid];
                	rightCell=chart[mid][end];
                	for (Production p : grammar.binaryProds) {
                		leftEdge = leftCell.getBestEdge(p.leftChild);
                		rightEdge = rightCell.getBestEdge(p.rightChild);
                		if ((leftEdge != null) && (rightEdge != null)) {
                			prob = p.prob + leftEdge.insideProb + rightEdge.insideProb;
                			//parentCell.addEdge(new ChartEdge(p, prob, leftCell, rightCell));
                			parentCell.addEdge(p, prob, leftCell, rightCell);
                		}
                	}
                }
            	
            	for (Production p : grammar.unaryProds) {
            		parentEdge = parentCell.getBestEdge(p.leftChild);
            		if ((parentEdge != null) && (parentEdge.p.isUnaryProd() == false)) {
            			prob = p.prob + parentEdge.insideProb;
            			parentCell.addEdge(new ChartEdge(p, prob, parentCell));
            		}
            	}  
            }
        }
    }	
}

package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.GrammarByLeftNonTermList;
import edu.ohsu.cslu.grammar.Grammar.Production;

public class ECPCellCrossList extends ExhaustiveChartParser {

	public ECPCellCrossList(GrammarByLeftNonTermList grammar, ParserOptions opts) {
		super(grammar, opts);
	}

	@Override
	protected void fillChart() {
    	ChartCell parentCell, leftCell, rightCell;
		ChartEdge rightEdge, parentEdge;
    	double prob;
    	int end;
    	GrammarByLeftNonTermList grammarByLeftNonTermList = (GrammarByLeftNonTermList)grammar;
    	
        for (int span=2; span<=chartSize; span++) {
            for (int beg=0; beg<chartSize-span+1; beg++) { // beginning
                end=beg+span;
                parentCell=chart[beg][end];
            	for (int mid=beg+1; mid<=beg+span-1; mid++) { // mid point
                	leftCell=chart[beg][mid];
                	rightCell=chart[mid][end];
                	for (ChartEdge leftEdge : leftCell.getBestLeftEdges()) {
                		for (Production p : grammarByLeftNonTermList.getBinaryProdsWithLeftChild(leftEdge.p.parent)) {
                			rightEdge = rightCell.getBestEdge(p.rightChild);
                			if (rightEdge != null) {
                				prob = p.prob + leftEdge.insideProb + rightEdge.insideProb;
                				parentCell.addEdge(p, prob, leftCell, rightCell);
                			}
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

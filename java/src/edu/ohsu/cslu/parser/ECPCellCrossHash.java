package edu.ohsu.cslu.parser;

import java.util.List;

import edu.ohsu.cslu.grammar.GrammarByLeftNonTermHash;
import edu.ohsu.cslu.grammar.Grammar.Production;


public class ECPCellCrossHash extends ExhaustiveChartParser {

	public ECPCellCrossHash(GrammarByLeftNonTermHash grammar, ParserOptions opts) {
		super(grammar, opts);
	}

	@Override
	protected void fillChart() {
    	ChartCell parentCell, leftCell, rightCell;
		ChartEdge parentEdge;
		List<Production> validProductions;
    	double prob;
    	int end;
    	
        for (int span=2; span<=chartSize; span++) {
            for (int beg=0; beg<chartSize-span+1; beg++) { // beginning
                end=beg+span;
                parentCell=chart[beg][end];
            	for (int mid=beg+1; mid<=beg+span-1; mid++) { // mid point
                	leftCell=chart[beg][mid];
                	rightCell=chart[mid][end];
                	for (ChartEdge leftEdge : leftCell.getBestLeftEdges()) {
                		for (ChartEdge rightEdge : rightCell.getBestRightEdges()) {
                			validProductions = ((GrammarByLeftNonTermHash)grammar).getBinaryProdsByChildren(leftEdge.p.parent, rightEdge.p.parent);
                			if (validProductions != null) {
                				for (Production p : validProductions) {
                					prob = p.prob + leftEdge.insideProb + rightEdge.insideProb;
                					parentCell.addEdge(p, prob, leftCell, rightCell);
                				}
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


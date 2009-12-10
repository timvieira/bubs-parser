package edu.ohsu.cslu.parser;

import java.util.LinkedList;
import java.util.List;

import edu.ohsu.cslu.grammar.GrammarByChildMatrix;
import edu.ohsu.cslu.grammar.Grammar.Production;


public class ECPCellCrossMatrix extends ExhaustiveChartParser {

	public ECPCellCrossMatrix(GrammarByChildMatrix grammar, ParserOptions opts) {
		super(grammar, opts);
	}

	@Override
	protected void fillChart() {
    	ChartCell parentCell, leftCell, rightCell;
		ChartEdge parentEdge;
		List<Production> validProductions;
		//ArrayList<LinkedList<Production> > gramByLeft;
		LinkedList<Production>[] gramByLeft;
    	double prob;
    	int end;
    	GrammarByChildMatrix grammarByChildMatrix = (GrammarByChildMatrix)grammar;
    	
        for (int span=2; span<=chartSize; span++) {
            for (int beg=0; beg<chartSize-span+1; beg++) { // beginning
                end=beg+span;
                parentCell=chart[beg][end];
            	for (int mid=beg+1; mid<=beg+span-1; mid++) { // mid point
                	leftCell=chart[beg][mid];
                	rightCell=chart[mid][end];
                	for (ChartEdge leftEdge : leftCell.getBestLeftEdges()) {
                		//gramByLeft = grammarByChildMatrix.binaryProdMatrix.get(leftEdge.p.parent);
                		gramByLeft = grammarByChildMatrix.binaryProdMatrix2[leftEdge.p.parent];
                		//if (gramByLeft != null) { // this is always true since we are getting only the left children from the left cell
                		for (ChartEdge rightEdge : rightCell.getBestRightEdges()) {
                			//validProductions = gramByLeft.get(rightEdge.p.parent);
                			validProductions = gramByLeft[rightEdge.p.parent];
                			if (validProductions != null) {
                				for (Production p : validProductions) {
                					prob = p.prob + leftEdge.insideProb + rightEdge.insideProb;
                					parentCell.addEdge(p, prob, leftCell, rightCell);
                				}
                			}
                		}
                		//}
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



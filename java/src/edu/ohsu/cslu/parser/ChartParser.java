package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.parser.util.ParseTree;

public abstract class ChartParser extends Parser {

	protected ChartCell chart[][];
	protected int chartSize;
	protected ChartCell rootChartCell;
	
	public ChartParser(Grammar grammar, ParserOptions opts) {
		super(grammar, opts);
	}
	
	protected void initParser(int sentLength) {
		chartSize = sentLength;
		chart = new ChartCell[chartSize][chartSize+1];
        
		// The chart is (chartSize+1)*chartSize/2
		for (int start=0; start<chartSize; start++) {
            for (int end=start+1; end<chartSize+1; end++) {
                chart[start][end] = new ChartCell(start,end,grammar);
            }
        }
		rootChartCell = chart[0][chartSize];
	}
	
    protected ParseTree extractBestParse() {
    	return extractBestParse(this.rootChartCell, this.grammar.startSymbol);
    }
	
	protected ParseTree extractBestParse(ChartCell cell, int nonTermIndex) {
        ChartEdge bestEdge;
        ParseTree curNode = null;

        if (cell != null) {
            bestEdge = cell.getBestEdge(nonTermIndex);
            if (bestEdge != null) {
                curNode = new ParseTree(bestEdge);
                if (bestEdge.p.isUnaryProd()) {
                    if (bestEdge.p.isLexProd()) {
                        curNode.children.add(new ParseTree(bestEdge));
                    } else {
                        curNode.children.add(extractBestParse(bestEdge.leftCell, bestEdge.p.leftChild));
                    }
                } else { // binary production
                    curNode.children.add(extractBestParse(bestEdge.leftCell, bestEdge.p.leftChild));
                    curNode.children.add(extractBestParse(bestEdge.rightCell, bestEdge.p.rightChild));
                }
            }
        }

        return curNode;
    }
	
	public String getStats() {
		String result = "";
		int con=0, add=0;
		
		for (int start=0; start<chartSize; start++) {
			for (int end=start+1; end<chartSize+1; end++) {
				con+=chart[start][end].numEdgesConsidered;
				add+=chart[start][end].numEdgesAdded;
			}
		}
		
		result += "STAT: sentLen="+chartSize+" chartEdges="+add+" processedEdges="+con;
		return result;
	}
}

package edu.ohsu.cslu.parser;

import java.util.LinkedList;
import java.util.PriorityQueue;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.GrammarByLeftNonTermList;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.fom.EdgeFOM;
import edu.ohsu.cslu.parser.fom.EdgeFOM.EdgeFOMType;
import edu.ohsu.cslu.parser.traversal.ChartTraversal.ChartTraversalType;
import edu.ohsu.cslu.parser.util.ParseTree;

public class CellAgendaChartParser extends ChartParserByTraversal implements HeuristicParser {

	private GrammarByLeftNonTermList grammarByChildren;
	private FrontierCell frontier[][];
	private EdgeFOM edgeFOM;

	protected class FrontierCell {
		protected ChartCell chartCell; 
		protected PriorityQueue<ChartEdge> edgeAgenda;
		
		public FrontierCell(ChartCell chartCell) {
			this.chartCell=chartCell;
			this.edgeAgenda = new PriorityQueue<ChartEdge>();
		}
		
		public void addEdge(ChartEdge edge) {
			this.edgeAgenda.add(edge);
		}
	}
	
	public CellAgendaChartParser(Grammar grammar, ChartTraversalType traversalType, EdgeFOMType fomType) {
		super(grammar, traversalType);

		grammarByChildren = (GrammarByLeftNonTermList)grammar;
		this.edgeFOM = EdgeFOM.create(fomType);
	}

	protected void initParser(int sentLength) {
		super.initParser(sentLength);
		
		frontier = new FrontierCell[chartSize][chartSize+1];
        
		// parallel structure with chart to hold possible edges
		for (int start=0; start<chartSize; start++) {
            for (int end=start+1; end<chartSize+1; end++) {
            	frontier[start][end] = new FrontierCell(chart[start][end]);
            }
        }
	}
	
	public ParseTree findGoodParse(String sentence) throws Exception {
		// will traverse chart cells in ChartTraversal order and populate
		// cells via populateCell(cell)
		return super.findParse(sentence);
	}

	
	@Override
	protected void visitCell(ChartCell cell) {
		ChartEdge edge = frontier[cell.start][cell.end].edgeAgenda.poll();
		boolean addedEdge = cell.addEdge(edge);
		
		if (addedEdge == true) {
			expandFrontier(edge.p.parent, cell);
		}
	}
	
	private void expandFrontier(int nonTerm, ChartCell cell) {
		LinkedList<Production> possibleGrammarProds;
		ChartEdge leftEdge, rightEdge, edge, addedEdge = cell.getBestEdge(nonTerm);
		ChartCell rightCell, leftCell;
		FrontierCell frontierCell;
		float insideProb;
		
		// unary edges are always possible in any cell, although we don't allow unary chains
		frontierCell = frontier[cell.start][cell.end];
		if (addedEdge.p.isUnaryProd() == false || addedEdge.p.isLexProd() == true) {
			for (Production p : grammar.getUnaryProdsWithChild(addedEdge.p.parent)) {
    			insideProb = addedEdge.insideProb + p.prob; 
				edge = new ChartEdgeWithFOM(p, cell, insideProb, edgeFOM);
    			frontierCell.addEdge(edge);
			}
    	}  
		
		// connect edge as possible right non-term
		for (int beg=0; beg < cell.start; beg++) {
			leftCell=chart[beg][cell.start];
			frontierCell = frontier[beg][cell.end];
			possibleGrammarProds = grammarByChildren.getBinaryProdsWithRightChild(nonTerm);
			if (possibleGrammarProds != null) {
				for (Production p : possibleGrammarProds) {
					leftEdge = leftCell.getBestEdge(p.leftChild);
					if (leftEdge != null && chart[beg][cell.end].getBestEdge(p.parent) == null) {
						insideProb = leftEdge.insideProb + p.prob + addedEdge.insideProb;
						edge=new ChartEdgeWithFOM(p, leftCell, cell, insideProb, edgeFOM);
						frontierCell.addEdge(edge);
					}
				}
			}
		}
		
		// connect edge as possible left non-term
		for (int end=cell.end+1; end <= chartSize; end++) {
			rightCell=chart[cell.end][end];
			frontierCell = frontier[cell.start][end];
			possibleGrammarProds = grammarByChildren.getBinaryProdsWithLeftChild(nonTerm);
			if (possibleGrammarProds != null) {
				for (Production p : possibleGrammarProds) {
					rightEdge = rightCell.getBestEdge(p.rightChild);
					if (rightEdge != null && chart[cell.start][end].getBestEdge(p.parent) == null) {
						insideProb = addedEdge.insideProb + p.prob + rightEdge.insideProb;
						edge = new ChartEdgeWithFOM(p, cell, rightCell, insideProb, edgeFOM);
						frontierCell.addEdge(edge);
					}
				}
			}
		}
	}
}

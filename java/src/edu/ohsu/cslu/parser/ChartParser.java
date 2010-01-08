package edu.ohsu.cslu.parser;

import java.util.List;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.grammar.Tokenizer.Token;
import edu.ohsu.cslu.parser.util.ParseTree;

public abstract class ChartParser implements Parser {

	public ArrayChartCell chart[][];
	public int chartSize;
	public ArrayChartCell rootChartCell;
	public Grammar grammar;
	public ParserOptions opts = null; // TODO: fix this

	public ChartParser(final Grammar grammar) {
		this.grammar = grammar;
	}

	protected void initParser(final int sentLength) {
		chartSize = sentLength;
		chart = new ArrayChartCell[chartSize][chartSize + 1];

		// The chart is (chartSize+1)*chartSize/2
		for (int start = 0; start < chartSize; start++) {
			for (int end = start + 1; end < chartSize + 1; end++) {
				chart[start][end] = new ArrayChartCell(start, end, grammar);
			}
		}
		rootChartCell = chart[0][chartSize];
	}

	protected void addLexicalProductions(final Token sent[]) throws Exception {
		List<Production> validProductions;
		float edgeLogProb;

		// add lexical productions and unary productions to the base cells of the chart
		for (int i = 0; i < chartSize; i++) {
			for (final Production lexProd : grammar.getLexProdsForToken(sent[i])) {
				chart[i][i + 1].addEdge(new ChartEdge(lexProd, chart[i][i + 1], lexProd.prob));

				validProductions = grammar.getUnaryProdsWithChild(lexProd.parent);
				if (validProductions != null) {
					for (final Production unaryProd : validProductions) {
						edgeLogProb = unaryProd.prob + lexProd.prob;
						chart[i][i + 1].addEdge(new ChartEdge(unaryProd, chart[i][i + 1], edgeLogProb));
					}
				}
			}
		}
	}

	/*
	 * private void addFinalProductions() { // add TOP productions ChartCell topCell = chart[0][chartSize]; ChartEdge topCellEdge; float edgeLogProb;
	 * 
	 * //for (ChartEdge topCellEdge : topCell.getAllBestEdges()) { for(int i=0; i<grammar.numNonTerms(); i++) { topCellEdge = topCell.getBestEdge(i); if (topCellEdge != null) { for
	 * (Production p : grammar.getUnaryProdsWithChild(topCellEdge.p.parent)) { if (p.parent == grammar.startSymbol) { edgeLogProb = p.prob + topCellEdge.insideProb;
	 * topCell.addEdge(new ChartEdge(p, topCell, topCell, edgeLogProb)); } } } } }
	 */

	protected ParseTree extractBestParse() {
		return extractBestParse(this.rootChartCell, this.grammar.startSymbol);
	}

	protected ParseTree extractBestParse(final ArrayChartCell cell, final int nonTermIndex) {
		ChartEdge bestEdge;
		ParseTree curNode = null;

		if (cell != null) {
			bestEdge = cell.getBestEdge(nonTermIndex);
			if (bestEdge != null) {
				curNode = new ParseTree(bestEdge);
				if (bestEdge.p.isUnaryProd()) {
					if (bestEdge.p.isLexProd()) {
						curNode.addChild(new ParseTree(bestEdge.p.childrenToString()));
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
		int con = 0, add = 0;

		for (int start = 0; start < chartSize; start++) {
			for (int end = start + 1; end < chartSize + 1; end++) {
				con += chart[start][end].numEdgesConsidered;
				add += chart[start][end].numEdgesAdded;
			}
		}

		result += " chartEdges=" + add + " processedEdges=" + con;
		return result;
	}
}

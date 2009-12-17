package edu.ohsu.cslu.parser;

import java.util.LinkedList;
import java.util.PriorityQueue;

import edu.ohsu.cslu.grammar.GrammarByLeftNonTermList;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.grammar.Tokenizer.Token;
import edu.ohsu.cslu.parser.fom.EdgeFOM;
import edu.ohsu.cslu.parser.fom.EdgeFOM.EdgeFOMType;
import edu.ohsu.cslu.parser.util.Log;
import edu.ohsu.cslu.parser.util.ParseTree;


public class AgendaChartParser extends ChartParser implements MaximumLikelihoodParser {

	protected PriorityQueue<ChartEdgeWithFOM> agenda;
	protected GrammarByLeftNonTermList grammarByChildren;
	protected int nAgendaPush, nAgendaPop, nChartEdges, nGhostEdges;
	protected EdgeFOM edgeFOM;
	
	public AgendaChartParser(GrammarByLeftNonTermList grammar, EdgeFOMType fomType) {
		super(grammar);
		grammarByChildren = (GrammarByLeftNonTermList)grammar;
		this.edgeFOM = EdgeFOM.create(fomType);
	}
	
	protected void initParser(int sentLength) {
		super.initParser(sentLength);
		
		agenda = new PriorityQueue<ChartEdgeWithFOM>();
		nAgendaPush=nAgendaPop=nChartEdges=nGhostEdges=0;
		//System.out.println(grammar.numNonTerms()+" "+chartSize);
	}
	
	public ParseTree findMLParse(String sentence) throws Exception {
		return findParse(sentence);
	}
	
	public ParseTree findParse(String sentence) throws Exception {
		ChartCell parentCell;
		boolean edgeAdded;
		ChartEdge edge;
		Token sent[] = grammar.tokenize(sentence);
		
		initParser(sent.length);
		addLexicalProductions(sent);
				
		while (!agenda.isEmpty() && (rootChartCell.getBestEdge(grammar.startSymbol) == null)) {
			edge = agenda.poll(); // get and remove top agenda edge
			//System.out.println("Agenda Pop: "+edge);
			nAgendaPop+=1;
			
			if (edge.p.isUnaryProd()) {
				parentCell = edge.leftCell;
			} else {
				parentCell = chart[edge.leftCell.start][edge.rightCell.end];
			}
			edgeAdded = parentCell.addEdge(edge);
			
			// if A->B C is added to chart but A was already in this chart cell, then the
			// first edge must have been better than the current edge because we pull edges
			// from the agenda best-first.  This also means that the entire frontier 
			// has already been added.  
			if (edgeAdded) {
				expandAgendaFrontier(edge.p.parent, parentCell);
				nChartEdges+=1;
			}
		}
		
		if (agenda.isEmpty()) {
			Log.info(1, "WARNING: Agenda is empty.  All edges have been added to chart.");
		}
		
       	return extractBestParse();
    }
	
	protected void addEdgeToAgenda(ChartEdgeWithFOM edge) {
		//System.out.println("Agenda Push: "+edge);
		nAgendaPush+=1;
		agenda.add(edge);
	}

	protected void addLexicalProductions(Token sent[]) throws Exception {
		// add lexical productions and unary productions to the base cells of the chart
        for (int i=0; i<chartSize; i++) {
        	for (Production lexProd : grammar.getLexProdsForToken(sent[i])) {
        		addEdgeToAgenda(new ChartEdgeWithFOM(lexProd, chart[i][i+1], lexProd.prob, edgeFOM));
            }
        }
	}
	
	protected void expandAgendaFrontier(int nonTerm, ChartCell cell) {
		LinkedList<Production> possibleGrammarProds;
		ChartEdge newEdge = cell.getBestEdge(nonTerm);
		ChartEdge leftEdge, rightEdge;
		ChartCell rightCell, leftCell;
		float prob;
		
		// unary edges are always possible in any cell, although we don't allow unary chains
		if (newEdge.p.isUnaryProd() == false || newEdge.p.isLexProd() == true) {
			for (Production p : grammar.getUnaryProdsWithChild(newEdge.p.parent)) {
    			prob = p.prob + newEdge.insideProb;
    			addEdgeToAgenda(new ChartEdgeWithFOM(p, cell, prob, edgeFOM));
			}
    	}  
		
		if (cell == rootChartCell) {
			// no possible connections to other edges other than unary ... only add the root productions
			// actually, TOP edges will already be added in the unary step
			// TODO: separate TOP->X from other unary productions
    		/*
			for (Production p : grammar.getUnaryProdsWithChild(newEdge.p.parent)) {
                if (p.parent == grammar.startSymbol) {
                	prob = p.prob+newEdge.insideProb;
                	addEdgeToAgenda(new ChartEdge(p, rootChartCell, rootChartCell, prob));
                }
            }
            */
		} else {
			// connect edge as possible right non-term
			for (int beg=0; beg < cell.start; beg++) {
				leftCell=chart[beg][cell.start];
				possibleGrammarProds = grammarByChildren.getBinaryProdsWithRightChild(nonTerm);
				if (possibleGrammarProds != null) {
					for (Production p : possibleGrammarProds) {
						leftEdge = leftCell.getBestEdge(p.leftChild);
						if (leftEdge != null && chart[beg][cell.end].getBestEdge(p.parent) == null) {
							prob = p.prob + newEdge.insideProb + leftEdge.insideProb;
							//System.out.println("LEFT:"+new ChartEdge(p, prob, leftCell, cell));
							addEdgeToAgenda(new ChartEdgeWithFOM(p, leftCell, cell, prob, edgeFOM));
						}
					}
				}
			}
			
			// connect edge as possible left non-term
			for (int end=cell.end+1; end <= chartSize; end++) {
				rightCell=chart[cell.end][end];
				possibleGrammarProds = grammarByChildren.getBinaryProdsWithLeftChild(nonTerm);
				if (possibleGrammarProds != null) {
					for (Production p : possibleGrammarProds) {
						rightEdge = rightCell.getBestEdge(p.rightChild);
						if (rightEdge != null && chart[cell.start][end].getBestEdge(p.parent) == null) {
							prob = p.prob + rightEdge.insideProb + newEdge.insideProb; 
							//System.out.println("RIGHT: "+new ChartEdge(p, prob, cell, rightCell));
							addEdgeToAgenda(new ChartEdgeWithFOM(p, cell, rightCell, prob, edgeFOM));
						}
					}
				}
			}
		}
	}
	
	public String getStats() {	
		return " chartEdges="+nChartEdges+" agendaPush="+nAgendaPush+
			" agendaPop="+nAgendaPop+" ghostEdges="+nGhostEdges;
	}
}

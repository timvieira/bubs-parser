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

public class AgendaChartParserGhostEdges extends AgendaChartParser {

    protected PriorityQueue<ChartEdge> agenda;;
    protected GrammarByLeftNonTermList grammarByChildren;
    protected LinkedList<ChartEdge>[][] needLeftGhostEdges, needRightGhostEdges;
    protected int nAgendaPush, nAgendaPop, nChartEdges, nGhostEdges;
    protected EdgeFOM edgeFOM;

    public AgendaChartParserGhostEdges(GrammarByLeftNonTermList grammar, EdgeFOMType fomType) {
        super(grammar, fomType);
        grammarByChildren = (GrammarByLeftNonTermList) grammar;
    }

    @SuppressWarnings("unchecked")
    protected void initParser(int sentLength) {
        super.initParser(sentLength);

        agenda = new PriorityQueue<ChartEdge>();
        needLeftGhostEdges = (LinkedList<ChartEdge>[][]) new LinkedList[grammar.numNonTerms()][chartSize + 1];
        needRightGhostEdges = (LinkedList<ChartEdge>[][]) new LinkedList[grammar.numNonTerms()][chartSize + 1];
        nAgendaPush = nAgendaPop = nChartEdges = nGhostEdges = 0;
        // System.out.println(grammar.numNonTerms()+" "+chartSize);
    }

    public ParseTree findMLParse(String sentence) throws Exception {
        ChartCell parentCell;
        boolean edgeAdded;
        ChartEdge edge;
        Token sent[] = grammar.tokenize(sentence);

        initParser(sent.length);
        addLexicalProductions(sent);

        while (!agenda.isEmpty() && (rootChartCell.getBestEdge(grammar.startSymbol) == null)) {
            edge = agenda.poll(); // get and remove top agenda edge
            // System.out.println("Agenda Pop: "+edge);
            nAgendaPop += 1;

            if (edge.p.isUnaryProd()) {
                parentCell = edge.leftCell;
            } else {
                parentCell = chart[edge.leftCell.start][edge.rightCell.end];
            }
            edgeAdded = parentCell.addEdge(edge);

            // if A->B C is added to chart but A was already in this chart cell, then the
            // first edge must have been better than the current edge because we pull edges
            // from the agenda best-first. This also means that the exact same ghost edges
            // have already been added. Edges can be popped from the agenda, though, and be
            // worse than the current best edge in the cell. We just pass these by.
            if (edgeAdded) {
                expandAgendaFrontier(edge.p.parent, parentCell);
                nChartEdges += 1;
            }
        }

        if (agenda.isEmpty()) {
            Log.info(1, "WARNING: Agenda is empty.  All edges have been added to chart.");
        }

        return extractBestParse();
    }

    protected void addEdgeToAgenda(ChartEdge edge) {
        // There are a lot of unnecessary edges being added to the agenda. For example,
        // edge[i][j][A] w/ prob=0.1 can be pushed, and if it isn't popped off the agenda
        // by the time edge[i][j][A] w/ prob=0.0001 can be pushed, then it is also added
        // to the agenda
        // System.out.println("Agenda Push: "+edge);
        nAgendaPush += 1;
        agenda.add(edge);
    }

    protected void addLexicalProductions(Token sent[]) throws Exception {
        // add lexical productions and unary productions to the base cells of the chart
        for (int i = 0; i < chartSize; i++) {
            for (Production lexProd : grammar.getLexProdsForToken(sent[i])) {
                addEdgeToAgenda(new ChartEdge(lexProd, chart[i][i + 1], lexProd.prob));
            }
        }
    }

    protected void expandAgendaFrontier(int nonTerm, ChartCell cell) {
        LinkedList<ChartEdge> possibleEdges;
        LinkedList<Production> possibleRules;
        ChartEdge newEdge = cell.getBestEdge(nonTerm);
        ChartEdge curBestEdge;
        float prob, partialProb;

        // unary edges are always possible in any cell, although we don't allow unary chains
        if (newEdge.p.isUnaryProd() == false || newEdge.p.isLexProd() == true) {
            for (Production p : grammar.getUnaryProdsWithChild(newEdge.p.parent)) {
                prob = p.prob + newEdge.insideProb;
                addEdgeToAgenda(new ChartEdge(p, cell, prob));
            }
        }

        if (cell == rootChartCell) {
            // no ghost edges possible from here ... only add the root productions
            // actually, TOP edges will already be added in the unary step
            /*
             * for (Production p : grammar.getUnaryProdsWithChild(newEdge.p.parent)) { if (p.parent ==
             * grammar.startSymbol) { prob = p.prob+newEdge.insideProb; addEdgeToAgenda(new ChartEdge(p,
             * rootChartCell, rootChartCell, prob)); } }
             */
        } else {
            // connect ghost edges that need left side
            possibleEdges = needLeftGhostEdges[nonTerm][cell.end];
            if (possibleEdges != null) {
                for (ChartEdge ghostEdge : possibleEdges) {
                    curBestEdge = chart[cell.start][ghostEdge.rightCell.end].getBestEdge(ghostEdge.p.parent);
                    if (curBestEdge == null) {
                        // ghost edge inside prob = grammar rule prob + ONE CHILD inside prob
                        prob = newEdge.insideProb + ghostEdge.insideProb;
                        addEdgeToAgenda(new ChartEdge(ghostEdge.p, cell, ghostEdge.rightCell, prob));
                    }
                }
            }

            // connect ghost edges that need right side
            possibleEdges = needRightGhostEdges[nonTerm][cell.start];
            if (possibleEdges != null) {
                for (ChartEdge ghostEdge : possibleEdges) {
                    curBestEdge = chart[ghostEdge.leftCell.start][cell.end].getBestEdge(ghostEdge.p.parent);
                    if (curBestEdge == null) {
                        prob = newEdge.insideProb + ghostEdge.insideProb;
                        addEdgeToAgenda(new ChartEdge(ghostEdge.p, ghostEdge.leftCell, cell, prob));
                    }
                }
            }

            // create left ghost edges. Can't go left if we are on the very left side of the chart
            if (cell.start > 0) {
                possibleRules = grammarByChildren.getBinaryProdsWithRightChild(nonTerm);
                if (possibleRules != null) {
                    for (Production p : possibleRules) {
                        if (needLeftGhostEdges[p.leftChild][cell.start] == null) {
                            needLeftGhostEdges[p.leftChild][cell.start] = new LinkedList<ChartEdge>();
                        }
                        partialProb = p.prob + newEdge.insideProb;
                        needLeftGhostEdges[p.leftChild][cell.start].add(new ChartEdge(p, null, cell,
                            partialProb));
                        nGhostEdges += 1;
                    }
                }
            }

            // create right ghost edges. Can't go right if we are on the very right side of the chart
            if (cell.end < chartSize - 1) {
                possibleRules = grammarByChildren.getBinaryProdsWithLeftChild(nonTerm);
                if (possibleRules != null) {
                    for (Production p : possibleRules) {
                        if (needRightGhostEdges[p.rightChild][cell.end] == null) {
                            needRightGhostEdges[p.rightChild][cell.end] = new LinkedList<ChartEdge>();
                        }
                        partialProb = p.prob + newEdge.insideProb;
                        needRightGhostEdges[p.rightChild][cell.end].add(new ChartEdge(p, cell, null,
                            partialProb));
                        nGhostEdges += 1;
                    }
                }
            }
        }
    }

    public String getStats() {
        // for (ChartEdge edge : needLeftGhostEdges[200][2]) {
        // System.out.println(edge);
        // }

        return " chartEdges=" + nChartEdges + " agendaPush=" + nAgendaPush + " agendaPop=" + nAgendaPop
                + " ghostEdges=" + nGhostEdges;
    }

}

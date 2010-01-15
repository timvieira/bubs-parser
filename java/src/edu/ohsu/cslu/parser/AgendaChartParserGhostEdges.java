package edu.ohsu.cslu.parser;

import java.util.LinkedList;

import edu.ohsu.cslu.grammar.GrammarByLeftNonTermList;
import edu.ohsu.cslu.grammar.BaseGrammar.Production;
import edu.ohsu.cslu.parser.fom.EdgeFOM;

public class AgendaChartParserGhostEdges extends AgendaChartParser {

    protected LinkedList<ChartEdge>[][] needLeftGhostEdges, needRightGhostEdges;
    int nGhostEdges;

    public AgendaChartParserGhostEdges(final GrammarByLeftNonTermList grammar, final EdgeFOM edgeFOM) {
        super(grammar, edgeFOM);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void initParser(final int sentLength) {
        super.initParser(sentLength);

        needLeftGhostEdges = new LinkedList[grammar.numNonTerms()][chartSize + 1];
        needRightGhostEdges = new LinkedList[grammar.numNonTerms()][chartSize + 1];
        nGhostEdges = 0;
    }

    @Override
    protected void addEdgeToFrontier(final ChartEdgeWithFOM edge) {
        // There are a lot of unnecessary edges being added to the agenda. For example,
        // edge[i][j][A] w/ prob=0.1 can be pushed, and if it isn't popped off the agenda
        // by the time edge[i][j][A] w/ prob=0.0001 can be pushed, then it is also added
        // to the agenda
        // System.out.println("Agenda Push: "+edge);
        nAgendaPush += 1;
        agenda.add(edge);
    }

    @Override
    protected void expandFrontier(final ChartEdge newEdge, final ArrayChartCell cell) {
        LinkedList<ChartEdge> possibleEdges;
        LinkedList<Production> possibleRules;
        ChartEdge curBestEdge;
        float prob, partialProb;
        final int nonTerm = newEdge.p.parent;

        // unary edges are always possible in any cell, although we don't allow
        // unary chains
        if (newEdge.p.isUnaryProd() == false || newEdge.p.isLexProd() == true) {
            for (final Production p : grammar.getUnaryProdsWithChild(newEdge.p.parent)) {
                prob = p.prob + newEdge.insideProb;
                addEdgeToFrontier(new ChartEdgeWithFOM(p, cell, prob, edgeFOM, this));
            }
        }

        if (cell == rootChartCell) {
            // no ghost edges possible from here ... only add the root productions
            // actually, TOP edges will already be added in the unary step
            /*
             * for (final Production p : grammar.getUnaryProdsWithChild(newEdge.p.parent)) { if (p.parent == grammar.startSymbol) { prob = p.prob + newEdge.insideProb;
             * addEdgeToAgenda(new ChartEdgeWithFOM(p, rootChartCell, rootChartCell, prob, edgeFOM)); } }
             */
        } else {
            // connect ghost edges that need left side
            possibleEdges = needLeftGhostEdges[nonTerm][cell.end];
            if (possibleEdges != null) {
                for (final ChartEdge ghostEdge : possibleEdges) {
                    curBestEdge = chart[cell.start][ghostEdge.rightCell.end()].getBestEdge(ghostEdge.p.parent);
                    if (curBestEdge == null) {
                        // ghost edge inside prob = grammar rule prob + ONE
                        // CHILD inside prob
                        prob = newEdge.insideProb + ghostEdge.insideProb;
                        addEdgeToFrontier(new ChartEdgeWithFOM(ghostEdge.p, cell, (BaseChartCell) ghostEdge.rightCell, prob, edgeFOM, this));
                    }
                }
            }

            // connect ghost edges that need right side
            possibleEdges = needRightGhostEdges[nonTerm][cell.start];
            if (possibleEdges != null) {
                for (final ChartEdge ghostEdge : possibleEdges) {
                    curBestEdge = chart[ghostEdge.leftCell.start()][cell.end].getBestEdge(ghostEdge.p.parent);
                    if (curBestEdge == null) {
                        prob = newEdge.insideProb + ghostEdge.insideProb;
                        addEdgeToFrontier(new ChartEdgeWithFOM(ghostEdge.p, (BaseChartCell) ghostEdge.leftCell, cell, prob, edgeFOM, this));
                    }
                }
            }

            // create left ghost edges. Can't go left if we are on the very left
            // side of the chart
            if (cell.start > 0) {
                possibleRules = grammarByChildren.getBinaryProdsWithRightChild(nonTerm);
                if (possibleRules != null) {
                    for (final Production p : possibleRules) {
                        if (needLeftGhostEdges[p.leftChild][cell.start] == null) {
                            needLeftGhostEdges[p.leftChild][cell.start] = new LinkedList<ChartEdge>();
                        }
                        partialProb = p.prob + newEdge.insideProb;
                        needLeftGhostEdges[p.leftChild][cell.start].add(new ChartEdge(p, null, cell, partialProb));
                        nGhostEdges += 1;
                    }
                }
            }

            // create right ghost edges. Can't go right if we are on the very
            // right side of the chart
            if (cell.end < chartSize - 1) {
                possibleRules = grammarByChildren.getBinaryProdsWithLeftChild(nonTerm);
                if (possibleRules != null) {
                    for (final Production p : possibleRules) {
                        if (needRightGhostEdges[p.rightChild][cell.end] == null) {
                            needRightGhostEdges[p.rightChild][cell.end] = new LinkedList<ChartEdge>();
                        }
                        partialProb = p.prob + newEdge.insideProb;
                        needRightGhostEdges[p.rightChild][cell.end].add(new ChartEdge(p, cell, null, partialProb));
                        nGhostEdges += 1;
                    }
                }
            }
        }
    }

    @Override
    public String getStats() {
        return super.getStats() + " ghostEdges=" + nGhostEdges;
    }

}

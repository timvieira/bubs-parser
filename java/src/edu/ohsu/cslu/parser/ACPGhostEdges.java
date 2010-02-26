package edu.ohsu.cslu.parser;

import java.util.Collection;
import java.util.LinkedList;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector;

public class ACPGhostEdges extends AgendaChartParser {

    protected LinkedList<ChartEdge>[][] needLeftGhostEdges, needRightGhostEdges;
    int nGhostEdges;

    public ACPGhostEdges(final Grammar grammar, final EdgeSelector edgeSelector) {
        super(grammar, edgeSelector);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void initParser(final int sentLength) {
        super.initParser(sentLength);

        needLeftGhostEdges = new LinkedList[grammar.numNonTerms()][chart.size() + 1];
        needRightGhostEdges = new LinkedList[grammar.numNonTerms()][chart.size() + 1];
        nGhostEdges = 0;
    }

    @Override
    protected void addEdgeToFrontier(final ChartEdge edge) {
        // There are a lot of unnecessary edges being added to the agenda. For example,
        // edge[i][j][A] w/ prob=0.1 can be pushed, and if it isn't popped off the agenda
        // by the time edge[i][j][A] w/ prob=0.0001 can be pushed, then it is also added
        // to the agenda
        // System.out.println("Agenda Push: "+edge);
        nAgendaPush += 1;
        agenda.add(edge);
    }

    @Override
    protected void expandFrontier(final ChartEdge newEdge, final ChartCell cell) {
        LinkedList<ChartEdge> possibleEdges;
        Collection<Production> possibleRules;
        ChartEdge curBestEdge;
        float prob, partialProb;
        final int nonTerm = newEdge.prod.parent;

        // unary edges are always possible in any cell, although we don't allow
        // unary chains
        if (newEdge.prod.isUnaryProd() == false || newEdge.prod.isLexProd() == true) {
            for (final Production p : grammar.getUnaryProductionsWithChild(newEdge.prod.parent)) {
                prob = p.prob + newEdge.inside;
                addEdgeToFrontier(new ChartEdge(p, cell, prob, edgeSelector));
            }
        }

        if (cell == chart.getRootCell()) {
            // no ghost edges possible from here ... only add the root productions
            // actually, TOP edges will already be added in the unary step
            /*
             * for (final Production p : grammar.getUnaryProdsWithChild(newEdge.p.parent)) { if (p.parent == grammar.startSymbol) { prob = p.prob + newEdge.insideProb;
             * addEdgeToAgenda(new ChartEdge(p, rootChartCell, rootChartCell, prob, edgeFOM)); } }
             */
        } else {
            // connect ghost edges that need left side
            possibleEdges = needLeftGhostEdges[nonTerm][cell.end()];
            if (possibleEdges != null) {
                for (final ChartEdge ghostEdge : possibleEdges) {
                    curBestEdge = chart.getCell(cell.start(), ghostEdge.end()).getBestEdge(ghostEdge.prod.parent);
                    if (curBestEdge == null) {
                        // ghost edge inside prob = grammar rule prob + ONE
                        // CHILD inside prob
                        prob = newEdge.inside + ghostEdge.inside;
                        addEdgeToFrontier(new ChartEdge(ghostEdge.prod, cell, ghostEdge.rightCell, prob, edgeSelector));
                    }
                }
            }

            // connect ghost edges that need right side
            possibleEdges = needRightGhostEdges[nonTerm][cell.start()];
            if (possibleEdges != null) {
                for (final ChartEdge ghostEdge : possibleEdges) {
                    curBestEdge = chart.getCell(ghostEdge.start(), cell.end()).getBestEdge(ghostEdge.prod.parent);
                    if (curBestEdge == null) {
                        prob = newEdge.inside + ghostEdge.inside;
                        addEdgeToFrontier(new ChartEdge(ghostEdge.prod, ghostEdge.leftCell, cell, prob, edgeSelector));
                    }
                }
            }

            // create left ghost edges. Can't go left if we are on the very left
            // side of the chart
            if (cell.start() > 0) {
                possibleRules = grammar.getBinaryProductionsWithRightChild(nonTerm);
                if (possibleRules != null) {
                    for (final Production p : possibleRules) {
                        if (needLeftGhostEdges[p.leftChild][cell.start()] == null) {
                            needLeftGhostEdges[p.leftChild][cell.start()] = new LinkedList<ChartEdge>();
                        }
                        partialProb = p.prob + newEdge.inside;
                        needLeftGhostEdges[p.leftChild][cell.start()].add(new ChartEdge(p, null, cell, partialProb));
                        nGhostEdges += 1;
                    }
                }
            }

            // create right ghost edges. Can't go right if we are on the very
            // right side of the chart
            if (cell.end() < chart.size() - 1) {
                possibleRules = grammar.getBinaryProductionsWithLeftChild(nonTerm);
                if (possibleRules != null) {
                    for (final Production p : possibleRules) {
                        if (needRightGhostEdges[p.rightChild][cell.end()] == null) {
                            needRightGhostEdges[p.rightChild][cell.end()] = new LinkedList<ChartEdge>();
                        }
                        partialProb = p.prob + newEdge.inside;
                        needRightGhostEdges[p.rightChild][cell.end()].add(new ChartEdge(p, cell, null, partialProb));
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

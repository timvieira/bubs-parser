package edu.ohsu.cslu.parser;

import java.util.LinkedList;
import java.util.PriorityQueue;

import edu.ohsu.cslu.grammar.GrammarByLeftNonTermList;
import edu.ohsu.cslu.grammar.ArrayGrammar.Production;
import edu.ohsu.cslu.grammar.Tokenizer.Token;
import edu.ohsu.cslu.parser.fom.EdgeFOM;
import edu.ohsu.cslu.parser.util.Log;
import edu.ohsu.cslu.parser.util.ParseTree;

public class AgendaChartParser extends ChartParser implements MaximumLikelihoodParser {

    protected PriorityQueue<ChartEdgeWithFOM> agenda;
    protected GrammarByLeftNonTermList grammarByChildren;
    protected int nAgendaPush, nAgendaPop, nChartEdges;
    protected EdgeFOM edgeFOM;

    public AgendaChartParser(final GrammarByLeftNonTermList grammar, final EdgeFOM edgeFOM) {
        super(grammar);
        grammarByChildren = grammar;
        // this.edgeFOM = EdgeFOM.create(fomType, this);
        this.edgeFOM = edgeFOM;
    }

    @Override
    protected void initParser(final int sentLength) {
        super.initParser(sentLength);

        agenda = new PriorityQueue<ChartEdgeWithFOM>();
        nAgendaPush = nAgendaPop = nChartEdges = 0;
    }

    public ParseTree findMLParse(final String sentence) throws Exception {
        return findBestParse(sentence);
    }

    public ParseTree findBestParse(final String sentence) throws Exception {
        ChartEdgeWithFOM edge;
        boolean edgeAdded;
        final Token sent[] = grammar.tokenize(sentence);

        initParser(sent.length);
        addLexicalProductions(sent);

        while (!agenda.isEmpty() && (rootChartCell.getBestEdge(grammar.startSymbol) == null)) {
            edge = agenda.poll(); // get and remove top agenda edge
            nAgendaPop += 1;
            // System.out.println("AgendaPop: " + edge);
            ArrayChartCell parentCell;

            parentCell = chart[edge.start()][edge.end()];
            edgeAdded = parentCell.addEdge(edge);

            // if A->B C is added to chart but A->X Y was already in this chart cell, then the
            // first edge must have been better than the current edge because we pull edges
            // from the agenda best-first. This also means that the entire frontier
            // has already been added.
            if (edgeAdded) {
                expandAgendaFrontier(edge.p.parent, parentCell);
                nChartEdges += 1;
            }
        }

        if (agenda.isEmpty()) {
            Log.info(1, "WARNING: Agenda is empty.  All edges have been added to chart.");
        }

        // agenda.clear();
        // System.gc();
        return extractBestParse();
    }

    protected void addEdgeToAgenda(final ChartEdgeWithFOM edge) {
        // System.out.println("AgendaPush: " + edge);
        // System.out.println("AgendaPush: " + edge.spanLength() + " " + edge.insideProb + " " + edge.figureOfMerit);
        nAgendaPush += 1;
        agenda.add(edge);
    }

    @Override
    protected void addLexicalProductions(final Token sent[]) throws Exception {
        ChartEdgeWithFOM newEdge;
        final LinkedList<ChartEdgeWithFOM> edgesToExpand = new LinkedList<ChartEdgeWithFOM>();

        // add lexical productions and unary productions to the base cells of the chart
        for (int i = 0; i < chartSize; i++) {
            for (final Production lexProd : grammar.getLexProdsForToken(sent[i])) {
                newEdge = new ChartEdgeWithFOM(lexProd, chart[i][i + 1], lexProd.prob, edgeFOM, this);
                // addEdgeToAgenda(newEdge);
                // Add lexical prods directly to the chart instead of to the agenda because
                // the boundary FOM (and possibly others use the surrounding POS tags to calculate
                // the fit of a new edge. If the POS tags don't exist yet (are still in the agenda)
                // it skew probs (to -Infinity) and never allow some edges that should be allowed
                chart[i][i + 1].addEdge(newEdge);
                edgesToExpand.add(newEdge);
                // System.out.println("Addding: " + newEdge);
            }
        }

        edgeFOM.init(this);

        for (final ChartEdgeWithFOM edge : edgesToExpand) {
            expandAgendaFrontier(edge.p.parent, chart[edge.leftCell.start][edge.leftCell.end]);
        }
    }

    protected void expandAgendaFrontier(final int nonTerm, final ArrayChartCell cell) {
        LinkedList<Production> possibleGrammarProds;
        final ChartEdge newEdge = cell.getBestEdge(nonTerm);
        ChartEdge leftEdge, rightEdge;
        ArrayChartCell rightCell, leftCell;
        float prob;

        // unary edges are always possible in any cell, although we don't allow
        // unary chains
        if (newEdge.p.isUnaryProd() == false || newEdge.p.isLexProd() == true) {
            for (final Production p : grammar.getUnaryProdsWithChild(newEdge.p.parent)) {
                prob = p.prob + newEdge.insideProb;
                addEdgeToAgenda(new ChartEdgeWithFOM(p, cell, prob, edgeFOM, this));
            }
        }

        if (cell == rootChartCell) {
            // no possible connections to other edges other than unary ... only
            // add the root productions
            // actually, TOP edges will already be added in the unary step
            // TODO: separate TOP->X from other unary productions
            /*
             * for (Production p : grammar.getUnaryProdsWithChild(newEdge.p.parent)) { if (p.parent == grammar.startSymbol) { prob = p.prob+newEdge.insideProb; addEdgeToAgenda(new
             * ChartEdge(p, rootChartCell, rootChartCell, prob)); } }
             */
        } else {
            // connect edge as possible right non-term
            for (int beg = 0; beg < cell.start; beg++) {
                leftCell = chart[beg][cell.start];
                possibleGrammarProds = grammarByChildren.getBinaryProdsWithRightChild(nonTerm);
                if (possibleGrammarProds != null) {
                    for (final Production p : possibleGrammarProds) {
                        leftEdge = leftCell.getBestEdge(p.leftChild);
                        if (leftEdge != null && chart[beg][cell.end].getBestEdge(p.parent) == null) {
                            prob = p.prob + newEdge.insideProb + leftEdge.insideProb;
                            // System.out.println("LEFT:"+new ChartEdge(p, prob,
                            // leftCell, cell));
                            addEdgeToAgenda(new ChartEdgeWithFOM(p, leftCell, cell, prob, edgeFOM, this));
                        }
                    }
                }
            }

            // connect edge as possible left non-term
            for (int end = cell.end + 1; end <= chartSize; end++) {
                rightCell = chart[cell.end][end];
                possibleGrammarProds = grammarByChildren.getBinaryProdsWithLeftChild(nonTerm);
                if (possibleGrammarProds != null) {
                    for (final Production p : possibleGrammarProds) {
                        rightEdge = rightCell.getBestEdge(p.rightChild);
                        if (rightEdge != null && chart[cell.start][end].getBestEdge(p.parent) == null) {
                            prob = p.prob + rightEdge.insideProb + newEdge.insideProb;
                            // System.out.println("RIGHT: "+new ChartEdge(p,prob, cell,rightCell));
                            addEdgeToAgenda(new ChartEdgeWithFOM(p, cell, rightCell, prob, edgeFOM, this));
                        }
                    }
                }
            }
        }
    }

    @Override
    public String getStats() {
        return " chartEdges=" + nChartEdges + " agendaPush=" + nAgendaPush + " agendaPop=" + nAgendaPop;
    }
}

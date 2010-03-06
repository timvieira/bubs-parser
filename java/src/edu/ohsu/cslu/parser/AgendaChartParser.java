package edu.ohsu.cslu.parser;

import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import edu.ohsu.cslu.grammar.LeftRightListsGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector;
import edu.ohsu.cslu.parser.util.Log;
import edu.ohsu.cslu.parser.util.ParseTree;

public class AgendaChartParser extends ChartParser {

    protected PriorityQueue<ChartEdge> agenda;
    protected int nAgendaPush, nAgendaPop, nChartEdges;
    protected EdgeSelector edgeSelector;
    private LeftRightListsGrammar leftRightListsGrammar;

    public AgendaChartParser(final LeftRightListsGrammar grammar, final EdgeSelector edgeSelector) {
        super(grammar);
        this.edgeSelector = edgeSelector;
        leftRightListsGrammar = grammar;
    }

    @Override
    protected void initParser(final int sentLength) {
        super.initParser(sentLength);

        agenda = new PriorityQueue<ChartEdge>();
        nAgendaPush = nAgendaPop = nChartEdges = 0;
    }

    public ParseTree findMLParse(final String sentence) throws Exception {
        return findBestParse(sentence);
    }

    @Override
    public ParseTree findBestParse(final String sentence) throws Exception {
        ChartEdge edge;
        ChartCell parentCell;
        boolean edgeAdded;
        final int sent[] = grammar.tokenizer.tokenizeToIndex(sentence);

        initParser(sent.length);
        final List<ChartEdge> edgesToExpand = addLexicalProductions(sent);
        edgeSelector.init(this);

        for (final ChartEdge lexEdge : edgesToExpand) {
            expandFrontier(lexEdge, chart.getCell(lexEdge.start(), lexEdge.end()));
        }

        while (!agenda.isEmpty() && !hasCompleteParse()) {
            edge = agenda.poll(); // get and remove top agenda edge
            nAgendaPop += 1;
            // System.out.println("AgendaPop: " + ((BoundaryInOut) edgeFOM).calcFOMToString(edge));

            parentCell = chart.getCell(edge.start(), edge.end());
            edgeAdded = parentCell.addEdge(edge);

            // if A->B C is added to chart but A->X Y was already in this chart cell, then the
            // first edge must have been better than the current edge because we pull edges
            // from the agenda best-first. This also means that the entire frontier
            // has already been added.
            if (edgeAdded) {
                expandFrontier(edge, parentCell);
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

    protected void addEdgeToFrontier(final ChartEdge edge) {
        // System.out.println("AgendaPush: " + edge.spanLength() + " " + edge.inside + " " + edge.fom);
        nAgendaPush += 1;
        agenda.add(edge);
    }

    @Override
    protected List<ChartEdge> addLexicalProductions(final int sent[]) throws Exception {
        ChartEdge newEdge;
        ChartCell cell;
        final List<ChartEdge> edgesToExpand = new LinkedList<ChartEdge>();

        // add lexical productions and unary productions to the base cells of the chart
        for (int i = 0; i < chart.size(); i++) {
            cell = chart.getCell(i, i + 1);
            for (final Production lexProd : leftRightListsGrammar.getLexicalProductionsWithChild(sent[i])) {
                newEdge = new ChartEdge(lexProd, cell, lexProd.prob, edgeSelector);
                // Add lexical prods directly to the chart instead of to the agenda because
                // the boundary FOM (and possibly others use the surrounding POS tags to calculate
                // the fit of a new edge. If the POS tags don't exist yet (are still in the agenda)
                // it skew probs (to -Infinity) and never allow some edges that should be allowed
                cell.addEdge(newEdge);
                edgesToExpand.add(newEdge);
            }
        }

        return edgesToExpand;
    }

    protected void expandFrontier(final ChartEdge newEdge, final ChartCell cell) {
        ChartEdge leftEdge, rightEdge;
        ChartCell rightCell, leftCell;
        float prob;
        final int nonTerm = newEdge.prod.parent;

        // unary edges are always possible in any cell, although we don't allow unary chains
        if (newEdge.prod.isUnaryProd() == false || newEdge.prod.isLexProd() == true) {
            for (final Production p : leftRightListsGrammar.getUnaryProductionsWithChild(newEdge.prod.parent)) {
                prob = p.prob + newEdge.inside;
                addEdgeToFrontier(new ChartEdge(p, cell, prob, edgeSelector));
            }
        }

        // connect edge as possible right non-term
        for (int beg = 0; beg < cell.start(); beg++) {
            leftCell = chart.getCell(beg, cell.start());
            for (final Production p : leftRightListsGrammar.getBinaryProductionsWithRightChild(nonTerm)) {
                leftEdge = leftCell.getBestEdge(p.leftChild);
                if (leftEdge != null && chart.getCell(beg, cell.end()).getBestEdge(p.parent) == null) {
                    prob = p.prob + newEdge.inside + leftEdge.inside;
                    // System.out.println("LEFT:"+new ChartEdge(p, prob, leftCell, cell));
                    addEdgeToFrontier(new ChartEdge(p, leftCell, cell, prob, edgeSelector));
                }
            }
        }

        // connect edge as possible left non-term
        for (int end = cell.end() + 1; end <= chart.size(); end++) {
            rightCell = chart.getCell(cell.end(), end);
            for (final Production p : leftRightListsGrammar.getBinaryProductionsWithLeftChild(nonTerm)) {
                rightEdge = rightCell.getBestEdge(p.rightChild);
                if (rightEdge != null && chart.getCell(cell.start(), end).getBestEdge(p.parent) == null) {
                    prob = p.prob + rightEdge.inside + newEdge.inside;
                    // System.out.println("RIGHT: "+new ChartEdge(p,prob, cell,rightCell));
                    addEdgeToFrontier(new ChartEdge(p, cell, rightCell, prob, edgeSelector));
                }
            }
        }
    }

    @Override
    public String getStats() {
        return " chartEdges=" + nChartEdges + " agendaPush=" + nAgendaPush + " agendaPop=" + nAgendaPop;
    }
}

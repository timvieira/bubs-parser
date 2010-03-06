package edu.ohsu.cslu.parser;

import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;

import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector;
import edu.ohsu.cslu.parser.util.ParseTree;

public class CoarseCellAgendaParser extends ChartParser {

    EdgeSelector edgeSelector;
    float[][] maxEdgeFOM;
    PriorityQueue<ChartCell> spanAgenda;
    protected LeftHashGrammar leftHashGrammar;

    public CoarseCellAgendaParser(final LeftHashGrammar grammar, final EdgeSelector edgeFOM) {
        super(grammar);
        this.edgeSelector = edgeFOM;
        leftHashGrammar = grammar;
    }

    @Override
    protected void initParser(final int n) {
        super.initParser(n);
        this.maxEdgeFOM = new float[chart.size()][chart.size() + 1];
        this.spanAgenda = new PriorityQueue<ChartCell>();

        // The chart is (chart.size()+1)*chart.size()/2
        for (int start = 0; start < chart.size(); start++) {
            for (int end = start + 1; end < chart.size() + 1; end++) {
                maxEdgeFOM[start][end] = Float.NEGATIVE_INFINITY;
            }
        }
    }

    @Override
    public ParseTree findBestParse(final String sentence) throws Exception {
        ChartCell cell;
        final int sent[] = grammar.tokenizer.tokenizeToIndex(sentence);
        currentSentence = sentence;

        initParser(sent.length);
        addLexicalProductions(sent);
        edgeSelector.init(this);
        addUnaryExtensionsToLexProds();

        for (int i = 0; i < chart.size(); i++) {
            expandFrontier(chart.getCell(i, i + 1));
        }

        while (hasNext() && !hasCompleteParse()) {
            cell = next();
            // System.out.println(" nextCell: " + cell);
            visitCell(cell);
            expandFrontier(cell);
        }

        return extractBestParse();
    }

    protected void visitCell(final ChartCell cell) {
        final int start = cell.start(), end = cell.end();
        Collection<Production> possibleProds;
        ChartEdge edge;
        final ChartEdge[] bestEdges = new ChartEdge[grammar.numNonTerms()]; // inits to null

        final int maxEdgesToAdd = (int) ParserDriver.param2;

        for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
            final ChartCell leftCell = chart.getCell(start, mid);
            final ChartCell rightCell = chart.getCell(mid, end);
            for (final ChartEdge leftEdge : leftCell.getBestLeftEdges()) {
                for (final ChartEdge rightEdge : rightCell.getBestRightEdges()) {
                    possibleProds = leftHashGrammar.getBinaryProductionsWithChildren(leftEdge.prod.parent, rightEdge.prod.parent);
                    if (possibleProds != null) {
                        for (final Production p : possibleProds) {
                            final float prob = p.prob + leftEdge.inside + rightEdge.inside;
                            edge = new ChartEdge(p, leftCell, rightCell, prob, edgeSelector);
                            addEdgeToArray(edge, bestEdges);
                        }
                    }
                }
            }
        }

        addBestEdgesToChart(cell, bestEdges, maxEdgesToAdd);
    }

    protected void addEdgeToArray(final ChartEdge edge, final ChartEdge[] bestEdges) {
        final int parent = edge.prod.parent;
        if (bestEdges[parent] == null || edge.fom > bestEdges[parent].fom) {
            bestEdges[parent] = edge;
        }
    }

    private void addEdgeToAgenda(final ChartEdge edge, final PriorityQueue<ChartEdge> agenda) {
        agenda.add(edge);
    }

    protected void addBestEdgesToChart(final ChartCell cell, final ChartEdge[] bestEdges, final int maxEdgesToAdd) {
        ChartEdge edge, unaryEdge;
        boolean addedEdge;
        int numAdded = 0;

        final PriorityQueue<ChartEdge> agenda = new PriorityQueue<ChartEdge>();
        for (int i = 0; i < bestEdges.length; i++) {
            if (bestEdges[i] != null) {
                addEdgeToAgenda(bestEdges[i], agenda);
            }
        }

        while (agenda.isEmpty() == false && numAdded <= maxEdgesToAdd) {
            edge = agenda.poll();
            addedEdge = cell.addEdge(edge);
            if (addedEdge) {
                // System.out.println(" addingEdge: " + edge);
                numAdded++;
                // Add unary productions to agenda so they can compete with binary productions
                for (final Production p : grammar.getUnaryProductionsWithChild(edge.prod.parent)) {
                    unaryEdge = new ChartEdge(p, cell, p.prob + edge.inside, edgeSelector);
                    addEdgeToAgenda(unaryEdge, agenda);
                }
            }
        }

        // TODO: should I decrease the maxEdgeFOM here according to the best edge NOT in the chart?
        // won't this just be overrun when we expand the frontier?
        if (agenda.isEmpty()) {
            maxEdgeFOM[cell.start()][cell.end()] = Float.NEGATIVE_INFINITY;
        } else {
            maxEdgeFOM[cell.start()][cell.end()] = agenda.peek().fom;
        }
    }

    protected boolean hasNext() {
        return true;
    }

    protected ChartCell next() {
        // return spanAgenda.poll();
        ChartCell bestSpan = null;
        float bestScore = Float.NEGATIVE_INFINITY;
        for (int span = 1; span <= chart.size(); span++) {
            for (int beg = 0; beg < chart.size() - span + 1; beg++) { // beginning
                if (maxEdgeFOM[beg][beg + span] > bestScore) {
                    bestScore = maxEdgeFOM[beg][beg + span];
                    bestSpan = chart.getCell(beg, beg + span);
                }
            }
        }
        return bestSpan;
    }

    @Override
    protected List<ChartEdge> addLexicalProductions(final int sent[]) throws Exception {
        ChartEdge newEdge;
        // final LinkedList<ChartEdge> edgesToExpand = new LinkedList<ChartEdge>();

        for (int i = 0; i < chart.size(); i++) {
            for (final Production lexProd : grammar.getLexicalProductionsWithChild(sent[i])) {
                newEdge = new ChartEdge(lexProd, chart.getCell(i, i + 1), lexProd.prob, edgeSelector);
                chart.getCell(i, i + 1).addEdge(newEdge);
            }
        }
        return null;
    }

    public void addUnaryExtensionsToLexProds() {
        for (int i = 0; i < chart.size(); i++) {
            final ChartCell cell = chart.getCell(i, i + 1);
            for (final int pos : cell.getPosEntries()) {
                for (final Production unaryProd : grammar.getUnaryProductionsWithChild(pos)) {
                    cell.addEdge(unaryProd, cell, null, cell.getBestEdge(pos).inside + unaryProd.prob);
                }
            }
        }
    }

    // protected void addSpanToFrontier(final ChartCell span) {
    // //System.out.println("AgendaPush: " + edge.spanLength() + " " + edge.inside + " " + edge.fom);
    // if (maxEdgeFOM[span.start()][span.end()] > Float.NEGATIVE_INFINITY) {
    // spanAgenda.remove(span);
    // }
    // spanAgenda.add(span);
    // }

    protected void expandFrontier(final ChartCell cell) {

        // connect edge as possible right non-term
        for (int start = 0; start < cell.start(); start++) {
            setSpanMaxEdgeFOM(chart.getCell(start, cell.start()), cell);
        }

        // connect edge as possible left non-term
        for (int end = cell.end() + 1; end <= chart.size(); end++) {
            setSpanMaxEdgeFOM(cell, chart.getCell(cell.end(), end));
        }
    }

    protected void setSpanMaxEdgeFOM(final ChartCell leftCell, final ChartCell rightCell) {
        ChartEdge edge;
        final int start = leftCell.start(), end = rightCell.end();
        float bestFOM = maxEdgeFOM[start][end];

        // System.out.println(" setSpanMax: " + leftCell + " && " + rightCell);

        final List<ChartEdge> leftEdgeList = leftCell.getBestLeftEdges();
        final List<ChartEdge> rightEdgeList = rightCell.getBestRightEdges();
        Collection<Production> possibleProds;
        if (rightEdgeList.size() > 0 && leftEdgeList.size() > 0) {
            for (final ChartEdge leftEdge : leftEdgeList) {
                for (final ChartEdge rightEdge : rightEdgeList) {
                    possibleProds = leftHashGrammar.getBinaryProductionsWithChildren(leftEdge.prod.parent, rightEdge.prod.parent);
                    if (possibleProds != null) {
                        for (final Production p : possibleProds) {
                            final float prob = p.prob + leftEdge.inside + rightEdge.inside;
                            edge = new ChartEdge(p, leftCell, rightCell, prob, edgeSelector);
                            // System.out.println(" considering: " + edge);
                            if (edge.fom > bestFOM) {
                                bestFOM = edge.fom;
                            }
                        }
                    }
                }
            }
        }

        if (bestFOM > maxEdgeFOM[start][end]) {
            final ChartCell parentCell = chart.getCell(start, end);
            // if (maxEdgeFOM[start][end] > Float.NEGATIVE_INFINITY) {
            // spanAgenda.remove(parentCell);
            // }
            maxEdgeFOM[start][end] = bestFOM;
            parentCell.figureOfMerit = bestFOM;
            // spanAgenda.add(parentCell);
            // System.out.println(" addingSpan: " + parentCell);
        }
    }
}

package edu.ohsu.cslu.parser;

import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.grammar.Tokenizer.Token;
import edu.ohsu.cslu.parser.cellselector.CSLUTBlockedCells;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector;
import edu.ohsu.cslu.parser.util.ParseTree;

public class LocalBestFirstChartParser extends ChartParser {

    EdgeSelector edgeSelector;
    CellSelector cellSelector;
    int nAgendaPush;
    float fomInitSeconds;
    PriorityQueue<ChartEdge> agenda;

    int maxEdgesToAdd;
    float logBeamDeltaThresh;

    public LocalBestFirstChartParser(final Grammar grammar, final EdgeSelector edgeSelector, final CellSelector cellSelector) {
        super(grammar);
        this.edgeSelector = edgeSelector;
        this.cellSelector = cellSelector;

        maxEdgesToAdd = (int) ParserDriver.param1;
        if (maxEdgesToAdd < 0)
            maxEdgesToAdd = 10;

        // logBeamDeltaThresh = ParserDriver.param2;
        logBeamDeltaThresh = 9999;
        if (logBeamDeltaThresh < 0)
            logBeamDeltaThresh = 30;
    }

    @Override
    public ParseTree findBestParse(final String sentence) throws Exception {
        ChartCell cell;
        final Token sent[] = grammar.tokenize(sentence);
        currentSentence = sentence;

        initParser(sent.length);
        cellSelector.init(this);

        addLexicalProductions(sent);

        final double startTimeMS = System.currentTimeMillis();
        edgeSelector.init(this);
        fomInitSeconds = (float) ((System.currentTimeMillis() - startTimeMS) / 1000.0);

        nAgendaPush = 0;
        while (cellSelector.hasNext() && !hasCompleteParse()) {
            cell = cellSelector.next();
            visitCell(cell);
        }

        return extractBestParse();
    }

    @Override
    protected List<ChartEdge> addLexicalProductions(final Token sent[]) throws Exception {
        ChartCell cell;

        // add lexical productions to the base cells of the chart
        for (int i = 0; i < chart.size(); i++) {
            cell = chart.getCell(i, i + 1);
            for (final Production lexProd : grammar.getLexProdsByToken(sent[i])) {
                cell.addEdge(new ChartEdge(lexProd, cell, lexProd.prob));
            }
        }
        return null;
    }

    protected void visitCell(final ChartCell cell) {
        final int start = cell.start(), end = cell.end();
        final int spanWidth = end - start;
        Collection<Production> possibleProds;
        ChartEdge edge;

        boolean onlyFactored = false;
        if (cellSelector.type == CellSelector.CellSelectorType.CSLUT) {
            onlyFactored = ((CSLUTBlockedCells) cellSelector).isCellOpenOnlyToFactored(start, end);
        }

        edgeCollectionInit();

        if (spanWidth == 1) {
            for (final int pos : cell.getPosEntries()) {
                for (final Production p : grammar.getUnaryProductionsWithChild(pos)) {
                    final float prob = p.prob + cell.getBestEdge(pos).inside;
                    edge = new ChartEdge(p, cell, prob, edgeSelector);
                    addEdgeToCollection(edge);
                }
            }
        } else {
            for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
                final ChartCell leftCell = chart.getCell(start, mid);
                final ChartCell rightCell = chart.getCell(mid, end);
                for (final ChartEdge leftEdge : leftCell.getBestLeftEdges()) {
                    for (final ChartEdge rightEdge : rightCell.getBestRightEdges()) {
                        possibleProds = grammar.getBinaryProductionsWithChildren(leftEdge.prod.parent, rightEdge.prod.parent);
                        if (possibleProds != null) {
                            for (final Production p : possibleProds) {
                                if (!onlyFactored || grammar.getNonterminal(p.parent).isFactored()) {
                                    final float prob = p.prob + leftEdge.inside + rightEdge.inside;
                                    edge = new ChartEdge(p, leftCell, rightCell, prob, edgeSelector);
                                    addEdgeToCollection(edge);
                                }
                            }
                        }
                    }
                }
            }
        }

        addEdgeCollectionToChart(cell);
    }

    protected void edgeCollectionInit() {
        agenda = new PriorityQueue<ChartEdge>();
    }

    protected void addEdgeToCollection(final ChartEdge edge) {
        agenda.add(edge);
        nAgendaPush++;
    }

    protected void addEdgeCollectionToChart(final ChartCell cell) {
        ChartEdge edge, unaryEdge;
        boolean addedEdge;
        boolean edgeBelowThresh = false;
        int numAdded = 0;
        float bestFOM = Float.NEGATIVE_INFINITY;
        if (!agenda.isEmpty()) {
            bestFOM = agenda.peek().fom;
        }

        while (agenda.isEmpty() == false && numAdded <= maxEdgesToAdd && !edgeBelowThresh) {
            edge = agenda.poll();
            if (edge.fom < bestFOM - logBeamDeltaThresh) {
                edgeBelowThresh = true;
            } else {
                addedEdge = cell.addEdge(edge);
                if (addedEdge) {
                    numAdded++;

                    // Add unary productions to agenda so they can compete with binary productions
                    for (final Production p : grammar.getUnaryProductionsWithChild(edge.prod.parent)) {
                        final float prob = p.prob + edge.inside;
                        unaryEdge = new ChartEdge(p, cell, prob, edgeSelector);
                        if (unaryEdge.fom > bestFOM - logBeamDeltaThresh) {
                            addEdgeToCollection(unaryEdge);
                        }
                    }
                }
            }
        }
    }

    @Override
    public String getStats() {
        return super.getStats() + " agendaPush=" + nAgendaPush + " fomInitSec=" + fomInitSeconds;
    }
}

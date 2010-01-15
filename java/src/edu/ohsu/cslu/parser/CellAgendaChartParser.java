package edu.ohsu.cslu.parser;

import java.util.PriorityQueue;

import edu.ohsu.cslu.grammar.GrammarByLeftNonTermList;
import edu.ohsu.cslu.grammar.Tokenizer.Token;
import edu.ohsu.cslu.parser.fom.EdgeFOM;
import edu.ohsu.cslu.parser.traversal.ChartTraversal;
import edu.ohsu.cslu.parser.traversal.ChartTraversal.ChartTraversalType;
import edu.ohsu.cslu.parser.util.ParseTree;

public class CellAgendaChartParser extends AgendaChartParser {

    private FrontierCell frontier[][];
    private ChartTraversalType traversalType;

    public CellAgendaChartParser(final GrammarByLeftNonTermList grammar, final EdgeFOM edgeFOM, final ChartTraversalType traversalType) {
        super(grammar, edgeFOM);
        this.traversalType = traversalType;
    }

    @Override
    protected void initParser(final int sentLength) {
        super.initParser(sentLength);

        // parallel structure with chart to hold possible edges
        frontier = new FrontierCell[chartSize][chartSize + 1];
        for (int start = 0; start < chartSize; start++) {
            for (int end = start + 1; end < chartSize + 1; end++) {
                frontier[start][end] = new FrontierCell((ArrayChartCell) chart[start][end]);
            }
        }

        // we shouldn't be using the global edge agenda ...
        // TODO: if we generalize to a FrontierManager, then each subclass
        // can manage how the frontier is expanded on their own
        agenda = null;
    }

    @Override
    public ParseTree findBestParse(final String sentence) throws Exception {
        ArrayChartCell cell;
        final Token sent[] = grammar.tokenize(sentence);

        initParser(sent.length);
        addLexicalProductions(sent);

        final ChartTraversal chartTraversal = ChartTraversal.create(traversalType, this);
        while (chartTraversal.hasNext()) {
            cell = (ArrayChartCell) chartTraversal.next();
            visitCell(cell);
            // System.out.println(cell + " numFront=" + frontier[cell.start][cell.end].edgeAgenda.size());
        }

        return extractBestParse();
    }

    protected void visitCell(final ArrayChartCell cell) {
        ChartEdgeWithFOM edge;
        boolean addedEdge = false;
        final FrontierCell frontierCell = frontier[cell.start][cell.end];

        // while (addedEdge == false) {
        while (!frontierCell.edgeAgenda.isEmpty()) {
            nAgendaPop += 1;
            edge = frontierCell.edgeAgenda.poll();
            addedEdge = cell.addEdge(edge);

            if (addedEdge == true) {
                expandFrontier(edge, cell);
                nChartEdges += 1;
            }
        }
    }

    @Override
    protected void addEdgeToFrontier(final ChartEdgeWithFOM edge) {
        final boolean addedEdge = frontier[edge.start()][edge.end()].addEdgeToCellAgenda(edge);
        if (addedEdge) {
            nAgendaPush += 1;
        }
    }

    protected class FrontierCell {
        // protected ArrayChartCell chartCell;
        protected PriorityQueue<ChartEdgeWithFOM> edgeAgenda;
        protected ChartEdgeWithFOM agendaMemory[];

        public FrontierCell(final ArrayChartCell chartCell) {
            // this.chartCell = chartCell;
            this.edgeAgenda = new PriorityQueue<ChartEdgeWithFOM>();
            this.agendaMemory = new ChartEdgeWithFOM[grammar.numNonTerms()];
        }

        public boolean addEdgeToCellAgenda(final ChartEdgeWithFOM edge) {
            final ChartEdgeWithFOM bestAgendaEdge = agendaMemory[edge.p.parent];
            if (bestAgendaEdge == null || edge.figureOfMerit > bestAgendaEdge.figureOfMerit) {
                this.edgeAgenda.add(edge);
                agendaMemory[edge.p.parent] = edge;
                return true;
            }
            return false;
        }
    }

}

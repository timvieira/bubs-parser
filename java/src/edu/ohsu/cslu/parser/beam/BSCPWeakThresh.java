package edu.ohsu.cslu.parser.beam;

import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;

public class BSCPWeakThresh extends BeamSearchChartParser<LeftHashGrammar, CellChart> {

    public BSCPWeakThresh(final ParserDriver opts, final LeftHashGrammar grammar) {
        super(opts, grammar);
    }

    // track the worst FOM score in the agenda while we add edges. This
    // eliminates about 10% of the pushes, but actually ends up slowing the
    // parser down a little for the Berkeley grammar w/ BoundaryInOut FOM
    @Override
    protected void addEdgeToCollection(final ChartEdge edge) {
        cellConsidered++;
        if (fomCheckAndUpdate(edge)) {
            if (agenda.size() < beamWidth || edge.fom > localWorstFOM) {
                agenda.add(edge);
                cellPushed++;

                if (edge.fom < localWorstFOM) {
                    localWorstFOM = edge.fom;
                }
            }
        }
    }
}

package edu.ohsu.cslu.parser.beam;

import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;
import edu.ohsu.cslu.perceptron.AveragedPerceptron;

public class BSCPBeamConf extends BSCPPruneViterbi {

    protected AveragedPerceptron beamConfModel;

    public BSCPBeamConf(final ParserDriver opts, final LeftHashGrammar grammar, final AveragedPerceptron beamWidthModel) {
        super(opts, grammar);
        this.beamConfModel = beamWidthModel;
    }

    @Override
    protected void visitCell(final short start, final short end) {
        final HashSetChartCell cell = chart.getCell(start, end);
        ChartEdge edge;

        if (end - start == 1) {
            beamWidth = Integer.MAX_VALUE;
        } else {
            final SparseBitVector feats = getCellFeatures(start, end, beamConfModel.featureTemplate());
            beamWidth = (int) beamConfModel.class2value(beamConfModel.classify(feats));
        }

        System.out.println("[" + start + "," + end + "] beam=" + beamWidth);

        if (beamWidth > 0) {
            // final boolean onlyFactored = hasCellConstraints && cellConstraints.factoredParentsOnly(start, end);
            edgeCollectionInit();

            if (end - start == 1) {
                // lexical and unary productions can't compete in the same agenda until their FOM
                // scores are changed to be comparable
                for (final Production lexProd : grammar.getLexicalProductionsWithChild(chart.tokens[start])) {
                    cell.updateInside(lexProd, cell, null, lexProd.prob);
                    for (final Production unaryProd : grammar.getUnaryProductionsWithChild(lexProd.parent)) {
                        addEdgeToCollection(chart.new ChartEdge(unaryProd, cell));
                    }

                }
            } else {
                for (int mid = start + 1; mid < end; mid++) { // mid point
                    final HashSetChartCell leftCell = chart.getCell(start, mid);
                    final HashSetChartCell rightCell = chart.getCell(mid, end);
                    for (final int leftNT : leftCell.getLeftChildNTs()) {
                        for (final int rightNT : rightCell.getRightChildNTs()) {
                            for (final Production p : grammar.getBinaryProductionsWithChildren(leftNT, rightNT)) {
                                // if (!onlyFactored || grammar.getNonterminal(p.parent).isFactored()) {
                                edge = chart.new ChartEdge(p, leftCell, rightCell);
                                addEdgeToCollection(edge);
                                // }
                            }
                        }
                    }
                }
            }

            addEdgeCollectionToChart(cell);
        }
    }

}

package edu.ohsu.cslu.parser;

import java.util.Arrays;

import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;

public class ECPCellCrossHashGrammarLoop extends ChartParser<LeftHashGrammar, CellChart> {

    float[][] bestPairScore;
    int[][] bestPairMid;

    public ECPCellCrossHashGrammarLoop(final ParserDriver opts, final LeftHashGrammar grammar) {
        super(opts, grammar);
        bestPairScore = new float[grammar.numNonTerms()][grammar.numNonTerms()];
        bestPairMid = new int[grammar.numNonTerms()][grammar.numNonTerms()];
    }

    @Override
    protected void visitCell(final short start, final short end) {
        final HashSetChartCell cell = chart.getCell(start, end);
        float insideProb;

        final int nt = grammar.numNonTerms();
        for (int i = 0; i < nt; i++) {
            Arrays.fill(bestPairScore[i], Float.NEGATIVE_INFINITY);
            Arrays.fill(bestPairMid[i], -1);
        }

        for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
            final HashSetChartCell leftCell = chart.getCell(start, mid);
            final HashSetChartCell rightCell = chart.getCell(mid, end);
            for (final int leftNT : leftCell.getLeftChildNTs()) {
                final float leftIn = leftCell.getInside(leftNT);
                for (final int rightNT : rightCell.getRightChildNTs()) {
                    final float rightIn = rightCell.getInside(rightNT);
                    if (leftIn + rightIn > bestPairScore[leftNT][rightNT]) {
                        bestPairScore[leftNT][rightNT] = leftIn + rightIn;
                        bestPairMid[leftNT][rightNT] = leftCell.end();
                    }
                }
            }
        }

        for (int leftNT = 0; leftNT < nt; leftNT++) {
            for (int rightNT = 0; rightNT < nt; rightNT++) {
                if (bestPairScore[leftNT][rightNT] > Float.NEGATIVE_INFINITY) {
                    for (final Production p : grammar.getBinaryProductionsWithChildren(leftNT, rightNT)) {
                        insideProb = p.prob + bestPairScore[leftNT][rightNT];
                        final int mid = bestPairMid[leftNT][rightNT];
                        cell.updateInside(p, chart.getCell(start, mid), chart.getCell(mid, end), insideProb);
                    }
                }
            }
        }

        for (final int childNT : cell.getNtArray()) {
            for (final Production p : grammar.getUnaryProductionsWithChild(childNT)) {
                cell.updateInside(p, p.prob + cell.getInside(childNT));
            }
        }
    }
}

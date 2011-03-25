package edu.ohsu.cslu.parser;

import java.util.HashMap;
import java.util.Map;

import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.CellChart.HashSetChartCell;

public class ECPCellCrossHashGrammarLoop2 extends ChartParser<LeftHashGrammar, CellChart> {

    HashMap<Integer, Container> childHash;

    public ECPCellCrossHashGrammarLoop2(final ParserDriver opts, final LeftHashGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    protected void visitCell(final short start, final short end) {
        final HashSetChartCell cell = chart.getCell(start, end);
        float insideProb;

        childHash = new HashMap<Integer, Container>();
        final int numNT = grammar.numNonTerms();

        for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
            final HashSetChartCell leftCell = chart.getCell(start, mid);
            final HashSetChartCell rightCell = chart.getCell(mid, end);
            for (final int leftNT : leftCell.getLeftChildNTs()) {
                final float leftIn = leftCell.getInside(leftNT);
                for (final int rightNT : rightCell.getRightChildNTs()) {
                    final float rightIn = rightCell.getInside(rightNT);
                    final int key = leftNT * numNT + rightNT;
                    final Container old = childHash.get(key);
                    if (old == null) {
                        childHash.put(key, new Container(leftCell, rightCell, leftIn + rightIn));
                    } else if (old.score < leftIn + rightIn) {
                        old.left = leftCell;
                        old.right = rightCell;
                        old.score = leftIn + rightIn;
                    }
                }
            }
        }

        for (final Map.Entry<Integer, Container> entry : childHash.entrySet()) {
            final int leftNT = entry.getKey() / numNT;
            final int rightNT = entry.getKey() % numNT;
            for (final Production p : grammar.getBinaryProductionsWithChildren(leftNT, rightNT)) {
                insideProb = p.prob + entry.getValue().score;
                cell.updateInside(p, entry.getValue().left, entry.getValue().right, insideProb);
            }
        }

        for (final int childNT : cell.getNtArray()) {
            for (final Production p : grammar.getUnaryProductionsWithChild(childNT)) {
                cell.updateInside(p, p.prob + cell.getInside(childNT));
            }
        }
    }

    private class Container {
        public HashSetChartCell left, right;
        public float score;

        public Container(final HashSetChartCell left, final HashSetChartCell right, final float score) {
            this.left = left;
            this.right = right;
            this.score = score;
        }
    }
}

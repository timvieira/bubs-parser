package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.ArrayGrammar;
import edu.ohsu.cslu.grammar.BaseGrammar.Production;
import edu.ohsu.cslu.parser.traversal.ChartTraversal.ChartTraversalType;
import edu.ohsu.cslu.parser.util.ParseTree;

public class ECPGramLoop extends ExhaustiveChartParser {

    public ECPGramLoop(final ArrayGrammar grammar, final ChartTraversalType traversalType) {
        super(grammar, traversalType);
    }

    @Override
    public ParseTree findMLParse(final String sentence) throws Exception {
        return findBestParse(sentence);
    }

    @Override
    protected void visitCell(final ChartCell cell) {
        final ArrayChartCell arrayChartCell = (ArrayChartCell) cell;
        final ArrayGrammar arrayGrammar = (ArrayGrammar) grammar;
        final int start = arrayChartCell.start;
        final int end = arrayChartCell.end;

        for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
            // naive traversal through all grammar rules
            final ArrayChartCell leftCell = (ArrayChartCell) chart[start][mid];
            final ArrayChartCell rightCell = (ArrayChartCell) chart[mid][end];
            for (final Production p : arrayGrammar.binaryProds) {
                final ChartEdge leftEdge = leftCell.getBestEdge(p.leftChild);
                final ChartEdge rightEdge = rightCell.getBestEdge(p.rightChild);
                if ((leftEdge != null) && (rightEdge != null)) {
                    final float prob = p.prob + leftEdge.insideProb + rightEdge.insideProb;
                    // parentCell.addEdge(new ChartEdge(p, prob, leftCell, rightCell));
                    arrayChartCell.addEdge(p, prob, leftCell, rightCell);
                }
            }
        }

        for (final Production p : arrayGrammar.unaryProds) {
            final ChartEdge parentEdge = arrayChartCell.getBestEdge(p.leftChild);
            if ((parentEdge != null) && (parentEdge.p.isUnaryProd() == false)) {
                final float prob = p.prob + parentEdge.insideProb;
                arrayChartCell.addEdge(new ChartEdge(p, arrayChartCell, prob));
            }
        }
    }
}

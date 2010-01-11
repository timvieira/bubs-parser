package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.ArrayGrammar;
import edu.ohsu.cslu.grammar.ArrayGrammar.Production;
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
    protected void visitCell(final ArrayChartCell cell) {
        ArrayChartCell leftCell, rightCell;
        ChartEdge leftEdge, rightEdge, parentEdge;
        float prob;
        final int start = cell.start;
        final int end = cell.end;

        for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
            // naive traversal through all grammar rules
            leftCell = chart[start][mid];
            rightCell = chart[mid][end];
            for (final Production p : grammar.binaryProds) {
                leftEdge = leftCell.getBestEdge(p.leftChild);
                rightEdge = rightCell.getBestEdge(p.rightChild);
                if ((leftEdge != null) && (rightEdge != null)) {
                    prob = p.prob + leftEdge.insideProb + rightEdge.insideProb;
                    // parentCell.addEdge(new ChartEdge(p, prob, leftCell, rightCell));
                    cell.addEdge(p, prob, leftCell, rightCell);
                }
            }
        }

        for (final Production p : grammar.unaryProds) {
            parentEdge = cell.getBestEdge(p.leftChild);
            if ((parentEdge != null) && (parentEdge.p.isUnaryProd() == false)) {
                prob = p.prob + parentEdge.insideProb;
                cell.addEdge(new ChartEdge(p, cell, prob));
            }
        }
    }
}

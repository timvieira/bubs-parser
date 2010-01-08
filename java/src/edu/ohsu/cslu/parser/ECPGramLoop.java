package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.ArrayGrammar;
import edu.ohsu.cslu.grammar.ArrayGrammar.Production;
import edu.ohsu.cslu.parser.traversal.ChartTraversal.ChartTraversalType;
import edu.ohsu.cslu.parser.util.ParseTree;

public class ECPGramLoop extends ExhaustiveChartParser {

    public ECPGramLoop(ArrayGrammar grammar, ChartTraversalType traversalType) {
        super(grammar, traversalType);
    }

    @Override
    public ParseTree findMLParse(String sentence) throws Exception {
        return findParse(sentence);
    }

    protected void visitCell(ArrayChartCell cell) {
        ArrayChartCell leftCell, rightCell;
        ChartEdge leftEdge, rightEdge, parentEdge;
        float prob;
        int start = cell.start;
        int end = cell.end;

        for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
            // naive traversal through all grammar rules
            leftCell = chart[start][mid];
            rightCell = chart[mid][end];
            for (Production p : grammar.binaryProds) {
                leftEdge = leftCell.getBestEdge(p.leftChild);
                rightEdge = rightCell.getBestEdge(p.rightChild);
                if ((leftEdge != null) && (rightEdge != null)) {
                    prob = p.prob + leftEdge.insideProb + rightEdge.insideProb;
                    // parentCell.addEdge(new ChartEdge(p, prob, leftCell, rightCell));
                    cell.addEdge(p, prob, leftCell, rightCell);
                }
            }
        }

        for (Production p : grammar.unaryProds) {
            parentEdge = cell.getBestEdge(p.leftChild);
            if ((parentEdge != null) && (parentEdge.p.isUnaryProd() == false)) {
                prob = p.prob + parentEdge.insideProb;
                cell.addEdge(new ChartEdge(p, cell, prob));
            }
        }
    }
}

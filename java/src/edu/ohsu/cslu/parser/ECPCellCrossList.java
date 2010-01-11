package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.GrammarByLeftNonTermList;
import edu.ohsu.cslu.grammar.ArrayGrammar.Production;
import edu.ohsu.cslu.parser.traversal.ChartTraversal.ChartTraversalType;
import edu.ohsu.cslu.parser.util.ParseTree;

public class ECPCellCrossList extends ExhaustiveChartParser {

    private GrammarByLeftNonTermList grammarByLeftNonTermList;

    public ECPCellCrossList(final GrammarByLeftNonTermList grammar, final ChartTraversalType traversalType) {
        super(grammar, traversalType);
        grammarByLeftNonTermList = grammar;
    }

    @Override
    public ParseTree findMLParse(final String sentence) throws Exception {
        return findBestParse(sentence);
    }

    @Override
    protected void visitCell(final ArrayChartCell cell) {
        ArrayChartCell leftCell, rightCell;
        ChartEdge rightEdge, parentEdge;
        float prob;
        final int start = cell.start;
        final int end = cell.end;

        for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
            leftCell = chart[start][mid];
            rightCell = chart[mid][end];
            for (final ChartEdge leftEdge : leftCell.getBestLeftEdges()) {
                for (final Production p : grammarByLeftNonTermList.getBinaryProdsWithLeftChild(leftEdge.p.parent)) {
                    rightEdge = rightCell.getBestEdge(p.rightChild);
                    if (rightEdge != null) {
                        prob = p.prob + leftEdge.insideProb + rightEdge.insideProb;
                        cell.addEdge(p, prob, leftCell, rightCell);
                    }
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

package edu.ohsu.cslu.parser;

import java.util.List;

import edu.ohsu.cslu.grammar.GrammarByLeftNonTermHash;
import edu.ohsu.cslu.grammar.ArrayGrammar.Production;
import edu.ohsu.cslu.parser.traversal.ChartTraversal.ChartTraversalType;
import edu.ohsu.cslu.parser.util.ParseTree;

public class ECPCellCrossHash extends ExhaustiveChartParser {

    public ECPCellCrossHash(final GrammarByLeftNonTermHash grammar, final ChartTraversalType traversalType) {
        super(grammar, traversalType);
    }

    @Override
    public ParseTree findMLParse(final String sentence) throws Exception {
        return findBestParse(sentence);
    }

    @Override
    protected void visitCell(final ArrayChartCell cell) {
        ArrayChartCell leftCell, rightCell;
        ChartEdge parentEdge;
        List<Production> validProductions;
        float prob;
        final int start = cell.start;
        final int end = cell.end;

        for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
            leftCell = chart[start][mid];
            rightCell = chart[mid][end];
            for (final ChartEdge leftEdge : leftCell.getBestLeftEdges()) {
                for (final ChartEdge rightEdge : rightCell.getBestRightEdges()) {
                    validProductions = ((GrammarByLeftNonTermHash) grammar).getBinaryProdsByChildren(leftEdge.p.parent, rightEdge.p.parent);
                    if (validProductions != null) {
                        for (final Production p : validProductions) {
                            prob = p.prob + leftEdge.insideProb + rightEdge.insideProb;
                            cell.addEdge(p, prob, leftCell, rightCell);
                        }
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

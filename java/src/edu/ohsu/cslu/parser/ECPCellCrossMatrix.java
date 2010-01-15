package edu.ohsu.cslu.parser;

import java.util.LinkedList;
import java.util.List;

import edu.ohsu.cslu.grammar.ArrayGrammar;
import edu.ohsu.cslu.grammar.GrammarByChildMatrix;
import edu.ohsu.cslu.grammar.BaseGrammar.Production;
import edu.ohsu.cslu.parser.traversal.ChartTraversal.ChartTraversalType;
import edu.ohsu.cslu.parser.util.ParseTree;

public class ECPCellCrossMatrix extends ExhaustiveChartParser {

    public ECPCellCrossMatrix(final GrammarByChildMatrix grammar, final ChartTraversalType traversalType) {
        super(grammar, traversalType);
    }

    @Override
    public ParseTree findMLParse(final String sentence) throws Exception {
        return findBestParse(sentence);
    }

    @Override
    protected void visitCell(final ChartCell cell) {
        final ArrayChartCell arrayChartCell = (ArrayChartCell) cell;
        final int start = arrayChartCell.start;
        final int end = arrayChartCell.end;
        final GrammarByChildMatrix grammarByChildMatrix = (GrammarByChildMatrix) grammar;

        for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
            final ArrayChartCell leftCell = (ArrayChartCell) chart[start][mid];
            final ArrayChartCell rightCell = (ArrayChartCell) chart[mid][end];
            for (final ChartEdge leftEdge : leftCell.getBestLeftEdges()) {
                // gramByLeft = grammarByChildMatrix.binaryProdMatrix.get(leftEdge.p.parent);
                final LinkedList<Production>[] gramByLeft = grammarByChildMatrix.binaryProdMatrix2[leftEdge.p.parent];
                for (final ChartEdge rightEdge : rightCell.getBestRightEdges()) {
                    // validProductions = gramByLeft.get(rightEdge.p.parent);
                    final List<Production> validProductions = gramByLeft[rightEdge.p.parent];
                    if (validProductions != null) {
                        for (final Production p : validProductions) {
                            final float prob = p.prob + leftEdge.insideProb + rightEdge.insideProb;
                            arrayChartCell.addEdge(p, prob, leftCell, rightCell);
                        }
                    }
                }
            }
        }

        for (final Production p : ((ArrayGrammar) grammar).unaryProds) {
            final ChartEdge parentEdge = arrayChartCell.getBestEdge(p.leftChild);
            if ((parentEdge != null) && (parentEdge.p.isUnaryProd() == false)) {
                final float prob = p.prob + parentEdge.insideProb;
                arrayChartCell.addEdge(new ChartEdge(p, arrayChartCell, prob));
            }
        }
    }
}

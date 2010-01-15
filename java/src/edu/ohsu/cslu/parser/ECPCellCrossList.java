package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.ArrayGrammar;
import edu.ohsu.cslu.grammar.GrammarByLeftNonTermList;
import edu.ohsu.cslu.grammar.BaseGrammar.Production;
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
    protected void visitCell(final ChartCell cell) {
        final ArrayChartCell arrayChartCell = (ArrayChartCell) cell;
        final int start = arrayChartCell.start;
        final int end = arrayChartCell.end;

        for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
            final ArrayChartCell leftCell = (ArrayChartCell) chart[start][mid];
            final ArrayChartCell rightCell = (ArrayChartCell) chart[mid][end];
            for (final ChartEdge leftEdge : leftCell.getBestLeftEdges()) {
                for (final Production p : grammarByLeftNonTermList.getBinaryProdsWithLeftChild(leftEdge.p.parent)) {
                    final ChartEdge rightEdge = rightCell.getBestEdge(p.rightChild);
                    if (rightEdge != null) {
                        final float prob = p.prob + leftEdge.insideProb + rightEdge.insideProb;
                        arrayChartCell.addEdge(p, prob, leftCell, rightCell);
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

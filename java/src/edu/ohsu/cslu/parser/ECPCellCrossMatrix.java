package edu.ohsu.cslu.parser;

import java.util.LinkedList;
import java.util.List;

import edu.ohsu.cslu.grammar.GrammarByChildMatrix;
import edu.ohsu.cslu.grammar.ArrayGrammar.Production;
import edu.ohsu.cslu.parser.traversal.ChartTraversal.ChartTraversalType;
import edu.ohsu.cslu.parser.util.ParseTree;

public class ECPCellCrossMatrix extends ExhaustiveChartParser {

    public ECPCellCrossMatrix(GrammarByChildMatrix grammar, ChartTraversalType traversalType) {
        super(grammar, traversalType);
    }

    @Override
    public ParseTree findMLParse(String sentence) throws Exception {
        return findParse(sentence);
    }

    @Override
    protected void visitCell(ArrayChartCell cell) {
        ArrayChartCell leftCell, rightCell;
        ChartEdge parentEdge;
        List<Production> validProductions;
        LinkedList<Production>[] gramByLeft;
        float prob;
        int start = cell.start;
        int end = cell.end;
        GrammarByChildMatrix grammarByChildMatrix = (GrammarByChildMatrix) grammar;

        for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
            leftCell = chart[start][mid];
            rightCell = chart[mid][end];
            for (ChartEdge leftEdge : leftCell.getBestLeftEdges()) {
                // gramByLeft = grammarByChildMatrix.binaryProdMatrix.get(leftEdge.p.parent);
                gramByLeft = grammarByChildMatrix.binaryProdMatrix2[leftEdge.p.parent];
                for (ChartEdge rightEdge : rightCell.getBestRightEdges()) {
                    // validProductions = gramByLeft.get(rightEdge.p.parent);
                    validProductions = gramByLeft[rightEdge.p.parent];
                    if (validProductions != null) {
                        for (Production p : validProductions) {
                            prob = p.prob + leftEdge.insideProb + rightEdge.insideProb;
                            cell.addEdge(p, prob, leftCell, rightCell);
                        }
                    }
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

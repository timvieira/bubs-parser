package edu.ohsu.cslu.parser.fom;

import java.io.BufferedReader;
import java.io.BufferedWriter;

import edu.ohsu.cslu.grammar.InsideOutsideCscSparseMatrixGrammar;
import edu.ohsu.cslu.parser.ParseContext;
import edu.ohsu.cslu.parser.Parser.DecodeMethod;
import edu.ohsu.cslu.parser.Parser.ResearchParserType;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.cellselector.LeftRightBottomTopTraversal;
import edu.ohsu.cslu.parser.fom.FigureOfMerit.FOMType;
import edu.ohsu.cslu.parser.ml.InsideOutsideCphSpmlParser;
import edu.ohsu.cslu.parser.spmv.SparseMatrixVectorParser.CartesianProductFunctionType;

public class DiscriminativeFOM extends FigureOfMeritModel {

    public DiscriminativeFOM(final FOMType type) {
        super(type);
    }

    public static void train(final BufferedReader inStream, final BufferedWriter outStream, final String grammarFile)
            throws Exception {

        final ParserDriver opts = new ParserDriver();
        opts.cellSelectorModel = LeftRightBottomTopTraversal.MODEL;
        opts.decodeMethod = DecodeMethod.Goodman;

        final InsideOutsideCscSparseMatrixGrammar grammar = (InsideOutsideCscSparseMatrixGrammar) ParserDriver
                .readGrammar(grammarFile, ResearchParserType.InsideOutsideCartesianProductHash,
                        CartesianProductFunctionType.PerfectHash);
        final InsideOutsideCphSpmlParser parser = new InsideOutsideCphSpmlParser(opts, grammar);

        String line;
        while ((line = inStream.readLine()) != null) {
            final ParseContext result = parser.parseSentence(line);
            System.out.println("Result:" + result.parseBracketString(false, false) + result.statsString());

            // final CellSelector cellSelector = opts.cellSelectorModel.createCellSelector();
            parser.cellSelector.reset();
            while (parser.cellSelector.hasNext()) {
                final short[] startAndEnd = parser.cellSelector.next();
                final short start = startAndEnd[0];
                final short end = startAndEnd[1];

                // for (int nt = parser.chart.cellNonTermStartIndex(start, end); nt <
                // parser.chart.cellNonTermEndIndex(start, end); nt++) {
                for (int nt = 0; nt < grammar.numNonTerms(); nt++) {
                    // int cellIndex = parser.chart.cellIndex(start, end);
                    // int offset = parser.chart.offset(cellIndex);
                    // int numEntries = parser.chart.numNonTerminals()[cellIndex];
                    // for (int i=0; i<numEntries; i++) {
                    // int nt = parser.chart.nonTerminalIndices()[i];
                    final float normInOut = parser.getInside(start, end, nt) + parser.getOutside(start, end, nt)
                            - result.insideProbability;
                    if (normInOut > Float.NEGATIVE_INFINITY) {
                        System.out.println(String.format("[%d,%d] %s in=%f out=%f norm=%f", start, end,
                                grammar.nonTermSet.getSymbol(nt), parser.getInside(start, end, nt),
                                parser.getOutside(start, end, nt), normInOut));
                    }
                }
            }
        }
    }

}

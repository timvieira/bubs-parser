package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.GrammarByChildMatrix;
import edu.ohsu.cslu.grammar.GrammarByLeftNonTermHash;
import edu.ohsu.cslu.grammar.GrammarByLeftNonTermList;
import edu.ohsu.cslu.parser.util.Log;
import edu.ohsu.cslu.parser.util.ParseTree;
import edu.ohsu.cslu.parser.util.ParserUtil;
import edu.ohsu.cslu.parser.util.StringToMD5;

public class ParserDriver {

    public static void main(String[] argv) throws Exception {
        ParseTree bestParseTree = null;
        String sentence;
        int sentNum = 0;
        Parser parser = null;

        ParserOptions opts = new ParserOptions(argv);
        Grammar grammar;

        if (opts.parserType == ParserOptions.ParserType.ExhaustiveChartParser) {
            if (opts.chartCellVisitationType == ParserOptions.ChartCellVisitationType.CellCrossList) {
                grammar = new GrammarByLeftNonTermList(opts.pcfgFileName, opts.lexFileName);
                parser = new ECPCellCrossList((GrammarByLeftNonTermList) grammar, opts.chartTraversalType);
            } else if (opts.chartCellVisitationType == ParserOptions.ChartCellVisitationType.CellCrossHash) {
                grammar = new GrammarByLeftNonTermHash(opts.pcfgFileName, opts.lexFileName);
                parser = new ECPCellCrossHash((GrammarByLeftNonTermHash) grammar, opts.chartTraversalType);
            } else if (opts.chartCellVisitationType == ParserOptions.ChartCellVisitationType.CellCrossMatrix) {
                grammar = new GrammarByChildMatrix(opts.pcfgFileName, opts.lexFileName);
                parser = new ECPCellCrossMatrix((GrammarByChildMatrix) grammar, opts.chartTraversalType);
            } else if (opts.chartCellVisitationType == ParserOptions.ChartCellVisitationType.GrammarLoop) {
                grammar = new Grammar(opts.pcfgFileName, opts.lexFileName);
                parser = new ECPGramLoop(grammar, opts.chartTraversalType);
            } else if (opts.chartCellVisitationType == ParserOptions.ChartCellVisitationType.GrammarLoopBerkeleyFilter) {
                grammar = new Grammar(opts.pcfgFileName, opts.lexFileName);
                parser = new ECPGramLoopBerkFilter(grammar, opts.chartTraversalType);
            }
        }

        if (opts.parserType == ParserOptions.ParserType.AgendaParser) {
            grammar = new GrammarByLeftNonTermList(opts.pcfgFileName, opts.lexFileName);
            parser = new AgendaChartParser((GrammarByLeftNonTermList) grammar, opts.edgeFOMType);
        }

        if (opts.parserType == ParserOptions.ParserType.AgendaParserWithGhostEdges) {
            grammar = new GrammarByLeftNonTermList(opts.pcfgFileName, opts.lexFileName);
            parser = new AgendaChartParserGhostEdges((GrammarByLeftNonTermList) grammar, opts.edgeFOMType);
        }

        System.err.println(opts.toString("PARM: "));

        // System.out.println(parser.getBestParse("the aged bottle Other-xx flies fast").toString());
        // System.exit(1);

        sentNum = 0;
        String stats;
        while ((sentence = opts.inputStream.readLine()) != null) {
            if (parser instanceof MaximumLikelihoodParser) {
                bestParseTree = ((MaximumLikelihoodParser) parser).findMLParse(sentence.trim());
            } else if (parser instanceof HeuristicParser) {
                bestParseTree = ((HeuristicParser) parser).findGoodParse(sentence.trim());
            } else {
                Log.info(0, "ERROR: Parser does not implement necessary decoding interface.");
                System.exit(1);
            }

            stats = " sentNum=" + sentNum + " sentLen=" + ParserUtil.tokenize(sentence).length + " md5="
                    + StringToMD5.computeMD5(sentence);
            if (bestParseTree == null) {
                System.out.println("No parse found.");
                stats += " inside=-inf";
            } else {
                System.out.println(bestParseTree.toString(opts.printInsideProbs));
                // System.out.println("STAT: sentNum="+sentNum+" inside="+bestParseTree.chartEdge.insideProb);
                stats += " inside=" + bestParseTree.chartEdge.insideProb;
            }
            stats += parser.getStats();
            System.out.println("STAT:" + stats);
            sentNum++;
        }
    }
}

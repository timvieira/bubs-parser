package edu.ohsu.cslu.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.EnumAliasMap;
import org.kohsuke.args4j.Option;

import cltool.BaseCommandlineTool;
import edu.ohsu.cslu.grammar.ArrayGrammar;
import edu.ohsu.cslu.grammar.GrammarByChildMatrix;
import edu.ohsu.cslu.grammar.GrammarByLeftNonTermHash;
import edu.ohsu.cslu.grammar.GrammarByLeftNonTermList;
import edu.ohsu.cslu.parser.fom.EdgeFOM;
import edu.ohsu.cslu.parser.fom.EdgeFOM.EdgeFOMType;
import edu.ohsu.cslu.parser.traversal.ChartTraversal.ChartTraversalType;
import edu.ohsu.cslu.parser.util.Log;
import edu.ohsu.cslu.parser.util.ParseTree;
import edu.ohsu.cslu.parser.util.ParserUtil;
import edu.ohsu.cslu.parser.util.StringToMD5;

public class ParserDriver extends BaseCommandlineTool {

    @Option(name = "-g", aliases = { "--grammar-file-prefix" }, required = true, metaVar = "prefix", usage = "Grammar file prefix")
    private String pcfgPrefix;

    @Option(name = "-scores", aliases = { "--print-inside-scores" }, usage = "Print inside probabilities")
    private boolean printInsideProbs = false;

    @Option(name = "-unk", aliases = { "--print-unk-labels" }, usage = "Print unknown labels")
    private boolean printUnkLabels = false;

    @Option(name = "-p", aliases = { "--parser", "--parser-implementation" }, metaVar = "parser", usage = "Parser implementation")
    private ParserType parserType = ParserType.ExhaustiveChartParser;

    // TODO Eventually we'd like to make this a command-line option, but not all combinations are implemented
    // yet
    private ChartTraversalType chartTraversalType = ChartTraversalType.LeftRightBottomTopTraversal;

    @Option(name = "-vt", aliases = { "--visitation-type" }, metaVar = "type", usage = "Visitation type")
    private ChartCellVisitationType chartCellVisitationType = ChartCellVisitationType.GrammarLoopBerkeleyFilter;

    public EdgeFOMType edgeFOMType = EdgeFOMType.Inside;

    private ArrayGrammar grammar;

    public static void main(final String[] args) throws Exception {
        run(args);
    }

    @Override
    public void setup(final CmdLineParser cmdlineParser) throws CmdLineException {
        final String pcfgFileName = pcfgPrefix + ".pcfg";
        final String lexFileName = pcfgPrefix + ".lex";

        try {
            switch (parserType) {

            case ExhaustiveChartParser:
                switch (chartCellVisitationType) {

                case CellCrossList:
                    grammar = new GrammarByLeftNonTermList(pcfgFileName, lexFileName);
                    break;

                case CellCrossHash:
                    grammar = new GrammarByLeftNonTermHash(pcfgFileName, lexFileName);
                    break;

                case CellCrossMatrix:
                    grammar = new GrammarByChildMatrix(pcfgFileName, lexFileName);
                    break;

                case GrammarLoop:
                case GrammarLoopBerkeleyFilter:
                    grammar = new ArrayGrammar(pcfgFileName, lexFileName);
                }
                break;

            // Both agenda parsers use GrammarByLeftNonTermList
            case AgendaParser:
            case AgendaParserWithGhostEdges:
                grammar = new GrammarByLeftNonTermList(pcfgFileName, lexFileName);
                break;

            default:
                throw new CmdLineException(cmdlineParser, "Unsupported parser type: " + parserType);

            }
        } catch (final IOException e) {
            throw new CmdLineException(cmdlineParser, e);
        }
    }

    @Override
    public void run() throws Exception {
        ParseTree bestParseTree = null;
        int sentNum = 0;
        Parser parser = null;
        EdgeFOM edgeFOM;

        switch (parserType) {
        case ExhaustiveChartParser:
            switch (chartCellVisitationType) {
            case CellCrossList:
                parser = new ECPCellCrossList((GrammarByLeftNonTermList) grammar, chartTraversalType);
                break;
            case CellCrossHash:
                parser = new ECPCellCrossHash((GrammarByLeftNonTermHash) grammar, chartTraversalType);
                break;
            case CellCrossMatrix:
                parser = new ECPCellCrossMatrix((GrammarByChildMatrix) grammar, chartTraversalType);
                break;
            case GrammarLoop:
                parser = new ECPGramLoop(grammar, chartTraversalType);
                break;
            case GrammarLoopBerkeleyFilter:
                parser = new ECPGramLoopBerkFilter(grammar, chartTraversalType);
            }
            break;

        case AgendaParser:
            edgeFOM = EdgeFOM.create(edgeFOMType, grammar);
            parser = new AgendaChartParser((GrammarByLeftNonTermList) grammar, edgeFOM);
            break;

        case AgendaParserWithGhostEdges:
            edgeFOM = EdgeFOM.create(edgeFOMType, grammar);
            parser = new AgendaChartParserGhostEdges((GrammarByLeftNonTermList) grammar, edgeFOM);
            break;

        default:
            throw new IllegalArgumentException("Unsupported parser type");
        }

        final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        for (String sentence = br.readLine(); sentence != null; sentence = br.readLine()) {
            if (parser instanceof MaximumLikelihoodParser) {
                bestParseTree = ((MaximumLikelihoodParser) parser).findMLParse(sentence.trim());
            } else if (parser instanceof HeuristicParser) {
                bestParseTree = ((HeuristicParser) parser).findGoodParse(sentence.trim());
            } else {
                Log.info(0, "ERROR: Parser does not implement necessary decoding interface.");
                System.exit(1);
            }

            String stats = " sentNum=" + sentNum + " sentLen=" + ParserUtil.tokenize(sentence).length + " md5=" + StringToMD5.computeMD5(sentence);
            if (bestParseTree == null) {
                System.out.println("No parse found.");
                stats += " inside=-inf";
            } else {
                System.out.println(bestParseTree.toString(printInsideProbs));
                // System.out.println("STAT: sentNum="+sentNum+" inside="+bestParseTree.chartEdge.insideProb);
                stats += " inside=" + bestParseTree.chartEdge.insideProb;
            }
            stats += parser.getStats();
            System.out.println("STAT:" + stats);
            sentNum++;
        }
    }

    static public enum ParserType {
        ExhaustiveChartParser("exhaustive"), AgendaParser("agenda"), AgendaParserWithGhostEdges("age"), SuperAgendaParser("sap");

        private ParserType(final String... aliases) {
            EnumAliasMap.singleton().addAliases(this, aliases);
        }
    }

    static public enum ChartCellVisitationType {
        CellCrossList("ccl"), CellCrossHash("cch"), CellCrossMatrix("ccm"), GrammarLoop("gl"), GrammarLoopBerkeleyFilter("glbf");

        private ChartCellVisitationType(final String... aliases) {
            EnumAliasMap.singleton().addAliases(this, aliases);
        }
    }

}

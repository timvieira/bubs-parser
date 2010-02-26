package edu.ohsu.cslu.parser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.EnumAliasMap;
import org.kohsuke.args4j.Option;

import cltool.BaseCommandlineTool;
import edu.ohsu.cslu.grammar.ChildMatrixGrammar;
import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.JsaSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.LeftListGrammar;
import edu.ohsu.cslu.grammar.LeftRightListsGrammar;
import edu.ohsu.cslu.parser.cellselector.CSLUTBlockedCells;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.parser.cellselector.PerceptronCellSelector;
import edu.ohsu.cslu.parser.cellselector.CellSelector.CellSelectorType;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector.EdgeSelectorType;
import edu.ohsu.cslu.parser.util.Log;

public class ParserDriver extends BaseCommandlineTool {

    @Option(name = "-g", aliases = { "--grammar-file-prefix" }, required = true, metaVar = "prefix", usage = "Grammar file prefix")
    private String pcfgFileName;

    @Option(name = "-lex", aliases = { "--lexicon-file" }, metaVar = "FILE", usage = "Lexicon file if full grammar file is specified for -g option")
    private String lexFileName = null;

    @Option(name = "-gf", aliases = { "--grammar-format" }, metaVar = "format", usage = "Format of grammar file")
    private GrammarFormatType grammarFormat = GrammarFormatType.CSLU;

    @Option(name = "-scores", aliases = { "--print-inside-scores" }, usage = "Print inside probabilities")
    private boolean printInsideProbs = false;

    @Option(name = "-unk", aliases = { "--print-unk-labels" }, usage = "Print unknown labels")
    private boolean printUnkLabels = false;

    @Option(name = "-p", aliases = { "--parser", "--parser-implementation" }, metaVar = "parser", usage = "Parser implementation")
    private ParserType parserType = ParserType.ECPCellCrossList;

    // @Option(name = "-cp", aliases = { "--cell-processing-type" }, metaVar = "type", usage = "Chart cell processing type")
    // private ChartCellProcessingType chartCellProcessingType = ChartCellProcessingType.CellCrossList;

    @Option(name = "-max", aliases = { "--max-length" }, metaVar = "len", usage = "Skip sentences longer than LEN")
    private int maxLength = 200;

    @Option(name = "-fom", aliases = { "--figure-of-merit", "-FOM" }, metaVar = "fom", usage = "Figure of Merit")
    private EdgeSelectorType edgeFOMType = EdgeSelectorType.Inside;

    @Option(name = "-fomTrain", usage = "Train the specified FOM model")
    private boolean fomTrain = false;

    @Option(name = "-fomModel", metaVar = "file", usage = "FOM model file")
    private String fomModelFileName = null;
    private BufferedReader fomModelStream = null;

    @Option(name = "-cellTrain", usage = "Train the specified Cell Selection model")
    private boolean cellTrain = false;

    @Option(name = "-cellSelect", metaVar = "TYPE", usage = "Method for cell selection")
    private CellSelectorType cellSelectorType = CellSelectorType.LeftRightBottomTop;

    @Option(name = "-cellModel", metaVar = "file", usage = "Model for span selection")
    private String cellModelFileName = null;
    public BufferedReader cellModelStream = null;

    @Option(name = "-cslutCellScores", metaVar = "file", usage = "CSLUT cell scores used for perceptron training and decoding")
    private String cslutScoresFileName = null;
    private BufferedReader cslutScoresStream = null;

    @Option(name = "-x1", usage = "Tuning param #1")
    public static float param1 = -1;

    @Option(name = "-x2", usage = "Tuning param #2")
    public static float param2 = -1;

    private BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(System.out));
    private BufferedReader inputStream = new BufferedReader(new InputStreamReader(System.in));

    public static void main(final String[] args) throws Exception {
        run(args);
    }

    @Override
    public void setup(final CmdLineParser cmdlineParser) throws Exception {
        // setup() is run once for multiple threads

        // TODO: this is hacky. Should have different params: -grammar XXX | -pcfg XXX -lex YYY
        if (lexFileName == null) {
            final String grammarFileNamePrefix = pcfgFileName;
            pcfgFileName = grammarFileNamePrefix + ".pcfg";
            lexFileName = grammarFileNamePrefix + ".lex";
        }

        if (fomModelFileName != null) {
            fomModelStream = new BufferedReader(new FileReader(fomModelFileName));
        }

        if (cellModelFileName != null) {
            cellModelStream = new BufferedReader(new FileReader(cellModelFileName));
        }

        if (cslutScoresFileName != null) {
            cslutScoresStream = new BufferedReader(new FileReader(cslutScoresFileName));
        }

        // param validation checks
        if (edgeFOMType == EdgeSelectorType.BoundaryInOut && fomTrain == false && fomModelFileName == null) {
            throw new CmdLineException(cmdlineParser, "BoundaryInOut FOM must also have -fomTrain or -fomModel param set");
        }

        if (cellSelectorType == CellSelectorType.CSLUT && cellModelStream == null) {
            throw new CmdLineException(cmdlineParser, "CSLUT span selection must also have -spanModel");
        }

        if (cellSelectorType == CellSelectorType.Perceptron && cslutScoresStream == null) {
            throw new CmdLineException(cmdlineParser, "Perceptron span selection must specify -cslutSpanScores");
        }
    }

    public String optionsToString() {
        final String prefix = "OPTS: ";
        String s = "";
        s += prefix + "ParserType=" + parserType + "\n";
        s += prefix + "CellSelector=" + cellSelectorType + "\n";
        // s += prefix + "CellProcess=" + chartCellProcessingType + "\n";
        s += prefix + "FOM=" + edgeFOMType + "\n";
        s += prefix + "x1=" + param1 + "\n";
        s += prefix + "x2=" + param2;
        // Log.info(0, s);

        return s;
    }

    public static Grammar createGrammar(final ParserType parserType, final GrammarFormatType grammarFormat, final String pcfgFileName, final String lexFileName) throws Exception {
        switch (parserType) {
        case ECPCellCrossList:
            return new LeftListGrammar(pcfgFileName, lexFileName, grammarFormat);
        case ECPCellCrossHash:
            return new LeftHashGrammar(pcfgFileName, lexFileName, grammarFormat);
        case ECPCellCrossMatrix:
            return new ChildMatrixGrammar(pcfgFileName, lexFileName, grammarFormat);
        case ECPGrammarLoop:
        case ECPGrammarLoopBerkeleyFilter:
            return new Grammar(pcfgFileName, lexFileName, grammarFormat);

        case AgendaChartParser:
        case ACPWithMemory:
        case ACPGhostEdges:
            return new LeftRightListsGrammar(pcfgFileName, lexFileName, grammarFormat);

        case LocalBestFirst:
        case LBFPruneViterbi:
        case LBFOnlineBeam:
        case LBFSmallAgenda:
        case LBFExpDecay:
        case LBFPerceptronCell:
        case CoarseCellAgenda:
        case CoarseCellAgendaCSLUT:
            return new LeftHashGrammar(pcfgFileName, lexFileName, grammarFormat);

        case JsaSparseMatrixVector:
            return new JsaSparseMatrixGrammar(pcfgFileName, lexFileName, grammarFormat);
        case CsrSparseMatrixVector:
        case OpenClSparseMatrixVector:
            return new CsrSparseMatrixGrammar(pcfgFileName, lexFileName, grammarFormat);

        default:
            throw new Exception("Unsupported parser type: " + parserType);
        }
    }

    public static Parser createParser(final ParserType parserType, final Grammar grammar, final EdgeSelector edgeSelector, final CellSelector cellSelector) throws Exception {

        switch (parserType) {
        case ECPCellCrossList:
            return new ECPCellCrossList((LeftListGrammar) grammar, cellSelector);
        case ECPCellCrossHash:
            return new ECPCellCrossHash((LeftHashGrammar) grammar, cellSelector);
        case ECPCellCrossMatrix:
            return new ECPCellCrossMatrix((ChildMatrixGrammar) grammar, cellSelector);
        case ECPGrammarLoop:
            return new ECPGrammarLoop(grammar, cellSelector);
        case ECPGrammarLoopBerkeleyFilter:
            return new ECPGrammarLoopBerkFilter(grammar, cellSelector);

        case AgendaChartParser:
            return new AgendaChartParser(grammar, edgeSelector);
        case ACPWithMemory:
            return new ACPWithMemory(grammar, edgeSelector);
        case ACPGhostEdges:
            return new ACPGhostEdges(grammar, edgeSelector);

        case LocalBestFirst:
            return new LocalBestFirstChartParser(grammar, edgeSelector, cellSelector);
        case LBFPruneViterbi:
            return new LBFPruneViterbi(grammar, edgeSelector, cellSelector);
        case LBFOnlineBeam:
            return new LBFWeakThresh(grammar, edgeSelector, cellSelector);
        case LBFSmallAgenda:
            return new LBFSmallAgenda(grammar, edgeSelector, cellSelector);
        case LBFExpDecay:
            return new LBFExpDecay(grammar, edgeSelector, cellSelector);
        case LBFPerceptronCell:
            return new LBFSkipBaseCells(grammar, edgeSelector, cellSelector);

        case CoarseCellAgenda:
            return new CoarseCellAgendaParser(grammar, edgeSelector);
        case CoarseCellAgendaCSLUT:
            return new CoarseCellAgendaParserWithCSLUT(grammar, edgeSelector, (CSLUTBlockedCells) cellSelector);

        case JsaSparseMatrixVector:
            return new JsaSparseMatrixVectorParser((JsaSparseMatrixGrammar) grammar, cellSelector);
        case CsrSparseMatrixVector:
            return new CsrSparseMatrixVectorParser((CsrSparseMatrixGrammar) grammar, cellSelector);
        case OpenClSparseMatrixVector:
            return new OpenClSparseMatrixVectorParser((CsrSparseMatrixGrammar) grammar, cellSelector);

        default:
            throw new IllegalArgumentException("Unsupported parser type");
        }
    }

    @Override
    public void run() throws Exception {

        Log.info(0, optionsToString());
        final Grammar grammar = createGrammar(parserType, grammarFormat, pcfgFileName, lexFileName);

        // TODO: this whole FOM setup is pretty ugly. It needs to be changed
        // TODO: the program should know which FOM to use given the model file
        final EdgeSelector edgeSelector = EdgeSelector.create(edgeFOMType, fomModelStream, grammar);
        final CellSelector cellSelector = CellSelector.create(cellSelectorType, cellModelStream, cslutScoresStream);

        if (fomTrain == true) {
            edgeSelector.train(inputStream);
            edgeSelector.writeModel(outputStream);
        } else if (cellTrain == true) {
            // TODO: need to follow a similar train/writeModel method like edgeSelector
            final PerceptronCellSelector perceptronCellSelector = (PerceptronCellSelector) cellSelector;
            final LBFPerceptronCellTrainer parser = new LBFPerceptronCellTrainer(grammar, edgeSelector, perceptronCellSelector);
            perceptronCellSelector.train(inputStream, parser);
        } else {
            // run parser
            final Parser parser = createParser(parserType, grammar, edgeSelector, cellSelector);
            parser.parseStream(inputStream, outputStream, maxLength, printUnkLabels, printInsideProbs);
        }
    }

    static public enum ParserType {
        ECPCellCrossList("ecpccl"), ECPCellCrossHash("ecpcch"), ECPCellCrossMatrix("ecpccm"), ECPGrammarLoop("ecpgl"), ECPGrammarLoopBerkeleyFilter("ecpglbf"), AgendaChartParser(
                "acpall"), ACPWithMemory("acpwm"), ACPGhostEdges("acpge"), LocalBestFirst("lbf"), LBFPruneViterbi("lbfpv"), LBFOnlineBeam("lbfob"), LBFSmallAgenda("lbfsa"), LBFExpDecay(
                "lbfed"), LBFPerceptronCell("lbfpc"), CoarseCellAgenda("cc"), CoarseCellAgendaCSLUT("cccslut"), JsaSparseMatrixVector("jsa"), OpenClSparseMatrixVector("opencl"), CsrSparseMatrixVector(
                "csr");

        private ParserType(final String... aliases) {
            EnumAliasMap.singleton().addAliases(this, aliases);
        }
    }

    static public enum GrammarFormatType {
        CSLU, Roark, Berkeley;
    }
}

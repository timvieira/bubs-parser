package edu.ohsu.cslu.parser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import cltool.BaseCommandlineTool;
import edu.ohsu.cslu.grammar.ChildMatrixGrammar;
import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.GrammarByChild;
import edu.ohsu.cslu.grammar.JsaSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.LeftListGrammar;
import edu.ohsu.cslu.grammar.LeftRightListsGrammar;
import edu.ohsu.cslu.parser.ParserOptions.GrammarFormatType;
import edu.ohsu.cslu.parser.ParserOptions.ParserType;
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

    @Option(name = "-inOutSum", usage = "Use sum instead of max for inside and outside calculations")
    public boolean inOutSum = false;

    @Option(name = "-x1", usage = "Tuning param #1")
    public float param1 = -1;

    @Option(name = "-x2", usage = "Tuning param #2")
    public float param2 = -1;

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

    public ParserOptions createOptions() {
        final ParserOptions opts = new ParserOptions();

        opts.parserType = parserType;
        opts.edgeFOMType = edgeFOMType;
        opts.cellSelectorType = cellSelectorType;

        opts.fomTrain = fomTrain;
        opts.fomModelFileName = fomModelFileName;
        opts.fomModelStream = fomModelStream;

        opts.cellTrain = cellTrain;
        opts.cellModelFileName = cellModelFileName;
        opts.cellModelStream = cellModelStream;

        opts.cslutScoresFileName = cslutScoresFileName;
        opts.cslutScoresStream = cslutScoresStream;

        opts.pcfgFileName = pcfgFileName;
        opts.lexFileName = lexFileName;
        opts.grammarFormat = grammarFormat;

        opts.maxLength = maxLength;
        opts.printInsideProbs = printInsideProbs;
        opts.printUnkLabels = printUnkLabels;
        opts.viterbiMax = !inOutSum;
        ParserOptions.param1 = param1;
        ParserOptions.param2 = param2;

        opts.outputStream = outputStream;
        opts.inputStream = inputStream;

        return opts;
    }

    public static Grammar createGrammar(final ParserType parserType, final GrammarFormatType grammarFormat, final String pcfgFileName, final String lexFileName) throws Exception {
        switch (parserType) {
            case ECPInsideOutside:
            case ECPCellCrossList:
                return new LeftListGrammar(pcfgFileName, lexFileName, grammarFormat);
            case ECPCellCrossHash:
                return new LeftHashGrammar(pcfgFileName, lexFileName, grammarFormat);
            case ECPCellCrossMatrix:
                return new ChildMatrixGrammar(pcfgFileName, lexFileName, grammarFormat);
            case ECPGrammarLoop:
            case ECPGrammarLoopBerkeleyFilter:
                return new GrammarByChild(pcfgFileName, lexFileName, grammarFormat);

            case AgendaChartParser:
            case ACPWithMemory:
            case ACPGhostEdges:
                return new LeftRightListsGrammar(pcfgFileName, lexFileName, grammarFormat);

            case LocalBestFirst:
            case LBFPruneViterbi:
            case LBFOnlineBeam:
            case LBFBoundedHeap:
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

    // public static Parser createParser(final ParserType parserType, final Grammar grammar, final EdgeSelector edgeSelector, final CellSelector cellSelector) throws Exception {
    public static Parser<?> createParser(final ParserOptions opts, final Grammar grammar) throws Exception {

        switch (opts.parserType) {
            case ECPCellCrossList:
                return new ECPCellCrossList(opts, (LeftListGrammar) grammar);
            case ECPCellCrossHash:
                return new ECPCellCrossHash(opts, (LeftHashGrammar) grammar);
            case ECPCellCrossMatrix:
                return new ECPCellCrossMatrix(opts, (ChildMatrixGrammar) grammar);
            case ECPGrammarLoop:
                return new ECPGrammarLoop(opts, (GrammarByChild) grammar);
            case ECPGrammarLoopBerkeleyFilter:
                return new ECPGrammarLoopBerkFilter(opts, (GrammarByChild) grammar);
            case ECPInsideOutside:
                return new ECPInsideOutside(opts, (LeftListGrammar) grammar);

            case AgendaChartParser:
                return new AgendaChartParser(opts, (LeftRightListsGrammar) grammar);
            case ACPWithMemory:
                return new ACPWithMemory(opts, (LeftRightListsGrammar) grammar);
            case ACPGhostEdges:
                return new ACPGhostEdges(opts, (LeftRightListsGrammar) grammar);

            case LocalBestFirst:
                return new LocalBestFirstChartParser(opts, (LeftHashGrammar) grammar);
            case LBFPruneViterbi:
                return new LBFPruneViterbi(opts, (LeftHashGrammar) grammar);
            case LBFOnlineBeam:
                return new LBFWeakThresh(opts, (LeftHashGrammar) grammar);
            case LBFBoundedHeap:
                return new LBFBoundedHeap(opts, (LeftHashGrammar) grammar);
            case LBFExpDecay:
                return new LBFExpDecay(opts, (LeftHashGrammar) grammar);
            case LBFPerceptronCell:
                return new LBFSkipBaseCells(opts, (LeftHashGrammar) grammar);

            case CoarseCellAgenda:
                return new CoarseCellAgendaParser(opts, (LeftHashGrammar) grammar);
            case CoarseCellAgendaCSLUT:
                final CSLUTBlockedCells cslutScores = (CSLUTBlockedCells) CellSelector.create(opts.cellSelectorType, opts.cellModelStream, opts.cslutScoresStream);
                return new CoarseCellAgendaParserWithCSLUT(opts, (LeftHashGrammar) grammar, cslutScores);

            case JsaSparseMatrixVector:
                return new JsaSparseMatrixVectorParser((JsaSparseMatrixGrammar) grammar);
            case CsrSparseMatrixVector:
                return new CsrSparseMatrixVectorParser((CsrSparseMatrixGrammar) grammar);
            case OpenClSparseMatrixVector:
                return new OpenClSparseMatrixVectorParser((CsrSparseMatrixGrammar) grammar);

            default:
                throw new IllegalArgumentException("Unsupported parser type");
        }
    }

    @Override
    public void run() throws Exception {
        runParser(createOptions());
    }

    public static void runParser(final ParserOptions opts) throws Exception {
        Log.info(0, opts.toString());
        final Grammar grammar = createGrammar(opts.parserType, opts.grammarFormat, opts.pcfgFileName, opts.lexFileName);

        // TODO: this whole FOM setup is pretty ugly. It needs to be changed
        // TODO: the program should know which FOM to use given the model file
        // final EdgeSelector edgeSelector = EdgeSelector.create(opts.edgeFOMType, opts.fomModelStream, grammar);
        // final CellSelector cellSelector = CellSelector.create(opts.cellSelectorType, opts.cellModelStream, opts.cslutScoresStream);

        if (opts.fomTrain == true) {
            final EdgeSelector edgeSelector = EdgeSelector.create(opts.edgeFOMType, opts.fomModelStream, grammar);
            edgeSelector.train(opts.inputStream);
            edgeSelector.writeModel(opts.outputStream);
        } else if (opts.cellTrain == true) {
            // TODO: need to follow a similar train/writeModel method like edgeSelector
            final PerceptronCellSelector perceptronCellSelector = (PerceptronCellSelector) CellSelector.create(opts.cellSelectorType, opts.cellModelStream, opts.cslutScoresStream);
            final LBFPerceptronCellTrainer parser = new LBFPerceptronCellTrainer(opts, (LeftHashGrammar) grammar);
            perceptronCellSelector.train(opts.inputStream, parser);
        } else {
            // run parser
            final Parser<?> parser = createParser(opts, grammar);
            // parser.parseStream(opts.inputStream, opts.outputStream, opts.maxLength, printUnkLabels, printInsideProbs);
            parser.parseStream(opts.inputStream, opts.outputStream);
        }
    }
}

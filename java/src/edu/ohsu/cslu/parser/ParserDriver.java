package edu.ohsu.cslu.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;

import cltool4j.ConfigProperties;
import cltool4j.GlobalConfigProperties;
import cltool4j.GlobalLogger;
import cltool4j.ThreadLocalLinewiseClTool;
import cltool4j.Threadable;
import cltool4j.args4j.CmdLineException;
import cltool4j.args4j.CmdLineParser;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.grammar.ChildMatrixGrammar;
import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.LeftListGrammar;
import edu.ohsu.cslu.grammar.LeftRightListsGrammar;
import edu.ohsu.cslu.grammar.RightCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.LeftShiftFunction;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashFilterFunction;
import edu.ohsu.cslu.parser.Parser.ParserType;
import edu.ohsu.cslu.parser.Parser.ResearchParserType;
import edu.ohsu.cslu.parser.agenda.APDecodeFOM;
import edu.ohsu.cslu.parser.agenda.APGhostEdges;
import edu.ohsu.cslu.parser.agenda.APWithMemory;
import edu.ohsu.cslu.parser.agenda.AgendaParser;
import edu.ohsu.cslu.parser.agenda.CoarseCellAgendaParser;
import edu.ohsu.cslu.parser.beam.BSCPBeamConfTrain;
import edu.ohsu.cslu.parser.beam.BSCPBoundedHeap;
import edu.ohsu.cslu.parser.beam.BSCPExpDecay;
import edu.ohsu.cslu.parser.beam.BSCPFomDecode;
import edu.ohsu.cslu.parser.beam.BSCPPruneViterbi;
import edu.ohsu.cslu.parser.beam.BSCPSkipBaseCells;
import edu.ohsu.cslu.parser.beam.BSCPWeakThresh;
import edu.ohsu.cslu.parser.beam.BeamSearchChartParser;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.parser.cellselector.LeftRightBottomTopTraversal;
import edu.ohsu.cslu.parser.cellselector.OHSUCellConstraints;
import edu.ohsu.cslu.parser.cellselector.PerceptronBeamWidth;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector.EdgeSelectorType;
import edu.ohsu.cslu.parser.ml.CartesianProductBinarySearchLeftChildSpmlParser;
import edu.ohsu.cslu.parser.ml.CartesianProductBinarySearchSpmlParser;
import edu.ohsu.cslu.parser.ml.CartesianProductHashSpmlParser;
import edu.ohsu.cslu.parser.ml.CartesianProductLeftChildHashSpmlParser;
import edu.ohsu.cslu.parser.ml.GrammarLoopSpmlParser;
import edu.ohsu.cslu.parser.ml.LeftChildLoopSpmlParser;
import edu.ohsu.cslu.parser.ml.RightChildLoopSpmlParser;
import edu.ohsu.cslu.parser.spmv.BeamCscSpmvParser;
import edu.ohsu.cslu.parser.spmv.CscSpmvParser;
import edu.ohsu.cslu.parser.spmv.CsrSpmvParser;
import edu.ohsu.cslu.parser.spmv.CsrSpmvPerMidpointParser;
import edu.ohsu.cslu.parser.spmv.DenseVectorOpenClSpmvParser;
import edu.ohsu.cslu.parser.spmv.PackedOpenClSpmvParser;
import edu.ohsu.cslu.parser.spmv.SparseMatrixVectorParser.CartesianProductFunctionType;

/**
 * Driver class for all parser implementations.
 * 
 * @author Nathan Bodenstab
 * @author Aaron Dunlop
 * @since 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
@Threadable(defaultThreads = 1)
public class ParserDriver extends ThreadLocalLinewiseClTool<Parser<?>> {

    // == Parser options ==
    @Option(name = "-p", aliases = { "--parser" }, metaVar = "parser", usage = "Parser implementation")
    private ParserType parserType = ParserType.CKY;

    @Option(name = "-rp", aliases = { "--research-parser" }, hidden = true, metaVar = "parser", usage = "Research Parser implementation")
    private ResearchParserType researchParserType = null;

    @Option(name = "-real", usage = "Use real semiring (sum) instead of tropical (max) for inside/outside calculations")
    public boolean realSemiring = false;

    // @Option(name = "-cpf", hidden = true, aliases = { "--cartesian-product-function" }, metaVar = "function", usage =
    // "Cartesian-product function (only used for SpMV parsers)")
    @Option(name = "-cpf", hidden = true, metaVar = "function", usage = "Cartesian-product function (only used for SpMV parsers)")
    private CartesianProductFunctionType cartesianProductFunctionType = CartesianProductFunctionType.PerfectHash2;

    // @Option(name = "-cp", aliases = { "--cell-processing-type" }, metaVar = "type", usage =
    // "Chart cell processing type")
    // private ChartCellProcessingType chartCellProcessingType = ChartCellProcessingType.CellCrossList;

    @Option(name = "-fom", metaVar = "fom", hidden = true, usage = "Figure of Merit to use for parser")
    public EdgeSelectorType edgeFOMType = EdgeSelectorType.Inside;
    public EdgeSelector edgeSelector;
    public CellSelector cellSelector = new LeftRightBottomTopTraversal();

    @Option(name = "-fomModel", metaVar = "file", hidden = true, usage = "FOM model file")
    private String fomModelFileName = null;
    public BufferedReader fomModelStream = null;

    @Option(name = "-beamConfModel", usage = "required Beam Confidence Model for beamconf Parser")
    private String beamConfModelFileName = null;
    // private AveragedPerceptron beamConfModel = null;

    @Option(name = "-beamConfBias", usage = "comma seperated bias for each bin in model; default is no bias")
    public String beamConfBias = null;

    @Option(name = "-beamMultiBin", hidden = true, usage = "Use old multi-bin classification instead of multiple binary classifiers")
    public static boolean multiBin = false;

    @Option(name = "-reparse", metaVar = "N", hidden = true, usage = "If no solution, loosen constraints and reparse N times")
    public int reparse = 0;

    // Nate: I don't think we need to expose this to the user. Instead
    // there should be different possible parsers since changing the
    // cell selection strategy only matters for a few of them
    // @Option(name = "-cellSelect", hidden = true, metaVar = "TYPE", usage = "Method for cell selection")
    // public CellSelectorType cellSelectorType = CellSelectorType.LeftRightBottomTop;
    // public CellSelector cellSelector;
    //
    // @Option(name = "-cellModel", metaVar = "file", hidden = true, usage = "Model for span selection")
    // private String cellModelFileName = null;
    // public BufferedReader cellModelStream = null;

    @Option(name = "-beamTune", usage = "Tuning params for beam search: maxBeamWidth,globalScoreDelta,localScoreDelta,factoredCellBeamWidth")
    public String beamTune = "15,INF,7,15";

    @Option(name = "-ccModel", metaVar = "file", usage = "CSLU Chart Constraints model (Roark and Hollingshead, 2009)")
    private String chartConstraintsModel = null;

    @Option(name = "-ccTune", metaVar = "val", usage = "CSLU Chart Constraints for Absolute (A), High Precision (P), or Linear (N): A,start,end,unary | P,pct | N,int")
    public String chartConstraintsThresh = "A,120,120,inf";

    // (1) absolute thresh A,start,end,unary
    // (2) high precision P,pct (pct cells closed score > 0)
    // (3) linear complexity N,int (x*N max open)

    @Option(name = "-ccPrint", hidden = true, usage = "Print Cell Constraints for each input sentence and exit (no parsing done)")
    public static boolean chartConstraintsPrint = false;

    // == Grammar options ==
    @Option(name = "-g", metaVar = "grammar", usage = "Grammar file (text, gzipped text, or binary serialized")
    private String grammarFile = null;

    @Option(name = "-m", metaVar = "file", usage = "Model file (binary serialized")
    private File modelFile = null;

    // == Output options ==
    @Option(name = "-max", aliases = { "--max-length" }, metaVar = "len", usage = "Skip sentences longer than LEN")
    int maxLength = 200;

    // TODO: option doesn't work anymore
    // @Option(name = "-scores", usage = "Print inside scores for each non-term in result tree")
    // boolean printInsideProbs = false;

    @Option(name = "-unk", usage = "Print unknown words as their UNK replacement class")
    boolean printUnkLabels = false;

    @Option(name = "-binary", usage = "Leave parse tree output in binary-branching form")
    public boolean binaryTreeOutput = false;

    // == Other options ==
    // TODO These shouldn't really be static. Parser implementations should use the ParserDriver instance passed in
    @Option(name = "-x1", hidden = true, usage = "Tuning param #1")
    public static float param1 = -1;

    @Option(name = "-x2", hidden = true, usage = "Tuning param #2")
    public static float param2 = -1;

    @Option(name = "-x3", hidden = true, usage = "Tuning param #3")
    public static float param3 = -1;

    @Option(name = "-feats", hidden = true, usage = "Feature template string: lt rt lt_lt-1 rw_rt loc ...")
    public static String featTemplate;

    // TODO: embed this info into the grammar file as meta data and remove these options
    @Option(name = "-hMarkov", hidden = true, usage = "Horizontal Markov order of input Grammar")
    private int horizontalMarkov = 0;

    @Option(name = "-vMarkov", hidden = true, usage = "Vertical Markov order of input Grammar")
    private int verticalMarkov = 0;

    @Option(name = "-annotatePOS", hidden = true, usage = "Input Grammar has annotation on POS tags")
    private boolean annotatePOS = false;

    @Option(name = "-oldunk", hidden = true, usage = "Use old method of UNK replacement to match old grammars")
    public static boolean oldUNK = false;

    private Grammar grammar;
    private long parseStartTime;
    public boolean collectDetailedStatistics = false;

    public static void main(final String[] args) throws Exception {
        run(args);
    }

    @Override
    // run once at initialization despite number of threads
    public void setup(final CmdLineParser cmdlineParser) throws Exception {

        // Collect detailed statistics for high verbosity levels (e.g., NTs per cell, cartesian-product size,etc.)
        collectDetailedStatistics = GlobalLogger.singleton().isLoggable(Level.FINER);

        // map simplified parser choices to the specific research version
        if (researchParserType == null) {
            switch (parserType) {
            case CKY:
                researchParserType = ResearchParserType.ECPCellCrossList;
                break;
            case Agenda:
                researchParserType = ResearchParserType.APWithMemory;
                break;
            case Beam:
                researchParserType = ResearchParserType.BeamSearchChartParser;
                // researchParserType = ResearchParserType.BSCPPruneViterbi;
                // Using the above beam parser until all the model stuff has been finished
                // researchParserType = ResearchParserType.BeamCscSpmv;
                break;
            case Matrix:
                researchParserType = ResearchParserType.CscSpmv;
                cartesianProductFunctionType = CartesianProductFunctionType.PerfectHash2;
                break;
            default:
                throw new IllegalArgumentException("Unsupported parser type");
            }
        }

        if (researchParserType == ResearchParserType.ECPInsideOutside) {
            this.realSemiring = true;
        }

        if (modelFile != null) {
            final InputStream is = modelFile.getName().endsWith(".gz") ? new GZIPInputStream(new FileInputStream(
                    modelFile)) : new FileInputStream(modelFile);
            final ObjectInputStream ois = new ObjectInputStream(is);
            final String metadata = (String) ois.readObject();
            final ConfigProperties props = (ConfigProperties) ois.readObject();
            GlobalConfigProperties.singleton().mergeUnder(props);

            GlobalLogger.singleton().fine("Reading grammar...");
            this.grammar = (Grammar) ois.readObject();

            GlobalLogger.singleton().fine("Reading FOM...");
            this.edgeSelector = (EdgeSelector) ois.readObject();

        } else {

            // CellSelectorType cellSelectorType = CellSelectorType.LeftRightBottomTop;
            // final BufferedReader chartConstraintsModelStream = null;

            if (fomModelFileName != null) {
                // Handle gzipped and non-gzipped model files
                fomModelStream = fomModelFileName.endsWith(".gz") ? new BufferedReader(new InputStreamReader(
                        new GZIPInputStream(new FileInputStream(fomModelFileName)))) : new BufferedReader(
                        new FileReader(fomModelFileName));

                // NOTE: EdgeSelectorType.InsideWithFwdBkwd also uses this fomModelStream for right now
                if (edgeFOMType == EdgeSelectorType.Inside) {
                    edgeFOMType = EdgeSelectorType.BoundaryInOut;
                }
            }

            if (researchParserType == ResearchParserType.BSCPBeamConfTrain && featTemplate == null) {
                throw new CmdLineException(cmdlineParser,
                        "ERROR: BSCPTrainFOMConfidence requires -feats to be non-empty");
            }

            // param validation checks
            if ((edgeFOMType == EdgeSelectorType.BoundaryInOut || edgeFOMType == EdgeSelectorType.InsideWithFwdBkwd)
                    && fomModelFileName == null) {
                throw new CmdLineException(cmdlineParser, "BoundaryInOut FOM must also have -fomModel param set");
            }

            grammar = readGrammar(grammarFile, researchParserType, cartesianProductFunctionType);

            if (chartConstraintsModel != null) {
                // cellSelectorType = CellSelectorType.CSLUT;
                // chartConstraintsModelStream = new BufferedReader(new FileReader(chartConstraintsModel));
                // cellSelector = new CSLUTCellConstraints(new BufferedReader(new FileReader(chartConstraintsModel)),
                // chartConstraintsThresh);
                cellSelector = new OHSUCellConstraints(new BufferedReader(new FileReader(chartConstraintsModel)),
                        chartConstraintsThresh, grammar.isLeftFactored());
            }

            if (beamConfModelFileName != null) {
                cellSelector = new PerceptronBeamWidth(new BufferedReader(new FileReader(beamConfModelFileName)),
                        beamConfBias);
                // beamConfModel = new AveragedPerceptron(new BufferedReader(new FileReader(beamConfModelFileName)));
                // if (beamConfBias != null) {
                // beamConfModel.setBias(beamConfBias);
                // }
            }

            // Nate: perceptron span selection doesn't work any more
            // if (cellSelectorType == CellSelectorType.Perceptron && cslutScoresStream == null) {
            // throw new CmdLineException(cmdlineParser, "Perceptron span selection must specify -cslutSpanScores");
            // }

            // if (researchParserType == ResearchParserType.BSCPBeamConf && beamConfModel == null) {
            // throw new CmdLineException(cmdlineParser,
            // "BSCPBeamConf parser (-rp beamconf) must specify -beamConfModel");
            // }

            edgeSelector = EdgeSelector.create(edgeFOMType, grammar, fomModelStream);
        }

        GlobalLogger.singleton().fine(grammar.getStats());
        GlobalLogger.singleton().fine(optionsToString());

        // TODO: until we embed this info into the model itself ... read it from args
        grammar.annotatePOS = annotatePOS;
        grammar.isLatentVariableGrammar = true;
        grammar.horizontalMarkov = horizontalMarkov;
        grammar.verticalMarkov = verticalMarkov;

        parseStartTime = System.currentTimeMillis();
    }

    public static Grammar readGrammar(final String grammarFile, final ResearchParserType researchParserType,
            final CartesianProductFunctionType cartesianProductFunctionType) throws Exception {

        // Handle gzipped and non-gzipped grammar files
        final InputStream grammarInputStream = grammarFile.endsWith(".gz") ? new GZIPInputStream(new FileInputStream(
                grammarFile)) : new FileInputStream(grammarFile);

        // Read the generic grammar in either text or binary-serialized format.
        final Grammar genericGrammar = Grammar.read(grammarInputStream);

        // Construct the requested grammar type from the genric grammar
        return createGrammar(genericGrammar, researchParserType, cartesianProductFunctionType);
    }

    /**
     * Creates a specific {@link Grammar} subclass, based on the generic instance passed in.
     * 
     * @param genericGrammar
     * @param researchParserType
     * @return a Grammar instance
     * @throws Exception
     */
    public static Grammar createGrammar(final Grammar genericGrammar, final ResearchParserType researchParserType,
            final CartesianProductFunctionType cartesianProductFunctionType) throws Exception {

        switch (researchParserType) {
        case ECPInsideOutside:
        case ECPCellCrossList:
            return new LeftListGrammar(genericGrammar);

        case ECPCellCrossHash:
            return new LeftHashGrammar(genericGrammar);

        case ECPCellCrossMatrix:
            return new ChildMatrixGrammar(genericGrammar);

        case ECPGrammarLoop:
        case ECPGrammarLoopBerkeleyFilter:
            return genericGrammar;

        case AgendaParser:
        case APWithMemory:
        case APGhostEdges:
        case APDecodeFOM:
            return new LeftRightListsGrammar(genericGrammar);

        case BeamSearchChartParser:
        case BSCPPruneViterbi:
        case BSCPOnlineBeam:
        case BSCPBoundedHeap:
        case BSCPExpDecay:
        case BSCPPerceptronCell:
        case BSCPFomDecode:
        case BSCPBeamConfTrain:
            // case BSCPBeamConf:
        case CoarseCellAgenda:
        case CoarseCellAgendaCSLUT:
            return new LeftHashGrammar(genericGrammar);

        case CsrSpmv:
        case CsrSpmvPerMidpoint:
        case PackedOpenClSparseMatrixVector:
        case DenseVectorOpenClSparseMatrixVector:
        case CscSpmv:
        case BeamCscSpmv:
            switch (cartesianProductFunctionType) {
            case Simple:
                return new LeftCscSparseMatrixGrammar(genericGrammar, LeftShiftFunction.class);
            case PerfectHash2:
                return new LeftCscSparseMatrixGrammar(genericGrammar, PerfectIntPairHashFilterFunction.class);
            default:
                throw new Exception("Unsupported cartesian-product-function type: " + cartesianProductFunctionType);
            }

        case LeftChildMatrixLoop:
        case CartesianProductBinarySearch:
        case CartesianProductBinarySearchLeftChild:
        case CartesianProductHash:
        case CartesianProductLeftChildHash:
            return new LeftCscSparseMatrixGrammar(genericGrammar, LeftShiftFunction.class);
        case RightChildMatrixLoop:
            return new RightCscSparseMatrixGrammar(genericGrammar, LeftShiftFunction.class);
        case GrammarLoopMatrixLoop:
            return new CsrSparseMatrixGrammar(genericGrammar, LeftShiftFunction.class);

        default:
            throw new Exception("Unsupported parser type: " + researchParserType);
        }
    }

    @Override
    public Parser<?> createLocal() {
        // TODO: Why do we init the cellSelector here instaed of in setup? -nate
        // this.cellSelector = CellSelector.create(cellSelectorType, cellModelStream, cslutScoresStream);
        return createParser(researchParserType, grammar, this);
    }

    public static Parser<?> createParser(final ResearchParserType researchParserType, final Grammar grammar,
            final ParserDriver parserOptions) {
        switch (researchParserType) {
        case ECPCellCrossList:
            return new ECPCellCrossList(parserOptions, (LeftListGrammar) grammar);
        case ECPCellCrossHash:
            return new ECPCellCrossHash(parserOptions, (LeftHashGrammar) grammar);
        case ECPCellCrossMatrix:
            return new ECPCellCrossMatrix(parserOptions, (ChildMatrixGrammar) grammar);
        case ECPGrammarLoop:
            return new ECPGrammarLoop(parserOptions, grammar);
        case ECPGrammarLoopBerkeleyFilter:
            return new ECPGrammarLoopBerkFilter(parserOptions, grammar);
        case ECPInsideOutside:
            return new ECPInsideOutside(parserOptions, (LeftListGrammar) grammar);

        case AgendaParser:
            return new AgendaParser(parserOptions, (LeftRightListsGrammar) grammar);
        case APWithMemory:
            return new APWithMemory(parserOptions, (LeftRightListsGrammar) grammar);
        case APGhostEdges:
            return new APGhostEdges(parserOptions, (LeftRightListsGrammar) grammar);
        case APDecodeFOM:
            return new APDecodeFOM(parserOptions, (LeftRightListsGrammar) grammar);

        case BeamSearchChartParser:
            return new BeamSearchChartParser<LeftHashGrammar, CellChart>(parserOptions, (LeftHashGrammar) grammar);
        case BSCPPruneViterbi:
            return new BSCPPruneViterbi(parserOptions, (LeftHashGrammar) grammar);
        case BSCPOnlineBeam:
            return new BSCPWeakThresh(parserOptions, (LeftHashGrammar) grammar);
        case BSCPBoundedHeap:
            return new BSCPBoundedHeap(parserOptions, (LeftHashGrammar) grammar);
        case BSCPExpDecay:
            return new BSCPExpDecay(parserOptions, (LeftHashGrammar) grammar);
        case BSCPPerceptronCell:
            return new BSCPSkipBaseCells(parserOptions, (LeftHashGrammar) grammar);
        case BSCPFomDecode:
            return new BSCPFomDecode(parserOptions, (LeftHashGrammar) grammar);
            // case BSCPBeamConf:
            // return new BSCPBeamConf(parserOptions, (LeftHashGrammar) grammar, parserOptions.beamConfModel);
        case BSCPBeamConfTrain:
            return new BSCPBeamConfTrain(parserOptions, (LeftHashGrammar) grammar);

        case CoarseCellAgenda:
            return new CoarseCellAgendaParser(parserOptions, (LeftHashGrammar) grammar);
            // case CoarseCellAgendaCSLUT:
            // final CSLUTBlockedCells cslutScores = (CSLUTBlockedCells) CellSelector.create(
            // parserOptions.cellSelectorType, parserOptions.cellModelStream, parserOptions.cslutScoresStream);
            // return new CoarseCellAgendaParserWithCSLUT(parserOptions, (LeftHashGrammar) grammar, cslutScores);

        case CsrSpmv:
            return new CsrSpmvParser(parserOptions, (CsrSparseMatrixGrammar) grammar);
        case CsrSpmvPerMidpoint:
            return new CsrSpmvPerMidpointParser(parserOptions, (CsrSparseMatrixGrammar) grammar);
        case CscSpmv:
            return new CscSpmvParser(parserOptions, (LeftCscSparseMatrixGrammar) grammar);
        case BeamCscSpmv:
            return new BeamCscSpmvParser(parserOptions, (LeftCscSparseMatrixGrammar) grammar);
        case DenseVectorOpenClSparseMatrixVector:
            return new DenseVectorOpenClSpmvParser(parserOptions, (CsrSparseMatrixGrammar) grammar);
        case PackedOpenClSparseMatrixVector:
            return new PackedOpenClSpmvParser(parserOptions, (CsrSparseMatrixGrammar) grammar);

        case LeftChildMatrixLoop:
            return new LeftChildLoopSpmlParser(parserOptions, (LeftCscSparseMatrixGrammar) grammar);
        case RightChildMatrixLoop:
            return new RightChildLoopSpmlParser(parserOptions, (RightCscSparseMatrixGrammar) grammar);
        case GrammarLoopMatrixLoop:
            return new GrammarLoopSpmlParser(parserOptions, (CsrSparseMatrixGrammar) grammar);
        case CartesianProductBinarySearch:
            return new CartesianProductBinarySearchSpmlParser(parserOptions, (LeftCscSparseMatrixGrammar) grammar);
        case CartesianProductBinarySearchLeftChild:
            return new CartesianProductBinarySearchLeftChildSpmlParser(parserOptions,
                    (LeftCscSparseMatrixGrammar) grammar);
        case CartesianProductHash:
            return new CartesianProductHashSpmlParser(parserOptions, (LeftCscSparseMatrixGrammar) grammar);
        case CartesianProductLeftChildHash:
            return new CartesianProductLeftChildHashSpmlParser(parserOptions, (LeftCscSparseMatrixGrammar) grammar);

        default:
            throw new IllegalArgumentException("Unsupported parser type");
        }
    }

    @Override
    protected FutureTask<String> lineTask(final String sentence) {
        return new FutureTask<String>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                final Parser<?> parser = getLocal();
                final ParseStats parseStats = parser.parseSentence(sentence);

                // TODO Return an instance of ParseStats instead of String so we can log this after the parse?
                GlobalLogger.singleton().fine(parseStats.toString() + " " + parser.getStats());
                // if (parser instanceof ChartParser && GlobalLogger.singleton().isLoggable(Level.FINEST)) {
                // GlobalLogger.singleton().finest(((ChartParser<?, ?>) parser).chart.toString());
                // }
                return parseStats.parseBracketString;
            }
        });
    }

    @Override
    protected void cleanup() {
        final float parseTime = (System.currentTimeMillis() - parseStartTime) / 1000f;
        final float cpuTime = parseTime * maxThreads;
        final int sentencesParsed = Parser.sentenceNumber;

        GlobalLogger.singleton().info(
                String.format("INFO: numSentences=%d totalSeconds=%.3f cpuSeconds=%.3f avgSecondsPerSent=%.3f",
                        sentencesParsed, parseTime, cpuTime, cpuTime / sentencesParsed));
    }

    public String optionsToString() {
        String s = "OPTS:";
        s += " ParserType=" + researchParserType;
        // s += prefix + "CellSelector=" + cellSelectorType + "\n";
        s += " FOM=" + edgeFOMType;
        s += " ViterbiMax=" + viterbiMax();
        s += " x1=" + param1;
        s += " x2=" + param2;
        s += " x3=" + param3;
        return s;
    }

    static public ParserDriver defaultTestOptions() {
        final ParserDriver opts = new ParserDriver();
        opts.collectDetailedStatistics = true;
        return opts;
    }

    public boolean viterbiMax() {
        return !this.realSemiring;
    }
}

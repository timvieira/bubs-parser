package edu.ohsu.cslu.parser;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.zip.GZIPInputStream;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import cltool.ThreadLocalLinewiseClTool;
import cltool.Threadable;
import edu.ohsu.cslu.grammar.ChildMatrixGrammar;
import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.GrammarByChild;
import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.LeftListGrammar;
import edu.ohsu.cslu.grammar.LeftRightListsGrammar;
import edu.ohsu.cslu.grammar.RightCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.BitVectorExactFilterFunction;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectHashFilterFunction;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashFilterFunction;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.SimpleShiftFunction;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.UnfilteredFunction;
import edu.ohsu.cslu.parser.Parser.ParserType;
import edu.ohsu.cslu.parser.Parser.ResearchParserType;
import edu.ohsu.cslu.parser.agenda.ACPGhostEdges;
import edu.ohsu.cslu.parser.agenda.ACPWithMemory;
import edu.ohsu.cslu.parser.agenda.AgendaChartParser;
import edu.ohsu.cslu.parser.agenda.CoarseCellAgendaParser;
import edu.ohsu.cslu.parser.agenda.CoarseCellAgendaParserWithCSLUT;
import edu.ohsu.cslu.parser.beam.BSCPBoundedHeap;
import edu.ohsu.cslu.parser.beam.BSCPExpDecay;
import edu.ohsu.cslu.parser.beam.BSCPPruneViterbi;
import edu.ohsu.cslu.parser.beam.BSCPPruneViterbiStats;
import edu.ohsu.cslu.parser.beam.BSCPSkipBaseCells;
import edu.ohsu.cslu.parser.beam.BSCPWeakThresh;
import edu.ohsu.cslu.parser.beam.BeamSearchChartParser;
import edu.ohsu.cslu.parser.cellselector.CSLUTBlockedCells;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.parser.cellselector.CellSelector.CellSelectorType;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector.EdgeSelectorType;
import edu.ohsu.cslu.parser.ml.CartesianProductBinarySearchLeftChildSpmlParser;
import edu.ohsu.cslu.parser.ml.CartesianProductBinarySearchSpmlParser;
import edu.ohsu.cslu.parser.ml.CartesianProductHashSpmlParser;
import edu.ohsu.cslu.parser.ml.CartesianProductLeftChildHashSpmlParser;
import edu.ohsu.cslu.parser.ml.GrammarLoopSpmlParser;
import edu.ohsu.cslu.parser.ml.LeftChildLoopSpmlParser;
import edu.ohsu.cslu.parser.ml.RightChildLoopSpmlParser;
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
 * @since 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
@Threadable(defaultThreads = 1)
public class ParserDriver extends ThreadLocalLinewiseClTool<Parser<?>> {

    // == Parser options ==
    @Option(name = "-p", aliases = { "--parser" }, metaVar = "parser", usage = "Parser implementation")
    private ParserType parserType = ParserType.CKY;

    @Option(name = "-rp", aliases = { "--research-parser" }, metaVar = "parser", usage = "Research Parser implementation")
    private ResearchParserType researchParserType = null;

    @Option(name = "-inOutSum", usage = "Use sum instead of max for inside and outside calculations")
    public boolean viterbiMax = true;

    @Option(name = "-cpf", hidden = true, aliases = { "--cartesian-product-function" }, metaVar = "function", usage = "Cartesian-product function (only used for SpMV parsers)")
    private CartesianProductFunctionType cartesianProductFunctionType = CartesianProductFunctionType.PerfectHash2;

    // @Option(name = "-cp", aliases = { "--cell-processing-type" }, metaVar = "type", usage =
    // "Chart cell processing type")
    // private ChartCellProcessingType chartCellProcessingType = ChartCellProcessingType.CellCrossList;

    @Option(name = "-fom", metaVar = "fom", usage = "Figure of Merit to use for parser")
    EdgeSelectorType edgeFOMType = EdgeSelectorType.Inside;

    @Option(name = "-fomModel", metaVar = "file", usage = "FOM model file")
    private String fomModelFileName = null;
    BufferedReader fomModelStream = null;

    // Nate: I don't think we need to expose this to the user. Instead
    // there should be different possible parsers since changing the
    // cell selection strategy only matters for a few of them
    // @Option(name = "-cellSelect", metaVar = "TYPE", usage = "Method for cell selection")
    public CellSelectorType cellSelectorType = CellSelectorType.LeftRightBottomTop;

    @Option(name = "-cellModel", metaVar = "file", usage = "Model for span selection")
    private String cellModelFileName = null;
    public BufferedReader cellModelStream = null;

    @Option(name = "-cslutCellScores", metaVar = "file", usage = "CSLUT cell scores used for perceptron training and decoding")
    private String cslutScoresFileName = null;
    BufferedReader cslutScoresStream = null;

    // == Grammar options ==
    @Option(name = "-g", required = true, metaVar = "grammar", usage = "Grammar file (text, gzipped text, or binary serialized")
    private String grammarFile = null;

    // == Output options ==
    @Option(name = "-max", aliases = { "--max-length" }, metaVar = "len", usage = "Skip sentences longer than LEN")
    int maxLength = 200;

    @Option(name = "-scores", usage = "Print inside scores for each non-term in result tree")
    boolean printInsideProbs = false;

    @Option(name = "-unk", usage = "Print unknown words as their UNK replacement class")
    boolean printUnkLabels = false;

    @Option(name = "-stats", usage = "Collect detailed counts and statistics (e.g., non-terminals per cell, cartesian-product size, etc.)")
    public boolean collectDetailedStatistics = false;

    @Option(name = "-u", aliases = { "--unfactor" }, usage = "Unfactor parse trees and remove latent annotations")
    boolean unfactor = false;

    // == Other options ==
    // TODO These shouldn't really be static. Parser implementations should use the ParserDriver instance passed in
    @Option(name = "-x1", hidden = true, usage = "Tuning param #1")
    public static float param1 = -1;

    @Option(name = "-x2", hidden = true, usage = "Tuning param #2")
    public static float param2 = -1;

    @Option(name = "-x3", hidden = true, usage = "Tuning param #3")
    public static float param3 = -1;

    private Grammar grammar;
    private long parseStartTime;

    private final static short OBJECT_SIGNATURE = (short) 0xACED;

    public static void main(final String[] args) throws Exception {
        run(args);
    }

    @Override
    // run once at initialization despite number of threads
    public void setup(final CmdLineParser cmdlineParser) throws Exception {

        // map simplified parser choices to the specific research version
        if (researchParserType == null) {
            switch (parserType) {
            case CKY:
                researchParserType = ResearchParserType.ECPCellCrossList;
                break;
            case Agenda:
                researchParserType = ResearchParserType.ACPWithMemory;
                break;
            case Beam:
                researchParserType = ResearchParserType.BSCPPruneViterbi;
                break;
            default:
                throw new IllegalArgumentException("Unsupported parser type");
            }
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
        if (edgeFOMType == EdgeSelectorType.BoundaryInOut && fomModelFileName == null) {
            throw new CmdLineException(cmdlineParser, "BoundaryInOut FOM must also have -fomModel param set");
        }

        if (cellSelectorType == CellSelectorType.CSLUT && cellModelStream == null) {
            throw new CmdLineException(cmdlineParser, "CSLUT span selection must also have -spanModel");
        }

        if (cellSelectorType == CellSelectorType.Perceptron && cslutScoresStream == null) {
            throw new CmdLineException(cmdlineParser, "Perceptron span selection must specify -cslutSpanScores");
        }

        // Handle gzipped and non-gzipped grammar files
        final InputStream grammarInputStream = grammarFile.endsWith(".gz") ? new GZIPInputStream(new FileInputStream(
                grammarFile)) : new FileInputStream(grammarFile);

        // Read the grammar in either text or binary-serialized format.
        final BufferedInputStream bis = new BufferedInputStream(grammarInputStream);
        bis.mark(2);
        final DataInputStream dis = new DataInputStream(bis);

        // Look at the first 2 bytes of the file for the signature of a serialized java object
        final int signature = dis.readShort();
        bis.reset();

        if (signature == OBJECT_SIGNATURE) {
            final ObjectInputStream ois = new ObjectInputStream(bis);
            grammar = (Grammar) ois.readObject();
        } else {
            grammar = createGrammar(researchParserType, new BufferedReader(new InputStreamReader(bis)),
                    cartesianProductFunctionType);
        }

        if (this.collectDetailedStatistics) {
            logger.info(optionsToString());
        }

        parseStartTime = System.currentTimeMillis();
    }

    public static Grammar createGrammar(final ResearchParserType researchParserType, final Reader pcfgReader)
            throws Exception {
        return createGrammar(researchParserType, pcfgReader, null);
    }

    public static Grammar createGrammar(final ResearchParserType researchParserType, final Reader pcfgReader,
            final CartesianProductFunctionType cartesianProductFunctionType) throws Exception {

        switch (researchParserType) {
        case ECPInsideOutside:
        case ECPCellCrossList:
            return new LeftListGrammar(pcfgReader);

        case ECPCellCrossHash:
            return new LeftHashGrammar(pcfgReader);

        case ECPCellCrossMatrix:
            return new ChildMatrixGrammar(pcfgReader);

        case ECPGrammarLoop:
        case ECPGrammarLoopBerkeleyFilter:
            return new GrammarByChild(pcfgReader);

        case AgendaChartParser:
        case ACPWithMemory:
        case ACPGhostEdges:
            return new LeftRightListsGrammar(pcfgReader);

        case BeamSearchChartParser:
        case BSCPPruneViterbi:
        case BSCPOnlineBeam:
        case BSCPBoundedHeap:
        case BSCPExpDecay:
        case BSCPPerceptronCell:
        case CoarseCellAgenda:
        case CoarseCellAgendaCSLUT:
            return new LeftHashGrammar(pcfgReader);

        case CsrSpmv:
        case CsrSpmvPerMidpoint:
        case PackedOpenClSparseMatrixVector:
        case DenseVectorOpenClSparseMatrixVector:

        case CscSpmv:
            switch (cartesianProductFunctionType) {
            case Unfiltered:
                return new LeftCscSparseMatrixGrammar(pcfgReader, UnfilteredFunction.class);
            case Simple:
                return new LeftCscSparseMatrixGrammar(pcfgReader, SimpleShiftFunction.class);
            case BitMatrixExactFilter:
                return new LeftCscSparseMatrixGrammar(pcfgReader, BitVectorExactFilterFunction.class);
            case PerfectHash:
                return new LeftCscSparseMatrixGrammar(pcfgReader, PerfectHashFilterFunction.class);
            case PerfectHash2:
                return new LeftCscSparseMatrixGrammar(pcfgReader, PerfectIntPairHashFilterFunction.class);
            default:
                throw new Exception("Unsupported cartesian-product-function type: " + cartesianProductFunctionType);
            }

        case LeftChildMatrixLoop:
        case CartesianProductBinarySearch:
        case CartesianProductBinarySearchLeftChild:
        case CartesianProductHash:
        case CartesianProductLeftChildHash:
            return new LeftCscSparseMatrixGrammar(pcfgReader, SimpleShiftFunction.class);
        case RightChildMatrixLoop:
            return new RightCscSparseMatrixGrammar(pcfgReader);
        case GrammarLoopMatrixLoop:
            return new CsrSparseMatrixGrammar(pcfgReader, SimpleShiftFunction.class);

        default:
            throw new Exception("Unsupported parser type: " + researchParserType);
        }
    }

    @Override
    public Parser<?> createLocal() {
        return createParser(researchParserType, grammar, this);
    }

    @SuppressWarnings("unchecked")
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
            return new ECPGrammarLoop(parserOptions, (GrammarByChild) grammar);
        case ECPGrammarLoopBerkeleyFilter:
            return new ECPGrammarLoopBerkFilter(parserOptions, (GrammarByChild) grammar);
        case ECPInsideOutside:
            return new ECPInsideOutside(parserOptions, (LeftListGrammar) grammar);

        case AgendaChartParser:
            return new AgendaChartParser(parserOptions, (LeftRightListsGrammar) grammar);
        case ACPWithMemory:
            return new ACPWithMemory(parserOptions, (LeftRightListsGrammar) grammar);
        case ACPGhostEdges:
            return new ACPGhostEdges(parserOptions, (LeftRightListsGrammar) grammar);

        case BeamSearchChartParser:
            return new BeamSearchChartParser(parserOptions, (LeftHashGrammar) grammar);
        case BSCPPruneViterbi:
            if (parserOptions.collectDetailedStatistics) {
                return new BSCPPruneViterbiStats(parserOptions, (LeftHashGrammar) grammar);
            }
            return new BSCPPruneViterbi(parserOptions, (LeftHashGrammar) grammar);
        case BSCPOnlineBeam:
            return new BSCPWeakThresh(parserOptions, (LeftHashGrammar) grammar);
        case BSCPBoundedHeap:
            return new BSCPBoundedHeap(parserOptions, (LeftHashGrammar) grammar);
        case BSCPExpDecay:
            return new BSCPExpDecay(parserOptions, (LeftHashGrammar) grammar);
        case BSCPPerceptronCell:
            return new BSCPSkipBaseCells(parserOptions, (LeftHashGrammar) grammar);

        case CoarseCellAgenda:
            return new CoarseCellAgendaParser(parserOptions, (LeftHashGrammar) grammar);
        case CoarseCellAgendaCSLUT:
            final CSLUTBlockedCells cslutScores = (CSLUTBlockedCells) CellSelector.create(
                    parserOptions.cellSelectorType, parserOptions.cellModelStream, parserOptions.cslutScoresStream);
            return new CoarseCellAgendaParserWithCSLUT(parserOptions, (LeftHashGrammar) grammar, cslutScores);

        case CsrSpmv:
            return new CsrSpmvParser(parserOptions, (CsrSparseMatrixGrammar) grammar);
        case CsrSpmvPerMidpoint:
            return new CsrSpmvPerMidpointParser(parserOptions, (CsrSparseMatrixGrammar) grammar);
        case CscSpmv:
            return new CscSpmvParser(parserOptions, (LeftCscSparseMatrixGrammar) grammar);
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
                if (collectDetailedStatistics) {
                    return parseStats.parseBracketString + "\n" + parseStats.toString();
                }
                return parseStats.parseBracketString;
            }
        });
    }

    @Override
    protected void cleanup() {
        final float parseTime = (System.currentTimeMillis() - parseStartTime) / 1000f;
        final float cpuTime = parseTime * maxThreads;
        final int sentencesParsed = Parser.sentenceNumber;

        logger.info(String.format("INFO: numSentences=%d totalSeconds=%.3f cpuSeconds=%.3f avgSecondsPerSent=%.3f",
                sentencesParsed, parseTime, cpuTime, cpuTime / sentencesParsed));
    }

    public String optionsToString() {
        final String prefix = "OPTS: ";
        String s = "";
        s += prefix + "ParserType=" + parserType + "\n";
        s += prefix + "CellSelector=" + cellSelectorType + "\n";
        s += prefix + "FOM=" + edgeFOMType + "\n";
        s += prefix + "x1=" + param1 + "\n";
        s += prefix + "x2=" + param2 + "\n";
        s += prefix + "x3=" + param3;

        return s;
    }

    static public ParserDriver defaultTestOptions() {
        final ParserDriver opts = new ParserDriver();
        opts.collectDetailedStatistics = true;
        return opts;
    }
}

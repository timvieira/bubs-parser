package edu.ohsu.cslu.parser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.zip.GZIPInputStream;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.EnumAliasMap;
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
import edu.ohsu.cslu.grammar.Grammar.GrammarFormatType;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.BitVectorExactFilterFunction;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.DefaultFunction;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectHashFilterFunction;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashFilterFunction;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.UnfilteredFunction;
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
import edu.ohsu.cslu.parser.spmv.SortAndScanCsrSpmvParser;
import edu.ohsu.cslu.parser.util.StringToMD5;

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

	@Option(name = "-gp", aliases = { "--grammar-file-prefix" }, metaVar = "prefix", usage = "Grammar file prefix")
	private String grammarPrefix;

	@Option(name = "-pcfg", aliases = { "--pcfg-file" }, metaVar = "prefix", usage = "PCFG file")
	private String pcfgFileName;

	@Option(name = "-lex", aliases = { "--lexicon-file" }, metaVar = "FILE", usage = "Lexicon file if full grammar file is specified for -g option")
	private String lexFileName = null;

	@Option(name = "-gf", aliases = { "--grammar-format" }, metaVar = "format", usage = "Format of grammar file")
	private GrammarFormatType grammarFormat = GrammarFormatType.CSLU;

	@Option(name = "-scores", aliases = { "--print-inside-scores" }, usage = "Print inside probabilities")
	boolean printInsideProbs = false;

	@Option(name = "-unk", aliases = { "--print-unk-labels" }, usage = "Print unknown labels")
	boolean printUnkLabels = false;

	@Option(name = "-p", aliases = { "--parser", "--parser-implementation" }, metaVar = "parser", usage = "Parser implementation")
	private ParserType parserType = ParserType.ECPCellCrossList;

	// TODO Implement class name mappings in cltool and replace this with a class name
	@Option(name = "-cpf", aliases = { "--cartesian-product-function" }, metaVar = "function", usage = "Cartesian-product function (only used for SpMV parsers)")
	private CartesianProductFunctionType cartesianProductFunctionType = CartesianProductFunctionType.Default;

	// @Option(name = "-cp", aliases = { "--cell-processing-type" }, metaVar = "type", usage =
	// "Chart cell processing type")
	// private ChartCellProcessingType chartCellProcessingType = ChartCellProcessingType.CellCrossList;

	@Option(name = "-max", aliases = { "--max-length" }, metaVar = "len", usage = "Skip sentences longer than LEN")
	int maxLength = 200;

	@Option(name = "-fom", aliases = { "--figure-of-merit", "-FOM" }, metaVar = "fom", usage = "Figure of Merit")
	EdgeSelectorType edgeFOMType = EdgeSelectorType.Inside;

	@Option(name = "-fomTrain", usage = "Train the specified FOM model")
	private boolean fomTrain = false;

	@Option(name = "-fomModel", metaVar = "file", usage = "FOM model file")
	private String fomModelFileName = null;
	BufferedReader fomModelStream = null;

	@Option(name = "-cellTrain", usage = "Train the specified Cell Selection model")
	private boolean cellTrain = false;

	@Option(name = "-cellSelect", metaVar = "TYPE", usage = "Method for cell selection")
	public CellSelectorType cellSelectorType = CellSelectorType.LeftRightBottomTop;

	@Option(name = "-cellModel", metaVar = "file", usage = "Model for span selection")
	private String cellModelFileName = null;
	public BufferedReader cellModelStream = null;

	@Option(name = "-cslutCellScores", metaVar = "file", usage = "CSLUT cell scores used for perceptron training and decoding")
	private String cslutScoresFileName = null;
	BufferedReader cslutScoresStream = null;

	@Option(name = "-inOutSum", usage = "Use sum instead of max for inside and outside calculations")
	public boolean viterbiMax = true;

	@Option(name = "-ds", aliases = { "--detailed-stats" }, usage = "Collect detailed counts and statistics (e.g., non-terminals per cell, cartesian-product size, etc.)")
	public boolean collectDetailedStatistics = false;

	@Option(name = "-u", aliases = { "--unfactor" }, usage = "Unfactor parse trees")
	boolean unfactor = false;

	@Option(name = "-x1", usage = "Tuning param #1")
	public static float param1 = -1;

	@Option(name = "-x2", usage = "Tuning param #2")
	public static float param2 = -1;

	@Option(name = "-x3", usage = "Tuning param #3")
	public static float param3 = -1;

	private Grammar grammar;

	private long parseStartTime;
	private volatile int sentencesParsed;
	private volatile float totalInsideProbability;

	private BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(System.out));
	private BufferedReader inputStream = new BufferedReader(new InputStreamReader(System.in));

	public static void main(final String[] args) throws Exception {
		run(args);
	}

	@Override
	// run once at initialization despite number of threads
	public void setup(final CmdLineParser cmdlineParser) throws Exception {

		if (grammarPrefix != null) {
			// Handle prefixes with or without trailing periods.
			pcfgFileName = grammarPrefix + (grammarPrefix.endsWith(".") ? "" : ".") + "pcfg";
			lexFileName = grammarPrefix + (grammarPrefix.endsWith(".") ? "" : ".") + "lex";

			// Handle gzipped grammar files
			if (!new File(pcfgFileName).exists() && new File(pcfgFileName + ".gz").exists()) {
				pcfgFileName = pcfgFileName + ".gz";
			}
			if (!new File(lexFileName).exists() && new File(lexFileName + ".gz").exists()) {
				lexFileName = lexFileName + ".gz";
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
		if (edgeFOMType == EdgeSelectorType.BoundaryInOut && fomTrain == false && fomModelFileName == null) {
			throw new CmdLineException(cmdlineParser, "BoundaryInOut FOM must also have -fomTrain or -fomModel param set");
		}

		if (cellSelectorType == CellSelectorType.CSLUT && cellModelStream == null) {
			throw new CmdLineException(cmdlineParser, "CSLUT span selection must also have -spanModel");
		}

		if (cellSelectorType == CellSelectorType.Perceptron && cslutScoresStream == null) {
			throw new CmdLineException(cmdlineParser, "Perceptron span selection must specify -cslutSpanScores");
		}

		grammar = createGrammar();
		parseStartTime = System.currentTimeMillis();
	}

	public Grammar createGrammar() throws Exception {
		final Reader pcfgReader = pcfgFileName.endsWith(".gz") ? new InputStreamReader(new GZIPInputStream(new FileInputStream(pcfgFileName))) : new FileReader(pcfgFileName);
		final Reader lexReader = lexFileName.endsWith(".gz") ? new InputStreamReader(new GZIPInputStream(new FileInputStream(lexFileName))) : new FileReader(lexFileName);

		switch (parserType) {
		case ECPInsideOutside:
		case ECPCellCrossList:
			return new LeftListGrammar(pcfgReader, lexReader, grammarFormat);

		case ECPCellCrossHash:
			return new LeftHashGrammar(pcfgReader, lexReader, grammarFormat);

		case ECPCellCrossMatrix:
			return new ChildMatrixGrammar(pcfgReader, lexReader, grammarFormat);

		case ECPGrammarLoop:
		case ECPGrammarLoopBerkeleyFilter:
			return new GrammarByChild(pcfgReader, lexReader, grammarFormat);

		case AgendaChartParser:
		case ACPWithMemory:
		case ACPGhostEdges:
			return new LeftRightListsGrammar(pcfgReader, lexReader, grammarFormat);

		case LocalBestFirst:
		case LBFPruneViterbi:
		case LBFOnlineBeam:
		case LBFBoundedHeap:
		case LBFExpDecay:
		case LBFPerceptronCell:
		case CoarseCellAgenda:
		case CoarseCellAgendaCSLUT:
			return new LeftHashGrammar(pcfgReader, lexReader, grammarFormat);

		case CsrSpmv:
		case CsrSpmvPerMidpoint:
		case PackedOpenClSparseMatrixVector:
		case DenseVectorOpenClSparseMatrixVector:
		case SortAndScanSpmv:
			switch (cartesianProductFunctionType) {
			case Unfiltered:
				return new CsrSparseMatrixGrammar(pcfgReader, lexReader, grammarFormat, UnfilteredFunction.class);
			case Default:
				return new CsrSparseMatrixGrammar(pcfgReader, lexReader, grammarFormat, DefaultFunction.class);
			case BitMatrixExactFilter:
				return new CsrSparseMatrixGrammar(pcfgReader, lexReader, grammarFormat, BitVectorExactFilterFunction.class);
			case PerfectHash:
				return new CsrSparseMatrixGrammar(pcfgReader, lexReader, grammarFormat, PerfectHashFilterFunction.class);
			case PerfectHash2:
				return new CsrSparseMatrixGrammar(pcfgReader, lexReader, grammarFormat, PerfectIntPairHashFilterFunction.class);
			default:
				throw new Exception("Unsupported filter type: " + cartesianProductFunctionType);
			}

		case CscSpmv:
			switch (cartesianProductFunctionType) {
			case Unfiltered:
				return new LeftCscSparseMatrixGrammar(pcfgReader, lexReader, grammarFormat, UnfilteredFunction.class);
			case Default:
				return new LeftCscSparseMatrixGrammar(pcfgReader, lexReader, grammarFormat, DefaultFunction.class);
			case BitMatrixExactFilter:
				return new LeftCscSparseMatrixGrammar(pcfgReader, lexReader, grammarFormat, BitVectorExactFilterFunction.class);
			case PerfectHash:
				return new LeftCscSparseMatrixGrammar(pcfgReader, lexReader, grammarFormat, PerfectHashFilterFunction.class);
			case PerfectHash2:
				return new LeftCscSparseMatrixGrammar(pcfgReader, lexReader, grammarFormat, PerfectIntPairHashFilterFunction.class);
			default:
				throw new Exception("Unsupported filter type: " + cartesianProductFunctionType);
			}

		case LeftChildMatrixLoop:
		case CartesianProductBinarySearch:
		case CartesianProductBinarySearchLeftChild:
		case CartesianProductHash:
		case CartesianProductLeftChildHash:
			return new LeftCscSparseMatrixGrammar(pcfgReader, lexReader, grammarFormat, DefaultFunction.class);
		case RightChildMatrixLoop:
			return new RightCscSparseMatrixGrammar(pcfgReader, lexReader, grammarFormat);
		case GrammarLoopMatrixLoop:
			return new CsrSparseMatrixGrammar(pcfgReader, lexReader, grammarFormat, DefaultFunction.class);

		default:
			throw new Exception("Unsupported parser type: " + parserType);
		}
	}

	@Override
	public Parser<?> createLocal() {
		final ParserDriver parserOptions = this;

		switch (parserType) {
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

		case LocalBestFirst:
			return new BeamSearchChartParser(parserOptions, (LeftHashGrammar) grammar);
		case LBFPruneViterbi:
			if (collectDetailedStatistics) {
				return new BSCPPruneViterbiStats(parserOptions, (LeftHashGrammar) grammar);
			}
			return new BSCPPruneViterbi(parserOptions, (LeftHashGrammar) grammar);
		case LBFOnlineBeam:
			return new BSCPWeakThresh(parserOptions, (LeftHashGrammar) grammar);
		case LBFBoundedHeap:
			return new BSCPBoundedHeap(parserOptions, (LeftHashGrammar) grammar);
		case LBFExpDecay:
			return new BSCPExpDecay(parserOptions, (LeftHashGrammar) grammar);
		case LBFPerceptronCell:
			return new BSCPSkipBaseCells(parserOptions, (LeftHashGrammar) grammar);

		case CoarseCellAgenda:
			return new CoarseCellAgendaParser(parserOptions, (LeftHashGrammar) grammar);
		case CoarseCellAgendaCSLUT:
			final CSLUTBlockedCells cslutScores = (CSLUTBlockedCells) CellSelector.create(parserOptions.cellSelectorType, parserOptions.cellModelStream,
					parserOptions.cslutScoresStream);
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
		case SortAndScanSpmv:
			return new SortAndScanCsrSpmvParser((CsrSparseMatrixGrammar) grammar);

		case LeftChildMatrixLoop:
			return new LeftChildLoopSpmlParser(parserOptions, (LeftCscSparseMatrixGrammar) grammar);
		case RightChildMatrixLoop:
			return new RightChildLoopSpmlParser(parserOptions, (RightCscSparseMatrixGrammar) grammar);
		case GrammarLoopMatrixLoop:
			return new GrammarLoopSpmlParser(parserOptions, (CsrSparseMatrixGrammar) grammar);
		case CartesianProductBinarySearch:
			return new CartesianProductBinarySearchSpmlParser(parserOptions, (LeftCscSparseMatrixGrammar) grammar);
		case CartesianProductBinarySearchLeftChild:
			return new CartesianProductBinarySearchLeftChildSpmlParser(parserOptions, (LeftCscSparseMatrixGrammar) grammar);
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
			final int sentenceNumber = ++sentencesParsed;

			@Override
			public String call() throws Exception {
				// final StringBuilder sb = new StringBuilder(2048);
				final Parser<?> parser = getLocal();

				final long startTime = System.currentTimeMillis();
				// sb.append(parser.parseSentence(sentence, grammarFormat));
				final String parseResultStr = parser.parseSentence(sentence, grammarFormat);
				final float parseTime = (System.currentTimeMillis() - startTime) / 1000f;
				final float insideProbability = parser.getInside(0, parser.tokenCount, grammar.startSymbol);
				totalInsideProbability += insideProbability;

				// sb.append(String.format("\nSTAT: sentNum=%d  sentLen=%d md5=%s seconds=%.3f inside=%.5f %s", sentenceNumber, parser.tokenCount, StringToMD5.computeMD5(sentence),
				// parseTime, insideProbability, parser.getStats()));
				final String parseStats = String.format("\nSTAT: sentNum=%d  sentLen=%d md5=%s seconds=%.3f inside=%.5f %s", sentenceNumber, parser.tokenCount, StringToMD5
						.computeMD5(sentence), parseTime, insideProbability, parser.getStats());
				// return sb.toString();
				return parseResultStr + "\n" + parseStats;

				// TODO: I'm not sure we want to be working with strings here. Maybe we
				// should have ParseTree and a ParseStats objects to pass around
			}
		});
	}

	@Override
	protected void cleanup() {
		final float parseTime = (System.currentTimeMillis() - parseStartTime) / 1000f;
		final float cpuTime = parseTime * maxThreads;

		logger.info(String.format("INFO: numSentences=%d totalSeconds=%.3f cpuSeconds=%.3f avgSecondsPerSent=%.3f totalInside=%.5f", sentencesParsed, parseTime, cpuTime, cpuTime
				/ sentencesParsed, totalInsideProbability));
	}

	public String optionsToString() {
		final String prefix = "OPTS: ";
		String s = "";
		s += prefix + "ParserType=" + parserType + "\n";
		s += prefix + "CellSelector=" + cellSelectorType + "\n";
		s += prefix + "FOM=" + edgeFOMType + "\n";
		s += prefix + "x1=" + param1 + "\n";
		s += prefix + "x2=" + param2;
		s += prefix + "x3=" + param3;

		return s;
	}

	static public enum ParserType {
		ECPCellCrossList("ecpccl"),
		ECPCellCrossHash("ecpcch"),
		ECPCellCrossMatrix("ecpccm"),
		ECPGrammarLoop("ecpgl"),
		ECPGrammarLoopBerkeleyFilter("ecpglbf"),
		ECPInsideOutside("ecpio"),
		AgendaChartParser("acpall"),
		ACPWithMemory("acpwm"),
		ACPGhostEdges("acpge"),
		LocalBestFirst("lbf"),
		LBFPruneViterbi("lbfpv"),
		LBFOnlineBeam("lbfob"),
		LBFBoundedHeap("lbfbh"),
		LBFExpDecay("lbfed"),
		LBFPerceptronCell("lbfpc"),
		CoarseCellAgenda("cc"),
		CoarseCellAgendaCSLUT("cccslut"),
		JsaSparseMatrixVector("jsa"),
		DenseVectorOpenClSparseMatrixVector("dvopencl"),
		PackedOpenClSparseMatrixVector("popencl"),
		CsrSpmv("csr"),
		CsrSpmvPerMidpoint("csrpm"),
		CscSpmv("csc"),
		SortAndScanSpmv("sort-and-scan"),
		LeftChildMatrixLoop("lcml"),
		RightChildMatrixLoop("rcml"),
		GrammarLoopMatrixLoop("glml"),
		CartesianProductBinarySearch("cpbs"),
		CartesianProductBinarySearchLeftChild("cplbs"),
		CartesianProductHash("cph"),
		CartesianProductLeftChildHash("cplch");

		private ParserType(final String... aliases) {
			EnumAliasMap.singleton().addAliases(this, aliases);
		}
	}

	static public enum CartesianProductFunctionType {
		Default("d", "default"),
		Unfiltered("u", "unfiltered"),
		PosFactoredFiltered("pf"),
		BitMatrixExactFilter("bme", "bitmatrixexact"),
		PerfectHash("ph", "perfecthash"),
		PerfectHash2("ph2", "perfecthash2");

		private CartesianProductFunctionType(final String... aliases) {
			EnumAliasMap.singleton().addAliases(this, aliases);
		}
	}

	static public ParserDriver defaultTestOptions() {
		final ParserDriver opts = new ParserDriver();
		opts.collectDetailedStatistics = true;
		return opts;
	}
}

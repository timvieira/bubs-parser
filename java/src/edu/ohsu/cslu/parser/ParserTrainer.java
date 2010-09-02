package edu.ohsu.cslu.parser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.zip.GZIPInputStream;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import cltool.BaseCommandlineTool;
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
import edu.ohsu.cslu.parser.ParserDriver.CartesianProductFunctionType;
import edu.ohsu.cslu.parser.ParserDriver.ParserType;
import edu.ohsu.cslu.parser.cellselector.CSLUTBlockedCells;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.parser.cellselector.PerceptronCellSelector;
import edu.ohsu.cslu.parser.cellselector.CellSelector.CellSelectorType;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector;
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
import edu.ohsu.cslu.parser.util.Log;

public class ParserTrainer extends BaseCommandlineTool {

	@Option(name = "-gp", aliases = { "--grammar-file-prefix" }, metaVar = "prefix", usage = "Grammar file prefix")
	public String grammarPrefix;

	@Option(name = "-pcfg", aliases = { "--pcfg-file" }, metaVar = "prefix", usage = "PCFG file")
	public String pcfgFileName;

	@Option(name = "-lex", aliases = { "--lexicon-file" }, metaVar = "FILE", usage = "Lexicon file if full grammar file is specified for -g option")
	public String lexFileName = null;

	@Option(name = "-gf", aliases = { "--grammar-format" }, metaVar = "format", usage = "Format of grammar file")
	public GrammarFormatType grammarFormat = GrammarFormatType.CSLU;

	@Option(name = "-scores", aliases = { "--print-inside-scores" }, usage = "Print inside probabilities")
	public boolean printInsideProbs = false;

	@Option(name = "-unk", aliases = { "--print-unk-labels" }, usage = "Print unknown labels")
	public boolean printUnkLabels = false;

	@Option(name = "-p", aliases = { "--parser", "--parser-implementation" }, metaVar = "parser", usage = "Parser implementation")
	public ParserType parserType = ParserType.ECPCellCrossList;

	// TODO Implement class name mappings in cltool and replace this with a class name
	@Option(name = "-cpf", aliases = { "--cartesian-product-function" }, metaVar = "function", usage = "Cartesian-product function (only used for SpMV parsers)")
	public ParserOptions.CartesianProductFunctionType cartesianProductFunctionType = CartesianProductFunctionType.Default;

	// @Option(name = "-cp", aliases = { "--cell-processing-type" }, metaVar = "type", usage =
	// "Chart cell processing type")
	// private ChartCellProcessingType chartCellProcessingType = ChartCellProcessingType.CellCrossList;

	@Option(name = "-max", aliases = { "--max-length" }, metaVar = "len", usage = "Skip sentences longer than LEN")
	public int maxLength = 200;

	@Option(name = "-fom", aliases = { "--figure-of-merit", "-FOM" }, metaVar = "fom", usage = "Figure of Merit")
	public EdgeSelectorType edgeFOMType = EdgeSelectorType.Inside;

	@Option(name = "-fomTrain", usage = "Train the specified FOM model")
	public boolean fomTrain = false;

	@Option(name = "-fomModel", metaVar = "file", usage = "FOM model file")
	public String fomModelFileName = null;
	public BufferedReader fomModelStream = null;

	@Option(name = "-cellTrain", usage = "Train the specified Cell Selection model")
	public boolean cellTrain = false;

	@Option(name = "-cellSelect", metaVar = "TYPE", usage = "Method for cell selection")
	public CellSelectorType cellSelectorType = CellSelectorType.LeftRightBottomTop;

	@Option(name = "-cellModel", metaVar = "file", usage = "Model for span selection")
	public String cellModelFileName = null;
	public BufferedReader cellModelStream = null;

	@Option(name = "-cslutCellScores", metaVar = "file", usage = "CSLUT cell scores used for perceptron training and decoding")
	public String cslutScoresFileName = null;
	public BufferedReader cslutScoresStream = null;

	@Option(name = "-inOutSum", usage = "Use sum instead of max for inside and outside calculations")
	public boolean inOutSum = false;

	@Option(name = "-ds", aliases = { "--detailed-stats" }, usage = "Collect detailed counts and statistics (e.g., non-terminals per cell, cartesian-product size, etc.)")
	public boolean collectDetailedStatistics = false;

	@Option(name = "-u", aliases = { "--unfactor" }, usage = "Unfactor parse trees")
	public boolean unfactor = false;

	@Option(name = "-x1", usage = "Tuning param #1")
	public float param1 = -1;

	@Option(name = "-x2", usage = "Tuning param #2")
	public float param2 = -1;

	public BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(System.out));
	public BufferedReader inputStream = new BufferedReader(new InputStreamReader(System.in));

	public static void main(final String[] args) throws Exception {
		run(args);
	}

	@Override
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
	}

	public ParserOptions createOptions() {
		final ParserOptions opts = new ParserOptions();

		opts.setCollectDetailedStatistics(collectDetailedStatistics);
		opts.setUnfactor(unfactor);

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
			return new BeamSearchChartParser(opts, (LeftHashGrammar) grammar);
		case LBFPruneViterbi:
			return new BSCPPruneViterbi(opts, (LeftHashGrammar) grammar);
		case LBFOnlineBeam:
			return new BSCPWeakThresh(opts, (LeftHashGrammar) grammar);
		case LBFBoundedHeap:
			return new BSCPBoundedHeap(opts, (LeftHashGrammar) grammar);
		case LBFExpDecay:
			return new BSCPExpDecay(opts, (LeftHashGrammar) grammar);
		case LBFPerceptronCell:
			return new BSCPSkipBaseCells(opts, (LeftHashGrammar) grammar);

		case CoarseCellAgenda:
			return new CoarseCellAgendaParser(opts, (LeftHashGrammar) grammar);
		case CoarseCellAgendaCSLUT:
			final CSLUTBlockedCells cslutScores = (CSLUTBlockedCells) CellSelector.create(opts.cellSelectorType, opts.cellModelStream, opts.cslutScoresStream);
			return new CoarseCellAgendaParserWithCSLUT(opts, (LeftHashGrammar) grammar, cslutScores);

		case CsrSpmv:
			return new CsrSpmvParser(opts, (CsrSparseMatrixGrammar) grammar);
		case CsrSpmvPerMidpoint:
			return new CsrSpmvPerMidpointParser(opts, (CsrSparseMatrixGrammar) grammar);
		case CscSpmv:
			return new CscSpmvParser(opts, (LeftCscSparseMatrixGrammar) grammar);
		case DenseVectorOpenClSparseMatrixVector:
			return new DenseVectorOpenClSpmvParser(opts, (CsrSparseMatrixGrammar) grammar);
		case PackedOpenClSparseMatrixVector:
			return new PackedOpenClSpmvParser(opts, (CsrSparseMatrixGrammar) grammar);
		case SortAndScanSpmv:
			return new SortAndScanCsrSpmvParser((CsrSparseMatrixGrammar) grammar);

		case LeftChildMatrixLoop:
			return new LeftChildLoopSpmlParser((LeftCscSparseMatrixGrammar) grammar);
		case RightChildMatrixLoop:
			return new RightChildLoopSpmlParser((RightCscSparseMatrixGrammar) grammar);
		case GrammarLoopMatrixLoop:
			return new GrammarLoopSpmlParser((CsrSparseMatrixGrammar) grammar);
		case CartesianProductBinarySearch:
			return new CartesianProductBinarySearchSpmlParser((LeftCscSparseMatrixGrammar) grammar);
		case CartesianProductBinarySearchLeftChild:
			return new CartesianProductBinarySearchLeftChildSpmlParser((LeftCscSparseMatrixGrammar) grammar);
		case CartesianProductHash:
			return new CartesianProductHashSpmlParser((LeftCscSparseMatrixGrammar) grammar);
		case CartesianProductLeftChildHash:
			return new CartesianProductLeftChildHashSpmlParser((LeftCscSparseMatrixGrammar) grammar);

		default:
			throw new IllegalArgumentException("Unsupported parser type");
		}
	}

	@Override
	public void run() throws Exception {
		runParser(createOptions());
	}

	public void runParser(final ParserOptions opts) throws Exception {
		Log.info(0, opts.toString());
		final Grammar grammar = createGrammar();

		// TODO: this whole FOM setup is pretty ugly. It needs to be changed
		// TODO: the program should know which FOM to use given the model file
		// final EdgeSelector edgeSelector = EdgeSelector.create(opts.edgeFOMType, opts.fomModelStream,
		// grammar);
		// final CellSelector cellSelector = CellSelector.create(opts.cellSelectorType, opts.cellModelStream,
		// opts.cslutScoresStream);

		if (opts.fomTrain == true) {
			final EdgeSelector edgeSelector = EdgeSelector.create(opts.edgeFOMType, opts.fomModelStream, grammar);
			edgeSelector.train(opts.inputStream);
			edgeSelector.writeModel(opts.outputStream);
		} else if (opts.cellTrain == true) {
			// TODO: need to follow a similar train/writeModel method like edgeSelector
			final PerceptronCellSelector perceptronCellSelector = (PerceptronCellSelector) CellSelector.create(opts.cellSelectorType, opts.cellModelStream, opts.cslutScoresStream);
			final BSCPPerceptronCellTrainer parser = new BSCPPerceptronCellTrainer(opts, (LeftHashGrammar) grammar);
			perceptronCellSelector.train(opts.inputStream, parser);
		} else {
			// run parser
			final Parser<?> parser = createParser(opts, grammar);

			final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			for (String sentence = br.readLine(); sentence != null; sentence = br.readLine()) {
				parser.parseSentence(sentence, grammarFormat);
				logger.fine(parser.getStats());
			}

			logger.info("INFO: numSentences=" + parser.sentenceNumber + " totalSeconds=" + parser.totalParseTimeSec + " avgSecondsPerSent="
					+ (parser.totalParseTimeSec / parser.sentenceNumber) + " totalInsideScore=" + parser.totalInsideScore);
		}
	}
}

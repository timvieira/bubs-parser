/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
 * 
 * This file is part of the BUBS Parser.
 * 
 * The BUBS Parser is free software: you can redistribute it and/or 
 * modify  it under the terms of the GNU Affero General Public License 
 * as published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * The BUBS Parser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>
 */
package edu.ohsu.cslu.parser;

import java.io.File;
import java.io.ObjectInputStream;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;

import cltool4j.BaseLogger;
import cltool4j.ConfigProperties;
import cltool4j.GlobalConfigProperties;
import cltool4j.ThreadLocalLinewiseClTool;
import cltool4j.Threadable;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.grammar.ChildMatrixGrammar;
import edu.ohsu.cslu.grammar.CoarseGrammar;
import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.LeftListGrammar;
import edu.ohsu.cslu.grammar.LeftRightListsGrammar;
import edu.ohsu.cslu.grammar.RightCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.Int2IntHashPackingFunction;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.LeftShiftFunction;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashPackingFunction;
import edu.ohsu.cslu.parser.Parser.DecodeMethod;
import edu.ohsu.cslu.parser.Parser.InputFormat;
import edu.ohsu.cslu.parser.Parser.ParserType;
import edu.ohsu.cslu.parser.Parser.ResearchParserType;
import edu.ohsu.cslu.parser.agenda.APDecodeFOM;
import edu.ohsu.cslu.parser.agenda.APGhostEdges;
import edu.ohsu.cslu.parser.agenda.APWithMemory;
import edu.ohsu.cslu.parser.agenda.AgendaParser;
import edu.ohsu.cslu.parser.agenda.CoarseCellAgendaParser;
import edu.ohsu.cslu.parser.beam.BSCPBoundedHeap;
import edu.ohsu.cslu.parser.beam.BSCPExpDecay;
import edu.ohsu.cslu.parser.beam.BSCPFomDecode;
import edu.ohsu.cslu.parser.beam.BSCPPruneViterbi;
import edu.ohsu.cslu.parser.beam.BSCPSkipBaseCells;
import edu.ohsu.cslu.parser.beam.BSCPSplitUnary;
import edu.ohsu.cslu.parser.beam.BSCPWeakThresh;
import edu.ohsu.cslu.parser.beam.BeamSearchChartParser;
import edu.ohsu.cslu.parser.cellselector.CellSelectorFactory;
import edu.ohsu.cslu.parser.cellselector.LeftRightBottomTopTraversal;
import edu.ohsu.cslu.parser.cellselector.OHSUCellConstraintsFactory;
import edu.ohsu.cslu.parser.cellselector.PerceptronBeamWidthFactory;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.edgeselector.BoundaryInOut;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector.EdgeSelectorType;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelectorFactory;
import edu.ohsu.cslu.parser.ml.CartesianProductBinarySearchLeftChildSpmlParser;
import edu.ohsu.cslu.parser.ml.CartesianProductBinarySearchSpmlParser;
import edu.ohsu.cslu.parser.ml.CartesianProductHashSpmlParser;
import edu.ohsu.cslu.parser.ml.CartesianProductLeftChildHashSpmlParser;
import edu.ohsu.cslu.parser.ml.GrammarLoopSpmlParser;
import edu.ohsu.cslu.parser.ml.LeftChildLoopSpmlParser;
import edu.ohsu.cslu.parser.ml.RightChildLoopSpmlParser;
import edu.ohsu.cslu.parser.spmv.CscSpmvParser;
import edu.ohsu.cslu.parser.spmv.CsrSpmvParser;
import edu.ohsu.cslu.parser.spmv.DenseVectorOpenClSpmvParser;
import edu.ohsu.cslu.parser.spmv.GrammarParallelCscSpmvParser;
import edu.ohsu.cslu.parser.spmv.GrammarParallelCsrSpmvParser;
import edu.ohsu.cslu.parser.spmv.PackedOpenClSpmvParser;
import edu.ohsu.cslu.parser.spmv.SparseMatrixVectorParser;
import edu.ohsu.cslu.parser.spmv.SparseMatrixVectorParser.CartesianProductFunctionType;
import edu.ohsu.cslu.util.Evalb.BracketEvaluator;
import edu.ohsu.cslu.util.Evalb.EvalbResult;

/**
 * Driver class for all parser implementations. Based on the cltool4j command-line tool infrastructure
 * (http://code.google.com/p/cltool4j/).
 * 
 * @author Nathan Bodenstab
 * @author Aaron Dunlop
 * @since 2009
 */
@Threadable(defaultThreads = 1)
public class ParserDriver extends ThreadLocalLinewiseClTool<Parser<?>, ParseContext> {

    // Global vars to create parser
    public CellSelectorFactory cellSelectorFactory = LeftRightBottomTopTraversal.FACTORY;
    public EdgeSelectorFactory edgeSelectorFactory = new EdgeSelectorFactory(EdgeSelectorType.Inside);
    Grammar grammar, coarseGrammar;

    // == Parser options ==
    @Option(name = "-p", metaVar = "PARSER", usage = "Parser implementation (cyk|beam|agenda|matrix)")
    private ParserType parserType = ParserType.Matrix;

    @Option(name = "-rp", hidden = true, metaVar = "PARSER", usage = "Research Parser implementation")
    private ResearchParserType researchParserType = null;

    // == Grammar options ==
    @Option(name = "-g", metaVar = "FILE", usage = "Grammar file (text, gzipped text, or binary serialized)")
    private String grammarFile = null;

    @Option(name = "-coarseGrammar", hidden = true, metaVar = "FILE", usage = "Coarse grammar file (text, gzipped text, or binary serialized)")
    private String coarseGrammarFile = null;

    @Option(name = "-m", metaVar = "FILE", usage = "Model file (binary serialized)")
    private File modelFile = null;

    // @Option(name = "-real", hidden = true, usage =
    // "Use real semiring (sum) instead of tropical (max) for inside/outside calculations")
    // public boolean realSemiring = false;

    @Option(name = "-decode", metaVar = "TYPE", hidden = true, usage = "Method to extract best tree from forest")
    public DecodeMethod decodeMethod = DecodeMethod.ViterbiMax;

    // == Output options ==
    @Option(name = "-maxLength", metaVar = "LEN", usage = "Skip sentences longer than LEN")
    int maxLength = 200;

    // TODO: option doesn't work anymore
    // @Option(name = "-scores", usage = "Print inside scores for each non-term in result tree")
    // boolean printInsideProbs = false;

    @Option(name = "-printUNK", usage = "Print unknown words as their UNK replacement class")
    boolean printUnkLabels = false;

    @Option(name = "-oldUNK", hidden = true, usage = "Use old UNK function")
    public static boolean oldUNK = false;

    @Option(name = "-binary", usage = "Leave parse tree output in binary-branching form")
    public boolean binaryTreeOutput = false;

    @Option(name = "-if", metaVar = "FORMAT", usage = "Input format type.  Choosing 'text' will tokenize the input before parsing.")
    public InputFormat inputFormat = InputFormat.Token;

    @Option(name = "-reparse", metaVar = "N", hidden = true, usage = "If no solution, loosen constraints and reparse N times")
    public int reparse = 2;

    // == Specific parser options ==
    @Option(name = "-fom", metaVar = "FOM", usage = "Figure-of-Merit edge scoring function (name or model file)")
    private String fomTypeOrModel = "Inside";

    @Option(name = "-beamTune", metaVar = "VAL", usage = "Tuning params for beam search: maxBeamWidth,globalScoreDelta,localScoreDelta,factoredCellBeamWidth")
    public String beamTune = "30,20,8,30";

    // @Option(name = "-cpf", hidden = true, aliases = { "--cartesian-product-function" }, metaVar =
    // "function", usage = "Cartesian-product function (only used for SpMV parsers)")
    @Option(name = "-cpf", hidden = true, metaVar = "function", usage = "Cartesian-product function (only used for SpMV parsers)")
    private CartesianProductFunctionType cartesianProductFunctionType = CartesianProductFunctionType.PerfectHash;

    // @Option(name = "-cp", aliases = { "--cell-processing-type" }, metaVar = "type", usage =
    // "Chart cell processing type")
    // private ChartCellProcessingType chartCellProcessingType = ChartCellProcessingType.CellCrossList;

    @Option(name = "-beamModel", metaVar = "FILE", usage = "Beam-width prediction model (Bodenstab et al., 2011)")
    private String beamModelFileName = null;

    // TODO These default biases are specific to the 0,1,2,4 model, but defaulted here for the moment until we
    // can move them into a combined model file. First, we should make it a -O option instead of a
    // command-line parameter
    @Option(name = "-beamModelBias", metaVar = "VAL", usage = "Bias for each bin in model, seperated by commas")
    public String beamModelBias = "200,200,200,200";

    // @Option(name = "-beamModelFeats", metaVar = "VAL", hidden = true, usage =
    // "Feature template string: lt rt lt_lt-1 rw_rt loc ...")
    // public static String featTemplate;

    @Option(name = "-ccModel", metaVar = "FILE", usage = "CSLU Chart Constraints model (Roark and Hollingshead, 2008)")
    private String chartConstraintsModel = null;

    @Option(name = "-ccTune", metaVar = "VAL", usage = "CSLU Chart Constraints for Absolute (A), High Precision (P), or Linear (N): A,start,end,unary | P,pct | N,int")
    public String chartConstraintsThresh = "A,120,120,inf";

    // (1) absolute thresh A,start,end,unary
    // (2) high precision P,pct (pct cells closed score > 0)
    // (3) linear complexity N,int (x*N max open)

    @Option(name = "-ccPrint", hidden = true, usage = "Print Cell Constraints for each input sentence and exit (no parsing done)")
    public static boolean chartConstraintsPrint = false;

    // == Other options ==
    // TODO These shouldn't really be static. Parser implementations should use the ParserDriver instance
    // passed in
    /*
     * @Option(name = "-x1", hidden = true, usage = "Tuning param #1") public static float param1 = -1;
     * 
     * @Option(name = "-x2", hidden = true, usage = "Tuning param #2") public static float param2 = -1;
     * 
     * @Option(name = "-x3", hidden = true, usage = "Tuning param #3") public static float param3 = -1;
     */

    private long parseStartTime;
    private LinkedList<Parser<?>> parserInstances = new LinkedList<Parser<?>>();
    private final BracketEvaluator evaluator = new BracketEvaluator();

    /**
     * Configuration property key for the number of cell-level threads requested by the user. We handle threading at
     * three levels; threading per-sentence is handled by the command-line tool infrastructure and specified with the
     * standard '-xt' parameter. Cell-level and grammar-level threading are handled by the parser instance and specified
     * with this option and with {@link #OPT_GRAMMAR_THREAD_COUNT}.
     */
    public final static String OPT_CELL_THREAD_COUNT = "cellThreads";

    /**
     * Configuration property key for the number of grammar-level threads requested by the user. We handle threading at
     * three levels; threading per-sentence is handled by the command-line tool infrastructure and specified with the
     * standard '-xt' parameter. Cell-level and grammar-level threading are handled by the parser instance and specified
     * with this option and with {@link #OPT_CELL_THREAD_COUNT}.
     */
    public final static String OPT_GRAMMAR_THREAD_COUNT = "grammarThreads";

    /**
     * Configuration property key for the number of row-level or cell-level threads actually used. In some cases the
     * number of threads requested is impractical (e.g., if it is greater than the maximum number of cells in a row or
     * greater than the number of grammar rows). {@link Parser} instances which make use of
     * {@link #OPT_GRAMMAR_THREAD_COUNT} should populate this property to indicate the number of threads actually used.
     * Among other potential uses, this allows {@link #cleanup()} to report accurate timing information.
     */
    public final static String OPT_CONFIGURED_THREAD_COUNT = "actualThreads";

    public static void main(final String[] args) {
        run(args);
    }

    @Override
    // run once at initialization regardless of number of threads
    public void setup() throws Exception {

        // map simplified parser choices to the specific research version
        if (researchParserType == null) {
            researchParserType = parserType.researchParserType;
        }

        // if (researchParserType == ResearchParserType.ECPInsideOutside) {
        // this.realSemiring = true;
        // }

        grammar = readGrammar(grammarFile, researchParserType, cartesianProductFunctionType);

        if (modelFile != null) {
            final ObjectInputStream ois = new ObjectInputStream(fileAsInputStream(modelFile));
            final String metadata = (String) ois.readObject();
            final ConfigProperties props = (ConfigProperties) ois.readObject();
            GlobalConfigProperties.singleton().mergeUnder(props);

            BaseLogger.singleton().fine("Reading grammar...");
            this.grammar = (Grammar) ois.readObject();

            BaseLogger.singleton().fine("Reading FOM...");
            edgeSelectorFactory = (EdgeSelectorFactory) ois.readObject();

        } else {

            if (fomTypeOrModel.equals("Inside")) {
                edgeSelectorFactory = new EdgeSelectorFactory(EdgeSelectorType.Inside);
            } else if (fomTypeOrModel.equals("NormalizedInside")) {
                edgeSelectorFactory = new EdgeSelectorFactory(EdgeSelectorType.NormalizedInside);
            } else if (fomTypeOrModel.equals("InsideWithFwdBkwd")) {
                edgeSelectorFactory = new EdgeSelectorFactory(EdgeSelectorType.InsideWithFwdBkwd);
            } else if (new File(fomTypeOrModel).exists()) {
                // Assuming boundary FOM
                Grammar fomGrammar = grammar;
                if (this.coarseGrammarFile != null) {
                    // coarseGrammar = readGrammar(coarseGrammarFile, researchParserType, cartesianProductFunctionType);
                    coarseGrammar = new CoarseGrammar(coarseGrammarFile, this.grammar);
                    BaseLogger.singleton().fine("FOM coarse grammar stats: " + coarseGrammar.getStats());
                    fomGrammar = coarseGrammar;
                }
                edgeSelectorFactory = new BoundaryInOut(EdgeSelectorType.BoundaryInOut, fomGrammar,
                        fileAsBufferedReader(fomTypeOrModel));
            } else {
                throw new IllegalArgumentException("-fom value '" + fomTypeOrModel + "' not valid.");
            }

            // if (researchParserType == ResearchParserType.BSCPBeamConfTrain && featTemplate == null) {
            // throw new IllegalArgumentException("ERROR: BSCPTrainFOMConfidence requires -feats to be non-empty");
            // }

            if (chartConstraintsModel != null) {
                cellSelectorFactory = new OHSUCellConstraintsFactory(fileAsBufferedReader(chartConstraintsModel),
                        chartConstraintsThresh, grammar.isLeftFactored());
            }

            if (beamModelFileName != null) {
                cellSelectorFactory = new PerceptronBeamWidthFactory(fileAsBufferedReader(beamModelFileName),
                        beamModelBias);
            }
        }

        BaseLogger.singleton().fine(grammar.getStats());
        BaseLogger.singleton().fine(optionsToString());

        parseStartTime = System.currentTimeMillis();
    }

    public static Grammar readGrammar(final String grammarFile, final ResearchParserType researchParserType,
            final CartesianProductFunctionType cartesianProductFunctionType) throws Exception {

        // Handle gzipped and non-gzipped grammar files
        // Read the generic grammar in either text or binary-serialized format.
        final Grammar genericGrammar = Grammar.read(grammarFile);

        // Construct the requested grammar type from the generic grammar
        return createGrammar(genericGrammar, researchParserType, cartesianProductFunctionType);
    }

    /**
     * Creates a specific {@link Grammar} subclass, based on the generic instance passed in.
     * 
     * TODO Use the generic grammar types on the parser class to eliminate this massive switch?
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

            // case ECPInsideOutside:
        case ECPCellCrossHashGrammarLoop:
        case ECPCellCrossHashGrammarLoop2:
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
        case BSCPSplitUnary:
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
        case GrammarParallelCsrSpmv:
            switch (cartesianProductFunctionType) {
            case Simple:
                return new CsrSparseMatrixGrammar(genericGrammar, LeftShiftFunction.class);
            case PerfectHash:
                return new CsrSparseMatrixGrammar(genericGrammar, PerfectIntPairHashPackingFunction.class);
            default:
                throw new Exception("Unsupported cartesian-product-function type: " + cartesianProductFunctionType);
            }

        case PackedOpenClSpmv:
        case DenseVectorOpenClSpmv:
            return new CsrSparseMatrixGrammar(genericGrammar, LeftShiftFunction.class);

        case CscSpmv:
        case GrammarParallelCscSpmv:
            switch (cartesianProductFunctionType) {
            case Simple:
                return new LeftCscSparseMatrixGrammar(genericGrammar, LeftShiftFunction.class);
            case PerfectHash:
                return new LeftCscSparseMatrixGrammar(genericGrammar, PerfectIntPairHashPackingFunction.class);
            default:
                throw new Exception("Unsupported cartesian-product-function type: " + cartesianProductFunctionType);
            }

        case LeftChildMl:
        case CartesianProductBinarySearchMl:
        case CartesianProductBinarySearchLeftChildMl:
        case CartesianProductHashMl:
        case CartesianProductLeftChildHashMl:
            switch (cartesianProductFunctionType) {
            case Simple:
                return new LeftCscSparseMatrixGrammar(genericGrammar, LeftShiftFunction.class);
            case Hash:
                return new LeftCscSparseMatrixGrammar(genericGrammar, Int2IntHashPackingFunction.class);
            case PerfectHash:
                return new LeftCscSparseMatrixGrammar(genericGrammar, PerfectIntPairHashPackingFunction.class);
            default:
                throw new Exception("Unsupported cartesian-product-function type: " + cartesianProductFunctionType);
            }
        case RightChildMl:
            return new RightCscSparseMatrixGrammar(genericGrammar, LeftShiftFunction.class);
        case GrammarLoopMl:
            return new CsrSparseMatrixGrammar(genericGrammar, LeftShiftFunction.class);

        default:
            throw new Exception("Unsupported parser type: " + researchParserType);
        }
    }

    @Override
    public Parser<?> createLocal() {
        final Parser<?> parser = createParser();
        parserInstances.add(parser);
        return parser;
    }

    public Parser<?> createParser() {
        switch (researchParserType) {
        case ECPCellCrossList:
            return new ECPCellCrossList(this, (LeftListGrammar) grammar);
        case ECPCellCrossHash:
            return new ECPCellCrossHash(this, (LeftHashGrammar) grammar);
        case ECPCellCrossHashGrammarLoop:
            return new ECPCellCrossHashGrammarLoop(this, (LeftHashGrammar) grammar);
        case ECPCellCrossHashGrammarLoop2:
            return new ECPCellCrossHashGrammarLoop2(this, (LeftHashGrammar) grammar);
        case ECPCellCrossMatrix:
            return new ECPCellCrossMatrix(this, (ChildMatrixGrammar) grammar);
        case ECPGrammarLoop:
            return new ECPGrammarLoop(this, grammar);
        case ECPGrammarLoopBerkeleyFilter:
            return new ECPGrammarLoopBerkFilter(this, grammar);
        case ECPInsideOutside:
            return new ECPInsideOutside(this, (LeftListGrammar) grammar);
            // return new ECPInsideOutside2(parserOptions, (LeftHashGrammar) grammar);

        case AgendaParser:
            return new AgendaParser(this, (LeftRightListsGrammar) grammar);
        case APWithMemory:
            return new APWithMemory(this, (LeftRightListsGrammar) grammar);
        case APGhostEdges:
            return new APGhostEdges(this, (LeftRightListsGrammar) grammar);
        case APDecodeFOM:
            return new APDecodeFOM(this, (LeftRightListsGrammar) grammar);

        case BeamSearchChartParser:
            return new BeamSearchChartParser<LeftHashGrammar, CellChart>(this, (LeftHashGrammar) grammar);
        case BSCPSplitUnary:
            return new BSCPSplitUnary(this, (LeftHashGrammar) grammar);
        case BSCPPruneViterbi:
            return new BSCPPruneViterbi(this, (LeftHashGrammar) grammar);
        case BSCPOnlineBeam:
            return new BSCPWeakThresh(this, (LeftHashGrammar) grammar);
        case BSCPBoundedHeap:
            return new BSCPBoundedHeap(this, (LeftHashGrammar) grammar);
        case BSCPExpDecay:
            return new BSCPExpDecay(this, (LeftHashGrammar) grammar);
        case BSCPPerceptronCell:
            return new BSCPSkipBaseCells(this, (LeftHashGrammar) grammar);
        case BSCPFomDecode:
            return new BSCPFomDecode(this, (LeftHashGrammar) grammar);
            // case BSCPBeamConf:
            // return new BSCPBeamConf(this, (LeftHashGrammar) grammar, parserOptions.beamConfModel);
            // case BSCPBeamConfTrain:
            // return new BSCPBeamConfTrain(this, (LeftHashGrammar) grammar);

        case CoarseCellAgenda:
            return new CoarseCellAgendaParser(this, (LeftHashGrammar) grammar);
            // case CoarseCellAgendaCSLUT:
            // final CSLUTBlockedCells cslutScores = (CSLUTBlockedCells) CellSelector.create(
            // parserOptions.cellSelectorType, parserOptions.cellModelStream,
            // parserOptions.cslutScoresStream);
            // return new CoarseCellAgendaParserWithCSLUT(this, (LeftHashGrammar) grammar, cslutScores);

        case CsrSpmv:
            return new CsrSpmvParser(this, (CsrSparseMatrixGrammar) grammar);
        case GrammarParallelCsrSpmv:
            return new GrammarParallelCsrSpmvParser(this, (CsrSparseMatrixGrammar) grammar);
        case CscSpmv:
            return new CscSpmvParser(this, (LeftCscSparseMatrixGrammar) grammar);
        case GrammarParallelCscSpmv:
            return new GrammarParallelCscSpmvParser(this, (LeftCscSparseMatrixGrammar) grammar);
        case DenseVectorOpenClSpmv:
            return new DenseVectorOpenClSpmvParser(this, (CsrSparseMatrixGrammar) grammar);
        case PackedOpenClSpmv:
            return new PackedOpenClSpmvParser(this, (CsrSparseMatrixGrammar) grammar);

        case LeftChildMl:
            return new LeftChildLoopSpmlParser(this, (LeftCscSparseMatrixGrammar) grammar);
        case RightChildMl:
            return new RightChildLoopSpmlParser(this, (RightCscSparseMatrixGrammar) grammar);
        case GrammarLoopMl:
            return new GrammarLoopSpmlParser(this, (CsrSparseMatrixGrammar) grammar);
        case CartesianProductBinarySearchMl:
            return new CartesianProductBinarySearchSpmlParser(this, (LeftCscSparseMatrixGrammar) grammar);
        case CartesianProductBinarySearchLeftChildMl:
            return new CartesianProductBinarySearchLeftChildSpmlParser(this, (LeftCscSparseMatrixGrammar) grammar);
        case CartesianProductHashMl:
            return new CartesianProductHashSpmlParser(this, (LeftCscSparseMatrixGrammar) grammar);
        case CartesianProductLeftChildHashMl:
            return new CartesianProductLeftChildHashSpmlParser(this, (LeftCscSparseMatrixGrammar) grammar);

        default:
            throw new IllegalArgumentException("Unsupported parser type");
        }
    }

    @Override
    protected FutureTask<ParseContext> lineTask(final String input) {
        return new FutureTask<ParseContext>(new Callable<ParseContext>() {

            @Override
            public ParseContext call() throws Exception {
                return getLocal().parseSentence(input);
            }
        });
    }

    @Override
    protected void output(final ParseContext parseResult) {
        if (parseResult != null) {
            System.out.println(parseResult.parseBracketString);

            if (inputFormat == InputFormat.Tree) {
                // If parse failed, replace with ROOT => all-lexical-nodes
                if (parseResult.naryParse == null) {
                    parseResult.naryParse = new NaryTree<String>(grammar.startSymbolStr);
                    for (final NaryTree<String> leaf : parseResult.inputTree.leafTraversal()) {
                        parseResult.naryParse.addChild(leaf);
                    }
                }
                // TODO: can't output per-tree accuracy until evaluate() returns correct result
                // parseResult.evalb = evaluator.evaluate(parseResult.naryParse, parseResult.inputTree);
                evaluator.evaluate(parseResult.naryParse, parseResult.inputTree);
            }
            BaseLogger.singleton().fine(parseResult.toString() + " " + parseResult.parserStats);
        }
    }

    @Override
    protected void cleanup() {
        final float parseTime = (System.currentTimeMillis() - parseStartTime) / 1000f;

        // If the individual parser configured a thread count (e.g. CellParallelCsrSpmvParser), compute
        // CPU-time using
        // that thread count; otherwise, assume maxThreads is correct
        final int threads = GlobalConfigProperties.singleton().containsKey(OPT_CONFIGURED_THREAD_COUNT) ? GlobalConfigProperties
                .singleton().getIntProperty(OPT_CONFIGURED_THREAD_COUNT) : maxThreads;

        // Note that this CPU-time computation does not include GC time
        final float cpuTime = parseTime * threads;
        final int sentencesParsed = Parser.sentenceNumber;

        final StringBuilder sb = new StringBuilder();
        // TODO Add cpuSecondsPerSent and switch avgSecondsPerSent to report mean latency (not mean
        // throughput)
        sb.append(String.format(
                "INFO: numSentences=%d numFail=%d totalSeconds=%.3f cpuSeconds=%.3f avgSecondsPerSent=%.3f",
                sentencesParsed, Parser.failedParses, parseTime, cpuTime, cpuTime / sentencesParsed));

        if (parserInstances.getFirst() instanceof SparseMatrixVectorParser) {
            sb.append(String.format(" totalXProductTime=%d totalBinarySpMVTime=%d",
                    SparseMatrixVectorParser.totalCartesianProductTime, SparseMatrixVectorParser.totalBinarySpMVTime));
        }

        if (inputFormat == InputFormat.Tree) {
            final EvalbResult evalbResult = evaluator.accumulatedResult();
            sb.append(String.format(" f1=%.2f prec=%.2f recall=%.2f", evalbResult.f1 * 100,
                    evalbResult.precision * 100, evalbResult.recall * 100));
        }

        BaseLogger.singleton().info(sb.toString());

        for (final Parser<?> p : parserInstances) {
            try {
                p.shutdown();
            } catch (final Exception ignore) {
            }
        }
    }

    public String optionsToString() {
        String s = "INFO:";
        s += " ParserType=" + researchParserType;
        // s += prefix + "CellSelector=" + cellSelectorType + "\n";
        s += " FOM=" + fomTypeOrModel;
        s += " Decode=" + decodeMethod;
        return s;
    }

    static public ParserDriver defaultTestOptions() {
        final ParserDriver opts = new ParserDriver();
        BaseLogger.singleton().setLevel(Level.FINER);
        return opts;
    }
}

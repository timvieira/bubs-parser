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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.util.HashMap;
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
import edu.ohsu.cslu.datastructs.narytree.CharniakHeadPercolationRuleset;
import edu.ohsu.cslu.datastructs.narytree.HeadPercolationRuleset;
import edu.ohsu.cslu.datastructs.narytree.NaryTree.Binarization;
import edu.ohsu.cslu.grammar.ChildMatrixGrammar;
import edu.ohsu.cslu.grammar.CoarseGrammar;
import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.InsideOutsideCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.LeftListGrammar;
import edu.ohsu.cslu.grammar.LeftRightListsGrammar;
import edu.ohsu.cslu.grammar.ListGrammar;
import edu.ohsu.cslu.grammar.RightCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.Int2IntHashPackingFunction;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.LeftShiftFunction;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashPackingFunction;
import edu.ohsu.cslu.lela.ConstrainedCellSelector;
import edu.ohsu.cslu.parser.Parser.DecodeMethod;
import edu.ohsu.cslu.parser.Parser.InputFormat;
import edu.ohsu.cslu.parser.Parser.ParserType;
import edu.ohsu.cslu.parser.Parser.ReparseStrategy;
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
import edu.ohsu.cslu.parser.beam.BSCPSplitUnary;
import edu.ohsu.cslu.parser.beam.BSCPWeakThresh;
import edu.ohsu.cslu.parser.beam.BeamSearchChartParser;
import edu.ohsu.cslu.parser.cellselector.CellSelectorModel;
import edu.ohsu.cslu.parser.cellselector.DepGraphCellSelectorModel;
import edu.ohsu.cslu.parser.cellselector.LeftRightBottomTopTraversal;
import edu.ohsu.cslu.parser.cellselector.OHSUCellConstraintsModel;
import edu.ohsu.cslu.parser.cellselector.PerceptronBeamWidthModel;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.Chart;
import edu.ohsu.cslu.parser.chart.Chart.RecoveryStrategy;
import edu.ohsu.cslu.parser.ecp.ECPCellCrossHash;
import edu.ohsu.cslu.parser.ecp.ECPCellCrossHashGrammarLoop;
import edu.ohsu.cslu.parser.ecp.ECPCellCrossHashGrammarLoop2;
import edu.ohsu.cslu.parser.ecp.ECPCellCrossList;
import edu.ohsu.cslu.parser.ecp.ECPCellCrossMatrix;
import edu.ohsu.cslu.parser.ecp.ECPGrammarLoop;
import edu.ohsu.cslu.parser.ecp.ECPGrammarLoopBerkFilter;
import edu.ohsu.cslu.parser.ecp.ECPInsideOutside;
import edu.ohsu.cslu.parser.fom.BoundaryInOut;
import edu.ohsu.cslu.parser.fom.BoundaryLex;
import edu.ohsu.cslu.parser.fom.FigureOfMeritModel;
import edu.ohsu.cslu.parser.fom.FigureOfMeritModel.FOMType;
import edu.ohsu.cslu.parser.fom.InsideProb;
import edu.ohsu.cslu.parser.fom.NGramOutside;
import edu.ohsu.cslu.parser.fom.PriorFOM;
import edu.ohsu.cslu.parser.ml.CartesianProductBinarySearchLeftChildSpmlParser;
import edu.ohsu.cslu.parser.ml.CartesianProductBinarySearchSpmlParser;
import edu.ohsu.cslu.parser.ml.CartesianProductHashSpmlParser;
import edu.ohsu.cslu.parser.ml.CartesianProductLeftChildHashSpmlParser;
import edu.ohsu.cslu.parser.ml.ConstrainedCphSpmlParser;
import edu.ohsu.cslu.parser.ml.GrammarLoopSpmlParser;
import edu.ohsu.cslu.parser.ml.InsideOutsideCphSpmlParser;
import edu.ohsu.cslu.parser.ml.LeftChildLoopSpmlParser;
import edu.ohsu.cslu.parser.ml.RightChildLoopSpmlParser;
import edu.ohsu.cslu.parser.spmv.CscSpmvParser;
import edu.ohsu.cslu.parser.spmv.CsrSpmvParser;
import edu.ohsu.cslu.parser.spmv.DenseVectorOpenClSpmvParser;
import edu.ohsu.cslu.parser.spmv.GrammarParallelCscSpmvParser;
import edu.ohsu.cslu.parser.spmv.GrammarParallelCsrSpmvParser;
import edu.ohsu.cslu.parser.spmv.PackedOpenClSpmvParser;
import edu.ohsu.cslu.parser.spmv.SparseMatrixVectorParser;
import edu.ohsu.cslu.parser.spmv.SparseMatrixVectorParser.PackingFunctionType;
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
public class ParserDriver extends ThreadLocalLinewiseClTool<Parser<?>, ParseTask> {

    // Global vars to create parser
    public CellSelectorModel cellSelectorModel = LeftRightBottomTopTraversal.MODEL;
    public FigureOfMeritModel fomModel = null;// new InsideProb();
    Grammar grammar, coarseGrammar;
    static String commandLineArgStr = "";

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

    // == Input options ==
    @Option(name = "-if", metaVar = "FORMAT", usage = "Input format type.  Choosing 'text' will tokenize the input before parsing.")
    public InputFormat inputFormat = InputFormat.Token;

    @Option(name = "-maxLength", metaVar = "LEN", usage = "Skip sentences longer than LEN")
    int maxLength = 200;

    // == Output options ==
    @Option(name = "-printUNK", usage = "Print unknown words as their UNK replacement class")
    boolean printUnkLabels = false;

    @Option(name = "-oldUNK", hidden = true, usage = "Use old UNK function")
    public static boolean oldUNK = false;

    @Option(name = "-binary", usage = "Leave parse tree output in binary-branching form")
    public boolean binaryTreeOutput = false;

    // == Processing options ==
    @Option(name = "-decode", metaVar = "TYPE", hidden = true, usage = "Method to extract best tree from forest")
    public DecodeMethod decodeMethod = DecodeMethod.ViterbiMax;

    @Option(name = "-recovery", metaVar = "strategy", hidden = true, usage = "Recovery strategy in case of parse failure")
    public RecoveryStrategy recoveryStrategy = null;

    @Option(name = "-reparse", metaVar = "strategy or count", hidden = true, usage = "If no solution, loosen constraints and reparse using the specified strategy or double-beam-width n times")
    public ReparseStrategy reparseStrategy = ReparseStrategy.None;

    @Option(name = "-parseFromInputTags", hidden = true, usage = "Parse from input POS tags given by tagged or tree input.  Replaces 1-best tags from BoundaryInOut FOM if also specified.")
    public static boolean parseFromInputTags = false;

    @Option(name = "-inputTreeBeamRank", hidden = true, usage = "Print rank of input tree constituents during beam-search parsing.")
    public static boolean inputTreeBeamRank = false;

    @Option(name = "-fom", metaVar = "FOM", usage = "Figure-of-Merit edge scoring function (name or model file)")
    private String fomTypeOrModel = "Inside";

    @Option(name = "-pf", hidden = true, metaVar = "function", usage = "Packing function (only used for SpMV parsers)")
    private PackingFunctionType packingFunctionType = PackingFunctionType.PerfectHash;

    @Option(name = "-beamModel", metaVar = "FILE", usage = "Beam-width prediction model (Bodenstab et al., 2011)")
    private String beamModelFileName = null;

    @Option(name = "-head-rules", hidden = true, metaVar = "ruleset or file", usage = "Enables head-finding using a Charniak-style head-finding ruleset. Specify ruleset as 'charniak' or a rule file. Ignored if -binary is specified.")
    private String headRules = null;
    private HeadPercolationRuleset headPercolationRuleset = null;

    @Option(name = "-ngramOutsideModel", metaVar = "FILE", usage = "N-gram model for inside normalization.  Only needed for agenda parsers")
    private String ngramOutsideModelFileName = null;

    @Option(name = "-geometricInsideNorm", hidden = true, usage = "Use the geometric mean of the Inside score. Only needed for agenda parsers")
    public static boolean geometricInsideNorm = false;

    @Option(name = "-ccModel", hidden = true, metaVar = "FILE", usage = "CSLU Chart Constraints model (Roark and Hollingshead, 2008)")
    private String chartConstraintsModel = null;

    @Option(name = "-ccPrint", hidden = true, usage = "Print Cell Constraints for each input sentence and exit (no parsing done)")
    public static boolean chartConstraintsPrint = false;

    @Option(name = "-dcModel", hidden = true, metaVar = "FILE", usage = "Dependency constraints model file")
    private File dependencyConstraintsModel = null;

    @Option(name = "-help-long", usage = "List all research parsers and options")
    public boolean longHelp = false;

    @Option(name = "-debug", hidden = true, usage = "Exit on error with trace")
    public boolean debug = false;

    @Option(name = "-printFeatMap", hidden = true, usage = "Write lex/pos/nt feature strings and indicies for beam-width prediction and disc FOM to stdout.  Note this mapping must be identical for training and testing.")
    public boolean printFeatMap = false;

    // corpus stats
    private long parseStartTime;
    private volatile int sentencesParsed = 0, wordsParsed = 0, failedParses = 0, reparsedSentences = 0,
            totalReparses = 0;
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
        commandLineArgStr = Util.join(args, " ");
        run(args);
    }

    @Override
    // run once at initialization regardless of number of threads
    public void setup() throws Exception {

        if (this.longHelp) {

            BaseLogger.singleton().info("\nPossible values for -rp PARSER:");
            for (final ResearchParserType type : Parser.ResearchParserType.values()) {
                BaseLogger.singleton().info("\t" + type.toString());
            }
            // NB: Is there a way to print the entire properties file, comments and all?
            BaseLogger.singleton().info(
                    "\nDefault options using -O <key>=<value>:\n\t"
                            + GlobalConfigProperties.singleton().toString().replaceAll("\n", "\n\t"));
            System.exit(0);
        } else if (grammarFile == null && modelFile == null) {
            throw new IllegalArgumentException("-g GRAMMAR or -m MODEL is required");
        }

        // map simplified parser choices to the specific research version
        if (researchParserType == null) {
            researchParserType = parserType.researchParserType;
        }
        if (inputTreeBeamRank) {
            researchParserType = ResearchParserType.BSCPBeamConfTrain;
        }

        BaseLogger.singleton().info(
                "INFO: parser=" + researchParserType + " fom=" + fomTypeOrModel + " decode=" + decodeMethod);
        BaseLogger.singleton().info("INFO: " + commandLineArgStr);

        if (headRules != null) {
            if (headRules.equalsIgnoreCase("charniak")) {
                headPercolationRuleset = new CharniakHeadPercolationRuleset();
            } else {
                headPercolationRuleset = new HeadPercolationRuleset(new FileReader(headRules));
            }
        }

        if (modelFile != null) {
            final ObjectInputStream ois = new ObjectInputStream(fileAsInputStream(modelFile));
            @SuppressWarnings("unused")
            final String metadata = (String) ois.readObject();
            final ConfigProperties props = (ConfigProperties) ois.readObject();
            GlobalConfigProperties.singleton().mergeUnder(props);

            BaseLogger.singleton().finer("Reading grammar...");
            this.grammar = (Grammar) ois.readObject();

            BaseLogger.singleton().finer("Reading FOM...");
            fomModel = (FigureOfMeritModel) ois.readObject();

        } else {
            this.grammar = createGrammar(fileAsBufferedReader(grammarFile), researchParserType, packingFunctionType);

            if (printFeatMap) {
                Chart.printFeatMap(grammar);
                System.exit(1);
            }

            if (fomTypeOrModel.equals("Inside")) {
                fomModel = new InsideProb();
            } else if (fomTypeOrModel.equals("InsideWithFwdBkwd")) {
                // fomModel = new BoundaryInOut(FOMType.InsideWithFwdBkwd);
                throw new IllegalArgumentException("FOM InsideWithFwdBkwd no longer supported");
            } else if (new File(fomTypeOrModel).exists()) {
                // read first line and extract model type
                final BufferedReader tmp = fileAsBufferedReader(fomTypeOrModel);
                final HashMap<String, String> keyValue = Util.readKeyValuePairs(tmp.readLine().trim());
                tmp.close();

                if (!keyValue.containsKey("model")) {
                    throw new IllegalArgumentException(
                            "FOM model file has unexpected format.  Looking for 'model=' in first line.");
                }
                if (keyValue.get("model").equals("DiscriminativeFOM")) {
                    // Discriminative FOM
                    // fomModel = new DiscriminativeFOMLR(FOMType.Discriminative, grammar,
                    // fileAsBufferedReader(fomTypeOrModel));
                } else if (keyValue.get("model").equals("FOM") && keyValue.containsKey("type")) {
                    if (keyValue.get("type").equals("BoundaryInOut")) {
                        // BoundaryInOut FOM
                        Grammar fomGrammar = grammar;
                        if (this.coarseGrammarFile != null) {
                            coarseGrammar = new CoarseGrammar(coarseGrammarFile, this.grammar);
                            BaseLogger.singleton().fine("FOM coarse grammar stats: " + coarseGrammar.getStats());
                            fomGrammar = coarseGrammar;
                        }
                        fomModel = new BoundaryInOut(FOMType.BoundaryPOS, fomGrammar,
                                fileAsBufferedReader(fomTypeOrModel));
                    } else if (keyValue.get("type").equals("BoundaryLex")) {
                        fomModel = new BoundaryLex(FOMType.BoundaryLex, grammar, fileAsBufferedReader(fomTypeOrModel));
                    } else if (keyValue.get("type").equals("Prior")) {
                        fomModel = new PriorFOM(FOMType.Prior, grammar, fileAsBufferedReader(fomTypeOrModel));
                    } else {
                        throw new IllegalArgumentException("FOM model type '" + keyValue.get("type") + "' in file "
                                + fomTypeOrModel + "' not expected.");
                    }
                } else {
                    throw new IllegalArgumentException("Model value '" + keyValue.get("model") + "' in file "
                            + fomTypeOrModel + "' not expected.");
                }
            } else {
                throw new IllegalArgumentException("-fom value '" + fomTypeOrModel + "' not valid.");
            }

            if (chartConstraintsModel != null) {
                cellSelectorModel = new OHSUCellConstraintsModel(fileAsBufferedReader(chartConstraintsModel),
                        grammar.binarization() == Binarization.LEFT);
            } else if (dependencyConstraintsModel != null) {
                cellSelectorModel = new DepGraphCellSelectorModel(new FileReader(dependencyConstraintsModel));
            }

            if (beamModelFileName != null) {
                cellSelectorModel = new PerceptronBeamWidthModel(fileAsBufferedReader(beamModelFileName));
            }

            if (ngramOutsideModelFileName != null) {
                fomModel.ngramOutsideModel = new NGramOutside(grammar, fileAsBufferedReader(ngramOutsideModelFileName));
            }
        }

        BaseLogger.singleton().fine(grammar.getStats());

        parseStartTime = System.currentTimeMillis();
    }

    /**
     * Used by other tools
     */
    public static Grammar readGrammar(final String grammarFile, final ResearchParserType researchParserType,
            final PackingFunctionType packingFunctionType) throws Exception {
        // Handle gzipped and non-gzipped grammar files
        return createGrammar(new InputStreamReader(Util.file2inputStream(grammarFile)), researchParserType,
                packingFunctionType);
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
    public static Grammar createGrammar(final Reader grammarFile, final ResearchParserType researchParserType,
            final PackingFunctionType packingFunctionType) throws Exception {

        switch (researchParserType) {
        case ECPInsideOutside:
        case ECPCellCrossList:
            return new LeftListGrammar(grammarFile);

        case ECPCellCrossHashGrammarLoop:
        case ECPCellCrossHashGrammarLoop2:
        case ECPCellCrossHash:
            return new LeftHashGrammar(grammarFile);

        case ECPCellCrossMatrix:
            return new ChildMatrixGrammar(grammarFile);

        case ECPGrammarLoop:
        case ECPGrammarLoopBerkeleyFilter:
            return new ListGrammar(grammarFile);

        case AgendaParser:
        case APWithMemory:
        case APGhostEdges:
        case APDecodeFOM:
            return new LeftRightListsGrammar(grammarFile);

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
            return new LeftHashGrammar(grammarFile);

        case CsrSpmv:
        case GrammarParallelCsrSpmv:
            switch (packingFunctionType) {
            case Simple:
                return new CsrSparseMatrixGrammar(grammarFile, LeftShiftFunction.class);
            case PerfectHash:
                return new CsrSparseMatrixGrammar(grammarFile, PerfectIntPairHashPackingFunction.class);
            default:
                throw new IllegalArgumentException("Unsupported packing-function type: " + packingFunctionType);
            }

        case PackedOpenClSpmv:
        case DenseVectorOpenClSpmv:
            return new CsrSparseMatrixGrammar(grammarFile, LeftShiftFunction.class);

        case CscSpmv:
        case GrammarParallelCscSpmv:
            switch (packingFunctionType) {
            case Simple:
                return new LeftCscSparseMatrixGrammar(grammarFile, LeftShiftFunction.class);
            case PerfectHash:
                return new LeftCscSparseMatrixGrammar(grammarFile, PerfectIntPairHashPackingFunction.class);
            default:
                throw new IllegalArgumentException("Unsupported packing-function type: " + packingFunctionType);
            }

        case LeftChildMl:
        case CartesianProductBinarySearchMl:
        case CartesianProductBinarySearchLeftChildMl:
        case CartesianProductHashMl:
        case CartesianProductLeftChildHashMl:
            switch (packingFunctionType) {
            case Simple:
                return new LeftCscSparseMatrixGrammar(grammarFile, LeftShiftFunction.class);
            case Hash:
                return new LeftCscSparseMatrixGrammar(grammarFile, Int2IntHashPackingFunction.class);
            case PerfectHash:
                return new LeftCscSparseMatrixGrammar(grammarFile, PerfectIntPairHashPackingFunction.class);
            default:
                throw new IllegalArgumentException("Unsupported packing-function type: " + packingFunctionType);
            }
        case RightChildMl:
            return new RightCscSparseMatrixGrammar(grammarFile, LeftShiftFunction.class);
        case GrammarLoopMl:
            return new CsrSparseMatrixGrammar(grammarFile, LeftShiftFunction.class);
        case InsideOutsideCartesianProductHash:
            return new InsideOutsideCscSparseMatrixGrammar(grammarFile, PerfectIntPairHashPackingFunction.class);

        case ConstrainedCartesianProductHashMl:
            return new LeftCscSparseMatrixGrammar(grammarFile, LeftShiftFunction.class);

        default:
            throw new IllegalArgumentException("Unsupported parser type: " + researchParserType);
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
            return new ECPGrammarLoop(this, (ListGrammar) grammar);
        case ECPGrammarLoopBerkeleyFilter:
            return new ECPGrammarLoopBerkFilter(this, (ListGrammar) grammar);
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
        case BSCPBeamConfTrain:
            return new BSCPBeamConfTrain(this, (LeftHashGrammar) grammar, "");

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
        case InsideOutsideCartesianProductHash:
            return new InsideOutsideCphSpmlParser(this, (InsideOutsideCscSparseMatrixGrammar) grammar);

        case ConstrainedCartesianProductHashMl:
            cellSelectorModel = ConstrainedCellSelector.MODEL;
            return new ConstrainedCphSpmlParser(this, (LeftCscSparseMatrixGrammar) grammar);

        default:
            throw new IllegalArgumentException("Unsupported parser type");
        }
    }

    @Override
    protected FutureTask<ParseTask> lineTask(final String input) {
        return new FutureTask<ParseTask>(new Callable<ParseTask>() {

            @Override
            public ParseTask call() throws Exception {
                if (debug) {
                    return getLocal().parseSentence(input, recoveryStrategy);
                }
                try {
                    return getLocal().parseSentence(input, recoveryStrategy);
                } catch (final Exception e) {
                    BaseLogger.singleton().log(Level.SEVERE, e.toString());
                    return null;
                }
            }
        });
    }

    @Override
    protected void output(final ParseTask parseTask) {
        if (parseTask != null) {
            final StringBuilder output = new StringBuilder(512);
            output.append(parseTask.parseBracketString(binaryTreeOutput, printUnkLabels, headPercolationRuleset));
            try {
                parseTask.evaluate(evaluator);
                output.append(parseTask.statsString());
            } catch (final Exception e) {
                if (BaseLogger.singleton().isLoggable(Level.SEVERE)) {
                    output.append("\nERROR: Evaluation failed: " + e.toString());
                }
                output.append(parseTask.statsString());
            }
            System.out.println(output.toString());
            sentencesParsed++;
            wordsParsed += parseTask.sentenceLength();
            if (parseTask.parseFailed()) {
                failedParses++;
            } else if (parseTask.reparseStages > 0) {
                reparsedSentences++;
            }
            totalReparses += parseTask.reparseStages;
        } else {
            System.out.println("()");
        }
    }

    @Override
    protected void cleanup() {
        final float parseTime = (System.currentTimeMillis() - parseStartTime) / 1000f;

        // If the individual parser configured a thread count (e.g. CellParallelCsrSpmvParser), compute
        // CPU-time using that thread count; otherwise, assume maxThreads is correct
        final int threads = GlobalConfigProperties.singleton().containsKey(OPT_CONFIGURED_THREAD_COUNT) ? GlobalConfigProperties
                .singleton().getIntProperty(OPT_CONFIGURED_THREAD_COUNT) : maxThreads;

        // Note that this CPU-time computation does not include GC time
        final float cpuTime = parseTime * threads;

        final StringBuilder sb = new StringBuilder();
        // TODO Add cpuSecondsPerSent
        sb.append(String
                .format("INFO: numSentences=%d numFail=%d reparsedSentences=%d totalReparses=%d totalSeconds=%.3f cpuSeconds=%.3f avgSecondsPerSent=%.3f wordsPerSec=%.3f",
                        sentencesParsed, failedParses, reparsedSentences, totalReparses, parseTime, cpuTime, cpuTime
                                / sentencesParsed, wordsParsed / cpuTime));

        if (!parserInstances.isEmpty() && parserInstances.getFirst() instanceof SparseMatrixVectorParser) {
            sb.append(String.format(" totalXProductTime=%d totalBinarySpMVTime=%d",
                    SparseMatrixVectorParser.totalCartesianProductTime, SparseMatrixVectorParser.totalBinarySpmvNs));
        }

        if (inputFormat == InputFormat.Tree) {
            final EvalbResult evalbResult = evaluator.accumulatedResult();
            sb.append(String.format(" f1=%.2f prec=%.2f recall=%.2f", evalbResult.f1() * 100,
                    evalbResult.precision() * 100, evalbResult.recall() * 100));
        }

        BaseLogger.singleton().info(sb.toString());

        for (final Parser<?> p : parserInstances) {
            try {
                p.shutdown();
            } catch (final Exception ignore) {
            }
        }
    }

    static public ParserDriver defaultTestOptions() {
        final ParserDriver opts = new ParserDriver();
        BaseLogger.singleton().setLevel(Level.FINER);
        return opts;
    }
}

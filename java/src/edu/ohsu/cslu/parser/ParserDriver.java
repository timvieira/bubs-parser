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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.nio.charset.Charset;
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
import edu.ohsu.cslu.grammar.ChildMatrixGrammar;
import edu.ohsu.cslu.grammar.ClusterTaggerTokenClassifier;
import edu.ohsu.cslu.grammar.CoarseGrammar;
import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.DecisionTreeTokenClassifier;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.InsideOutsideCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.LeftListGrammar;
import edu.ohsu.cslu.grammar.LeftRightListsGrammar;
import edu.ohsu.cslu.grammar.ListGrammar;
import edu.ohsu.cslu.grammar.RightCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SerializeModel;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.Int2IntHashPackingFunction;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.LeftShiftFunction;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashPackingFunction;
import edu.ohsu.cslu.grammar.TokenClassifier;
import edu.ohsu.cslu.parser.Parser.DecodeMethod;
import edu.ohsu.cslu.parser.Parser.InputFormat;
import edu.ohsu.cslu.parser.Parser.ParserType;
import edu.ohsu.cslu.parser.Parser.ReparseStrategy;
import edu.ohsu.cslu.parser.Parser.ResearchParserType;
import edu.ohsu.cslu.parser.cellselector.AdaptiveBeamModel;
import edu.ohsu.cslu.parser.cellselector.CellConstraintsComboModel;
import edu.ohsu.cslu.parser.cellselector.CellSelectorModel;
import edu.ohsu.cslu.parser.cellselector.CompleteClosureModel;
import edu.ohsu.cslu.parser.cellselector.LeftRightBottomTopTraversal;
import edu.ohsu.cslu.parser.cellselector.LimitedSpanTraversalModel;
import edu.ohsu.cslu.parser.cellselector.OHSUCellConstraintsModel;
import edu.ohsu.cslu.parser.cellselector.PerceptronBeamWidthModel;
import edu.ohsu.cslu.parser.chart.Chart.RecoveryStrategy;
import edu.ohsu.cslu.parser.fom.BoundaryLex;
import edu.ohsu.cslu.parser.fom.BoundaryPosModel;
import edu.ohsu.cslu.parser.fom.FigureOfMeritModel;
import edu.ohsu.cslu.parser.fom.FigureOfMeritModel.FOMType;
import edu.ohsu.cslu.parser.fom.InsideProb;
import edu.ohsu.cslu.parser.real.RealInsideOutsideCscSparseMatrixGrammar;
import edu.ohsu.cslu.parser.spmv.SparseMatrixVectorParser;
import edu.ohsu.cslu.parser.spmv.SparseMatrixVectorParser.PackingFunctionType;
import edu.ohsu.cslu.perceptron.AdaptiveBeamClassifier;
import edu.ohsu.cslu.perceptron.CompleteClosureClassifier;
import edu.ohsu.cslu.util.Evalb.BracketEvaluator;
import edu.ohsu.cslu.util.Evalb.EvalbResult;

/**
 * BUBS Parser
 * 
 * Parses input text and returns constituency parses
 * 
 * Input: one sentence per line, plain text or parenthesis-bracketed trees. BUBS will tokenize input text using the
 * standard treebank-tokenization mappings. It will attempt to detect input in bracketed tree format. Input trees are
 * assumed to be gold trees, and verbose output will include bracket evaluation against those gold trees (using a
 * reimplementation of PARSEVAL / evalb). Finally, for pre-tokenized text, see the '-if' option.
 * 
 * Output: parse trees, one tree per line. At higher verbosity levels (see '-v'), other information will be interspersed
 * between parse output lines.
 * 
 * ===================================
 * 
 * BUBS is a grammar-agnostic context-free constituency parser. Using a high-accuracy grammar (such as the Berkeley
 * latent-variable grammar), it achieves high accuracy and throughput superior to other state-of-the-art parsers.
 * 
 * Some parser implementations accept or require configuration options. All configuration is specified with the '-O'
 * option, using either '-O key=value' or '-O <properties file>' form. Multiple -O options are allowed. key=value
 * options override those found in property files. So, for example, you might use a property file containing most
 * configuration and override a single option with:
 * 
 * parse -O parser.properties -O foo=bar
 * 
 * The default options are:
 * 
 * <pre>
 * cellThreads : 1 
 * grammarThreads : 1 (see below for details on threading)
 * maxBeamWidth : 30
 * lexicalRowBeamWidth : 60
 * lexicalRowUnaries : 20
 * </pre>
 * 
 * These beam limits assume a boundary FOM and Beam Confidence Model (see below). maxBeamWidth applies to cells of span
 * > 1. For span-1 cells, we allow a larger beam width, and reserve some space for unary productions. These default
 * options are tuned on a WSJ development set. Parsing out-of-domain text may require wider beam widths.
 * 
 * 
 * == Multithreading ==
 * 
 * The BUBS parser supports threading at several levels. Sentence-level threading assigns each sentence of the input to
 * a separate thread as one becomes available). The number of threads is controlled by the '-xt <count>' option. In
 * general, if threading only at the sentence level, you want to use the same number of threads as CPU cores (or
 * slightly lower, to reserve some CPU capacity for OS or other simultaneous tasks).
 * 
 * Cell-level and grammar-level threading are also supported. Cell-level threading assigns the processing of individual
 * chart cells to threads (again, as threads become available in the thread pool).
 * 
 * Grammar-level threading subdivides the grammar intersection operation within an individual cell and splits those
 * tasks across threads.
 * 
 * Cell-level and grammar-level threading are specified with the 'cellThreads' and 'grammarThreads' options. e.g.:
 * 
 * parse -O cellThreads=4 -O grammarThreads=2
 * 
 * The three levels of threading can interact safely (i.e., you can use -xt, cellThreads, and grammarThreads
 * simultaneously), and we have shown that cell-level and grammar-level threading can provide additive benefits, but we
 * make no claims about the efficiency impact of combining sentence-level threading with other parallelization methods.
 * 
 * 
 * == Research Parser Implementations ==
 * 
 * In addition to the standard implementations described above, many other parsing algorithms are available using the
 * -researchParserType option. The general classes are:
 * 
 * <pre>
 * --Other exhaustive implementations (ECPxyz). All exhaustive implementations produce identical parses, but use various
 * grammar intersection methods and differ considerably in efficiency.
 * --Agenda parsers (APxyz).
 * --Beam Search parsers (BSCPxyz). Various methods of beam pruning.
 * --SpMV parsers (xyzSpmv). Sparse Matrix x Vector grammar intersection methods.
 * --Matrix Loop parsers (xyzMl). Exhaustive parsers using a matrix grammar representation and implement various methods
 * of iterating over the matrix during grammar intersection. These methods vary greatly in efficiency, from around 4-5
 * seconds up to several minutes per sentence.
 * </pre>
 * 
 * Implementation Note: {@link ParserDriver} is based on the cltool4j command-line tool infrastructure
 * (http://code.google.com/p/cltool4j/), which provides command-line handling, threading support, and input/output
 * infrastructure.
 * 
 * == Citing ==
 * 
 * If you use the BUBS parser in research, please cite:
 * 
 * Adaptive Beam-Width Prediction for Efficient CYK Parsing Nathan Bodenstab, Aaron Dunlop, Keith Hall, and Brian Roark
 * - ACL/HLT 2011, pages 440-449.
 * 
 * Further documentation is available at https://code.google.com/p/bubs-parser/
 * 
 * @author Nathan Bodenstab
 * @author Aaron Dunlop
 * @since 2009
 */
@Threadable(defaultThreads = 1)
public class ParserDriver extends ThreadLocalLinewiseClTool<Parser<?>, ParseTask> {

    //
    // Global parser configuration
    //
    public CellSelectorModel cellSelectorModel = LeftRightBottomTopTraversal.MODEL;
    public FigureOfMeritModel fomModel = null;

    // == Parser options ==
    @Option(name = "-p", metaVar = "parser type", optionalChoiceGroup = "parserType", usage = "Parser implementation")
    private ParserType parserType = ParserType.Matrix;

    /**
     * Exposes all possible parser implementations. The most useful parser implementations for end-users are exposed via
     * the '-p' option. This option exposes other implementations (most of which are intended for specific experiments).
     */
    @Option(name = "-rp", hidden = true, optionalChoiceGroup = "parserType", metaVar = "parser type", usage = "Research Parser implementation")
    public ResearchParserType researchParserType = null;

    // == Grammar options ==
    @Option(name = "-g", metaVar = "grammar file", choiceGroup = "grammar", usage = "Grammar file (text, gzipped text, or binary serialized)")
    private String grammarFile = null;

    /**
     * Required by some prioritization (FOM) models. Primarily for experimental usage - to this point, a coarse FOM has
     * not proven widely useful.
     */
    @Option(name = "-coarseGrammar", hidden = true, metaVar = "model file", usage = "Coarse grammar file (text, gzipped text, or binary serialized)")
    private String coarseGrammarFile = null;

    /** A single model, serialized with {@link SerializeModel} */
    @Option(name = "-m", metaVar = "model file", choiceGroup = "grammar", usage = "Combined model file, combining grammar and pruning models (binary serialized)")
    private File modelFile = null;

    // == Input options ==
    @Option(name = "-if", metaVar = "format type", usage = "Input format type.  Choosing 'text' will tokenize the input before parsing.")
    public InputFormat inputFormat = InputFormat.Text;

    @Option(name = "-maxLength", metaVar = "length", usage = "Skip sentences longer than length")
    int maxLength = 200;

    // == Output options ==
    @Option(name = "-printUNK", optionalChoiceGroup = "UNK", usage = "Print unknown words as their UNK replacement class")
    boolean printUnkLabels = false;

    @Option(name = "-addUNK", optionalChoiceGroup = "UNK", usage = "Add the UNK replacement class to any unknown words (in the form 'UNK-class|token'")
    boolean addUnkLabels = false;

    /**
     * Used primarily in model training (e.g., to produce gold-constrained parses). Note: outputting binary tree
     * structures requires that factored labels are retained as well.
     */
    @Option(name = "-binary", optionalChoiceGroup = "binary", usage = "Leave parse tree output in binary-branching form")
    public boolean binaryTreeOutput = false;

    // == Processing options ==
    /**
     * Most alternate decoding methods depend on inside-outside inference. See the '-p IO' option.
     */
    @Option(name = "-decode", metaVar = "method", hidden = true, usage = "Method to extract best tree from forest")
    public DecodeMethod decodeMethod = DecodeMethod.ViterbiMax;

    /**
     * Provides a fallback in case of parse failures. Currently, the only implemented strategy is biased for
     * right-branching languages, and combines all completed subtrees at the right periphery.
     */
    @Option(name = "-recovery", metaVar = "strategy", hidden = true, usage = "Recovery strategy in case of parse failure")
    public RecoveryStrategy recoveryStrategy = null;

    /**
     * Most parsing consumers use approximate inference. In some cases, severe pruning can lead to parse failures that
     * would be avoided with looser pruning constraints. By default, we escalate through several levels of pruning
     * before finally performing exhaustive inference. Note: re-parsing runs can be expensive, particularly if
     * exhaustive inference is required. Applications with hard latency constraints may prefer to fail (or fall back to
     * a recovery mode, as in the '-recovery' option) rather than incurring this expense.
     */
    @Option(name = "-reparse", metaVar = "strategy", hidden = true, usage = "If no solution, loosen constraints and reparse using the specified strategy or double-beam-width n times")
    public ReparseStrategy reparseStrategy = ReparseStrategy.Escalate;

    @Option(name = "-parseFromInputTags", hidden = true, usage = "Parse from input POS tags given by tagged or tree input.  Replaces 1-best tags from BoundaryInOut FOM if also specified.")
    public static boolean parseFromInputTags = false;

    @Option(name = "-fom", metaVar = "model or type", usage = "Figure-of-Merit edge scoring function ('Inside', 'InsideWithFwdBkwd' or model file)")
    private String fomTypeOrModel = "Inside";

    @Option(name = "-pf", hidden = true, metaVar = "function", usage = "Packing function (only used for SpMV parsers)")
    private PackingFunctionType packingFunctionType = PackingFunctionType.PerfectHash;

    // TODO Remove - obsoleted by -abModel
    @Option(name = "-beamModel", metaVar = "model file", usage = "Beam-width prediction model (Bodenstab et al., 2011)")
    private String beamModelFileName = null;

    /** This option is largely obsoleted by '-ccClassifier' and '-abModel' */
    @Option(name = "-ccModel", hidden = true, metaVar = "model file", usage = "CSLU Chart Constraints model (Roark and Hollingshead, 2008)")
    private String chartConstraintsModel = null;

    /**
     * Complete closure classifier (as described in Bodenstab et al., 2011,
     * "Beam-Width Prediction for Efficient Context-Free Parsing"). These models are trained using
     * {@link CompleteClosureClassifier}.
     */
    @Option(name = "-ccClassifier", hidden = true, metaVar = "model file", usage = "Complete closure classifier model (Java Serialized)")
    private File completeClosureClassifierFile = null;

    /**
     * Adaptive beam-width model (as described in Bodenstab et al., 2011,
     * "Beam-Width Prediction for Efficient Context-Free Parsing"). These models are trained using
     * {@link AdaptiveBeamClassifier}.
     */
    @Option(name = "-abModel", hidden = true, metaVar = "model file", usage = "Adaptive-beam model (Java Serialized)")
    private File adaptiveBeamModelFile = null;

    // Leaving this around for a bit, in case we get back to limited-span parsing, but it doesn't work currently
    // @Option(name = "-lsccModel", hidden = true,
    // metaVar = "FILE", usage = "CSLU Chart Constraints model (Roark and Hollingshead, 2008)")
    // private String limitedSpanChartConstraintsModel = null;

    // TODO Document, and remove other options as appropriate. Make -ccClassifier, -abModel aliases, handle multiple
    // models properly
    @Option(name = "-pm", hidden = true, metaVar = "model file", usage = "Cell selector model file")
    private File[] pruningModels = null;

    /**
     * Classifies unknown or rare tokens. Used primarily for research, as the token clustering approaches don't seem to
     * work all that well
     */
    @Option(name = "-tcModel", hidden = true, metaVar = "model file", usage = "Token classifier model file")
    private File tokenClassifierModel = null;

    /**
     * Parses with a 'hedge' grammar, limiting the span of subconstituents (and combining those limited-span parses
     * using a heuristic approximation)
     */
    @Option(name = "-maxSubtreeSpan", hidden = true, metaVar = "span", usage = "Maximum subtree span for limited-depth parsing")
    private int maxSubtreeSpan;

    /**
     * Specifies a ruleset and performs head-finding (thus labeling dependency structure as well as constituency). This
     * head-finding approach is fairly simplistic, but much faster than the more accurate Stanford parser approach.
     */
    @Option(name = "-head-rules", hidden = true, optionalChoiceGroup = "binary", metaVar = "ruleset", usage = "Enables head-finding using a Charniak-style head-finding ruleset. Specify ruleset as 'charniak' or a rule file.")
    private String headRules = null;
    private HeadPercolationRuleset headPercolationRuleset = null;

    // TODO Remove - this option is obsolete
    @Option(name = "-ccPrint", hidden = true, usage = "Print Cell Constraints for each input sentence and exit (no parsing done)")
    public static boolean chartConstraintsPrint = false;

    @Option(name = "-debug", hidden = true, usage = "Exit on error with trace (by default, a parse error outputs '()' and continues)")
    public boolean debug = false;

    /**
     * Specifies the number of cell-level threads. We handle threading at three levels; threading per-sentence is
     * handled by the command-line tool infrastructure and specified with the standard '-xt' parameter. Cell-level and
     * grammar-level threading are handled by the parser instance and specified with this option and with
     * {@link #OPT_GRAMMAR_THREAD_COUNT}.
     */
    public final static String OPT_CELL_THREAD_COUNT = "cellThreads";

    /**
     * Specifies the number of grammar-level threads. We handle threading at three levels; threading per-sentence is
     * handled by the command-line tool infrastructure and specified with the standard '-xt' parameter. Cell-level and
     * grammar-level threading are handled by the parser instance and specified with this option and with
     * {@link #OPT_CELL_THREAD_COUNT}.
     */
    public final static String OPT_GRAMMAR_THREAD_COUNT = "grammarThreads";

    /**
     * The number of row-level or cell-level threads actually used. In some cases the number of threads requested is
     * impractical (e.g., if it is greater than the maximum number of cells in a row or greater than the number of
     * grammar rows). {@link Parser} instances which make use of {@link #OPT_GRAMMAR_THREAD_COUNT} should populate this
     * property to indicate the number of threads actually used. Among other potential uses, this allows
     * {@link #cleanup()} to report accurate timing information.
     */
    public final static String RUNTIME_CONFIGURED_THREAD_COUNT = "actualThreads";

    /**
     * Specifies the comparator class used to order non-terminals. Implementations are in {@link SparseMatrixGrammar}.
     * The default is "PosEmbeddedComparator". Other valid values are "PosFirstComparator", "LexicographicComparator".
     */
    public final static String OPT_NT_COMPARATOR_CLASS = "ntComparatorClass";

    /**
     * Enables complete categories above the span limit (when limiting span-length with -maxSubtreeSpan). By default,
     * only incomplete (factored) categories are allowed when L < span < n.
     */
    public final static String OPT_ALLOW_COMPLETE_ABOVE_SPAN_LIMIT = "allowCompleteAboveSpanLimit";

    /**
     * Enables bracket evaluation of parse failures (i.e., penalizing recall in the event of a parse failure). We
     * default to ignoring empty parses (and reporting them separately), to match the behavior of Collins' standard
     * <code>evalb</code> tool. But in some cases, including those failures directly in the F1 measure is useful. Note:
     * This option is ignored when parsing from input other than gold trees.
     */
    public final static String OPT_EVAL_PARSE_FAILURES = "evalParseFailures";

    /**
     * Skip log-sum operations if the log probabilities differ by more than x. Default is 16 (approximately the
     * resolution of a 32-bit IEEE float).
     */
    public final static String OPT_LOG_SUM_DELTA = "logSumDelta";

    /**
     * Use quantized approximations of the log and exp functions when performing log-sum operations. Approximations are
     * based on the IEEE floating-point representation, as described in Scraudolph, 1999
     * "A fast, compact approximation of the exponential function". Boolean property.
     */
    public final static String OPT_APPROXIMATE_LOG_SUM = "approxLogSum";

    /**
     * Compute the inside score only. Decode assuming all outside probabilities are 1. Note - in preliminary trials,
     * this method doesn't appear to work all that well. Boolean property.
     */
    public final static String OPT_INSIDE_ONLY = "insideOnly";

    /**
     * Use the prioritization / FOM model's estimate of outside probabilities (eliminating the outside pass). Note - in
     * preliminary trials, this method doesn't appear to work all that well. Boolean property.
     */
    public final static String OPT_HEURISTIC_OUTSIDE = "heuristicOutside";

    /** Disables factored-only classification in {@link AdaptiveBeamModel}. Boolean property. */
    public final static String OPT_DISABLE_FACTORED_ONLY_CLASSIFIER = "disableFactoredOnlyClassifier";

    /** Disables unary-constraint classification in {@link AdaptiveBeamModel}. Boolean property. */
    public final static String OPT_DISABLE_UNARY_CLASSIFIER = "disableUnaryClassifier";

    //
    // Corpus-wide statistics and timings
    //
    private long parseStartTime;
    private volatile int sentencesParsed = 0, wordsParsed = 0, failedParses = 0, reparsedSentences = 0,
            totalReparses = 0;

    //
    // ParserDriver state
    //
    private Grammar grammar;
    private LinkedList<Parser<?>> parserInstances = new LinkedList<Parser<?>>();
    private final BracketEvaluator evaluator = new BracketEvaluator();

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

        BaseLogger.singleton().info(
                "INFO: parser=" + researchParserType + " fom=" + fomTypeOrModel + " decode=" + decodeMethod);
        BaseLogger.singleton().info("INFO: " + commandLineArguments());

        if (headRules != null) {
            if (headRules.equalsIgnoreCase("charniak")) {
                headPercolationRuleset = new CharniakHeadPercolationRuleset();
            } else {
                headPercolationRuleset = new HeadPercolationRuleset(new FileReader(headRules));
            }
        }

        final TokenClassifier tokenClassifier = tokenClassifierModel != null ? new ClusterTaggerTokenClassifier(
                tokenClassifierModel) : new DecisionTreeTokenClassifier();

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
            this.grammar = createGrammar(fileAsBufferedReader(grammarFile), researchParserType, tokenClassifier,
                    packingFunctionType);

            if (fomTypeOrModel.equals("Inside")) {
                fomModel = new InsideProb();

            } else if (fomTypeOrModel.equals("InsideWithFwdBkwd")) {
                // fomModel = new BoundaryInOut(FOMType.InsideWithFwdBkwd);
                throw new IllegalArgumentException("FOM InsideWithFwdBkwd no longer supported");

            } else if (new File(fomTypeOrModel).exists()) {
                fomModel = readFomModel(fomTypeOrModel, coarseGrammarFile, grammar);

            } else {
                throw new IllegalArgumentException("-fom value '" + fomTypeOrModel + "' not valid.");
            }

            boolean defaultCellSelector = true;
            OHSUCellConstraintsModel cellConstraints = null;
            if (chartConstraintsModel != null) {
                cellConstraints = new OHSUCellConstraintsModel(fileAsBufferedReader(chartConstraintsModel), null);
                cellSelectorModel = cellConstraints;
                defaultCellSelector = false;
            }

            PerceptronBeamWidthModel beamConstraints = null;
            if (adaptiveBeamModelFile != null) {
                cellSelectorModel = new AdaptiveBeamModel(adaptiveBeamModelFile, grammar, defaultCellSelector ? null
                        : cellSelectorModel);
                defaultCellSelector = false;

            } else if (beamModelFileName != null) {
                beamConstraints = defaultCellSelector ? new PerceptronBeamWidthModel(
                        fileAsBufferedReader(beamModelFileName), null) : new PerceptronBeamWidthModel(
                        fileAsBufferedReader(beamModelFileName), cellSelectorModel);
                cellSelectorModel = beamConstraints;

            } else if (pruningModels != null && pruningModels.length > 0) {
                final ObjectInputStream ois = new ObjectInputStream(fileAsInputStream(pruningModels[0]));
                cellSelectorModel = (CellSelectorModel) ois.readObject();
                ois.close();
                cellSelectorModel = beamConstraints;
                defaultCellSelector = false;
            }

            if (cellConstraints != null && beamConstraints != null) {
                final CellConstraintsComboModel constraintsCombo = new CellConstraintsComboModel();
                constraintsCombo.addModel(cellConstraints);
                constraintsCombo.addModel(beamConstraints);
                cellSelectorModel = constraintsCombo;
                defaultCellSelector = false;
            } else if (maxSubtreeSpan != 0) {
                cellSelectorModel = defaultCellSelector ? new LimitedSpanTraversalModel(maxSubtreeSpan, null)
                        : new LimitedSpanTraversalModel(maxSubtreeSpan, cellSelectorModel);
                defaultCellSelector = false;
            }

            if (completeClosureClassifierFile != null) {
                cellSelectorModel = defaultCellSelector ? new CompleteClosureModel(completeClosureClassifierFile, null)
                        : new CompleteClosureModel(completeClosureClassifierFile, cellSelectorModel);
                if (((CompleteClosureModel) cellSelectorModel).binarization() != grammar.binarization()) {
                    throw new IllegalArgumentException("Binarization mismatch: Grammar binarization = "
                            + grammar.binarization() + " and complete-closure model was trained for "
                            + ((CompleteClosureModel) cellSelectorModel).binarization());
                }
            }
        }

        BaseLogger.singleton().fine(grammar.getStats());

        parseStartTime = System.currentTimeMillis();
    }

    public static FigureOfMeritModel readFomModel(final String fomModel, final String coarseGrammarFile,
            final Grammar grammar) throws IOException {

        // read first line and extract model type
        final BufferedReader tmp = fileAsBufferedReader(fomModel, Charset.defaultCharset());
        final HashMap<String, String> keyValue = Util.readKeyValuePairs(tmp.readLine().trim());
        tmp.close();

        if (!keyValue.containsKey("type")) {
            throw new IllegalArgumentException(
                    "FOM model file has unexpected format.  Looking for 'type=' in first line.");
        }
        final String fomType = keyValue.get("type");

        if (fomType.equals("BoundaryInOut")) {
            Grammar fomGrammar = grammar;
            if (coarseGrammarFile != null) {
                fomGrammar = new CoarseGrammar(coarseGrammarFile, grammar);
                BaseLogger.singleton().fine("FOM coarse grammar stats: " + fomGrammar.getStats());
            }
            return new BoundaryPosModel(FOMType.BoundaryPOS, fomGrammar, fileAsBufferedReader(fomModel,
                    Charset.defaultCharset()));

        } else if (fomType.equals("BoundaryLex")) {
            return new BoundaryLex(FOMType.BoundaryLex, grammar, fileAsBufferedReader(fomModel,
                    Charset.defaultCharset()));

        } else {
            throw new IllegalArgumentException("FOM model type '" + fomType + "' in file " + fomModel
                    + "' not expected.");
        }
    }

    /**
     * Reads in a grammar from a file and creates a {@link Grammar} instance of the appropriate class for the specified
     * parser type.
     * 
     * @param grammarFile
     * @param parserType
     * @return a {@link Grammar} instance appropriate for the specified parser type
     * @throws IOException
     */
    public static Grammar readGrammar(final String grammarFile, final ResearchParserType parserType,
            final PackingFunctionType packingFunctionType) throws IOException {
        // Handle gzipped and non-gzipped grammar files
        return createGrammar(fileAsBufferedReader(grammarFile, Charset.forName("UTF-8")), parserType,
                new DecisionTreeTokenClassifier(), packingFunctionType);
    }

    /**
     * Reads in a grammar from a file and creates a {@link Grammar} instance of the appropriate class for the specified
     * parser type.
     * 
     * @param grammarFile
     * @param parserType
     * @param tokenClassifier Type of token-classifier (e.g. decision-tree or tagger)
     * @return a {@link Grammar} instance appropriate for the specified parser type
     * @throws IOException
     */
    public static Grammar createGrammar(final Reader grammarFile, final ResearchParserType parserType,
            final TokenClassifier tokenClassifier, final PackingFunctionType packingFunctionType) throws IOException {

        switch (parserType) {
        case ECPInsideOutside:
        case ECPCellCrossList:
            return new LeftListGrammar(grammarFile, tokenClassifier);

        case ECPCellCrossHashGrammarLoop:
        case ECPCellCrossHashGrammarLoop2:
        case ECPCellCrossHash:
            return new LeftHashGrammar(grammarFile, tokenClassifier);

        case ECPCellCrossMatrix:
            return new ChildMatrixGrammar(grammarFile, tokenClassifier);

        case ECPGrammarLoop:
        case ECPGrammarLoopBerkeleyFilter:
            return new ListGrammar(grammarFile, tokenClassifier);

        case AgendaParser:
        case APWithMemory:
        case APGhostEdges:
        case APDecodeFOM:
            return new LeftRightListsGrammar(grammarFile, tokenClassifier);

        case BeamSearchChartParser:
        case BSCPSplitUnary:
        case BSCPPruneViterbi:
        case BSCPOnlineBeam:
        case BSCPBoundedHeap:
        case BSCPExpDecay:
        case BSCPPerceptronCell:
        case BSCPFomDecode:
        case CoarseCellAgenda:
        case CoarseCellAgendaCSLUT:
            return new LeftHashGrammar(grammarFile, tokenClassifier);

        case CsrSpmv:
        case GrammarParallelCsrSpmv:
            switch (packingFunctionType) {
            case Simple:
                return new CsrSparseMatrixGrammar(grammarFile, tokenClassifier, LeftShiftFunction.class);
            case PerfectHash:
                return new CsrSparseMatrixGrammar(grammarFile, tokenClassifier, PerfectIntPairHashPackingFunction.class);
            default:
                throw new IllegalArgumentException("Unsupported packing-function type: " + packingFunctionType);
            }

        case PackedOpenClSpmv:
        case DenseVectorOpenClSpmv:
            return new CsrSparseMatrixGrammar(grammarFile, tokenClassifier, LeftShiftFunction.class);

        case CscSpmv:
        case GrammarParallelCscSpmv:
            switch (packingFunctionType) {
            case Simple:
                return new LeftCscSparseMatrixGrammar(grammarFile, tokenClassifier, LeftShiftFunction.class);
            case PerfectHash:
                return new LeftCscSparseMatrixGrammar(grammarFile, tokenClassifier,
                        PerfectIntPairHashPackingFunction.class);
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
                return new LeftCscSparseMatrixGrammar(grammarFile, tokenClassifier, LeftShiftFunction.class);
            case Hash:
                return new LeftCscSparseMatrixGrammar(grammarFile, tokenClassifier, Int2IntHashPackingFunction.class);
            case PerfectHash:
                return new LeftCscSparseMatrixGrammar(grammarFile, tokenClassifier,
                        PerfectIntPairHashPackingFunction.class);
            default:
                throw new IllegalArgumentException("Unsupported packing-function type: " + packingFunctionType);
            }
        case RightChildMl:
            return new RightCscSparseMatrixGrammar(grammarFile, tokenClassifier, LeftShiftFunction.class);
        case GrammarLoopMl:
            return new CsrSparseMatrixGrammar(grammarFile, tokenClassifier, LeftShiftFunction.class);
        case InsideOutsideCartesianProductHash:
        case ViterbiInOutCph:
            return new InsideOutsideCscSparseMatrixGrammar(grammarFile, tokenClassifier,
                    PerfectIntPairHashPackingFunction.class);

        case ConstrainedCartesianProductHashMl:
            // Don't restrict the beam for constrained parsing
            GlobalConfigProperties.singleton().setProperty(Parser.PROPERTY_MAX_BEAM_WIDTH, "0");
            return new LeftCscSparseMatrixGrammar(grammarFile, tokenClassifier, LeftShiftFunction.class);

        case RealInsideOutsideCartesianProductHash:
            return new RealInsideOutsideCscSparseMatrixGrammar(grammarFile, tokenClassifier);

        default:
            throw new IllegalArgumentException("Unsupported parser type: " + parserType);
        }
    }

    @Override
    public Parser<?> createLocal() {
        try {
            // Apply specialized cell-selector model when appropriate (e.g., constrained parsing)
            final CellSelectorModel csm = researchParserType.cellSelectorModel();
            if (csm != null) {
                cellSelectorModel = csm;
            }

            // Construct an instance of the appropriate parser class
            @SuppressWarnings("unchecked")
            final Constructor<Parser<?>> c = (Constructor<Parser<?>>) Class.forName(researchParserType.classname())
                    .getConstructor(ParserDriver.class, grammar.getClass());

            final Parser<?> parser = c.newInstance(this, grammar);
            // Each thread creates its own parser instance. We need to maintain (and protect) a master list that we'll
            // use to shut them down in cleanup()
            synchronized (parserInstances) {
                parserInstances.add(parser);
            }
            return parser;

        } catch (final Exception e) {
            throw new IllegalArgumentException("Unsupported parser type: " + e.toString());
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
        // We'll count the sentence even if it failed with an exception (and record it as failed below). However, we
        // don't currently include the words of such sentences in wordsParsed. That's a bit of an inconsistency, but
        // it's OK for now.
        sentencesParsed++;
        if (parseTask != null) {
            final StringBuilder output = new StringBuilder(512);

            if (addUnkLabels) {
                output.append(parseTask.parseBracketString(binaryTreeOutput, true, true, headPercolationRuleset));
            } else {
                output.append(parseTask.parseBracketString(binaryTreeOutput, printUnkLabels, false,
                        headPercolationRuleset));
            }

            try {
                parseTask.evaluate(evaluator);
            } catch (final Exception e) {
                if (BaseLogger.singleton().isLoggable(Level.SEVERE)) {
                    output.append("\nERROR: Evaluation failed: " + e.toString());
                }
            }

            if (BaseLogger.singleton().isLoggable(Level.FINE)) {
                output.append(parseTask.statsString());
            }

            System.out.println(output.toString());
            wordsParsed += parseTask.sentenceLength();
            if (parseTask.parseFailed()) {
                failedParses++;
            } else if (parseTask.reparseStages > 0) {
                reparsedSentences++;
            }
            totalReparses += parseTask.reparseStages;

        } else {
            failedParses++;
            System.out.println("()");
        }
    }

    @Override
    protected void cleanup() {
        final float parseTime = (System.currentTimeMillis() - parseStartTime) / 1000f;

        // If the individual parser configured a thread count (e.g. CellParallelCsrSpmvParser), compute
        // CPU-time using that thread count; otherwise, assume maxThreads is correct
        final int threads = GlobalConfigProperties.singleton().containsKey(RUNTIME_CONFIGURED_THREAD_COUNT) ? GlobalConfigProperties
                .singleton().getIntProperty(RUNTIME_CONFIGURED_THREAD_COUNT) : maxThreads;

        // Note that this CPU-time computation does not include GC time
        final float cpuTime = parseTime * threads;

        final StringBuilder sb = new StringBuilder();
        sb.append(String
                .format("INFO: numSentences=%d numFail=%d reparsedSentences=%d totalReparses=%d totalSeconds=%.3f cpuSeconds=%.3f avgSecondsPerSent=%.3f wordsPerSec=%.3f",
                        sentencesParsed, failedParses, reparsedSentences, totalReparses, parseTime, cpuTime, parseTime
                                / sentencesParsed, wordsParsed / parseTime));

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

        // Synchronize again, just to be sure we don't somehow try to add a new instance during cleanup. It should be
        // rare, but the (usually) uncontested sync is cheap.
        synchronized (parserInstances) {
            for (final Parser<?> p : parserInstances) {
                try {
                    p.shutdown();
                } catch (final Exception ignore) {
                }
            }
        }
    }

    /**
     * Sets the grammar. Used when embedding BUBS into an independent system (see {@link EmbeddedExample}).
     * 
     * @param g Grammar
     */
    public void setGrammar(final Grammar g) {
        this.grammar = g;
    }

    static public ParserDriver defaultTestOptions() {
        final ParserDriver opts = new ParserDriver();
        BaseLogger.singleton().setLevel(Level.FINER);
        return opts;
    }
}

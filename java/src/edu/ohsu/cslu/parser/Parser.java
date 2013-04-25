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

import java.util.logging.Level;

import cltool4j.BaseCommandlineTool;
import cltool4j.BaseLogger;
import cltool4j.args4j.EnumAliasMap;
import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.lela.ConstrainedCellSelector;
import edu.ohsu.cslu.parser.agenda.APDecodeFOM;
import edu.ohsu.cslu.parser.agenda.APGhostEdges;
import edu.ohsu.cslu.parser.agenda.APWithMemory;
import edu.ohsu.cslu.parser.agenda.AgendaParser;
import edu.ohsu.cslu.parser.agenda.CoarseCellAgendaParser;
import edu.ohsu.cslu.parser.beam.BSCPBeamPredictTrain;
import edu.ohsu.cslu.parser.beam.BSCPBoundedHeap;
import edu.ohsu.cslu.parser.beam.BSCPExpDecay;
import edu.ohsu.cslu.parser.beam.BSCPFomDecode;
import edu.ohsu.cslu.parser.beam.BSCPPruneViterbi;
import edu.ohsu.cslu.parser.beam.BSCPSplitUnary;
import edu.ohsu.cslu.parser.beam.BSCPWeakThresh;
import edu.ohsu.cslu.parser.beam.BeamSearchChartParser;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.parser.cellselector.CellSelectorModel;
import edu.ohsu.cslu.parser.chart.Chart.RecoveryStrategy;
import edu.ohsu.cslu.parser.ecp.ECPCellCrossHash;
import edu.ohsu.cslu.parser.ecp.ECPCellCrossHashGrammarLoop;
import edu.ohsu.cslu.parser.ecp.ECPCellCrossHashGrammarLoop2;
import edu.ohsu.cslu.parser.ecp.ECPCellCrossList;
import edu.ohsu.cslu.parser.ecp.ECPCellCrossMatrix;
import edu.ohsu.cslu.parser.ecp.ECPGrammarLoop;
import edu.ohsu.cslu.parser.ecp.ECPGrammarLoopBerkFilter;
import edu.ohsu.cslu.parser.ecp.ECPInsideOutside;
import edu.ohsu.cslu.parser.fom.FigureOfMeritModel.FigureOfMerit;
import edu.ohsu.cslu.parser.ml.CartesianProductBinarySearchLeftChildSpmlParser;
import edu.ohsu.cslu.parser.ml.CartesianProductBinarySearchSpmlParser;
import edu.ohsu.cslu.parser.ml.CartesianProductHashSpmlParser;
import edu.ohsu.cslu.parser.ml.CartesianProductLeftChildHashSpmlParser;
import edu.ohsu.cslu.parser.ml.ConstrainedCphSpmlParser;
import edu.ohsu.cslu.parser.ml.GrammarLoopSpmlParser;
import edu.ohsu.cslu.parser.ml.InsideOutsideCphSpmlParser;
import edu.ohsu.cslu.parser.ml.LeftChildLoopSpmlParser;
import edu.ohsu.cslu.parser.ml.RightChildLoopSpmlParser;
import edu.ohsu.cslu.parser.ml.ViterbiInOutCphSpmlParser;
import edu.ohsu.cslu.parser.spmv.CscSpmvParser;
import edu.ohsu.cslu.parser.spmv.CsrSpmvParser;
import edu.ohsu.cslu.parser.spmv.GrammarParallelCscSpmvParser;
import edu.ohsu.cslu.parser.spmv.GrammarParallelCsrSpmvParser;

/**
 * Implements common data structures and operations shared by all parser implementations. Child classes implement
 * including various context-free parsing methods, including exhaustive and pruned CYK algorithms and several forms of
 * agenda parsing.
 * 
 * Note that Parser instances should be reused for multiple sentences, but are not expected to be thread-safe or
 * reentrant, so a separate Parser instance should be created for each thread if parsing multiple sentences in parallel.
 * 
 * Some Parser implementations are threaded internally, using multiple threads to speed parsing of a single sentence
 * (e.g. {@link CscSpmvParser}, {@link GrammarParallelCscSpmvParser}, {@link GrammarParallelCsrSpmvParser}).
 * 
 * Important methods and implementation notes:
 * <ul>
 * <li>The primary entry point is {@link #parseSentence(String)}, which expects a single sentence, with words delimited
 * by spaces.
 * <li>If a subclass allocates persistent resources which will not be cleaned up by normal garbage collection (e.g. a
 * thread pool), it should override {@link #shutdown()} to release those resources.
 * </ul>
 */
public abstract class Parser<G extends Grammar> {

    public final G grammar;

    /** Local cell beam width (0 = exhaustive) */
    public final static String PROPERTY_MAX_BEAM_WIDTH = "maxBeamWidth";

    /** The lexical (span-1) row often needs a larger beam width */
    public final static String PROPERTY_LEXICAL_ROW_BEAM_WIDTH = "lexicalRowBeamWidth";

    /** Reserve a portion of the lexical row entries for unary productions (span-1 constituents) */
    public final static String PROPERTY_LEXICAL_ROW_UNARIES = "lexicalRowUnaries";

    /** The maximum differential (in log inside probability) between the most and least probable constituents in a cell */
    public final static String PROPERTY_MAX_LOCAL_DELTA = "maxLocalDelta";

    /** Tunes maxc chart decoding between max-recall (0) and max-precision (1) */
    public final static String PROPERTY_MAXC_LAMBDA = "maxcLambda";

    /** Parser configuration */
    public ParserDriver opts;

    // TODO Make this reference final (once we work around the hack in CellChart)
    public FigureOfMerit figureOfMerit;
    public final CellSelector cellSelector;

    /**
     * True if we're collecting detailed counts of cell populations, cartesian-product sizes, etc. Set from the
     * command-line (using the '-v' option of {@link BaseCommandlineTool}, but it is duplicated here as a final
     * variable, so that the JIT can eliminate potentially-expensive counting code when we don't need it.
     */
    protected final boolean collectDetailedStatistics;

    public Parser(final ParserDriver opts, final G grammar) {
        this.grammar = grammar;
        this.opts = opts;
        this.figureOfMerit = opts.fomModel != null ? opts.fomModel.createFOM() : null;
        this.cellSelector = opts.cellSelectorModel.createCellSelector();

        this.collectDetailedStatistics = BaseLogger.singleton().isLoggable(Level.FINER);
    }

    public abstract float getInside(int start, int end, int nt);

    public abstract float getOutside(int start, int end, int nt);

    public abstract String getStats();

    protected abstract BinaryTree<String> findBestParse(ParseTask parseTask);

    /**
     * Waits until all active parsing tasks have completed. Intended for multi-threaded parsers (e.g.
     * {@link CsrSpmvParser}, {@link CscSpmvParser}) which may need to implement a barrier to synchronize all tasks
     * before proceeding on to dependent tasks.
     */
    public void waitForActiveTasks() {
    }

    /**
     * Wraps parse tree from findBestParse() with additional stats and cleans up output for consumption. Input can be a
     * sentence string or a parse tree. The input format is set to {@link InputFormat#Tree} if the input string starts
     * with '((', '(TOP', or '(ROOT'.
     * 
     * @param input
     * @return Parse output and state
     */
    public ParseTask parseSentence(final String input) {
        return parseSentence(input, null);
    }

    /**
     * Wraps parse tree from findBestParse() with additional stats and cleans up output for consumption. Input can be a
     * sentence string or a parse tree. The input format is set to {@link InputFormat#Tree} if the input string starts
     * with '((', '(TOP', or '(ROOT'.
     * 
     * @param input
     * @param recoveryStrategy Recovery strategy in case of parse failure
     * @return Parse output and state
     */
    public ParseTask parseSentence(String input, final RecoveryStrategy recoveryStrategy) {

        input = input.trim();
        if (input.length() == 0) {
            BaseLogger.singleton().info("WARNING: blank line in input.");
            return null;
        }

        synchronized (opts.inputFormat) {
            if (opts.inputFormat != InputFormat.Tree && input.charAt(0) == '('
                    && (input.startsWith("((") || input.startsWith("(TOP") || input.startsWith("(ROOT"))) {
                BaseLogger.singleton().fine(
                        "INFO: Auto-detecting inputFormat as Tree (originally " + opts.inputFormat + ")");
                opts.inputFormat = InputFormat.Tree;
            }
        }

        // TODO: make parseTask local and pass it around to required methods. Will probably need to add
        // instance methods of CellSelector, FOM, and Chart to it. Should make parse thread-safe.
        final ParseTask task = new ParseTask(input, opts.inputFormat, grammar, figureOfMerit, recoveryStrategy,
                opts.decodeMethod);

        if (task.sentenceLength() > opts.maxLength) {
            BaseLogger.singleton().info(
                    "INFO: Skipping sentence. Length of " + task.sentenceLength() + " is greater than maxLength ("
                            + opts.maxLength + ")");
        } else {
            task.startTime();

            task.binaryParse = findBestParse(task);

            task.stopTime();
            task.insideProbability = getInside(0, task.sentenceLength(), grammar.startSymbol);
            task.chartStats = getStats();
        }

        return task;
    }

    /**
     * Closes any resources maintained by the parser (e.g. thread-pools, socket connections, etc.). Subclasses which
     * allocate persistent resources should override {@link #shutdown()} to release those resources.
     */
    public void shutdown() {
    }

    /**
     * Ensure that we release all resources when garbage-collected
     */
    @Override
    public void finalize() {
        shutdown();
    }

    static public enum ParserType {

        CYK(ResearchParserType.LeftChildMl), Agenda(ResearchParserType.APWithMemory), Beam(
                ResearchParserType.BeamSearchChartParser), Matrix(ResearchParserType.CartesianProductHashMl);

        public ResearchParserType researchParserType;

        private ParserType(final ResearchParserType researchParserType) {
            this.researchParserType = researchParserType;
        }
    }

    static public enum ResearchParserType {
        ECPCellCrossList(ECPCellCrossList.class.getName(), "ecpccl"),
        ECPCellCrossHash(ECPCellCrossHash.class.getName(), "ecpcch"),
        ECPCellCrossHashGrammarLoop(ECPCellCrossHashGrammarLoop.class.getName(), "ecpcchgl"),
        ECPCellCrossHashGrammarLoop2(ECPCellCrossHashGrammarLoop2.class.getName(), "ecpcchgl2"),
        ECPCellCrossMatrix(ECPCellCrossMatrix.class.getName(), "ecpccm"),
        ECPGrammarLoop(ECPGrammarLoop.class.getName(), "ecpgl"),
        ECPGrammarLoopBerkeleyFilter(ECPGrammarLoopBerkFilter.class.getName(), "ecpglbf"),
        ECPInsideOutside(ECPInsideOutside.class.getName(), "ecpio"),
        AgendaParser(AgendaParser.class.getName(), "apall"),
        APWithMemory(APWithMemory.class.getName(), "apwm"),
        APGhostEdges(APGhostEdges.class.getName(), "apge"),
        APDecodeFOM(APDecodeFOM.class.getName(), "apfom"),
        BeamSearchChartParser(BeamSearchChartParser.class.getName(), "beam"),
        BSCPSplitUnary(BSCPSplitUnary.class.getName(), "bscpsu"),
        BSCPPruneViterbi(BSCPPruneViterbi.class.getName(), "beampv"),
        BSCPOnlineBeam(BSCPWeakThresh.class.getName(), "beamob"),
        BSCPBoundedHeap(BSCPBoundedHeap.class.getName(), "beambh"),
        BSCPExpDecay(BSCPExpDecay.class.getName(), "beamed"),
        BSCPPerceptronCell(BSCPFomDecode.class.getName(), "beampc"),
        BSCPFomDecode(BSCPFomDecode.class.getName(), "beamfom"),
        BSCPBeamConfTrain(BSCPBeamPredictTrain.class.getName(), "beamconftrain"),
        CoarseCellAgenda(CoarseCellAgendaParser.class.getName(), "cc"),
        CoarseCellAgendaCSLUT(null, "cccslut"), // Not currently supported

        // Hard-code class names for OpenCL parsers. Referencing the classes at runtime requires OpenCL libraries, which
        // aren't included in non-GPL builds
        DenseVectorOpenClSpmv("edu.ohsu.cslu.parser.spmv.DenseVectorOpenClSpmvParser", "dvopencl"),
        PackedOpenClSpmv("edu.ohsu.cslu.parser.spmv.PackedOpenClSpmvParser", "popencl"),

        CsrSpmv(CsrSpmvParser.class.getName(), "csr"),
        GrammarParallelCsrSpmv(GrammarParallelCsrSpmvParser.class.getName(), "gpcsr"),
        CscSpmv(CscSpmvParser.class.getName(), "csc"),
        GrammarParallelCscSpmv(GrammarParallelCscSpmvParser.class.getName(), "gpcsc"),
        LeftChildMl(LeftChildLoopSpmlParser.class.getName(), "lcml"),
        RightChildMl(RightChildLoopSpmlParser.class.getName(), "rcml"),
        GrammarLoopMl(GrammarLoopSpmlParser.class.getName(), "glml"),
        CartesianProductBinarySearchMl(CartesianProductBinarySearchSpmlParser.class.getName(), "cpbs"),
        CartesianProductBinarySearchLeftChildMl(CartesianProductBinarySearchLeftChildSpmlParser.class.getName(),
                "cplbs"),
        CartesianProductHashMl(CartesianProductHashSpmlParser.class.getName(), "cph"),
        CartesianProductLeftChildHashMl(CartesianProductLeftChildHashSpmlParser.class.getName(), "cplch"),
        InsideOutsideCartesianProductHash(InsideOutsideCphSpmlParser.class.getName(), "iocph"),
        ViterbiInOutCph(ViterbiInOutCphSpmlParser.class.getName(), "vitio"),
        ConstrainedCartesianProductHashMl(ConstrainedCphSpmlParser.class.getName(), ConstrainedCellSelector.MODEL,
                "const");

        /** The implementing class */
        private final String classname;

        /**
         * A few parsers require a specialized {@link CellSelectorModel}. If specified, this model will be used at
         * runtime
         */
        private final CellSelectorModel cellSelectorModel;

        private ResearchParserType(final String classname, final String... aliases) {
            this.classname = classname;
            this.cellSelectorModel = null;
            EnumAliasMap.singleton().addAliases(this, aliases);
        }

        private ResearchParserType(final String classname, final CellSelectorModel cellSelectorModel,
                final String... aliases) {
            this.classname = classname;
            this.cellSelectorModel = cellSelectorModel;
            EnumAliasMap.singleton().addAliases(this, aliases);
        }

        public String classname() {
            return classname;
        }

        public CellSelectorModel cellSelectorModel() {
            return cellSelectorModel;
        }
    }

    /**
     * Various input formats are supported, including free text, pre-tokenized text, gold trees, and gold POS tags.
     */
    static public enum InputFormat {
        Text, Token, Tree, Tagged; // CoNNL

        private InputFormat(final String... aliases) {
            EnumAliasMap.singleton().addAliases(this, aliases);
        }
    }

    /**
     * Methods of decoding a populated chart.
     */
    static public enum DecodeMethod {
        /** Depends only on inside probabilities and viterbi backpointers */
        ViterbiMax,

        /**
         * Requires posterior probabilities, but ignores backpointers except for unary productions. Max-Recall,
         * Max-Precision, and the combination between the two are controlled by lambda (
         * {@link Parser#PROPERTY_MAXC_LAMBDA}) (see Goodman, 1998 and Hollingshead and Roark, 2007).
         */
        Goodman,

        /**
         * Goodman decoding using a projection onto an unsplit grammar; sums over non-terminal splits (latent
         * annotations in a latent-variable grammar). Max-recall, Max-precision, and combined are again controlled by
         * lambda ({@link Parser#PROPERTY_MAXC_LAMBDA})
         */
        SplitSum,

        /**
         * Petrov's max-rule-product decoding. Requires inside/outside posterior probabilities and sums over
         * non-terminal splits.
         */
        MaxRuleSum, MaxRuleProd,

        FOMSum, FOMProd;

        private DecodeMethod(final String... aliases) {
            EnumAliasMap.singleton().addAliases(this, aliases);
        }
    }

    static public enum Language {
        English, Chinese, German, French;

        private Language(final String... aliases) {
            EnumAliasMap.singleton().addAliases(this, aliases);
        }
    }

    /**
     * Possible strategies for reparsing. The 'DoubleBeam...' options are intended to duplicate current behavior for the
     * '-reparse' command-line option, doubling 1-5X respectively. {@link #Escalate} defines an escalation hierarchy and
     * will likely be the normal strategy when reparsing is required.
     */
    static public enum ReparseStrategy {
        /** Normal parsing */
        None("default", Stage.NORMAL),

        /** 1 doubling */
        DoubleBeam1x("1", Stage.NORMAL, Stage.DOUBLE),

        /** 2 doublings */
        DoubleBeam2x("2", Stage.NORMAL, Stage.DOUBLE, Stage.DOUBLE),

        /** 3 doublings */
        DoubleBeam3x("3", Stage.NORMAL, Stage.DOUBLE, Stage.DOUBLE, Stage.DOUBLE),

        /** 4 doublings */
        DoubleBeam4x("4", Stage.NORMAL, Stage.DOUBLE, Stage.DOUBLE, Stage.DOUBLE, Stage.DOUBLE),

        /** 5 doublings */
        DoubleBeam5x("5", Stage.NORMAL, Stage.DOUBLE, Stage.DOUBLE, Stage.DOUBLE, Stage.DOUBLE, Stage.DOUBLE),

        /** Remove beam width prediction and cell closure, double beam 5X, and finally parse exhaustively */
        Escalate(null, Stage.NORMAL, Stage.FIXED_BEAM, Stage.DOUBLE, Stage.DOUBLE, Stage.DOUBLE, Stage.DOUBLE,
                Stage.DOUBLE, Stage.EXHAUSTIVE);

        private Stage[] stages;

        private ReparseStrategy(final String alias, final Stage... stages) {
            EnumAliasMap.singleton().addAliases(this, alias);
            this.stages = stages;
        }

        public Stage[] stages() {
            return stages;
        }

        static public enum Stage {
            /** Parse with the specified maximum beam, cell closure, and beam width prediction */
            NORMAL,

            /** Parse with the specified maximum beam, no cell closure or beam width prediction */
            FIXED_BEAM,

            /** Double the previous beam width and add a constant to maxLocalDelta */
            DOUBLE,

            /** Exhaustive parsing */
            EXHAUSTIVE;
        }
    }
}

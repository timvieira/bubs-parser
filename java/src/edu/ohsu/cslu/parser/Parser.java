package edu.ohsu.cslu.parser;

import java.util.logging.Level;

import cltool4j.BaseLogger;
import cltool4j.args4j.EnumAliasMap;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector;
import edu.ohsu.cslu.parser.ml.SparseMatrixLoopParser;
import edu.ohsu.cslu.tools.TreeTools;

public abstract class Parser<G extends Grammar> {

    public final G grammar;
    public ParserDriver opts;
    // TODO Make this reference final (once we work around the hack in CellChart)
    public EdgeSelector edgeSelector;
    public final CellSelector cellSelector;
    public ParseStats currentInput; // temporary so I don't break too much stuff at once

    // TODO Move global state back out of Parser
    static protected int sentenceNumber = 0;
    protected float totalParseTimeSec = 0;
    protected float totalInsideScore = 0;
    protected long totalMaxMemoryMB = 0;

    /**
     * True if we're collecting detailed counts of cell populations, cartesian-product sizes, etc. Set from
     * {@link ParserDriver}, but duplicated here as a final variable, so that the JIT can eliminate
     * potentially-expensive counting code when we don't need it.
     * 
     * TODO Move up to {@link ChartParser} (or even higher) and share with {@link SparseMatrixLoopParser}
     */
    protected final boolean collectDetailedStatistics;

    public Parser(final ParserDriver opts, final G grammar) {
        this.grammar = grammar;
        this.opts = opts;
        this.edgeSelector = opts.edgeSelector;
        this.cellSelector = opts.cellSelector;

        this.collectDetailedStatistics = BaseLogger.singleton().isLoggable(Level.FINER);

        // if (this.cellSelector instanceof CellConstraints) {
        // this.hasCellConstraints = true;
        // cellConstraints = (CellConstraints) cellSelector;
        // }
    }

    public abstract float getInside(int start, int end, int nt);

    public abstract float getOutside(int start, int end, int nt);

    public abstract String getStats();

    protected abstract ParseTree findBestParse(int[] tokens) throws Exception;

    // wraps parse tree from findBestParse() with additional stats and
    // cleans up output for consumption. Input can be a sentence string
    // or a parse tree
    public ParseStats parseSentence(final String input) throws Exception {
        final ParseStats stats = new ParseStats(input, grammar);
        currentInput = stats; // get ride of currentInput (and chart?). Just pass these around
        stats.sentenceNumber = sentenceNumber++;
        stats.tokens = grammar.tokenizer.tokenizeToIndex(stats.sentence);

        if (stats.sentenceLength > opts.maxLength) {
            BaseLogger.singleton().fine(
                    "INFO: Skipping sentence. Length of " + stats.sentenceLength + " is greater than maxLength ("
                            + opts.maxLength + ")");
        } else {
            stats.startTime();
            stats.parse = findBestParse(stats.tokens);
            stats.stopTime();

            if (stats.parse == null) {
                stats.parseBracketString = "()";
            } else {
                if (!opts.printUnkLabels) {
                    stats.parse.replaceLeafNodes(stats.strTokens);
                }

                // stats.parseBracketString = stats.parse.toString(opts.printInsideProbs);
                stats.parseBracketString = stats.parse.toString();
                stats.insideProbability = getInside(0, stats.sentenceLength, grammar.startSymbol);

                // TODO: we should be converting the tree in tree form, not in bracket string form
                if (opts.binaryTreeOutput == false) {
                    stats.parseBracketString = TreeTools.unfactor(stats.parseBracketString, grammar.grammarFormat);
                }

                // TODO: could evaluate accuracy here if input is a gold tree
            }
        }

        return stats;
    }

    static public enum ParserType {
        CKY, Agenda, Beam, Matrix;

        private ParserType(final String... aliases) {
            EnumAliasMap.singleton().addAliases(this, aliases);
        }
    }

    static public enum ResearchParserType {
        ECPCellCrossList("ecpccl"),
        ECPCellCrossHash("ecpcch"),
        ECPCellCrossMatrix("ecpccm"),
        ECPGrammarLoop("ecpgl"),
        ECPGrammarLoopBerkeleyFilter("ecpglbf"),
        ECPInsideOutside("ecpio"),
        AgendaParser("apall"),
        APWithMemory("apwm"),
        APGhostEdges("apge"),
        APDecodeFOM("apfom"),
        BeamSearchChartParser("beam"),
        BSCPPruneViterbi("beampv"),
        BSCPOnlineBeam("beamob"),
        BSCPBoundedHeap("beambh"),
        BSCPExpDecay("beamed"),
        BSCPPerceptronCell("beampc"),
        BSCPFomDecode("beamfom"),
        BSCPBeamConfTrain("beamconftrain"),
        // BSCPBeamConf("beamconf"),
        CoarseCellAgenda("cc"),
        CoarseCellAgendaCSLUT("cccslut"),
        JsaSparseMatrixVector("jsa"),
        DenseVectorOpenClSparseMatrixVector("dvopencl"),
        PackedOpenClSparseMatrixVector("popencl"),
        CsrSpmv("csr"),
        CsrSpmvPerMidpoint("csrpm"),
        CscSpmv("csc"),
        BeamCscSpmv("beamcsc"),
        LeftChildMatrixLoop("lcml"),
        RightChildMatrixLoop("rcml"),
        GrammarLoopMatrixLoop("glml"),
        CartesianProductBinarySearch("cpbs"),
        CartesianProductBinarySearchLeftChild("cplbs"),
        CartesianProductHash("cph"),
        CartesianProductLeftChildHash("cplch");

        private ResearchParserType(final String... aliases) {
            EnumAliasMap.singleton().addAliases(this, aliases);
        }
    }
}

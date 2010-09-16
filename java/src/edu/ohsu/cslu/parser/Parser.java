package edu.ohsu.cslu.parser;

import org.kohsuke.args4j.EnumAliasMap;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector;
import edu.ohsu.cslu.parser.ml.SparseMatrixLoopParser;
import edu.ohsu.cslu.parser.util.Log;
import edu.ohsu.cslu.parser.util.ParseTree;

public abstract class Parser<G extends Grammar> {

    public G grammar;
    public ParserDriver opts;
    public EdgeSelector edgeSelector;
    public CellSelector cellSelector;
    public ParseStats currentInput; // temporary so I don't break too much stuff at once

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

        try {
            edgeSelector = EdgeSelector.create(opts.edgeFOMType, grammar, opts.fomModelStream);
            cellSelector = CellSelector.create(opts.cellSelectorType, opts.cellModelStream, opts.cslutScoresStream);
        } catch (final Exception e) {
            e.printStackTrace();
        }
        this.collectDetailedStatistics = opts.collectDetailedStatistics;
    }

    public abstract float getInside(int start, int end, int nt);

    public abstract float getOutside(int start, int end, int nt);

    public abstract String getStats();

    protected abstract ParseTree findBestParse(int[] tokens) throws Exception;

    // wraps parse tree from findBestParse() with additional stats and
    // cleans up output for consumption
    public ParseStats parseSentence(final String sentence) throws Exception {
        final ParseStats stats = new ParseStats(sentence);
        currentInput = stats; // get ride of currentInput (and chart?). Just pass these around
        stats.sentenceNumber = sentenceNumber++;
        stats.tokens = grammar.tokenizer.tokenizeToIndex(sentence);

        if (stats.sentenceLength > opts.maxLength) {
            Log.info(0, "INFO: Skipping sentence. Length of " + stats.sentenceLength + " is greater than maxLength ("
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

                stats.parseBracketString = stats.parse.toString(opts.printInsideProbs);
                stats.insideProbability = getInside(0, stats.sentenceLength, grammar.startSymbol);

                // TODO: we should be converting the tree in tree form, not in bracket string form
                if (opts.unfactor) {
                    stats.parseBracketString = ParseTree.unfactor(stats.parseBracketString, grammar.grammarFormat);
                }

                // TODO: could evaluate accuracy here if input is a gold tree
            }
        }

        return stats;
    }

    static public enum ParserType {
        CKY, Agenda, Beam;

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
        CoarseCellAgenda("cc"),
        CoarseCellAgendaCSLUT("cccslut"),
        JsaSparseMatrixVector("jsa"),
        DenseVectorOpenClSparseMatrixVector("dvopencl"),
        PackedOpenClSparseMatrixVector("popencl"),
        CsrSpmv("csr"),
        CsrSpmvPerMidpoint("csrpm"),
        CscSpmv("csc"),
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

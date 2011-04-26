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

import cltool4j.BaseLogger;
import cltool4j.args4j.EnumAliasMap;
import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector;
import edu.ohsu.cslu.parser.spmv.CscSpmvParser;
import edu.ohsu.cslu.parser.spmv.CsrSpmvParser;
import edu.ohsu.cslu.parser.spmv.GrammarParallelCsrSpmvParser;
import edu.ohsu.cslu.tools.TreeTools;

public abstract class Parser<G extends Grammar> {

    public final G grammar;
    public ParserDriver opts;
    // TODO Make this reference final (once we work around the hack in CellChart)
    public EdgeSelector edgeSelector;
    public final CellSelector cellSelector;
    public ParseContext currentInput; // temporary so I don't break too much stuff at once

    // TODO Move global state back out of Parser
    static volatile protected int sentenceNumber = 0;
    protected float totalParseTimeSec = 0;
    protected float totalInsideScore = 0;
    protected long totalMaxMemoryMB = 0;

    /**
     * True if we're collecting detailed counts of cell populations, cartesian-product sizes, etc. Set from
     * {@link ParserDriver}, but duplicated here as a final variable, so that the JIT can eliminate
     * potentially-expensive counting code when we don't need it.
     */
    protected final boolean collectDetailedStatistics;

    public Parser(final ParserDriver opts, final G grammar) {
        this.grammar = grammar;
        this.opts = opts;
        this.edgeSelector = opts.edgeSelectorFactory.createEdgeSelector(grammar);
        this.cellSelector = opts.cellSelectorFactory.createCellSelector();

        this.collectDetailedStatistics = BaseLogger.singleton().isLoggable(Level.FINER);

        // if (this.cellSelector instanceof CellConstraints) {
        // this.hasCellConstraints = true;
        // cellConstraints = (CellConstraints) cellSelector;
        // }
    }

    public abstract float getInside(int start, int end, int nt);

    public abstract float getOutside(int start, int end, int nt);

    public abstract String getStats();

    protected abstract BinaryTree<String> findBestParse(int[] tokens);

    /**
     * Waits until all active parsing tasks have completed. Intended for multi-threaded parsers (e.g.
     * {@link CsrSpmvParser}, {@link CscSpmvParser}) which may need to implement a barrier to synchronize all tasks
     * before proceeding on to dependent tasks.
     */
    public void waitForActiveTasks() {
    }

    // wraps parse tree from findBestParse() with additional stats and
    // cleans up output for consumption. Input can be a sentence string
    // or a parse tree
    public ParseContext parseSentence(final String input) {
        final ParseContext result = new ParseContext(input, grammar);
        currentInput = result; // get ride of currentInput (and chart?). Just pass these around
        result.sentenceNumber = sentenceNumber++;
        result.tokens = grammar.tokenizer.tokenizeToIndex(result.sentence);

        if (result.sentenceLength > opts.maxLength) {
            BaseLogger.singleton().fine(
                    "INFO: Skipping sentence. Length of " + result.sentenceLength + " is greater than maxLength ("
                            + opts.maxLength + ")");
            result.parseBracketString = "()";
            return result;
        }

        try {
            result.startTime();
            result.parse = findBestParse(result.tokens);
            result.stopTime();

            if (result.parse == null) {
                result.parseBracketString = "()";
                return result;
            }

            if (!opts.printUnkLabels) {
                result.parse.replaceLeafLabels(result.strTokens);
            }

            // stats.parseBracketString = stats.parse.toString(opts.printInsideProbs);
            result.parseBracketString = result.parse.toString();
            result.insideProbability = getInside(0, result.sentenceLength, grammar.startSymbol);
            result.parserStats = getStats();

            // TODO: we should be converting the tree in tree form, not in bracket string form
            if (opts.binaryTreeOutput == false) {
                result.parseBracketString = TreeTools.unfactor(result.parseBracketString, grammar.grammarFormat);
            }

            // TODO: could evaluate accuracy here if input is a gold tree

            return result;

        } catch (final Exception e) {
            BaseLogger.singleton().fine("ERROR: " + e.getMessage());
            result.parseBracketString = "()";
            return result;
        }
    }

    /**
     * Closes any resources maintained by the parser (e.g. thread-pools as in {@link GrammarParallelCsrSpmvParser}.
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

        CYK(ResearchParserType.ECPCellCrossList), Agenda(ResearchParserType.APWithMemory), Beam(
                ResearchParserType.BeamSearchChartParser), Matrix(ResearchParserType.CscSpmv);

        public ResearchParserType researchParserType;

        private ParserType(final ResearchParserType researchParserType) {
            this.researchParserType = researchParserType;
        }
    }

    static public enum ResearchParserType {
        ECPCellCrossList("ecpccl"),
        ECPCellCrossHash("ecpcch"),
        ECPCellCrossHashGrammarLoop("ecpcchgl"),
        ECPCellCrossHashGrammarLoop2("ecpcchgl2"),
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
        DenseVectorOpenClSpmv("dvopencl"),
        PackedOpenClSpmv("popencl"),
        CsrSpmv("csr"),
        GrammarParallelCsrSpmv("gpcsr"),
        CscSpmv("csc"),
        GrammarParallelCscSpmv("gpcsc"),
        LeftChildMl("lcml"),
        RightChildMl("rcml"),
        GrammarLoopMl("glml"),
        CartesianProductBinarySearchMl("cpbs"),
        CartesianProductBinarySearchLeftChildMl("cplbs"),
        CartesianProductHashMl("cph"),
        CartesianProductLeftChildHashMl("cplch");

        private ResearchParserType(final String... aliases) {
            EnumAliasMap.singleton().addAliases(this, aliases);
        }
    }
}

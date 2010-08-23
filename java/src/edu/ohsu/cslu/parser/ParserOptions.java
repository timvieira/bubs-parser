package edu.ohsu.cslu.parser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import org.kohsuke.args4j.EnumAliasMap;

import edu.ohsu.cslu.parser.cellselector.CellSelector.CellSelectorType;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector.EdgeSelectorType;

public final class ParserOptions {

    // Parser
    public ParserType parserType = ParserType.ECPCellCrossList;
    public EdgeSelectorType edgeFOMType = EdgeSelectorType.Inside;
    public CellSelectorType cellSelectorType = CellSelectorType.LeftRightBottomTop;

    public boolean fomTrain = false;
    public String fomModelFileName = null;
    public BufferedReader fomModelStream = null;

    public boolean cellTrain = false;
    public String cellModelFileName = null;
    public BufferedReader cellModelStream = null;

    public String cslutScoresFileName = null;
    public BufferedReader cslutScoresStream = null;

    // Grammar
    public String pcfgFileName = null;
    public String lexFileName = null;
    public GrammarFormatType grammarFormat = GrammarFormatType.CSLU;

    // Options
    public int maxLength = 200;
    public boolean printInsideProbs = false;
    public boolean printUnkLabels = false;
    public boolean viterbiMax = true;

    private boolean collectDetailedStatistics;
    private boolean unfactor;

    public static float param1 = -1;
    public static float param2 = -1;

    public BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(System.out));
    public BufferedReader inputStream = new BufferedReader(new InputStreamReader(System.in));

    public boolean collectDetailedStatistics() {
        return collectDetailedStatistics;
    }

    public ParserOptions setCollectDetailedStatistics(final boolean collectDetailedStatistics) {
        this.collectDetailedStatistics = collectDetailedStatistics;
        return this;
    }

    public boolean unfactor() {
        return unfactor;
    }

    public ParserOptions setUnfactor(final boolean unfactor) {
        this.unfactor = unfactor;
        return this;
    }

    @Override
    public String toString() {
        final String prefix = "OPTS: ";
        String s = "";
        s += prefix + "ParserType=" + parserType + "\n";
        s += prefix + "CellSelector=" + cellSelectorType + "\n";
        // s += prefix + "CellProcess=" + chartCellProcessingType + "\n";
        s += prefix + "FOM=" + edgeFOMType + "\n";
        s += prefix + "x1=" + param1 + "\n";
        s += prefix + "x2=" + param2;
        // Log.info(0, s);

        return s;
    }

    static public enum ParserType {
        ECPCellCrossList("ecpccl"), ECPCellCrossHash("ecpcch"), ECPCellCrossMatrix("ecpccm"), ECPGrammarLoop(
                "ecpgl"), ECPGrammarLoopBerkeleyFilter("ecpglbf"), ECPInsideOutside("ecpio"), AgendaChartParser(
                "acpall"), ACPWithMemory("acpwm"), ACPGhostEdges("acpge"), LocalBestFirst("lbf"), LBFPruneViterbi(
                "lbfpv"), LBFOnlineBeam("lbfob"), LBFBoundedHeap("lbfbh"), LBFExpDecay("lbfed"), LBFPerceptronCell(
                "lbfpc"), CoarseCellAgenda("cc"), CoarseCellAgendaCSLUT("cccslut"), JsaSparseMatrixVector(
                "jsa"), DenseVectorOpenClSparseMatrixVector("dvopencl"), PackedOpenClSparseMatrixVector(
                "popencl"), CsrSpmv("csr"), CsrSpmvPerMidpoint("csrpm"), CscSpmv("csc"), SortAndScanSpmv(
                "sort-and-scan"), LeftChildMatrixLoop("lcml"), RightChildMatrixLoop("rcml"), GrammarLoopMatrixLoop(
                "glml"), CartesianProductBinarySearch("cpbs"), CartesianProductBinarySearchLeftChild("cplbs"), CartesianProductHash(
                "cph"), CartesianProductLeftChildHash("cplch");

        private ParserType(final String... aliases) {
            EnumAliasMap.singleton().addAliases(this, aliases);
        }
    }

    // static public enum ParserType {
    // ECPCellCrossList(ECPCellCrossList.class, "ecpccl"), //
    // ECPCellCrossHash(ECPCellCrossHash.class, "ecpcch"), //
    // ECPCellCrossMatrix(ECPCellCrossMatrix.class, "ecpccm"), //
    // ECPGrammarLoop(ECPGrammarLoop.class, "ecpgl"), //
    // ECPGrammarLoopBerkeleyFilter(ECPGrammarLoopBerkFilter.class, "ecpglbf"), //
    // ECPInsideOutside(ECPInsideOutside.class, "ecpio"), //
    // AgendaChartParser(AgendaChartParser.class, "acpall"), //
    // ACPWithMemory(ACPWithMemory.class, "acpwm"), //
    // ACPGhostEdges(ACPGhostEdges.class, "acpge"), //
    // // LocalBestFirst(LocalBestFirstChartParser.class, "lbf"), //
    // LBFPruneViterbi(LBFPruneViterbi.class, "lbfpv"), //
    // LBFOnlineBeam(LBFOnlineBeam.class, "lbfob"), //
    // LBFBoundedHeap(LBFBoundedHeap.class, "lbfbh"), //
    // LBFExpDecay(LBFExpDecay.class, "lbfed"), //
    // LBFPerceptronCell(LBFPerceptronCell.class, "lbfpc"), //
    // CoarseCellAgenda(CoarseCellAgendaParser.class, "cc"), //
    // CoarseCellAgendaCSLUT(CoarseCellAgendaParserWithCSLUT.class, "cccslut"), //
    // DenseVectorOpenClSparseMatrixVector(DenseVectorOpenClSpmvParser.class, "dvopencl"), //
    // PackedOpenClSparseMatrixVector(PackedOpenClSpmvParser.class, "popencl"), //
    // CsrSpmv(CsrSpmvParser.class, "csr"), //
    // CsrSpmvPerMidpoint(CsrSpmvPerMidpointParser.class, "csrpm"), //
    // CscSpmv(CscSpmvParser.class, "csc"), //
    // SortAndScanSpmv(SortAndScanCsrSpmvParser.class, "sort-and-scan"), //
    // LeftChildMatrixLoop(LeftChildLoopSpmlParser.class, "lcml"), //
    // RightChildMatrixLoop(RightChildLoopSpmlParser.class, "rcml"), //
    // GrammarLoopMatrixLoop(GrammarLoopSpmlParser.class, "glml"), //
    // CartesianProductBinarySearch(CartesianProductBinarySearchSpmlParser.class, "cpbs"), //
    // CartesianProductBinarySearchLeftChild(CartesianProductBinarySearchLeftChildSpmlParser.class, "cplbs"),
    // //
    // CartesianProductHash(CartesianProductHashSpmlParser.class, "cph"), //
    // CartesianProductLeftChildHash(CartesianProductLeftChildHashSpmlParser.class, "cplch");
    //
    // private final Class<? extends Parser<?>> parserClass;
    //
    // private ParserType(final Class<? extends Parser<?>> parserClass, final String... aliases) {
    // this.parserClass = parserClass;
    // EnumAliasMap.singleton().addAliases(this, aliases);
    // }
    //
    // public Parser<?> createParser() {
    // return null;
    // }
    // }

    static public enum CartesianProductFunctionType {
        Default("d", "default"), Unfiltered("u", "unfiltered"), PosFactoredFiltered("pf"), BitMatrixExactFilter(
                "bme", "bitmatrixexact"), PerfectHash("ph", "perfecthash"), PerfectHash2("ph2",
                "perfecthash2");

        private CartesianProductFunctionType(final String... aliases) {
            EnumAliasMap.singleton().addAliases(this, aliases);
        }
    }

    static public enum GrammarFormatType {
        CSLU, Roark, Berkeley;

        public String unsplitNonTerminal(final String nonTerminal) {
            switch (this) {
                case Berkeley:
                    return nonTerminal.replaceFirst("_[0-9]+$", "");
                case CSLU:
                    return nonTerminal.replaceFirst("[|^]<([A-Z]+)?>$", "");
                case Roark:
                    // TODO Support Roark format

                default:
                    throw new IllegalArgumentException("Unsupported format");

            }

        }

        public String factoredNonTerminal(final String nonTerminal) {
            switch (this) {
                case Berkeley:
                    return "@" + nonTerminal;
                case CSLU:
                    return nonTerminal + "|";
                case Roark:
                    // TODO Support Roark format

                default:
                    throw new IllegalArgumentException("Unsupported format");

            }

        }

        public boolean isFactored(final String nonTerminal) {
            switch (this) {
                case CSLU:
                    return nonTerminal.contains("|");
                case Berkeley:
                    return nonTerminal.startsWith("@");
                case Roark:
                    // TODO Support Roark format

                default:
                    throw new IllegalArgumentException("Unsupported format");
            }
        }
    }
}

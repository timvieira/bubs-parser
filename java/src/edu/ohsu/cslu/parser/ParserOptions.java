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

    public static float param1 = -1;
    public static float param2 = -1;

    public BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(System.out));
    public BufferedReader inputStream = new BufferedReader(new InputStreamReader(System.in));

    public boolean collectDetailedStatistics() {
        return collectDetailedStatistics;
    }

    public ParserOptions setCollectDetailedStatistics() {
        this.collectDetailedStatistics = true;
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
                "jsa"), OpenClSparseMatrixVector("opencl"), CsrSpmv("csr"), CsrSpmvPerMidpoint("csrpm"), CscSpmv(
                "csc"), SortAndScanSpmv("sort-and-scan");

        private ParserType(final String... aliases) {
            EnumAliasMap.singleton().addAliases(this, aliases);
        }
    }

    static public enum CartesianProductFunctionType {
        Default("d", "default"), Unfiltered("u", "unfiltered"), PosFactoredFiltered("pf"), BitMatrixExactFilter(
                "bme", "bitmatrixexact"), PerfectHash("ph", "perfecthash");

        private CartesianProductFunctionType(final String... aliases) {
            EnumAliasMap.singleton().addAliases(this, aliases);
        }
    }

    static public enum GrammarFormatType {
        CSLU, Roark, Berkeley;

        public boolean isFactored(final String nonTerminal) {
            switch (this) {
                case CSLU:
                    return nonTerminal.contains("|");
                case Berkeley:
                    return nonTerminal.startsWith("@");
                default:
                    throw new RuntimeException("Unable to determine factorization");
            }
        }
    }
}

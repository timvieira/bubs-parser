package edu.ohsu.cslu.parser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import org.kohsuke.args4j.EnumAliasMap;

import edu.ohsu.cslu.parser.cellselector.CellSelector.CellSelectorType;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector.EdgeSelectorType;

public class ParserOptions {

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
    public static float param1 = -1;
    public static float param2 = -1;

    public BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(System.out));
    public BufferedReader inputStream = new BufferedReader(new InputStreamReader(System.in));

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
                "jsa"), OpenClSparseMatrixVector("opencl"), CsrSpmv("csr"), CsrSpmvPerMidpoint("csrpm"), SortAndScanSpmv(
                "sort-and-scan");

        private ParserType(final String... aliases) {
            EnumAliasMap.singleton().addAliases(this, aliases);
        }
    }

    static public enum CartesianProductFunctionType {
        Default("d", "default"), Unfiltered("u", "unfiltered"), PosFactoredFiltered("pf"), BitMatrixExactFilter(
                "bme", "bitmatrixexact");

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

    //
    // public String pcfgFileName = null;
    // public String lexFileName = null;
    // public BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(System.out));
    // public BufferedReader inputStream = new BufferedReader(new InputStreamReader(System.in));
    // public BufferedReader fomModelStream = null;
    //
    // public boolean printInsideProbs = false;
    // public boolean printUnkLabels = false;
    // public boolean trainModel = false;
    //
    // public ParserType parserType = null;
    // public ChartTraversalType chartTraversalType = null;
    // public ChartCellVisitationType chartCellVisitationType = null;
    // public EdgeFOMType edgeFOMType = null;
    // public float fudgeFactor = Float.NaN;
    //
    // static public enum ParserType {
    // ExhaustiveChartParser, AgendaParser, AgendaParserWithGhostEdges, SuperAgendaParser
    // }
    //
    // static public enum ChartCellVisitationType {
    // CellCrossList, CellCrossHash, CellCrossMatrix, GrammarLoop, GrammarLoopBerkeleyFilter
    // }
    //
    // public ParserOptions(final String[] argv) throws IOException {
    // boolean foundGram = false;
    // for (int i = 0; i < argv.length; i++) {
    //
    // if (argv[i].equals("-scores")) {
    // printInsideProbs = true;
    // }
    // if (argv[i].equals("-input") || argv[i].equals("-i")) {
    // inputStream = new BufferedReader(new FileReader(argv[i + 1]));
    // }
    // if (argv[i].equals("-output") || argv[i].equals("-o")) {
    // outputStream = new BufferedWriter(new FileWriter(argv[i + 1]));
    // }
    // if (argv[i].equals("-train")) {
    // trainModel = true;
    // }
    // if (argv[i].equals("-unk")) {
    // printUnkLabels = true;
    // }
    // if (argv[i].equals("-g") || argv[i].equals("-grammar")) {
    // pcfgFileName = argv[i + 1] + ".pcfg";
    // lexFileName = argv[i + 1] + ".lex";
    // foundGram = true;
    // }
    // if (argv[i].equals("-p") || argv[i].equals("-parser")) {
    // if (argv[i + 1].equals("CCL")) {
    // parserType = ParserType.ExhaustiveChartParser;
    // chartTraversalType = ChartTraversalType.LeftRightBottomTop;
    // chartCellVisitationType = ChartCellVisitationType.CellCrossList;
    // } else if (argv[i + 1].equals("CCH")) {
    // parserType = ParserType.ExhaustiveChartParser;
    // chartTraversalType = ChartTraversalType.LeftRightBottomTop;
    // chartCellVisitationType = ChartCellVisitationType.CellCrossHash;
    // } else if (argv[i + 1].equals("CCM")) {
    // parserType = ParserType.ExhaustiveChartParser;
    // chartTraversalType = ChartTraversalType.LeftRightBottomTop;
    // chartCellVisitationType = ChartCellVisitationType.CellCrossMatrix;
    // } else if (argv[i + 1].equals("GL")) {
    // parserType = ParserType.ExhaustiveChartParser;
    // chartTraversalType = ChartTraversalType.LeftRightBottomTop;
    // chartCellVisitationType = ChartCellVisitationType.GrammarLoop;
    // } else if (argv[i + 1].equals("GLBF")) {
    // parserType = ParserType.ExhaustiveChartParser;
    // chartTraversalType = ChartTraversalType.LeftRightBottomTop;
    // chartCellVisitationType = ChartCellVisitationType.GrammarLoopBerkeleyFilter;
    // } else if (argv[i + 1].equals("A")) {
    // parserType = ParserType.AgendaParser;
    // edgeFOMType = EdgeFOMType.Inside;
    // } else if (argv[i + 1].equals("AGE")) {
    // parserType = ParserType.AgendaParserWithGhostEdges;
    // edgeFOMType = EdgeFOMType.Inside;
    // } else {
    // Log.info(0, "ERROR: -parser value '" + argv[i + 1] + "' not a valid option");
    // System.exit(1);
    // }
    // }
    //
    // if (argv[i].equals("-fom") || argv[i].equals("-FOM")) {
    // if (argv[i + 1].equals("inside")) {
    // edgeFOMType = EdgeFOMType.Inside;
    // } else if (argv[i + 1].equals("normInside")) {
    // edgeFOMType = EdgeFOMType.NormalizedInside;
    // if (Float.isNaN(fudgeFactor)) {
    // fudgeFactor = 4;
    // }
    // } else if (argv[i + 1].equals("boundary")) {
    // edgeFOMType = EdgeFOMType.BoundaryInOut;
    // if (Float.isNaN(fudgeFactor)) {
    // fudgeFactor = 1;
    // }
    // } else {
    // Log.info(0, "ERROR: -fom value '" + argv[i + 1] + "' is not a valid option");
    // System.exit(1);
    // }
    // }
    //
    // if (argv[i].equals("-fomModel")) {
    // fomModelStream = new BufferedReader(new FileReader(argv[i + 1]));
    // }
    //
    // if (argv[i].equals("-fudge")) {
    // fudgeFactor = Float.parseFloat(argv[i + 1]);
    // }
    // }
    //
    // if (foundGram == false) {
    // Log.info(0, "ERROR: Grammar file is required.  Use -g option to specify.");
    // System.exit(1);
    // }
    //
    // // default parser
    // if (parserType == null) {
    // parserType = ParserType.ExhaustiveChartParser;
    // chartTraversalType = ChartTraversalType.LeftRightBottomTop;
    // chartCellVisitationType = ChartCellVisitationType.CellCrossList;
    // }
    //
    // if (edgeFOMType == EdgeFOMType.BoundaryInOut && trainModel == false && fomModelStream == null) {
    // Log.info(0, "ERROR: FOM BoundaryNgram must also have either -train or -fomModel");
    // System.exit(1);
    // }
    // }
    //
    // @Override
    // public String toString() {
    // return toString("");
    // }
    //
    // public String toString(final String prefix) {
    // String s = "";
    // s += prefix + "ParserType=" + parserType + "\n";
    // s += prefix + "Traversal=" + chartTraversalType + "\n";
    // s += prefix + "CellVisit=" + chartCellVisitationType + "\n";
    // s += prefix + "FOM=" + edgeFOMType + "";
    //
    // return s;
    // }
}

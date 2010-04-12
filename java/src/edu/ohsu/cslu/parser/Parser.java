package edu.ohsu.cslu.parser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.LinkedList;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.parser.edgeselector.BoundaryInOut;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector;
import edu.ohsu.cslu.parser.util.Log;
import edu.ohsu.cslu.parser.util.ParseTree;
import edu.ohsu.cslu.parser.util.ParserUtil;
import edu.ohsu.cslu.parser.util.StringToMD5;

// TODO: allow gold trees as input and report F-score
// TODO: need to port python tree transforms / de-transforms to Java
// and either write our own eval or make external call to EVALB

public abstract class Parser<G extends Grammar> {

    public G grammar;
    public ParserOptions opts;
    public EdgeSelector edgeSelector;
    public CellSelector cellSelector;

    protected int sentenceNumber = 0;
    public String currentSentence;
    protected float totalParseTimeSec = 0;
    protected float totalInsideScore = 0;
    protected long totalMaxMemoryMB = 0;

    public Parser(final ParserOptions opts, final G grammar) {
        this.grammar = grammar;
        this.opts = opts;

        edgeSelector = EdgeSelector.create(opts.edgeFOMType, opts.fomModelStream, grammar);
        cellSelector = CellSelector.create(opts.cellSelectorType, opts.cellModelStream,
            opts.cslutScoresStream);
    }

    public abstract float getInside(int start, int end, int nt);

    public abstract float getOutside(int start, int end, int nt);

    public abstract String getStats();

    protected abstract ParseTree findBestParse(String sentence) throws Exception;

    public void parseSentence(String sentence, final BufferedWriter outputStream) throws Exception {
        ParseTree inputTree = null, bestParseTree = null;
        long sentStartTimeMS;
        final long sentMaxMemoryMB = 0;
        double sentParseTimeSec;
        // String insideProbStr;
        float insideScore;

        currentSentence = sentence;
        sentenceNumber++;

        // if input are trees, extract sentence from tree
        if (ParseTree.isBracketFormat(sentence)) {
            inputTree = ParseTree.readBracketFormat(sentence);
            sentence = ParserUtil.join(inputTree.getLeafNodesContent(), " ");
        }

        final String[] tokens = ParserUtil.tokenize(sentence);
        if (tokens.length > opts.maxLength) {
            Log.info(1, "INFO: Skipping sentence. Length of " + tokens.length
                    + " is greater than maxLength (" + opts.maxLength + ")");
            return;
        }

        // System.gc(); sentStartMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        sentStartTimeMS = System.currentTimeMillis();

        bestParseTree = this.findBestParse(sentence.trim());

        sentParseTimeSec = (System.currentTimeMillis() - sentStartTimeMS) / 1000.0;
        totalParseTimeSec += sentParseTimeSec;
        // System.gc(); totalParseMemMB = (Runtime.getRuntime().totalMemory() -
        // Runtime.getRuntime().freeMemory() - sentStartMem) / 1024.0 / 1024.0;

        if (bestParseTree == null) {
            outputStream.write("No parse found.\n");
            insideScore = Float.NEGATIVE_INFINITY;
            // insideProbStr = "-inf";
        } else {
            if (opts.printUnkLabels == false) {
                bestParseTree.replaceLeafNodes(tokens);
            }
            outputStream.write(bestParseTree.toString(opts.printInsideProbs) + "\n");
            insideScore = getInside(0, tokens.length, grammar.startSymbol);
            // insideProbStr = Float.toString(insideScore);
            // printTreeEdgeStats(findChartEdgesForTree(inputTree, (ChartParser)parser), parser);
            // printTreeEdgeStats(bestParseTree, parser);
        }

        totalInsideScore += insideScore;

        // TODO Use Log4J so this output can be suppressed
        final String stats = " sentNum=" + sentenceNumber + " sentLen=" + tokens.length + " md5="
                + StringToMD5.computeMD5(sentence) + " seconds=" + sentParseTimeSec + " mem="
                + sentMaxMemoryMB + " inside=" + insideScore + " " + this.getStats();
        outputStream.write("STAT:" + stats + "\n");
        outputStream.flush();
    }

    public void parseStream(final InputStream inputStream, final OutputStream outputStream) throws Exception {

        final BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        final BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(outputStream));
        for (String sentence = br.readLine(); sentence != null; sentence = br.readLine()) {
            parseSentence(sentence, bw);
        }

        Log.info(1, "INFO: numSentences=" + sentenceNumber + " totalSeconds=" + totalParseTimeSec
                + " avgSecondsPerSent=" + (totalParseTimeSec / sentenceNumber) + " totalInsideScore="
                + totalInsideScore);
    }

    public void printTreeEdgeStats(final ParseTree tree, final Parser<?> parser) {

        assert this instanceof ACPWithMemory;
        assert ((ACPWithMemory) this).edgeSelector instanceof BoundaryInOut;

        for (final ParseTree node : tree.preOrderTraversal()) {
            if (node.isNonTerminal()) {
                throw new RuntimeException("Doesn't work right now");
            }
        }
    }

    public ParseTree findChartEdgesForTree(final ParseTree tree, final ChartParser<?, ?> parser)
            throws Exception {
        final LinkedList<ParseTree> leafNodes = tree.getLeafNodes();
        for (final ParseTree node : tree.postOrderTraversal()) {
            final ParseTree leftLeaf = node.leftMostLeaf();
            final ParseTree rightLeaf = node.rightMostLeaf();
            int start = -1, end = -1, i = 0;
            for (final ParseTree leaf : leafNodes) {
                if (leaf == leftLeaf)
                    start = i;
                if (leaf == rightLeaf)
                    end = i + 1;
                i += 1;
            }

            throw new RuntimeException("Doesn't work right now");

            // if ((end - start >= 1) && node.isNonTerminal()) {
            // final int parentNonTermIndex = parser.grammar.mapNonterminal(node.contents);
            // if (parentNonTermIndex != -1) {
            // final ChartEdge edge = parser.chart.getRootCell().getBestEdge(parentNonTermIndex);
            // if (edge == null) {
            // // System.out.println("WARNING: edge[" + start + "][" + end + "][" + node.contents +
            // "] not in chart!");
            // node.chartEdge = ChartParser.nullEdge.copy();
            // // TODO: I think this will die when it tries to compute the FOM on a null left/right cell
            // } else {
            // node.chartEdge = edge;
            // }
            // } else {
            // Log.info(0, "WARNING: '" + node.contents + "' not in nonTermSet!");
            // }
            // }
        }
        return tree;
    }
}

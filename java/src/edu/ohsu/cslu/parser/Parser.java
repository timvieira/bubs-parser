package edu.ohsu.cslu.parser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.LinkedList;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.parser.edgeselector.BoundaryInOut;
import edu.ohsu.cslu.parser.util.Log;
import edu.ohsu.cslu.parser.util.ParseTree;
import edu.ohsu.cslu.parser.util.ParserUtil;
import edu.ohsu.cslu.parser.util.StringToMD5;

public abstract class Parser {

    public Grammar grammar;
    public static ChartEdge nullEdge = new ChartEdge(Grammar.nullProduction, null, null, Float.NEGATIVE_INFINITY);
    public String currentSentence;
    public ParserOptions opts = null; // TODO: fix this

    public abstract ParseTree findBestParse(String sentence) throws Exception;

    public abstract String getStats();

    public void parseStream(final BufferedReader inputStream, final BufferedWriter outputStream) throws Exception {
        parseStream(inputStream, outputStream, 200, false, false);
    }

    public void parseStream(final BufferedReader inputStream, final BufferedWriter outputStream, final int maxLength) throws Exception {
        parseStream(inputStream, outputStream, maxLength, false, false);
    }

    public void parseStream(final BufferedReader inputStream, final BufferedWriter outputStream, final int maxLength, final boolean printUnkLabels, final boolean printInsideProbs)
            throws Exception {
        ParseTree inputTree = null, bestParseTree = null;
        int sentNum = 0;
        long sentStartTimeMS;
        // final long sentStartMem;
        double sentParseTimeSeconds;
        final double totalParseMemMB = 0.0;
        double totalParseTimeSeconds = 0.0;
        String insideProbStr;

        for (String sentence = inputStream.readLine(); sentence != null; sentence = inputStream.readLine()) {

            // if input are trees, extract sentence from tree
            if (ParseTree.isBracketFormat(sentence)) {
                inputTree = ParseTree.readBracketFormat(sentence);
                sentence = ParserUtil.join(inputTree.getLeafNodesContent(), " ");
            }

            final String[] tokens = ParserUtil.tokenize(sentence);
            if (tokens.length <= maxLength) {
                // System.gc(); sentStartMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                sentStartTimeMS = System.currentTimeMillis();

                bestParseTree = this.findBestParse(sentence.trim());

                // if (parserType == ParserType.CellAgendaCDT) {
                // ((CellAgendaChartParserCDT) parser).runWithResult(sentence.trim(), bestParseTree);
                // }

                sentParseTimeSeconds = (System.currentTimeMillis() - sentStartTimeMS) / 1000.0;
                totalParseTimeSeconds += sentParseTimeSeconds;
                // System.gc(); totalParseMemMB = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() - sentStartMem) / 1024.0 / 1024.0;

                if (bestParseTree == null) {
                    outputStream.write("No parse found.\n");
                    insideProbStr = "-inf";
                } else {
                    if (printUnkLabels == false) {
                        bestParseTree.replaceLeafNodes(tokens);
                    }
                    outputStream.write(bestParseTree.toString(printInsideProbs) + "\n");
                    insideProbStr = Float.toString(bestParseTree.chartEdge.inside);
                    // printTreeEdgeStats(findChartEdgesForTree(inputTree, (ChartParser)parser), parser);
                    // printTreeEdgeStats(bestParseTree, parser);
                }

                final String stats = " sentNum=" + sentNum + " sentLen=" + tokens.length + " md5=" + StringToMD5.computeMD5(sentence) + " seconds=" + sentParseTimeSeconds
                        + " mem=" + totalParseMemMB + " inside=" + insideProbStr + " " + this.getStats();
                outputStream.write("STAT:" + stats + "\n");
                outputStream.flush();
                sentNum++;
            } else {
                Log.info(1, "INFO: Skipping sentence. Length of " + tokens.length + " is greater than maxLength (" + maxLength + ")");
            }

        }

        // TODO: allow gold trees as input and report F-score
        // TODO: need to port python tree transforms / de-transforms to Java
        // and either write our own eval or make external call to EVALB
        Log.info(1, "INFO: numSentences=" + sentNum + " totalSeconds=" + totalParseTimeSeconds + " avgSecondsPerSent=" + (totalParseTimeSeconds / sentNum));
    }

    public void printTreeEdgeStats(final ParseTree tree, final Parser parser) {

        assert this instanceof ACPWithMemory;
        assert ((ACPWithMemory) this).edgeSelector instanceof BoundaryInOut;

        for (final ParseTree node : tree.preOrderTraversal()) {
            if (node.isNonTerminal()) {
                System.out.println("FINAL: " + ((BoundaryInOut) ((AgendaChartParser) parser).edgeSelector).calcFOMToString(node.chartEdge));
            }
        }
    }

    public ParseTree findChartEdgesForTree(final ParseTree tree, final ChartParser parser) throws Exception {
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

            if ((end - start >= 1) && node.isNonTerminal()) {
                if (parser.grammar.nonTermSet.hasSymbol(node.contents)) {
                    final int parentNonTermIndex = parser.grammar.mapNonterminal(node.contents);
                    final ChartEdge edge = parser.chart.getRootCell().getBestEdge(parentNonTermIndex);
                    if (edge == null) {
                        // System.out.println("WARNING: edge[" + start + "][" + end + "][" + node.contents + "] not in chart!");
                        node.chartEdge = ChartParser.nullEdge.copy();
                        // TODO: I think this will die when it tries to compute the FOM on a null left/right cell
                    } else {
                        node.chartEdge = edge;
                    }
                } else {
                    Log.info(0, "WARNING: '" + node.contents + "' not in nonTermSet!");
                }
            }
        }
        return tree;
    }
}

package edu.ohsu.cslu.parser;

import java.io.BufferedWriter;

import edu.ohsu.cslu.datastructs.narytree.StringNaryTree;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.parser.ParserOptions.GrammarFormatType;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.parser.edgeselector.BoundaryInOut;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector;
import edu.ohsu.cslu.parser.util.Log;
import edu.ohsu.cslu.parser.util.ParseTree;
import edu.ohsu.cslu.parser.util.ParserUtil;
import edu.ohsu.cslu.parser.util.StringToMD5;

// TODO: allow gold trees as input and report F-score
// TODO: write our own eval or make external call to EVALB

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

    public void parseSentence(String sentence, final BufferedWriter outputStream,
            final GrammarFormatType grammarFormatType) throws Exception {
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
            final String parse = bestParseTree.toString(opts.printInsideProbs);
            outputStream.write(opts.unfactor() ? unfactor(parse, grammarFormatType) : parse);
            outputStream.write('\n');

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

    public void printTreeEdgeStats(final ParseTree tree, final Parser<?> parser) {

        assert this instanceof ACPWithMemory;
        assert ((ACPWithMemory) this).edgeSelector instanceof BoundaryInOut;

        for (final ParseTree node : tree.preOrderTraversal()) {
            if (node.isNonTerminal()) {
                throw new RuntimeException("Doesn't work right now");
            }
        }
    }

    /**
     * 'Un-factors' a binary-factored parse tree by removing category split labels and flattening
     * binary-factored subtrees.
     * 
     * @param bracketedTree Bracketed string parse tree
     * @param grammarFormatType Grammar format
     * @return Bracketed string representation of the un-factored tree
     */
    public static String unfactor(final String bracketedTree, final GrammarFormatType grammarFormatType) {
        final StringNaryTree factoredTree = StringNaryTree.read(bracketedTree);
        return unfactor(factoredTree, grammarFormatType).toString();
    }

    /**
     * 'Un-factors' a binary-factored parse tree by removing category split labels and flattening
     * binary-factored subtrees.
     * 
     * @param factoredTree Factored tree
     * @param grammarFormatType Grammar format
     * @return Un-factored tree
     */
    public static StringNaryTree unfactor(final StringNaryTree factoredTree,
            final GrammarFormatType grammarFormatType) {

        // Remove split category labels
        final String label = grammarFormatType.unsplitNonTerminal(factoredTree.label());

        final StringNaryTree unfactoredTree = new StringNaryTree(label);

        for (final StringNaryTree factoredChild : factoredTree.children()) {

            if (grammarFormatType.isFactored(factoredChild.label())) {
                // If the child is a factored non-terminal, add each (unfactored) child tree individually to
                // the unfactored tree
                for (final StringNaryTree unfactoredChild : unfactor(factoredChild, grammarFormatType)
                    .children()) {
                    unfactoredTree.addSubtree(unfactor(unfactoredChild, grammarFormatType));
                }
            } else {
                // Otherwise, add unfactor the child and add it as a subtree
                unfactoredTree.addSubtree(unfactor(factoredChild, grammarFormatType));
            }
        }

        return unfactoredTree;
    }
}

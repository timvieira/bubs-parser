package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.parser.ParserOptions.GrammarFormatType;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.parser.edgeselector.BoundaryInOut;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector;
import edu.ohsu.cslu.parser.util.ParseTree;
import edu.ohsu.cslu.parser.util.ParserUtil;

// TODO: allow gold trees as input and report F-score
// TODO: write our own eval or make external call to EVALB

public abstract class Parser<G extends Grammar> {

    public G grammar;
    public ParserOptions opts;
    public EdgeSelector edgeSelector;
    public CellSelector cellSelector;

    // TODO Remove a bunch of these fields once we trim down ParserTrainer (which is currently the only
    // consumer)

    protected int sentenceNumber = 0;
    public String currentSentence;
    protected float totalParseTimeSec = 0;
    protected float totalInsideScore = 0;
    protected long totalMaxMemoryMB = 0;
    public int tokenCount;

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

    public String parseSentence(String sentence, final GrammarFormatType grammarFormatType) throws Exception {
        ParseTree inputTree = null, bestParseTree = null;

        currentSentence = sentence;
        sentenceNumber++;

        // if input are trees, extract sentence from tree
        if (ParseTree.isBracketFormat(sentence)) {
            inputTree = ParseTree.readBracketFormat(sentence);
            sentence = ParserUtil.join(inputTree.getLeafNodesContent(), " ");
        }

        final String[] tokens = ParserUtil.tokenize(sentence);
        if (tokens.length > opts.maxLength) {
            return "INFO: Skipping sentence. Length of " + tokens.length + " is greater than maxLength ("
                    + opts.maxLength + ")";
        }
        tokenCount = tokens.length;

        bestParseTree = this.findBestParse(sentence.trim());

        if (bestParseTree == null) {
            return "No parse found.";
        }

        if (opts.printUnkLabels == false) {
            bestParseTree.replaceLeafNodes(tokens);
        }
        final String parse = bestParseTree.toString(opts.printInsideProbs);

        return opts.unfactor() ? unfactor(parse, grammarFormatType) : parse;
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
        final BinaryTree<String> factoredTree = BinaryTree.read(bracketedTree, String.class);
        return factoredTree.unfactor(grammarFormatType).toString();
    }
}

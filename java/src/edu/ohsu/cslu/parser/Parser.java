package edu.ohsu.cslu.parser;

import org.kohsuke.args4j.EnumAliasMap;

import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.Grammar.GrammarFormatType;
import edu.ohsu.cslu.parser.agenda.ACPWithMemory;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.edgeselector.BoundaryInOut;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector;
import edu.ohsu.cslu.parser.util.ParseTree;
import edu.ohsu.cslu.parser.util.ParserUtil;

// TODO: allow gold trees as input and report F-score
// TODO: write our own eval or make external call to EVALB

public abstract class Parser<G extends Grammar> {

	public G grammar;
	public ParserDriver opts;
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

	public String inputSentence; // should replace currentSentence
	public ParseTree inputTree; // only available when input is given as tree format
	public CellChart inputTreeChart;

	public Parser(final ParserDriver opts, final G grammar) {
		this.grammar = grammar;
		this.opts = opts;

		try {
			edgeSelector = EdgeSelector.create(opts.edgeFOMType, grammar, opts.fomModelStream);
			cellSelector = CellSelector.create(opts.cellSelectorType, opts.cellModelStream, opts.cslutScoresStream);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public abstract float getInside(int start, int end, int nt);

	public abstract float getOutside(int start, int end, int nt);

	public abstract String getStats();

	protected abstract ParseTree findBestParse(String sentence) throws Exception;

	public String parseSentence(final String sentence, final GrammarFormatType grammarFormatType) throws Exception {
		ParseTree bestParseTree = null;
		String parse;
		inputSentence = sentence;
		inputTree = null;

		currentSentence = sentence;
		sentenceNumber++;

		// if input is a tree, extract sentence from tree
		if (ParseTree.isBracketFormat(inputSentence)) {
			inputTree = ParseTree.readBracketFormat(inputSentence);
			inputTreeChart = new CellChart(inputTree, opts.viterbiMax, this);
			inputSentence = ParserUtil.join(inputTree.getLeafNodesContent(), " ");
		}

		final String[] tokens = ParserUtil.tokenize(inputSentence);
		tokenCount = tokens.length;
		if (tokenCount > opts.maxLength) {
			return "INFO: Skipping sentence. Length of " + tokens.length + " is greater than maxLength (" + opts.maxLength + ")";
		}

		bestParseTree = this.findBestParse(inputSentence.trim());

		if (bestParseTree == null)
			return "No parse found.";
		if (!opts.printUnkLabels)
			bestParseTree.replaceLeafNodes(tokens);
		parse = bestParseTree.toString(opts.printInsideProbs);
		if (opts.unfactor)
			parse = unfactor(parse, grammarFormatType);

		return parse;
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
	 * 'Un-factors' a binary-factored parse tree by removing category split labels and flattening binary-factored subtrees.
	 * 
	 * @param bracketedTree
	 *            Bracketed string parse tree
	 * @param grammarFormatType
	 *            Grammar format
	 * @return Bracketed string representation of the un-factored tree
	 */
	public static String unfactor(final String bracketedTree, final GrammarFormatType grammarFormatType) {
		final BinaryTree<String> factoredTree = BinaryTree.read(bracketedTree, String.class);
		return factoredTree.unfactor(grammarFormatType).toString();
	}

	static public enum ParserType {
		ECPCellCrossList("ecpccl"),
		ECPCellCrossHash("ecpcch"),
		ECPCellCrossMatrix("ecpccm"),
		ECPGrammarLoop("ecpgl"),
		ECPGrammarLoopBerkeleyFilter("ecpglbf"),
		ECPInsideOutside("ecpio"),
		AgendaChartParser("acpall"),
		ACPWithMemory("acpwm"),
		ACPGhostEdges("acpge"),
		LocalBestFirst("lbf"),
		LBFPruneViterbi("lbfpv"),
		LBFOnlineBeam("lbfob"),
		LBFBoundedHeap("lbfbh"),
		LBFExpDecay("lbfed"),
		LBFPerceptronCell("lbfpc"),
		CoarseCellAgenda("cc"),
		CoarseCellAgendaCSLUT("cccslut"),
		JsaSparseMatrixVector("jsa"),
		DenseVectorOpenClSparseMatrixVector("dvopencl"),
		PackedOpenClSparseMatrixVector("popencl"),
		CsrSpmv("csr"),
		CsrSpmvPerMidpoint("csrpm"),
		CscSpmv("csc"),
		SortAndScanSpmv("sort-and-scan"),
		LeftChildMatrixLoop("lcml"),
		RightChildMatrixLoop("rcml"),
		GrammarLoopMatrixLoop("glml"),
		CartesianProductBinarySearch("cpbs"),
		CartesianProductBinarySearchLeftChild("cplbs"),
		CartesianProductHash("cph"),
		CartesianProductLeftChildHash("cplch");

		private ParserType(final String... aliases) {
			EnumAliasMap.singleton().addAliases(this, aliases);
		}
	}
}

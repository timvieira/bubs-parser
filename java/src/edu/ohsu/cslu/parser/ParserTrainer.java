package edu.ohsu.cslu.parser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.zip.GZIPInputStream;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import cltool.BaseCommandlineTool;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.Grammar.GrammarFormatType;
import edu.ohsu.cslu.parser.Parser.ParserType;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector.EdgeSelectorType;

public class ParserTrainer extends BaseCommandlineTool {

	@Option(name = "-gp", aliases = { "--grammar-file-prefix" }, metaVar = "prefix", usage = "Grammar file prefix")
	public String grammarPrefix;

	@Option(name = "-gf", aliases = { "--grammar-format" }, metaVar = "format", usage = "Format of grammar file")
	public GrammarFormatType grammarFormat = GrammarFormatType.CSLU;

	@Option(name = "-p", aliases = { "--parser", "--parser-implementation" }, metaVar = "parser", usage = "Parser implementation")
	public ParserType parserType = ParserType.ECPCellCrossList;

	@Option(name = "-fom", aliases = { "--figure-of-merit", "-FOM" }, metaVar = "fom", usage = "Figure of Merit")
	public EdgeSelectorType edgeFOMType = null;

	@Option(name = "-cellSelect", usage = "Train the specified Cell Selection model")
	public boolean cellTrain = false;

	@Option(name = "-cellConstraints", usage = "Train a Cell Constraints model")
	public boolean cellConstraints = false;

	public BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(System.out));
	public BufferedReader inputStream = new BufferedReader(new InputStreamReader(System.in));

	private Grammar grammar;

	public static void main(final String[] args) throws Exception {
		run(args);
	}

	@Override
	public void setup(final CmdLineParser cmdlineParser) throws Exception {

		// Handle prefixes with or without trailing periods.
		String pcfgFileName = grammarPrefix + (grammarPrefix.endsWith(".") ? "" : ".") + "pcfg";
		String lexFileName = grammarPrefix + (grammarPrefix.endsWith(".") ? "" : ".") + "lex";

		// Handle gzipped grammar files
		if (!new File(pcfgFileName).exists() && new File(pcfgFileName + ".gz").exists()) {
			pcfgFileName = pcfgFileName + ".gz";
		}
		if (!new File(lexFileName).exists() && new File(lexFileName + ".gz").exists()) {
			lexFileName = lexFileName + ".gz";
		}

		final Reader pcfgReader = pcfgFileName.endsWith(".gz") ? new InputStreamReader(new GZIPInputStream(new FileInputStream(pcfgFileName))) : new FileReader(pcfgFileName);
		final Reader lexReader = lexFileName.endsWith(".gz") ? new InputStreamReader(new GZIPInputStream(new FileInputStream(lexFileName))) : new FileReader(lexFileName);
		grammar = ParserDriver.createGrammar(parserType, pcfgReader, lexReader, grammarFormat);

	}

	@Override
	public void run() throws Exception {

		if (edgeFOMType != null) {
			// To train a BoundaryInOut FOM model we need a grammar and
			// binarized gold input trees with NTs from same grammar
			final EdgeSelector edgeSelector = EdgeSelector.create(edgeFOMType, grammar, null);
			edgeSelector.train(inputStream);
			edgeSelector.writeModel(outputStream);
		} else if (cellTrain == true) {
			// final PerceptronCellSelector perceptronCellSelector = (PerceptronCellSelector) CellSelector.create(cellSelectorType, cellModelStream, cslutScoresStream);
			// final BSCPPerceptronCellTrainer parser = new BSCPPerceptronCellTrainer(opts, (LeftHashGrammar) grammar);
			// perceptronCellSelector.train(inputStream, parser);
		} else {
			System.out.println("ERROR.");
		}
	}
}

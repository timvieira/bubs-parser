package edu.ohsu.cslu.grammar;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.zip.GZIPInputStream;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import cltool.BaseCommandlineTool;
import edu.ohsu.cslu.grammar.Grammar.GrammarFormatType;

public class SummarizeGrammar extends BaseCommandlineTool {

	@Argument(index = 0, required = true, metaVar = "gp", usage = "Grammar file prefix")
	private String prefix;

	@Option(name = "-gf", aliases = { "--grammar-format" }, metaVar = "format", usage = "Format of grammar file")
	private GrammarFormatType grammarFormat = GrammarFormatType.CSLU;

	public static void main(final String[] args) {
		run(args);
	}

	@Override
	protected void run() throws Exception {

		File pcfg = new File(prefix + ".pcfg");
		File lexicon = new File(prefix + ".lex");
		Reader pcfgReader, lexiconReader;

		if (pcfg.exists()) {
			pcfgReader = new FileReader(pcfg);
			lexiconReader = new FileReader(lexicon);
		} else {
			pcfg = new File(prefix + ".pcfg.gz");
			lexicon = new File(prefix + ".lex.gz");

			if (!pcfg.exists()) {
				throw new FileNotFoundException("Unable to find " + prefix + ".pcfg or " + prefix + ".pcfg.gz");
			}

			pcfgReader = new InputStreamReader(new GZIPInputStream(new FileInputStream(pcfg)));
			lexiconReader = new InputStreamReader(new GZIPInputStream(new FileInputStream(lexicon)));
		}

		final SummaryGrammar grammar = new SummaryGrammar(pcfgReader, lexiconReader, grammarFormat);
		System.out.print(grammar.getStats());
	}

	private class SummaryGrammar extends SortedGrammar {

		private final int V_l, V_r;

		public SummaryGrammar(final Reader grammarFile, final Reader lexiconFile, final GrammarFormatType grammarFormat) throws Exception {
			super(grammarFile, lexiconFile, grammarFormat);

			final IntSet vlSet = new IntOpenHashSet();
			final IntSet vrSet = new IntOpenHashSet();
			for (final Production p : binaryProductions) {
				vlSet.add(p.leftChild);
				vrSet.add(p.rightChild);
			}
			V_l = vlSet.size();
			V_r = vrSet.size();
		}

		@Override
		public String getStats() {
			return super.getStats() + "V_l: " + V_l + '\n' + "V_r: " + V_r + '\n';
		}

	}
}

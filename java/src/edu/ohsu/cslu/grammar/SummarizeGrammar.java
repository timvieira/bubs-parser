package edu.ohsu.cslu.grammar;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import cltool.BaseCommandlineTool;
import edu.ohsu.cslu.parser.ParserOptions.GrammarFormatType;

public class SummarizeGrammar extends BaseCommandlineTool {

    @Option(name = "-c", aliases = { "--class" }, metaVar = "class name", usage = "Grammar class")
    private String grammarClass = Grammar.class.getName();

    // TODO Switch to a mode enum

    @Option(name = "-rm", aliases = { "--recognition-matrix" }, usage = "Print recognition matrix (populated pairs in |V| x |V| matrix)")
    private boolean recognitionMatrix;

    @Option(name = "-rmh", aliases = { "--recognition-matrix-histogram" }, usage = "Print recognition matrix histogram (counts of occurrences in the |V| x |V| recognition matrix)")
    private boolean recognitionMatrixHistogram;

    @Argument(index = 0, required = true, metaVar = "prefix", usage = "Grammar file prefix")
    private String prefix;

    @Option(name = "-gf", aliases = { "--grammar-format" }, metaVar = "format", usage = "Format of grammar file")
    private GrammarFormatType grammarFormat = GrammarFormatType.CSLU;

    public static void main(final String[] args) {
        run(args);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void run() throws Exception {
        if (recognitionMatrixHistogram) {
            grammarClass = CsrSparseMatrixGrammar.class.getName();
        }

        final Class<Grammar> gc = (Class<Grammar>) Class.forName(grammarClass);
        final java.lang.reflect.Constructor<Grammar> c = gc.getConstructor(Reader.class, Reader.class,
            GrammarFormatType.class);

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
                throw new FileNotFoundException("Unable to find " + prefix + ".pcfg or " + prefix
                        + ".pcfg.gz");
            }

            pcfgReader = new InputStreamReader(new GZIPInputStream(new FileInputStream(pcfg)));
            lexiconReader = new InputStreamReader(new GZIPInputStream(new FileInputStream(lexicon)));
        }

        final Grammar g = c.newInstance(pcfgReader, lexiconReader, grammarFormat);

        if (recognitionMatrix) {
            System.out.print(g.recognitionMatrix());
        } else if (recognitionMatrixHistogram) {
            final CsrSparseMatrixGrammar spmg = (CsrSparseMatrixGrammar) g;
            final Int2IntMap map = new Int2IntOpenHashMap();

            for (final int children : spmg.binaryRuleMatrixColumnIndices()) {
                map.put(children, map.get(children) + 1);
            }

            final int[] counts = map.values().toIntArray();
            Arrays.sort(counts);

            int prev = counts[0], prevCount = 0;
            for (int i = 0; i < counts.length; i++) {
                if (counts[i] != prev) {
                    System.out.println(prev + ":" + prevCount);
                    prev = counts[i];
                    prevCount = 0;
                }
                prevCount++;
            }
            System.out.println(prev + ":" + prevCount);
        } else {
            System.out.println(g.getStats());
        }
    }

}

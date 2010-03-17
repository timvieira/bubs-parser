package edu.ohsu.cslu.grammar;

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
import edu.ohsu.cslu.parser.ParserOptions.GrammarFormatType;

public class SummarizeGrammar extends BaseCommandlineTool {

    @Option(name = "-c", aliases = { "--class" }, metaVar = "class name", usage = "Grammar class")
    private String grammarClass = Grammar.class.getName();

    @Argument(index = 0, required = true, metaVar = "prefix", usage = "Grammar file prefix")
    private String prefix;

    public static void main(final String[] args) {
        run(args);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void run() throws Exception {
        final Class<Grammar> gc = (Class<Grammar>) Class.forName(grammarClass);
        final java.lang.reflect.Constructor<Grammar> c = gc.getConstructor(Reader.class, Reader.class, GrammarFormatType.class);

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

        final Grammar g = c.newInstance(pcfgReader, lexiconReader, GrammarFormatType.CSLU);
        System.out.println(g.getStats());
    }

}

package edu.ohsu.cslu.grammar;

import java.io.File;

import cltool4j.BaseCommandlineTool;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.util.Strings;

/**
 * Replaces any unknown words in the input with the appropriate UNK token from the grammar
 * 
 * @author Aaron Dunlop
 */
public class ReplaceUnks extends BaseCommandlineTool {

    @Option(name = "-g", metaVar = "FILE", required = true, usage = "Grammar file (text, gzipped text, or binary serialized)")
    private File grammarFile;

    @Override
    protected void run() throws Exception {
        final Grammar g = new ListGrammar(fileAsBufferedReader(grammarFile));

        for (final String s : inputLines()) {
            final String treebankTokens[] = Tokenizer.treebankTokenize(s).split(" ");
            for (int i = 0; i < treebankTokens.length; i++) {
                treebankTokens[i] = ((DecisionTreeTokenClassifier) g.tokenClassifier).lexiconEntry(treebankTokens[i], i == 0);
            }

            System.out.println(Strings.join(treebankTokens, " "));
        }
    }

    public static void main(final String[] args) {
        run(args);
    }

}

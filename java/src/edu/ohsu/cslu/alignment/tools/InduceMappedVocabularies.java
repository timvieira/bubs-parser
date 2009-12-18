package edu.ohsu.cslu.alignment.tools;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.kohsuke.args4j.Option;

import cltool.BaseCommandlineTool;
import edu.ohsu.cslu.alignment.LogLinearVocabulary;
import edu.ohsu.cslu.alignment.SimpleVocabulary;
import edu.ohsu.cslu.common.Vocabulary;

/**
 * Induces {@link Vocabulary} instances given an input in bracketed format.
 * 
 * The default behavior is to induce a separate vocabulary for each sequential tag - e.g. "(DT The) (NN boy)"
 * would produce 2 vocabularies, one for parts-of-speech and one for words.
 * 
 * Alternatively, if invoked with the '-u' switch, {@link InduceMappedVocabularies} will induce a single
 * logLinear vocabulary covering all tokens encountered. This mode is especially useful for binary
 * (log-linear) modeling.
 * 
 * In either case, the tokens appear in the resulting vocabularies in the same order they occur in the input
 * document.
 * 
 * TODO: Support square-bracketed and slash-delimited input.
 * 
 * @see Vocabulary
 * 
 * @author Aaron Dunlop
 * @since Feb 5, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class InduceMappedVocabularies extends BaseCommandlineTool {

    @Option(name = "-t", metaVar = "tag", usage = "Tag number")
    private int tag = -1;

    @Option(name = "-l", usage = "Log-linear - create a single vocabulary mapping all tokens")
    private boolean logLinear;

    @Option(name = "-rtc", metaVar = "cutoff", usage = "Rare token cutoff. Default = 0")
    private final int rareTokenCutoff = 0;

    public static void main(final String[] args) {
        run(args);
    }

    @Override
    public void run() throws Exception {
        if (logLinear) {
            final LogLinearVocabulary vocabulary = LogLinearVocabulary.induce(new BufferedReader(
                new InputStreamReader(System.in)), rareTokenCutoff);
            System.out.println(vocabulary.toString());
            return;
        }

        // Induce separate vocabularies for each tag in a set of tokens
        final SimpleVocabulary[] vocabularies = SimpleVocabulary.induceVocabularies(new BufferedReader(
            new InputStreamReader(System.in)));

        if (tag >= 0) {
            System.out.println(vocabularies[tag - 1].toString());
        } else {
            for (int i = 0; i < vocabularies.length; i++) {
                System.out.println(vocabularies[i].toString());
            }
        }
    }
}

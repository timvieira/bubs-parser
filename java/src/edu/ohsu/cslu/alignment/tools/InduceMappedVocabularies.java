package edu.ohsu.cslu.alignment.tools;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import edu.ohsu.cslu.alignment.LogLinearVocabulary;
import edu.ohsu.cslu.alignment.SimpleVocabulary;
import edu.ohsu.cslu.common.Vocabulary;
import edu.ohsu.cslu.common.tools.BaseCommandlineTool;

/**
 * Induces {@link Vocabulary} instances given an input in bracketed format.
 * 
 * The default behavior is to induce a separate vocabulary for each sequential tag - e.g.
 * "(DT The) (NN boy)" would produce 2 vocabularies, one for parts-of-speech and one for words.
 * 
 * Alternatively, if invoked with the '-u' switch, {@link InduceMappedVocabularies} will induce a
 * single logLinear vocabulary covering all tokens encountered. This mode is especially useful for
 * binary (log-linear) modeling.
 * 
 * In either case, the tokens appear in the resulting vocabularies in the same order they occur in
 * the input document.
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
public class InduceMappedVocabularies extends BaseCommandlineTool
{
    private int tag = -1;
    private boolean logLinear;
    private int rareTokenCutoff;

    public static void main(String[] args)
    {
        run(args);
    }

    @Override
    public void execute() throws Exception
    {
        if (logLinear)
        {
            LogLinearVocabulary vocabulary = LogLinearVocabulary.induce(new BufferedReader(new InputStreamReader(
                System.in)));
            System.out.println(vocabulary.toString());
            return;
        }

        // Induce separate vocabularies for each tag in a set of tokens
        SimpleVocabulary[] vocabularies = SimpleVocabulary.induceVocabularies(new BufferedReader(new InputStreamReader(
            System.in)));

        if (tag > 0)
        {
            System.out.println(vocabularies[tag - 1].toString());
        }
        else
        {
            for (int i = 0; i < vocabularies.length; i++)
            {
                System.out.println(vocabularies[i].toString());
            }
        }
    }

    @Override
    @SuppressWarnings("static-access")
    protected Options options() throws Exception
    {
        Options options = basicOptions();

        options.addOption(OptionBuilder.hasArg().withArgName("tag").withDescription("tag number (default 1)").create(
            't'));
        options.addOption(OptionBuilder.withDescription("Log-Linear - create a single vocabulary mapping all tokens")
            .create('l'));
        options.addOption(OptionBuilder.hasArg().withArgName("cutoff").withDescription("Rare token cutoff (default 0)")
            .create("rtc"));

        return options;
    }

    @Override
    public void setToolOptions(CommandLine commandLine)
    {
        tag = Integer.parseInt(commandLine.getOptionValue('t', "-1"));
        logLinear = commandLine.hasOption('l');
        rareTokenCutoff = Integer.parseInt(commandLine.getOptionValue("rtc", "0"));
    }

    @Override
    protected String usageArguments() throws Exception
    {
        return "[filename]";
    }

}

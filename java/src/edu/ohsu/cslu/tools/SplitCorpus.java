package edu.ohsu.cslu.tools;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import edu.ohsu.cslu.common.tools.BaseCommandlineTool;

/**
 * Splits a line-based corpus into training and development (or training and test) sets, writing one
 * to STDOUT and the other to STDERR.
 * 
 * @author Aaron Dunlop
 * @since Oct 22, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class SplitCorpus extends BaseCommandlineTool
{
    private int trainingSetSize;
    private int developmentSetSize;
    private boolean percentage = false;

    @Override
    public void execute() throws Exception
    {
        final ArrayList<String> sentences = new ArrayList<String>();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        for (String line = reader.readLine(); line != null; line = reader.readLine())
        {
            sentences.add(line);
        }
        reader.close();

        // Shuffle sentences randomly
        Collections.shuffle(sentences);

        if (percentage)
        {
            trainingSetSize = (int) (sentences.size() * trainingSetSize / 100f);
        }

        // Print out training set
        for (int i = 0; i < trainingSetSize; i++)
        {
            System.out.println(sentences.get(i));
        }

        // Print out development set
        for (int i = trainingSetSize; i < sentences.size(); i++)
        {
            System.err.println(sentences.get(i));
        }
    }

    @Override
    @SuppressWarnings("static-access")
    protected Options options() throws Exception
    {
        Options options = new Options();

        options.addOption(OptionBuilder.hasArg().withArgName("size").withDescription("Training set size (count or %)")
            .create('t'));
        options.addOption(OptionBuilder.hasArg().withArgName("size").withDescription(
            "Development set size (count or %)").create('d'));

        return options;
    }

    @Override
    public void setToolOptions(CommandLine commandLine) throws ParseException
    {
        if (commandLine.hasOption('t'))
        {
            String arg = commandLine.getOptionValue('t');
            if (arg.indexOf('%') >= 0)
            {
                percentage = true;
                trainingSetSize = Integer.parseInt(arg.substring(0, arg.indexOf('%')));
            }
            else
            {
                trainingSetSize = Integer.parseInt(arg);
            }
        }

        if (commandLine.hasOption('d'))
        {
            String arg = commandLine.getOptionValue('d');
            if (arg.indexOf('%') >= 0)
            {
                percentage = true;
                developmentSetSize = Integer.parseInt(arg.substring(0, arg.indexOf('%')));
            }
            else
            {
                developmentSetSize = Integer.parseInt(arg);
            }
        }

        if (percentage)
        {
            if (trainingSetSize != 0 && developmentSetSize == 0)
            {
                developmentSetSize = 100 - trainingSetSize;
            }
            else if (developmentSetSize != 0 && trainingSetSize == 0)
            {
                trainingSetSize = 100 - developmentSetSize;
            }
            else if ((trainingSetSize + developmentSetSize) != 100)
            {
                throw new ParseException("Training and Development percentages must sum to 100%");
            }
        }
    }

    @Override
    protected String usageArguments() throws Exception
    {
        return "[filenames]";
    }

    public static void main(String[] args)
    {
        run(args);
    }
}

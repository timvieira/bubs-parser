package edu.ohsu.cslu.tools;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import cltool.BaseCommandlineTool;

/**
 * Splits a line-based corpus into training and development (or training and test) sets, writing one
 * to STDOUT and the other to STDERR.
 * 
 * The relative size of each set is configured on the command-line.
 * 
 * Lines are permuted randomly and output in that permuted order.
 * 
 * @author Aaron Dunlop
 * @since Oct 22, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class SplitCorpus extends BaseCommandlineTool
{
    // TODO Convert parameter parsing back to allow '%' on the end of the cmdline
    @Option(name = "-ts", aliases = {"--training-set-size"}, metaVar = "size", usage = "Training set size")
    private int trainingSetSize;

    @Option(name = "-ds", aliases = {"--devset-size", "--development-set-size"}, metaVar = "size", usage = "Development set size")
    private int developmentSetSize;

    @Option(name = "-p", aliases = {"--percentage"}, usage = "Treat size(s) as percentages")
    private final boolean percentage = false;

    @Override
    public void run() throws Exception
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
    public void setup(CmdLineParser parser) throws CmdLineException
    {
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
                throw new CmdLineException(parser, "Training and Development percentages must sum to 100%");
            }
        }
    }

    public static void main(String[] args)
    {
        run(args);
    }
}

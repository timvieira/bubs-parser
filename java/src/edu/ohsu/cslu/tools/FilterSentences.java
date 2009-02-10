package edu.ohsu.cslu.tools;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;


import edu.ohsu.cslu.common.tools.BaseCommandlineTool;
import edu.ohsu.cslu.parsing.trees.StringNaryTree;

/**
 * Selects sentences out of a corpus, filtering by the supplied criteria.
 * 
 * @author Aaron Dunlop
 * @since Sep 29, 2008
 * 
 *        $Id$
 */
public class FilterSentences extends BaseCommandlineTool
{
    private int minLength;
    private int maxLength;
    private int count;
    private FileType fileType;
    List<String> filenames;

    @Override
    public void execute() throws Exception
    {
        final ArrayList<String> sentences = new ArrayList<String>();
        for (String filename : filenames)
        {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(fileAsInputStream(filename)));
            for (String line = reader.readLine(); line != null; line = reader.readLine())
            {
                switch (fileType)
                {
                    case Simple :
                        System.err.println("Not Implemented");
                        break;

                    case Parsed :
                        StringNaryTree parseTree = StringNaryTree.read(line);
                        // Skip sentences which do not meet the size criteria
                        if (parseTree.leaves() < minLength || parseTree.leaves() > maxLength)
                        {
                            continue;
                        }

                        sentences.add(parseTree.toString());
                        break;

                    case Tagged :
                        System.err.println("Not Implemented");
                        break;
                }
            }
            reader.close();
        }

        if (count == Integer.MAX_VALUE)
        {
            count = sentences.size();
        }
        else
        {
            // Shuffle sentences randomly
            Collections.shuffle(sentences);
        }

        for (int i = 0; i < count; i++)
        {
            System.out.println(sentences.get(i));
        }
    }

    @Override
    @SuppressWarnings("static-access")
    protected Options options() throws Exception
    {
        Options options = basicOptions();

        options.addOption(OptionBuilder.hasArg().withArgName("type").withDescription(
            "File Type (parsed, tagged, simple). Default simple").create('t'));
        options.addOption(OptionBuilder.hasArg().withArgName("length").withDescription("Minimum length").create("ml"));
        options.addOption(OptionBuilder.hasArg().withArgName("length").withDescription("Maximum length").create("xl"));
        options.addOption(OptionBuilder.hasArg().withArgName("count").withDescription("Number of sentences")
            .create('c'));

        return options;
    }

    @Override
    public void setToolOptions(CommandLine commandLine)
    {
        fileType = FileType.forString(commandLine.getOptionValue('t'));
        minLength = commandLine.hasOption("ml") ? Integer.parseInt(commandLine.getOptionValue("ml")) : 0;
        maxLength = commandLine.hasOption("xl") ? Integer.parseInt(commandLine.getOptionValue("xl"))
            : Integer.MAX_VALUE;
        count = commandLine.hasOption('c') ? Integer.parseInt(commandLine.getOptionValue('c')) : Integer.MAX_VALUE;

        filenames = Arrays.asList(commandLine.getArgs());
    }

    @Override
    protected String usageArguments() throws Exception
    {
        return "[filenames]";
    }

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        run(args);
    }

    private enum FileType {
        Parsed, Tagged, Simple;

        public static FileType forString(String s)
        {
            if ("parsed".equalsIgnoreCase(s))
            {
                return Parsed;
            }

            if ("tagged".equalsIgnoreCase(s))
            {
                return Tagged;
            }

            return Simple;
        }
    }
}

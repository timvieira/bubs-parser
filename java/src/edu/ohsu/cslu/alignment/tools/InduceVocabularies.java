package edu.ohsu.cslu.alignment.tools;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;


import edu.ohsu.cslu.alignment.SimpleVocabulary;
import edu.ohsu.cslu.common.tools.BaseCommandlineTool;

/**
 * Induces vocabularies given an input in bracketed format. The tokens appear in the resulting
 * vocabularies in the same order they occur in the input document.
 * 
 * TODO: Support square-bracketed and slash-delimited input.
 * 
 * @author Aaron Dunlop
 * @since Feb 5, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class InduceVocabularies extends BaseCommandlineTool
{
    private int tag = -1;

    public static void main(String[] args)
    {
        run(args);
    }

    @Override
    public void execute() throws Exception
    {
        SimpleVocabulary[] vocabularies = SimpleVocabulary.induceVocabularies(new BufferedReader(new InputStreamReader(
            System.in)));

        if (tag >= 0)
        {
            System.out.println(vocabularies[tag].toString());
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

        return options;
    }

    @Override
    public void setToolOptions(CommandLine commandLine)
    {
        tag = commandLine.hasOption('t') ? Integer.parseInt(commandLine.getOptionValue('t')) - 1 : 0;
    }

    @Override
    protected String usageArguments() throws Exception
    {
        return "[filename]";
    }

}

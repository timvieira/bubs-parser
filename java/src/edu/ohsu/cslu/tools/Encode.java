package edu.ohsu.cslu.tools;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import edu.ohsu.cslu.common.tools.LinewiseCommandlineTool;

/**
 * Encodes a file into a specified content encoding
 * 
 * @author aarond
 * @since Jun 11, 2008
 * 
 * @version $Revision$
 */
public class Encode extends LinewiseCommandlineTool
{
    public static void main(String[] args)
    {
        run(args);
    }

    @Override
    @SuppressWarnings("static-access")
    protected Options options() throws Exception
    {
        Options options = new Options();

        options.addOption(OptionBuilder.isRequired().hasArg().withArgName("encoding").withDescription(
            "Encoding (us-ascii, utf-8, utf-16, etc)").create('e'));

        return options;
    }

    @Override
    public void setToolOptions(CommandLine commandLine) throws ParseException
    {
        String encoding = commandLine.getOptionValue('e').toUpperCase();
        try
        {
            System.setOut(new PrintStream(System.out, true, encoding));
        }
        catch (UnsupportedEncodingException e)
        {
            throw new ParseException("Unknown encoding: " + encoding);
        }
    }

    @Override
    protected String usageArguments() throws Exception
    {
        return "[filenames]";
    }

    @Override
    protected LineTask lineTask(String line)
    {
        return new LineTask(line)
        {
            @Override
            public String call()
            {
                return line;
            }
        };
    }
}

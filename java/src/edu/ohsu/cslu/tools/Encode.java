package edu.ohsu.cslu.tools;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.Callable;

import org.apache.commons.cli.ParseException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import cltool.LinewiseCommandlineTool;

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
    @Option(name = "-e", aliases = {"--encoding"}, metaVar = "encoding", usage = "Encoding (us-ascii | utf-8 | utf-16 | ...) Default = utf-8")
    private final String encoding = "utf-8";

    public static void main(String[] args)
    {
        run(args);
    }

    @Override
    public void setup(CmdLineParser parser) throws ParseException
    {
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
    protected Callable<String> lineTask(final String line)
    {
        return new Callable<String>()
        {
            @Override
            public String call()
            {
                return line;
            }
        };
    }
}

package edu.ohsu.cslu.common.tools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;

import edu.ohsu.cslu.tests.SharedNlpTests;

/**
 * Base class for unit tests of command-line tools.
 * 
 * @author Aaron Dunlop
 * @since Mar 16, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public abstract class ToolTestCase
{
    /**
     * Executes the tool with the given arguments, returning the tool output as a String.
     * 
     * @param args Command-line
     * @param input Standard Input
     * @return Tool output
     * @throws Exception if something bad happens
     */
    protected String executeTool(String args, String input) throws Exception
    {
        return executeTool(args, new ByteArrayInputStream(input.getBytes()));
    }

    /**
     * Executes the tool with the given arguments, returning the tool output as a String.
     * 
     * @param args Command-line
     * @param inputFilename File from unit-test-data directory to use as tool input.
     * @return Tool output
     * @throws Exception if something bad happens
     */
    protected String executeToolFromFile(String args, String inputFilename) throws Exception
    {
        return executeTool(args, SharedNlpTests.unitTestDataAsStream(inputFilename));
    }

    /**
     * Executes the tool with the given arguments, using the specified InputStream as input.
     * 
     * @param args Command-line
     * @param inputFilename File from unit-test-data directory to use as tool input.
     * @return Tool output
     * @throws Exception if something bad happens
     */
    private String executeTool(String args, InputStream input) throws Exception
    {
        BaseCommandlineTool tool = tool();
        String[] splitArgs = args.split(" ");
        CommandLine commandLine = new GnuParser().parse(tool.options(), splitArgs);
        tool.setBasicToolOptions(commandLine);
        tool.setToolOptions(commandLine);

        InputStream systemIn = System.in;
        ByteArrayOutputStream bos = new ByteArrayOutputStream(8192);
        try
        {
            System.setIn(input);
            System.setOut(new PrintStream(bos));
            tool.execute();
        }
        finally
        {
            // Restore System.in to its original value
            System.setIn(systemIn);
        }
        String output = new String(bos.toByteArray());

        // Just to avoid cross-platform issues, we'll replace all forms of newline with '\n'
        return output.replaceAll("\r\n|\r", "\n");
    }

    /**
     * @return an instance of the command-line tool to be tested.
     */
    protected abstract BaseCommandlineTool tool();

}

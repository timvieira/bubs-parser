package edu.ohsu.cslu.common.tools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;

public class ToolTestCase
{
    protected String executeTool(BaseCommandlineTool tool, String args, String input) throws Exception
    {
        String[] splitArgs = args.split(" ");
        CommandLine commandLine = new GnuParser().parse(tool.options(), splitArgs);
        tool.setBasicToolOptions(commandLine);
        tool.setToolOptions(commandLine);

        ByteArrayOutputStream bos = new ByteArrayOutputStream(8192);
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        System.setOut(new PrintStream(bos));
        tool.execute();
        return new String(bos.toByteArray());
    }

}

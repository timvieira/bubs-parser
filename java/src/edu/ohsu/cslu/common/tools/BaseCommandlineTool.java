package edu.ohsu.cslu.common.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Base class for any tools which should be executable from the command-line. This class implements
 * the majority of the functionality needed to execute java code as a 'standard' command-line tool,
 * including parsing command-line options and reading input from either STDIN or from multiple files
 * specified on the command-line.
 * 
 * Unfortunately, it doesn't appear possible to determine the actual class being executed within
 * <code>main(String[])</code>, so each subclass must implement a <code>main(String[])</code> method
 * and call {@link BaseCommandlineTool#run(String[])} from within it.
 * 
 * In addition, subclasses should include a no-argument constructor and the abstract methods
 * declared here in the superclass.
 * 
 * 
 * @author Aaron Dunlop
 * @since Aug 14, 2008
 * 
 *        $Id$
 */
public abstract class BaseCommandlineTool
{
    protected boolean verbose;

    /** Non-threadable tools use a single thread */
    protected int maxThreads = 1;

    protected List<String> dataFiles;

    /**
     * Runs the tool, reading input from System.in
     */
    public abstract void execute() throws Exception;

    /**
     * Sets up the tool based on the specified command-line options
     * 
     * @param commandLine
     */
    public abstract void setToolOptions(CommandLine commandLine) throws ParseException;

    /**
     * Returns a populated 'Options' object. This method will generally call 'basicOptions()'.
     * 
     * @return A populated Options
     */
    protected abstract Options options() throws Exception;

    /**
     * Returns a string denoting the arguments (if any) required when executing this tool. This
     * information will be included when displaying usage information to the user. e.g. "[files]"
     * 
     * @return Program arguments other than those defined in options().
     */
    protected abstract String usageArguments() throws Exception;

    /**
     * Generally called from options() in subclasses.
     * 
     * TODO: Refactor so this functionality is called from {@link #run(String[])} and controlled by
     * attributes?
     * 
     * @return {@link Options} shared by all subclasses
     * @throws Exception
     */
    @SuppressWarnings("static-access")
    protected Options basicOptions() throws Exception
    {
        // Create an instance of the Options class specifying options shared by all tools
        Options options = new Options();
        options.addOption(OptionBuilder.withDescription("Verbose").create('v'));
        if (getClass().getAnnotation(Threadable.class) != null)
        {
            options.addOption(OptionBuilder.hasArg().withArgName("threads").withDescription("Maximum Threads").create(
                "xt"));
        }
        return options;
    }

    /**
     * Configures the tool with the options created in {@link #basicOptions()} common to all
     * command-line tools.
     * 
     * @param commandLine
     */
    @SuppressWarnings("unchecked")
    protected void setBasicToolOptions(CommandLine commandLine)
    {
        // TODO: Add another annotation for 'verbosable' ?
        verbose = commandLine.hasOption('v');

        // If this tool is threadable, default the thread pool size to the number of CPUs on
        // the machine
        if (getClass().getAnnotation(Threadable.class) != null)
        {
            maxThreads = commandLine.hasOption("xt") ? Integer.parseInt(commandLine.getOptionValue("xt")) : Runtime
                .getRuntime().availableProcessors();
        }
        else
        {
            maxThreads = 1;
        }

        dataFiles = commandLine.getArgList();
    }

    /**
     * This method should be overridden by tools which do <i>not</i> want command-line arguments
     * treated as input files.
     * 
     * TODO: Would this option be better handled as an annotation?
     * 
     * @return true if this tool should treat command-line arguments as input data files.
     */
    protected boolean handleArgsAsInput()
    {
        return true;
    }

    /**
     * Parses command-line arguments and executes the tool. This method should be called from within
     * the main() methods of all subclasses.
     * 
     * @param args
     */
    public final static void run(String[] args)
    {
        try
        {
            BaseCommandlineTool tool = (BaseCommandlineTool) Class.forName(
                Thread.currentThread().getStackTrace()[2].getClassName()).getConstructor(new Class[] {}).newInstance(
                new Object[] {});

            Options options = tool.options();
            try
            {
                CommandLine commandLine = new GnuParser().parse(options, args);
                tool.setBasicToolOptions(commandLine);
                tool.setToolOptions(commandLine);

                if (tool.dataFiles.size() > 0 && tool.handleArgsAsInput())
                {
                    // Handle one or more input files from the command-line, translating gzipped
                    // files as appropriate.
                    // TODO: Some tools will prefer all files treated as a single input stream
                    for (String filename : commandLine.getArgs())
                    {
                        InputStream is = fileAsInputStream(filename);
                        System.setIn(is);
                        tool.execute();
                        is.close();
                    }
                }
                else
                {
                    // Handle input on STDIN
                    tool.execute();
                }
            }
            catch (ParseException e)
            {
                if (e.getMessage() != null)
                {
                    System.err.println(e.getMessage());
                }
                String classname = tool.getClass().getName();
                classname = classname.substring(classname.lastIndexOf('.') + 1);
                new HelpFormatter().printHelp(classname + " [options] " + tool.usageArguments(), options);
            }
        }
        catch (IllegalArgumentException e)
        {
            System.err.println(e.getMessage());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Open the specified file, uncompressing GZIP'd files as appropriate
     * 
     * @param filename
     * @return InputStream
     * @throws IOException
     */
    protected static InputStream fileAsInputStream(String filename) throws IOException
    {
        File f = new File(filename);
        if (!f.exists())
        {
            System.err.println("Unable to find file: " + filename);
            System.exit(-1);
        }

        InputStream is = new FileInputStream(filename);
        if (filename.endsWith(".gz"))
        {
            is = new GZIPInputStream(is);
        }
        return is;
    }

    /**
     * Open the specified file, uncompressing GZIP'd files as appropriate
     * 
     * @param filename
     * @return Reader
     * @throws IOException
     */
    protected static Reader fileAsReader(String filename) throws IOException
    {
        return new InputStreamReader(fileAsInputStream(filename));
    }

    /**
     * Returns the contents of the specified file
     * 
     * @param filename
     * @return Contents of the specified file
     * @throws IOException
     */
    protected static String fileAsString(String filename) throws IOException
    {
        BufferedReader br = new BufferedReader(fileAsReader(filename));
        StringBuilder sb = new StringBuilder(1024);
        for (String line = br.readLine(); line != null; line = br.readLine())
        {
            sb.append(line);
            sb.append('\n');
        }
        return sb.toString();
    }
}

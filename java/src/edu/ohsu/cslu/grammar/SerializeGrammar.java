package edu.ohsu.cslu.grammar;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import cltool.BaseCommandlineTool;

/**
 * Command-line tool to convert textual grammar formats to Java serialized object format.
 * 
 * @author Aaron Dunlop
 * @since Sep 9, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class SerializeGrammar extends BaseCommandlineTool {

    @Argument(index = 0, metaVar = "grammar", usage = "Grammar file (text, gzipped text, or binary serialized")
    private String inputGrammarFile;

    @Option(name = "-ser", required = true, metaVar = "filename", usage = "Serialized output file")
    private String serializedOutputFile;

    public static void main(final String[] args) {
        run(args);
    }

    @Override
    protected void run() throws Exception {
        Reader grammarReader;

        if (inputGrammarFile != null) {
            if (inputGrammarFile.endsWith(".gz")) {
                grammarReader = new InputStreamReader(new GZIPInputStream(new FileInputStream(inputGrammarFile)));
            } else {
                grammarReader = new FileReader(inputGrammarFile);
            }
        } else {
            grammarReader = new InputStreamReader(System.in);
        }

        // Try to create the output stream before we read in the source file. At least it will fail a little sooner if
        // the path is invalid
        OutputStream os = new FileOutputStream(serializedOutputFile);
        if (serializedOutputFile.endsWith(".gz")) {
            os = new GZIPOutputStream(os);
        }
        final ObjectOutputStream oos = new ObjectOutputStream(os);

        final Grammar g = new Grammar(grammarReader);

        logger.info("Writing serialized grammar");
        oos.writeObject(g);
        oos.close();
    }

}

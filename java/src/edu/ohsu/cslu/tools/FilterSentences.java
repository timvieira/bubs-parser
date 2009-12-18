package edu.ohsu.cslu.tools;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;

import org.kohsuke.args4j.Option;

import cltool.BaseCommandlineTool;
import edu.ohsu.cslu.datastructs.narytree.StringNaryTree;

/**
 * Selects sentences out of a corpus, filtering by the supplied criteria.
 * 
 * @author Aaron Dunlop
 * @since Sep 29, 2008
 * 
 *        $Id$
 */
public class FilterSentences extends BaseCommandlineTool {

    @Option(name = "-ml", aliases = { "--minlength" }, metaVar = "length", usage = "Minimum length (words)")
    private int minLength;

    @Option(name = "-xl", aliases = { "--maxlength" }, metaVar = "length", usage = "Maximum length (words)")
    private int maxLength;

    @Option(name = "-c", aliases = { "--count" }, metaVar = "count", usage = "Number of sentences")
    private int count;

    @Option(name = "-i", aliases = { "--input-format" }, metaVar = "format (tree|bracketed|square-bracketed|stanford)", usage = "Input format. Default = bracketed.")
    private final FileFormat inputFormat = FileFormat.Bracketed;

    public static void main(String[] args) {
        run(args);
    }

    @Override
    public void run() throws Exception {
        final ArrayList<String> sentences = new ArrayList<String>();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            switch (inputFormat) {
            case BracketedTree:
                StringNaryTree parseTree = StringNaryTree.read(line);
                // Skip sentences which do not meet the size criteria
                if (parseTree.leaves() < minLength || parseTree.leaves() > maxLength) {
                    continue;
                }

                sentences.add(parseTree.toString());
                break;

            // TODO: Implement other input formats
            default:
                throw new IllegalArgumentException("Unknown input format: " + inputFormat.toString());
            }
        }
        reader.close();

        if (count == Integer.MAX_VALUE) {
            count = sentences.size();
        } else {
            // Shuffle sentences randomly
            Collections.shuffle(sentences);
        }

        for (int i = 0; i < count; i++) {
            System.out.println(sentences.get(i));
        }
    }
}
